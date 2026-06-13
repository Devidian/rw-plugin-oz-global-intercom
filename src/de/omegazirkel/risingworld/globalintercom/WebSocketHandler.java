package de.omegazirkel.risingworld.globalintercom;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.omegazirkel.risingworld.GlobalIntercom;
import de.omegazirkel.risingworld.globalintercom.entities.GlobalIntercomPlayer;
import de.omegazirkel.risingworld.globalintercom.entities.PlayerJoinChannelMessage;
import de.omegazirkel.risingworld.globalintercom.entities.WSMessage;
import de.omegazirkel.risingworld.tools.Colors;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.OZLogger;
import de.omegazirkel.risingworld.tools.WSClientEndpoint;
import net.risingworld.api.Server;
import net.risingworld.api.objects.Player;

public class WebSocketHandler implements de.omegazirkel.risingworld.tools.WebSocketHandler {
    GlobalIntercom plugin = null;
    static WSClientEndpoint wsc;
    static final Colors c = Colors.getInstance();
    static final PluginSettings s = PluginSettings.getInstance();

    private static I18n t() {
        return I18n.getInstance(GlobalIntercom.name);
    }

    public static OZLogger logger() {
        return GlobalIntercom.logger();
    }

    public WebSocketHandler(GlobalIntercom plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onConnected(WSClientEndpoint wsce) {
        wsc = wsce;
        logger().info("🔌 Websocket Connected");
    }

    @Override
    public void onTextMessage(String message) {
        try {
            IncomingRelayMessage incoming = IncomingRelayMessage.parse(message);
            plugin.dispatchServer(() -> handleIncomingMessage(incoming));
        } catch (RuntimeException ex) {
            logger().warn("Invalid WebSocket message: " + ex.getMessage());
        }
    }

    private void handleIncomingMessage(IncomingRelayMessage incoming) {
        if (incoming instanceof IncomingRelayMessage.Broadcast broadcast) {
            logger().debug("New BC Message <" + broadcast.content() + "> from " + broadcast.playerName());
            plugin.broadcastMessage(broadcast);
        } else if (incoming instanceof IncomingRelayMessage.PlayerEvent playerEvent) {
            handlePlayerMessage(playerEvent);
        }
    }

    private void handlePlayerMessage(IncomingRelayMessage.PlayerEvent wsmsg) {
        String event = wsmsg.event;
        GlobalIntercomPlayer giPlayer = wsmsg.player.toEntity();
        GlobalIntercom.playerMap.put(giPlayer._id, giPlayer);
        Player player = Server.getPlayerByUID(giPlayer.id64);
        if (player == null) {
            logger().debug("Ignoring WebSocket event <" + event + "> for offline player " + giPlayer.id64);
            return;
        }
        String lang = player.getSystemLanguage();

            if (event.contentEquals("directContactMessage")) {
                // Not yet implemented
            }
            // else if (wsm.event.contentEquals("registerPlayer")) {
            // player.sendTextMessage(c.okay + pluginName + ":> " + c.text +
            // t().get("MSG_REGISTERED", lang));
            // } else if (wsm.event.contentEquals("unregisterPlayer")) {
            // player.sendTextMessage(c.okay + pluginName + ":> " + c.text +
            // t().get("MSG_UNREGISTERED", lang));
            // }
            else if (event.contentEquals("playerOnline")) {
                if (!giPlayer.saveSettings && s.joinDefault && !giPlayer.isInChannel(s.defaultChannel)) {
                    PlayerJoinChannelMessage msg = new PlayerJoinChannelMessage(player);
                    msg.channel = s.defaultChannel;
                    transmitMessageWS(player, new WSMessage<>("playerJoinChannel", msg));
                    // event.getPlayer().setAttribute("gi." + defaultChannel, true);
                    // String lang = event.getPlayer().getSystemLanguage();
                }
            } else if (event.contentEquals("playerOffline")) {
                // Currently nothing to do here
            } else if (event.contentEquals("playerOverrideChange")) {
                {
                    boolean newVal = wsmsg.subject.contentEquals("true");
                    String msg = c.okay + plugin.getName() + ":> " + c.text + t().get("MSG_CMD_OVERRIDE_STATE", lang);

                    if (newVal) {
                        msg = msg.replace("PH_STATE", c.okay + t().get("STATE_ON", lang) + c.text);
                    } else {
                        msg = msg.replace("PH_STATE", c.error + t().get("STATE_OFF", lang) + c.text);
                    }

                    player.sendTextMessage(msg);
                }
            } else if (event.contentEquals("playerJoinChannel")) {
                String chName = wsmsg.subject;
                player.sendTextMessage(
                        c.okay + plugin.getName() + ":> " + c.text
                                + t().get("MSG_JOIN", lang).replace("PH_CHANNEL", chName));
            } else if (event.contentEquals("playerLeaveChannel")) {
                String chName = wsmsg.subject;
                player.sendTextMessage(c.warning + plugin.getName() + ":> " + c.text
                        + t().get("MSG_LEAVE", lang).replace("PH_CHANNEL", chName));
            } else if (event.contentEquals("playerCreateChannel")) {
                String chName = wsmsg.subject;
                player.sendTextMessage(
                        c.okay + plugin.getName() + ":> " + c.text
                                + t().get("MSG_CREATE", lang).replace("PH_CHANNEL", chName));
            } else if (event.contentEquals("playerResponseError")) {
                String code = wsmsg.errorCode;
                String baseMessage = c.error + plugin.getName() + ":> " + c.text + t().get(code, lang);
                switch (code) {
                    case "RELAY_CHANNEL_NOTMEMBER":
                        baseMessage = baseMessage.replace("PH_CHANNEL", c.warning + wsmsg.subject + c.text);
                        baseMessage = baseMessage.replace("PH_CMD",
                                c.command + "/" + GlobalIntercom.pluginCMD + " join " + wsmsg.subject + " [password]"
                                        + c.text);
                        break;
                    case "RELAY_UNREGISTER_CHOWNER":
                        // no placeholder
                        break;
                    case "RELAY_JOIN_NOACCESS":
                        baseMessage = baseMessage.replace("PH_CHANNEL", c.warning + wsmsg.subject + c.text);
                        break;
                    case "RELAY_CHANNEL_UNKNOWN":
                        baseMessage = baseMessage.replace("PH_CHANNEL", c.warning + wsmsg.subject + c.text);
                        baseMessage = baseMessage.replace("PH_CMD",
                                c.command + "/" + GlobalIntercom.pluginCMD + " create " + wsmsg.subject + " [password]"
                                        + c.text);
                        break;
                    case "RELAY_LEAVE_OWNER":
                        baseMessage = baseMessage.replace("PH_CHANNEL", c.warning + wsmsg.subject + c.text);
                        baseMessage = baseMessage.replace("PH_CMD",
                                c.command + "/" + GlobalIntercom.pluginCMD + " close " + wsmsg.subject + c.text);
                        break;
                    case "RELAY_CREATE_NOTREGISTERED":
                        // no replacements
                        break;
                    case "RELAY_CREATE_NOGLOBAL":
                        baseMessage = baseMessage.replace("PH_CHANNEL", c.warning + wsmsg.subject + c.text);
                        break;
                    case "RELAY_CREATE_LENGTH":
                        break;
                    case "RELAY_CREATE_EXISTS":
                        baseMessage = baseMessage.replace("PH_CHANNEL", c.warning + wsmsg.subject + c.text);
                        baseMessage = baseMessage.replace("PH_CMD",
                                c.command + "/" + GlobalIntercom.pluginCMD + " join " + wsmsg.subject + c.text);
                        break;
                    case "RELAY_CH_CLOSE_NOTEXISTS":
                        baseMessage = baseMessage.replace("PH_CHANNEL", c.warning + wsmsg.subject + c.text);
                        break;
                    case "RELAY_CH_CLOSE_NOTOWNER":
                        baseMessage = baseMessage.replace("PH_CHANNEL", c.warning + wsmsg.subject + c.text);
                        break;
                    case "RELAY_CH_CLOSED":
                        baseMessage = baseMessage.replace("PH_CHANNEL", c.warning + wsmsg.subject + c.text);
                        break;
                    default:
                        break;
                }
                player.sendTextMessage(baseMessage);
            } else if (event.contentEquals("playerResponseSuccess")) {
                String code = wsmsg.successCode;
                String baseMessage = c.okay + plugin.getName() + ":> " + c.text + t().get(code, lang);
                switch (code) {
                    case "RELAY_SUCCESS_REGISTER":
                        // no placeholder
                        break;
                    case "RELAY_SUCCESS_UNREGISTER":
                        // no placeholder
                        break;
                    case "RELAY_JOIN_SUCCESS":
                        baseMessage = baseMessage.replace("PH_CHANNEL", c.warning + wsmsg.subject + c.text);
                        break;
                    case "RELAY_LEAVE_SUCCESS":
                        baseMessage = baseMessage.replace("PH_CHANNEL", c.warning + wsmsg.subject + c.text);
                        break;
                    case "RELAY_CREATE_SUCCESS":
                        baseMessage = baseMessage.replace("PH_CHANNEL", c.warning + wsmsg.subject + c.text);
                        break;
                    case "RELAY_CH_CLOSE_SUCCESS":
                        baseMessage = baseMessage.replace("PH_CHANNEL", c.warning + wsmsg.subject + c.text);
                        break;
                    default:
                        break;
                }
                player.sendTextMessage(baseMessage);
            } else if (event.contentEquals("playerResponseInfo")) {
                String code = wsmsg.infoCode;
                String baseMessage = c.text + plugin.getName() + ":> " + c.text + t().get(code, lang);
                // switch (code) {

                // default:
                // break;
                // }
                player.sendTextMessage(baseMessage);
            } else {
                logger().warn("Unknown message type <" + event + ">");
            }
    }

    /**
     * todo: check if we can move gsb and gson to elsewhere and dont recreate them
     * everytime
     *
     * this Method converts a WSMessage to JSON and sends it through WS
     */
    public void transmitMessageWS(Player player, WSMessage<?> wsmsg) {
        String lang = player.getSystemLanguage();

        try {
            if (!transmitMessageWS(wsmsg)) {
                player.sendTextMessage(c.error + plugin.getName() + ":> " + c.text + t().get("MSG_WS_OFFLINE", lang));
            }
        } catch (Exception e) {
            player.sendTextMessage(c.error + plugin.getName() + ":>" + c.text + " " + e.getMessage());
            // this.initWebSocketClient();
        }
    }

    public boolean transmitMessageWS(WSMessage<?> wsmsg) {
        if (wsc == null || !wsc.isConnected()) {
            return false;
        }
        Gson gson = new GsonBuilder().create();
        return wsc.send(gson.toJson(wsmsg));
    }

}
