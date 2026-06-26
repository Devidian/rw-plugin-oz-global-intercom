/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.omegazirkel.risingworld;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import de.omegazirkel.risingworld.globalintercom.PluginSettings;
import de.omegazirkel.risingworld.globalintercom.ChatShortcutParser;
import de.omegazirkel.risingworld.globalintercom.IncomingRelayMessage;
import de.omegazirkel.risingworld.globalintercom.WebSocketHandler;
import de.omegazirkel.risingworld.globalintercom.entities.ChatMessage;
import de.omegazirkel.risingworld.globalintercom.entities.GlobalIntercomPlayer;
import de.omegazirkel.risingworld.globalintercom.entities.PlayerCloseChannelMessage;
import de.omegazirkel.risingworld.globalintercom.entities.PlayerCreateChannelMessage;
import de.omegazirkel.risingworld.globalintercom.entities.PlayerJoinChannelMessage;
import de.omegazirkel.risingworld.globalintercom.entities.PlayerLeaveChannelMessage;
import de.omegazirkel.risingworld.globalintercom.entities.PlayerOfflineMessage;
import de.omegazirkel.risingworld.globalintercom.entities.PlayerOnlineMessage;
import de.omegazirkel.risingworld.globalintercom.entities.PlayerOverrideChangeMessage;
import de.omegazirkel.risingworld.globalintercom.entities.PlayerRegisterMessage;
import de.omegazirkel.risingworld.globalintercom.entities.PlayerUnregisterMessage;
import de.omegazirkel.risingworld.globalintercom.entities.WSMessage;
import de.omegazirkel.risingworld.tools.Colors;
import de.omegazirkel.risingworld.tools.FileChangeListener;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.OZLogger;
import de.omegazirkel.risingworld.tools.PlayerSettings;
import de.omegazirkel.risingworld.tools.ServerThreadDispatcher;
import de.omegazirkel.risingworld.tools.WSClientEndpoint;
import de.omegazirkel.risingworld.tools.db.SQLiteConnectionFactory;
import de.omegazirkel.risingworld.tools.ui.AssetManager;
import de.omegazirkel.risingworld.tools.ui.MenuItem;
import de.omegazirkel.risingworld.tools.ui.PluginInfoStatusProviders;
import de.omegazirkel.risingworld.tools.ui.PluginMenuManager;
import de.omegazirkel.risingworld.tools.ui.PluginShortcutVisibility;
import de.omegazirkel.risingworld.tools.settings.PlayerPluginAdminSettings;
import de.omegazirkel.risingworld.tools.ui.PlayerPluginSettingsOverlay;
import de.omegazirkel.risingworld.globalintercom.GlobalIntercomPluginInfoStatusProvider;
import de.omegazirkel.risingworld.globalintercom.ui.GlobalIntercomPlayerPluginSettings;
import net.risingworld.api.Plugin;
import net.risingworld.api.Server;
import net.risingworld.api.events.EventMethod;
import net.risingworld.api.events.Listener;
import net.risingworld.api.events.player.PlayerChatEvent;
import net.risingworld.api.events.player.PlayerCommandEvent;
import net.risingworld.api.events.player.PlayerConnectEvent;
import net.risingworld.api.events.player.PlayerDisconnectEvent;
import net.risingworld.api.events.player.PlayerSpawnEvent;
import net.risingworld.api.objects.Player;

/**
 *
 * @author Maik Laschober
 */
public class GlobalIntercom extends Plugin implements Listener, FileChangeListener {
	public static final String pluginCMD = "gi";

	public static OZLogger logger() {
		return OZLogger.getInstance("OZ.GlobalIntercom");
	}

	private static final Colors c = Colors.getInstance();
	private static I18n t = null;
	private static PluginSettings s = null;
	public static String name;
	public static Connection sqliteCon;
	public static PlayerSettings playerSettings;
	private ServerThreadDispatcher serverThreadDispatcher;

	static boolean flagRestart = false;

	// WebSocket
	static WSClientEndpoint wsc;
	static WebSocketHandler wsh;

	public static final Map<String, GlobalIntercomPlayer> playerMap = new HashMap<String, GlobalIntercomPlayer>();

