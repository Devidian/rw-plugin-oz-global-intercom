/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.omegazirkel.risingworld;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.imageio.ImageIO;

import de.omegazirkel.risingworld.tools.Colors;
import de.omegazirkel.risingworld.tools.FileChangeListener;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.OZLogger;
import de.omegazirkel.risingworld.tools.WSClientEndpoint;
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
	static final String pluginCMD = "gi";

	public static OZLogger logger() {
		return OZLogger.getInstance("OZ.GlobalIntercom");
	}

	static final Colors c = Colors.getInstance();
	private static I18n t = null;

	// Settings
	// static int logLevel = 0;
	static boolean restartOnUpdate = true;
	static boolean sendPluginWelcome = false;
	static boolean joinDefault = false;
	static URI webSocketURI;
	static String defaultChannel = "global";

	static String colorOther = "<color=#3881f7>";
	static String colorSelf = "<color=#37f7da>";
	static String colorLocal = "<color=#FFFFFF>";

	static boolean allowScreenshots = true;
	static int maxScreenWidth = 1920;
	// END Settings

	static boolean flagRestart = false;

	// WebSocket
	static WSClientEndpoint wsc;
	static WebSocketHandler wsh;

	static final Map<String, GlobalIntercomPlayer> playerMap = new HashMap<String, GlobalIntercomPlayer>();

	@Override
	public void onEnable() {
		t = t != null ? t : new I18n(this);
		registerEventListener(this);

		this.initSettings();

		wsh = new WebSocketHandler(this, t);
		wsc = WSClientEndpoint.getInstance(webSocketURI, wsh);

		logger().info("✅ " + this.getName() + " Plugin is enabled version:" + this.getDescription("version"));
	}

	@Override
	public void onDisable() {
		if (wsc != null) {
			wsc.shutdown();
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
						msg.channel = defaultChannel;
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
						msg.channel = defaultChannel;
					}
					WSMessage<PlayerLeaveChannelMessage> wsmsg = new WSMessage<>("playerLeaveChannel", msg);
					wsh.transmitMessageWS(player, wsmsg);

				}
					break;
				case "info":
					String infoMessage = t.get("CMD_INFO", lang);
					player.sendTextMessage(c.okay + this.getName() + ":> " + infoMessage);
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
					String lastCH = "lokal";
					if (player.hasAttribute("gilastch")) {
						lastCH = (String) player.getAttribute("gilastch");
					}

					String wsStatus = c.error + t.get("STATE_DISCONNECTED", lang);
					if (wsc.isConnected()) {
						wsStatus = c.okay + t.get("STATE_CONNECTED", lang);
					}

					String saveStatus = c.error + t.get("STATE_INACTIVE", lang);
					if (giPlayer != null && giPlayer.saveSettings) {
						saveStatus = c.okay + t.get("STATE_ACTIVE", lang);
					}

					String overrideStatus = "";
					if (giPlayer != null && giPlayer.override) {
						overrideStatus = c.okay + t.get("STATE_ON", lang);
					} else {
						overrideStatus = c.error + t.get("STATE_OFF", lang);
					}
					String playerChannelList = "";
					if (giPlayer != null) {
						playerChannelList = giPlayer.getChannelList();
					}

					String statusMessage = t.get("CMD_STATUS", lang)
							.replace("PH_VERSION", c.okay + this.getDescription("version") + c.text)
							.replace("PH_LANGUAGE",
									colorSelf + player.getLanguage() + " / " + player.getSystemLanguage() + c.text)
							.replace("PH_USEDLANG", colorOther + t.getLanguageUsed(lang) + c.text)
							.replace("PH_LANG_AVAILABLE", c.okay + t.getLanguageAvailable() + c.text)
							.replace("PH_STATE_WS", wsStatus + c.text)
							.replace("PH_STATE_CH", c.command + lastCH + c.text)
							.replace("PH_STATE_SAVE", saveStatus + c.text)
							.replace("PH_STATE_OR", overrideStatus + c.text)
							.replace("PH_CHLIST", c.command + playerChannelList + c.text);

					player.sendTextMessage(c.okay + this.getName() + ":> " + statusMessage);
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
				event.setChatMessage(colorLocal + noColorText.substring(2));
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
				channel = defaultChannel;
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
			event.setChatMessage(colorLocal + noColorText);
			return; // no Global Intercom Chat message
		}

		if (giPlayer == null || !giPlayer.isInChannel(channel)) {
			// The player is not in that channel, return to local
			player.deleteAttribute("gilastch");
			event.setChatMessage(colorLocal + noColorText);
			return; // no Global Intercom Chat message
		}

		event.setCancelled(true);
		ChatMessage cmsg = new ChatMessage(player, chatMessage, channel);

		if (chatMessage.contains("+screen")) {
			if (allowScreenshots == true) {
				int playerResolutionX = player.getScreenResolutionX();
				float sizeFactor = 1.0f;
				if (playerResolutionX > maxScreenWidth) {
					sizeFactor = maxScreenWidth * 1f / playerResolutionX * 1f;
				}

				player.createScreenshot(sizeFactor, 1, !chatMessage.contains("+screennogui"), (BufferedImage bimg) -> {
					final ByteArrayOutputStream os = new ByteArrayOutputStream();
					try {
						ImageIO.write(bimg, "jpg", os);
						cmsg.attachment = Base64.getEncoder().encodeToString(os.toByteArray());
					} catch (Exception e) {
						// throw new UncheckedIOException(ioe);
						logger().fatal("Exception on createScreenshot-> " + e.toString());
						// e.printStackTrace();
					}
					cmsg.chatContent = chatMessage.replace("+screen", "[screenshot.jpg]");
					WSMessage<ChatMessage> wsbcm = new WSMessage<>("broadcastMessage", cmsg);
					wsh.transmitMessageWS(player, wsbcm);
				});
			} else {
				cmsg.chatContent = chatMessage.replace("+screen", "[noimage.jpg]");
				player.sendTextMessage(t.get("MSG_SCREEN_NOTALLOWED", lang));
				WSMessage<ChatMessage> wsbcm = new WSMessage<>("broadcastMessage", cmsg);
				wsh.transmitMessageWS(player, wsbcm);
			}
		} else {
			WSMessage<ChatMessage> wsbcm = new WSMessage<>("broadcastMessage", cmsg);
			wsh.transmitMessageWS(player, wsbcm);
		}
	}

	/**
	 *
	 * @param event
	 */
	@EventMethod
	public void onPlayerSpawn(PlayerSpawnEvent event) {
		Player player = event.getPlayer();
		if (sendPluginWelcome) {
			String lang = player.getSystemLanguage();
			player.sendTextMessage(t.get("MSG_PLUGIN_WELCOME", lang));
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

	/**
	 *
	 */
	private void initSettings() {
		Properties settings = new Properties();
		FileInputStream in;
		try {
			in = new FileInputStream(getPath() + "/settings.properties");
			settings.load(new InputStreamReader(in, "UTF8"));
			in.close();
			// fill global values
			// logLevel = Integer.parseInt(settings.getProperty("logLevel", "1"));
			webSocketURI = new URI(settings.getProperty("webSocketURI", "wss://rw.gi.omega-zirkel.de/ws"));
			defaultChannel = settings.getProperty("defaultChannel", "global");
			joinDefault = settings.getProperty("joinDefault", "true").contentEquals("true");
			colorOther = settings.getProperty("colorOther", "<color=#3881f7>");
			colorSelf = settings.getProperty("colorSelf", "<color=#37f7da>");
			colorLocal = settings.getProperty("colorLocal", "<color=#FFFFFF>");

			sendPluginWelcome = settings.getProperty("sendPluginWelcome", "true").contentEquals("true");

			allowScreenshots = settings.getProperty("allowScreenshots", "true").contentEquals("true");
			maxScreenWidth = Integer.parseInt(settings.getProperty("maxScreenWidth", "1920"));

			// restart settings
			restartOnUpdate = settings.getProperty("restartOnUpdate", "false").contentEquals("true");
			logger().info(this.getName() + " Plugin settings loaded");
		} catch (IOException ex) {
			logger().fatal("IOException@initSettings: " + ex.getMessage());
			// e.printStackTrace();
		} catch (NumberFormatException ex) {
			logger().fatal("NumberFormatException@initSettings: " + ex.getMessage());
		} catch (URISyntaxException ex) {
			logger().fatal("Exception@initSettings: " + ex.getMessage());
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
				return; // Player not initialized with GI
			}
			GlobalIntercomPlayer giPlayer = playerMap.get(player.getUID() + "");
			if (giPlayer != null && giPlayer.isInChannel(cmsg.chatChannel)) {
				String color = colorOther;
				if ((player.getUID() + "").contentEquals(cmsg.playerUID)) {
					color = colorSelf;
				}
				player.sendTextMessage(color + "[" + cmsg.chatChannel.toUpperCase() + "] " + cmsg.playerName + ": "
						+ c.text + cmsg.chatContent);
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
		this.initSettings();
		// updated settings msg to all
		for (Player player : Server.getAllPlayers()) {
			player.sendTextMessage(c.okay + this.getName() + ":> " + c.endTag
					+ t.get("MSG_SETTINGS_UPDATED", player.getSystemLanguage()));
		}
	}

}