	@Override
	public void onEnable() {
		name = this.getDescription("name");
		serverThreadDispatcher = new ServerThreadDispatcher(this);
		s = PluginSettings.getInstance(this);
		t = I18n.getInstance(this);
		registerEventListener(this);

		s.initSettings();
		sqliteCon = SQLiteConnectionFactory.open(this);
		playerSettings = new PlayerSettings(sqliteCon);

		wsh = new WebSocketHandler(this);
		connectRelay(true);
		AssetManager.loadIconFromPlugin(this, "icon-ki-global-intercom",
				"/resources/assets/icons/icon-ki-global-intercom.png");
		PlayerPluginSettingsOverlay
				.registerPlayerPluginSettings(new GlobalIntercomPlayerPluginSettings(getDescription("version")));
		PlayerPluginSettingsOverlay.registerPlayerPluginAdminSettings(
				new PlayerPluginAdminSettings(name, getDescription("version"), () -> s.adminSettingsEntries(),
						s::initSettings));
			PluginInfoStatusProviders
					.registerProvider(new GlobalIntercomPluginInfoStatusProvider(this, getDescription("version")));
			PluginShortcutVisibility.register(name, GlobalIntercomPlayerPluginSettings::shortcutVisible);
			PluginMenuManager.registerPluginMenu(new MenuItem(name, AssetManager.getIcon("icon-ki-global-intercom"),
					"Global Intercom", player -> {
						player.hideRadialMenu(true);
						PluginInfoStatusProviders.show(player, name);
					}));

			logger().info("✅ " + this.getName() + " Plugin is enabled version:" + this.getDescription("version"));
	}

	@Override
	public void onDisable() {
		if (serverThreadDispatcher != null) {
			serverThreadDispatcher.close();
		}
		if (name != null) {
			PluginShortcutVisibility.unregister(name);
			PluginInfoStatusProviders.unregisterProvider(name);
		}
		if (wsc != null) {
			wsc.shutdown();
		}
		if (sqliteCon != null) {
			try {
				sqliteCon.close();
			} catch (SQLException ex) {
				logger().warn("Failed to close Global Intercom database connection: " + ex.getMessage());
			}
		}
		logger().warn("❌ " + this.getName() + " disabled.");
	}

	@EventMethod
	public void onPlayerConnect(PlayerConnectEvent event) {
		Player player = event.getPlayer();
		sendPlayerOnlineNotification(player);
	}

	@EventMethod
	public void onPlayerDisconnect(PlayerDisconnectEvent event) {
		Player player = event.getPlayer();
		PlayerOfflineMessage msg = new PlayerOfflineMessage(player);
		WSMessage<PlayerOfflineMessage> wsmsg = new WSMessage<>("playerOffline", msg);

		wsh.transmitMessageWS(player, wsmsg);
	}

	@EventMethod
	public void onPlayerCommand(PlayerCommandEvent event) {
		Player player = event.getPlayer();
		String command = event.getCommand();
		String lang = event.getPlayer().getSystemLanguage();
		GlobalIntercomPlayer giPlayer = playerMap.get(player.getUID() + "");

		String[] cmd = command.split(" ");

		if (cmd[0].equals("/" + pluginCMD)) {
			// Invalid number of arguments (0)
			if (cmd.length < 2) {
				player.sendTextMessage(c.error + this.getName() + ":>" + c.text
						+ t.get("MSG_CMD_ERR_ARGUMENTS", lang).replace("PH_CMD", c.error + command + c.text)
								.replace("PH_COMMAND_HELP", c.command + "/" + pluginCMD + " help\n" + c.text));
				return;
			}
			String option = cmd[1];
			// String channel = defaultChannel;
			switch (option) {
				case "save":
					if (cmd.length > 2) {
						if (cmd[2].toLowerCase().contentEquals("true")) {
							WSMessage<PlayerRegisterMessage> wsmsg = new WSMessage<>("registerPlayer",
									new PlayerRegisterMessage(player));
							wsh.transmitMessageWS(player, wsmsg);
						} else {
							WSMessage<PlayerUnregisterMessage> wsmsg = new WSMessage<>("unregisterPlayer",
									new PlayerUnregisterMessage(player));
							wsh.transmitMessageWS(player, wsmsg);
						}
					} else {
						player.sendTextMessage(c.error + this.getName() + ":>" + c.text
								+ t.get("MSG_CMD_ERR_ARGUMENTS", lang).replace("PH_CMD", c.error + command + c.text)
										.replace("PH_COMMAND_HELP",
												c.command + "/" + pluginCMD + " save true|false\n" + c.text));
					}
					break;
				case "create": {
					if (cmd.length > 2) {
						PlayerCreateChannelMessage msg = new PlayerCreateChannelMessage(player);
						msg.channel = cmd[2].toLowerCase();
						if (cmd.length > 3) {
							msg.password = cmd[3];
						}
						WSMessage<PlayerCreateChannelMessage> wsmsg = new WSMessage<>("playerCreateChannel", msg);
						wsh.transmitMessageWS(player, wsmsg);
					} else {
						player.sendTextMessage(c.error + this.getName() + ":>" + c.text
								+ t.get("MSG_CMD_ERR_ARGUMENTS", lang).replace("PH_CMD", c.error + command + c.text)
										.replace("PH_COMMAND_HELP",
												c.command + "/" + pluginCMD + " create channelname [password]\n"
														+ c.text));
					}
				}
					break;
				case "close": {
					if (cmd.length > 2) {
						PlayerCloseChannelMessage msg = new PlayerCloseChannelMessage(player);
						msg.channel = cmd[2].toLowerCase();
						WSMessage<PlayerCloseChannelMessage> wsmsg = new WSMessage<>("playerCloseChannel", msg);
						wsh.transmitMessageWS(player, wsmsg);
					} else {
						player.sendTextMessage(c.error + this.getName() + ":>" + c.text
								+ t.get("MSG_CMD_ERR_ARGUMENTS", lang).replace("PH_CMD", c.error + command + c.text)
										.replace("PH_COMMAND_HELP",
												c.command + "/" + pluginCMD + " close channelname\n" + c.text));
					}
				}
					break;
				case "join": {
					PlayerJoinChannelMessage msg = new PlayerJoinChannelMessage(player);

					if (cmd.length > 2) {
						msg.channel = cmd[2].toLowerCase();
					} else {
						msg.channel = s.defaultChannel;
					}
					if (cmd.length > 3) {
						msg.password = cmd[3];
					}
					WSMessage<PlayerJoinChannelMessage> wsmsg = new WSMessage<>("playerJoinChannel", msg);
					wsh.transmitMessageWS(player, wsmsg);
				}
					break;
				case "leave": {

					PlayerLeaveChannelMessage msg = new PlayerLeaveChannelMessage(player);
					if (cmd.length > 2) {
						msg.channel = cmd[2].toLowerCase();
					} else {
						msg.channel = s.defaultChannel;
					}
					WSMessage<PlayerLeaveChannelMessage> wsmsg = new WSMessage<>("playerLeaveChannel", msg);
					wsh.transmitMessageWS(player, wsmsg);

				}
					break;
				case "info":
					PluginInfoStatusProviders.show(player, name);
					break;
				case "help":
					String helpMessage = t.get("CMD_HELP", lang)
							.replace("PH_CMD_JOIN",
									c.command + "/" + pluginCMD + " join channelname [password]" + c.text)
							.replace("PH_CMD_LEAVE", c.command + "/" + pluginCMD + " leave channelname" + c.text)
							.replace("PH_CMD_CHAT_DEFAULT", c.command + "#HelloWorld" + c.text)
							.replace("PH_CMD_CHAT_OTHER", c.command + "##other HelloWorld" + c.text)
							.replace("PH_CMD_CHAT_LOCAL", c.command + "#%local HelloWorld" + c.text)
							.replace("PH_CMD_OVERRIDE", c.command + "/" + pluginCMD + " override true|false" + c.text)
							.replace("PH_CMD_HELP", c.command + "/" + pluginCMD + " help" + c.text)
							.replace("PH_CMD_INFO", c.command + "/" + pluginCMD + " info" + c.text)
							.replace("PH_CMD_STATUS", c.command + "/" + pluginCMD + " status" + c.text)
							.replace("PH_CMD_CREATE",
									c.command + "/" + pluginCMD + " create channelname [password]" + c.text)
							.replace("PH_CMD_CLOSE", c.command + "/" + pluginCMD + " close channelname" + c.text)
							.replace("PH_CMD_SAVE", c.command + "/" + pluginCMD + " save true|false" + c.text);
					player.sendTextMessage(c.okay + this.getName() + ":> " + helpMessage);
					break;
				case "status":
					PluginInfoStatusProviders.show(player, name);
					break;
				case "override":
					if (cmd.length > 2) {
						{
							PlayerOverrideChangeMessage msg = new PlayerOverrideChangeMessage(player);
							msg.override = cmd[2].toLowerCase().contentEquals("true");
							WSMessage<PlayerOverrideChangeMessage> wsmsg = new WSMessage<>("playerOverrideChange", msg);
							wsh.transmitMessageWS(player, wsmsg);
						}
					} else {
						String message = c.okay + this.getName() + ":> " + c.text
								+ t.get("MSG_CMD_OVERRIDE_NOTSET", lang)
										.replace("PH_CMD",
												c.command + "/" + pluginCMD + " override [true|false] " + c.text);
						player.sendTextMessage(message);
					}
					break;
				case "reconnect":
					if (!player.isAdmin())
						break;
					connectRelay(true);
					player.sendTextMessage(c.okay + this.getName() + ":> " + c.text
							+ t.get("MSG_RECONNECT", lang).replace("PH_URI", s.webSocketURI.toString()));
					break;
				default:
					player.sendTextMessage(c.error + this.getName() + ":> " + c.text
							+ t.get("MSG_CMD_ERR_UNKNOWN_OPTION", lang).replace("PH_OPTION", option));
					break;
			}
		}
	}

	/**
	 *
	 * @param event
	 * @return
	 */
	public boolean isGIMessage(Player player, String message) {
		// Player player = event.getPlayer();
		// String message = event.getChatMessage();
		String noColorText = message.replaceAll("</?color(?:=#?[A-Fa-f0-9]{6})?>", "");
		GlobalIntercomPlayer giPlayer = playerMap.get(player.getUID() + "");
		boolean override = giPlayer != null && giPlayer.override;
		boolean isValidLastChannel = override && player.hasAttribute("gilastch")
				&& giPlayer.isInChannel((String) player.getAttribute("gilastch"));

		// its a GI message if it starts with # or gilastch is set with override true
		// AND chat doesnt start with #%
		return (noColorText.startsWith("#") || (isValidLastChannel && !noColorText.startsWith("#%")));
	}

	@EventMethod
	public void onPlayerChat(PlayerChatEvent event) {

		Player player = event.getPlayer();
		String message = event.getChatMessage();
		String chatMessage;
		String channel;
		String lang = event.getPlayer().getSystemLanguage();
		// log.out("message: "+message,0);
		String noColorText = message.replaceAll("</?color(?:=#?[A-Fa-f0-9]{6})?>", "");
		// log.out("noColorText: "+noColorText,0);
		GlobalIntercomPlayer giPlayer = playerMap.get(player.getUID() + "");
		if (giPlayer == null) {
			if (noColorText.startsWith("#")) {
				player.sendTextMessage(c.error + this.getName() + ":> " + c.text + t.get("MSG_ERR_GI_INIT", lang));
				event.setCancelled(true);
			}
			return;
		}
		boolean override = giPlayer != null && giPlayer.override;

		// long uid = player.getUID();

		if (noColorText.startsWith("#%")) {
			// reset to local chat
			player.deleteAttribute("gilastch");
			if (noColorText.substring(2).length() > 0) {
				event.setChatMessage(s.colorLocal + noColorText.substring(2));
			} else {
				player.sendTextMessage(
						c.okay + this.getName() + ":>" + c.text + t.get("MSG_INFO_CH_DEFAULT_RESET", lang));
				event.setCancelled(true); // No text, don't proceed
			}
			return;
		} else if (noColorText.startsWith("#")) {
			if (noColorText.startsWith("##")) {
				// this is a message into a special channel
				String[] msgParts = noColorText.substring(2).split(" ", 2);
				channel = msgParts[0].toLowerCase();
				if (msgParts.length > 1) {
					chatMessage = msgParts[1];
				} else {
					chatMessage = "";
				}
			} else {
				channel = s.defaultChannel;
				chatMessage = noColorText.substring(1);
			}
			if (channel.length() > 20) {
				player.sendTextMessage(c.error + this.getName() + ":> " + c.text
						+ t.get("MSG_ERR_CH_LENGTH", lang).replace("PH_CHANNEL", channel));
				event.setCancelled(true); // do not post to local chat
				return;
			} else if (channel.length() < 3) {
				player.sendTextMessage(c.error + this.getName() + ":>" + c.text
						+ t.get("MSG_ERR_CH_LENGTH", lang).replace("PH_CHANNEL", channel));
				event.setCancelled(true); // do not post to local chat
				return;
			} else if (giPlayer == null || !giPlayer.isInChannel(channel)) {
				player.sendTextMessage(c.error + this.getName() + ":>" + c.text
						+ t.get("MSG_ERR_CH_NOMEMBER", lang).replace("PH_CHANNEL", channel) + "\n"
						+ t.get("MSG_INFO_CH_JOIN", lang).replace("PH_CMD_JOIN",
								c.command + "/" + pluginCMD + " join " + channel + c.text));
				event.setCancelled(true); // do not post to local chat
				return;
			}

			// Override default text channel
			if (override) {
				player.setAttribute("gilastch", channel);
			}

		} else if (player.hasAttribute("gilastch") && override) {
			channel = (String) player.getAttribute("gilastch");
			chatMessage = noColorText;
		} else {
			event.setChatMessage(s.colorLocal + noColorText);
			return; // no Global Intercom Chat message
		}

		if (giPlayer == null || !giPlayer.isInChannel(channel)) {
			// The player is not in that channel, return to local
			player.deleteAttribute("gilastch");
			event.setChatMessage(s.colorLocal + noColorText);
			return; // no Global Intercom Chat message
		}

		event.setCancelled(true);
		ChatShortcutParser.Result shortcuts = ChatShortcutParser.parse(chatMessage);
		ChatMessage cmsg = new ChatMessage(player, shortcuts.message(), channel);

		if (shortcuts.hasScreenshot()) {
			if (s.allowScreenshots == true) {
				int playerResolutionX = player.getScreenResolutionX();
				float sizeFactor = 1.0f;
				if (playerResolutionX > s.maxScreenWidth) {
					sizeFactor = s.maxScreenWidth * 1f / playerResolutionX * 1f;
				}

				player.createScreenshot(sizeFactor, 1, !shortcuts.screenshotWithoutGui(), (BufferedImage bimg) -> {
					final ByteArrayOutputStream os = new ByteArrayOutputStream();
					try {
						ImageIO.write(bimg, "jpg", os);
						cmsg.attachment = Base64.getEncoder().encodeToString(os.toByteArray());
					} catch (Exception e) {
						// throw new UncheckedIOException(ioe);
						logger().fatal("Exception on createScreenshot-> " + e.toString());
						// e.printStackTrace();
					}
					WSMessage<ChatMessage> wsbcm = new WSMessage<>("broadcastMessage", cmsg);
					wsh.transmitMessageWS(wsbcm);
				});
			} else {
				player.sendTextMessage(t.get("MSG_SCREEN_NOTALLOWED", lang));
				WSMessage<ChatMessage> wsbcm = new WSMessage<>("broadcastMessage", cmsg);
				wsh.transmitMessageWS(player, wsbcm);
			}
		} else {
			WSMessage<ChatMessage> wsbcm = new WSMessage<>("broadcastMessage", cmsg);
			wsh.transmitMessageWS(player, wsbcm);
		}
	}

	public boolean dispatchServer(Runnable task) {
		return serverThreadDispatcher != null && serverThreadDispatcher.dispatch(task);
	}

	/**
	 *
	 * @param event
	 */
	@EventMethod
	public void onPlayerSpawn(PlayerSpawnEvent event) {
		Player player = event.getPlayer();
		if (s.sendPluginWelcome) {
			String lang = player.getSystemLanguage();
			player.sendTextMessage(t.get("MSG_PLUGIN_WELCOME", lang)
					.replace("PH_PLUGIN_NAME", getDescription("name"))
					.replace("PH_PLUGIN_CMD", pluginCMD)
					.replace("PH_PLUGIN_VERSION", getDescription("version")));
		}

		if (Server.getType() == Server.Type.Singleplayer) {
			sendPlayerOnlineNotification(player);
		}

	}

	/**
	 *
	 * @param player
	 */
	public void sendPlayerOnlineNotification(Player player) {
		PlayerOnlineMessage msg = new PlayerOnlineMessage(player);
		WSMessage<PlayerOnlineMessage> wsmsg = new WSMessage<>("playerOnline", msg);

		wsh.transmitMessageWS(player, wsmsg);
	}

	public void resyncOnlinePlayersWithRelay() {
		for (Player player : Server.getAllPlayers()) {
			sendPlayerOnlineNotification(player);
		}
	}

	/**
	 *
	 * @version 0.8.1
	 * @param cmsg
	 */
	public void broadcastMessage(ChatMessage cmsg) {

		for (Player player : Server.getAllPlayers()) {
			if (!playerMap.containsKey(player.getUID() + "")) {
				continue; // Player not initialized with GI
			}
			GlobalIntercomPlayer giPlayer = playerMap.get(player.getUID() + "");
			if (giPlayer != null && giPlayer.isInChannel(cmsg.chatChannel)) {
				String color = s.colorOther;
				if ((player.getUID() + "").contentEquals(cmsg.playerUID)) {
					color = s.colorSelf;
				}
				player.sendTextMessage(color + "[" + cmsg.chatChannel.toUpperCase() + "] " + cmsg.playerName + ": "
						+ c.text + cmsg.chatContent);
			}
		}

	}

	public void broadcastMessage(IncomingRelayMessage.Broadcast message) {
		for (Player player : Server.getAllPlayers()) {
			if (!playerMap.containsKey(player.getUID() + "")) {
				continue;
			}
			GlobalIntercomPlayer giPlayer = playerMap.get(player.getUID() + "");
			if (giPlayer != null && giPlayer.isInChannel(message.channel())) {
				String color = (player.getUID() + "").contentEquals(message.playerUid()) ? s.colorSelf : s.colorOther;
				player.sendTextMessage(color + "[" + message.channel().toUpperCase() + "] " + message.playerName()
						+ ": " + c.text + message.content());
			}
		}
	}

	/**
	 *
	 * @param i18nIndex
	 * @param playerCount
	 */
	public void broadcastMessage(String i18nIndex, int playerCount) {
		for (Player player : Server.getAllPlayers()) {
			try {
				String lang = player.getSystemLanguage();
				player.sendTextMessage(c.warning + this.getName() + ":> " + c.endTag
						+ t.get(i18nIndex, lang).replace("PH_PLAYERS", playerCount + ""));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static PluginSettings getSettings() {
		return s;
	}

	public static boolean isRelayConnected() {
		return wsc != null && wsc.isConnected();
	}

	public String getPluginVersion() {
		return getDescription("version");
	}

	public String getCommandName() {
		return pluginCMD;
	}

	public String getPlayerLastChannel(Player player) {
		if (player != null && player.hasAttribute("gilastch")) {
			return (String) player.getAttribute("gilastch");
		}
		return "lokal";
	}

	private void connectRelay(boolean reconnect) {
		if (!isRelayConnected() || reconnect) {
			if (isRelayConnected())
				wsc.shutdown();
			wsc = WSClientEndpoint.getInstance(s.webSocketURI, wsh);
			wsc.init();
		}
	}

	public GlobalIntercomPlayer getIntercomPlayer(Player player) {
		return player == null ? null : playerMap.get(player.getUID() + "");
	}

	// All stuff for plugin updates

	@Override
	public void onJarChanged(Path file) {
		File f = new File(getPath());
		if (f.getAbsolutePath().equals(file.toFile().getAbsolutePath())) {
			// Plugin updated msg to all
			for (Player player : Server.getAllPlayers()) {
				player.sendTextMessage(c.okay + this.getName() + ":> " + c.endTag
						+ t.get("MSG_PLUGIN_UPDATED", player.getSystemLanguage()));
			}
		}
	}

	@Override
	public void onSettingsChanged(Path file) {
		s.initSettings(file.toString());
		logger().setLevel(s.logLevel);
		// reconnect websocket
		connectRelay(true);
		// updated settings msg to all
		for (Player player : Server.getAllPlayers()) {
			player.sendTextMessage(c.okay + this.getName() + ":> " + c.endTag
					+ t.get("MSG_SETTINGS_UPDATED", player.getSystemLanguage()));
		}
	}

}
