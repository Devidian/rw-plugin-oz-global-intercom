package de.omegazirkel.risingworld.globalintercom;

import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import de.omegazirkel.risingworld.GlobalIntercom;
import de.omegazirkel.risingworld.globalintercom.entities.ChatMessage;
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
	private static I18n t = I18n.getInstance();


    public static OZLogger logger() {
        return OZLogger.getInstance("OZ.GlobalIntercom.WSC");
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

        // log.out(message, 0);
        WSMessage<?> wsm = new Gson().fromJson(message, WSMessage.class);
        if (wsm.event.contentEquals("broadcastMessage")) {
            Type type = new TypeToken<WSMessage<ChatMessage>>() {
            }.getType();
            // System.out.println(type);
            WSMessage<ChatMessage> wscmsg = new Gson().fromJson(message, type);
            // System.out.println(wscmsg.toString());
            ChatMessage cmsg = wscmsg.payload;
            logger().debug("New BC Message <" + cmsg.chatContent + "> from " + cmsg.playerName);
            plugin.broadcastMessage(cmsg);
        } else {
            Type type = new TypeToken<WSMessage<GlobalIntercomPlayer>>() {
            }.getType();
            WSMessage<GlobalIntercomPlayer> wsmsg = new Gson().fromJson(message, type);
            GlobalIntercomPlayer giPlayer = wsmsg.payload;
            GlobalIntercom.playerMap.put(giPlayer._id, giPlayer);
            Player player = Server.getPlayerByUID(giPlayer.id64);
            String lang = player.getSystemLanguage();

            if (wsm.event.contentEquals("directContactMessage")) {
                // Not yet implemented
            }
            // else if (wsm.event.contentEquals("registerPlayer")) {
            // player.sendTextMessage(c.okay + pluginName + ":> " + c.text +
            // t.get("MSG_REGISTERED", lang));
            // } else if (wsm.event.contentEquals("unregisterPlayer")) {
            // player.sendTextMessage(c.okay + pluginName + ":> " + c.text +
            // t.get("MSG_UNREGISTERED", lang));
            // }
            else if (wsm.event.contentEquals("playerOnline")) {
                if (!giPlayer.saveSettings && s.joinDefault && !giPlayer.isInChannel(s.defaultChannel)) {
                    PlayerJoinChannelMessage msg = new PlayerJoinChannelMessage(player);
                    msg.channel = s.defaultChannel;
                    transmitMessageWS(player, new WSMessage<>("playerJoinChannel", msg));
                    // event.getPlayer().setAttribute("gi." + defaultChannel, true);
                    // String lang = event.getPlayer().getSystemLanguage();
                }
            } else if (wsm.event.contentEquals("playerOffline")) {
                // Currently nothing to do here
            } else if (wsm.event.contentEquals("playerOverrideChange")) {
                {
                    boolean newVal = wsmsg.subject.contentEquals("true");
                    String msg = c.okay + plugin.getName() + ":> " + c.text + t.get("MSG_CMD_OVERRIDE_STATE", lang);

                    if (newVal) {
                        msg = msg.replace("PH_STATE", c.okay + t.get("STATE_ON", lang) + c.text);
                    } else {
                        msg = msg.replace("PH_STATE", c.error + t.get("STATE_OFF", lang) + c.text);
                    }

                    player.sendTextMessage(msg);
                }
            } else if (wsm.event.contentEquals("playerJoinChannel")) {
                String chName = wsmsg.subject;
                player.sendTextMessage(
                        c.okay + plugin.getName() + ":> " + c.text + t.get("MSG_JOIN", lang).replace("PH_CHANNEL", chName));
            } else if (wsm.event.contentEquals("playerLeaveChannel")) {
                String chName = wsmsg.subject;
                player.sendTextMessage(c.warning + plugin.getName() + ":> " + c.text
                        + t.get("MSG_LEAVE", lang).replace("PH_CHANNEL", chName));
            } else if (wsm.event.contentEquals("playerCreateChannel")) {
                String chName = wsmsg.subject;
                player.sendTextMessage(
                        c.okay + plugin.getName() + ":> " + c.text + t.get("MSG_CREATE", lang).replace("PH_CHANNEL", chName));
            } else if (wsm.event.contentEquals("playerResponseError")) {
                String code = wsmsg.errorCode;
                String baseMessage = c.error + plugin.getName() + ":> " + c.text + t.get(code, lang);
                switch (code) {
                    case "RELAY_CHANNEL_NOTMEMBER":
                        baseMessage = baseMessage.replace("PH_CHANNEL", c.warning + wsmsg.subject + c.text);
                        baseMessage = baseMessage.replace("PH_CMD",
                                c.command + "/" + GlobalIntercom.pluginCMD + " join " + wsmsg.subject + " [password]" + c.text);
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
                                c.command + "/" + GlobalIntercom.pluginCMD + " create " + wsmsg.subject + " [password]" + c.text);
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
            } else if (wsm.event.contentEquals("playerResponseSuccess")) {
                String code = wsmsg.successCode;
                String baseMessage = c.okay + plugin.getName() + ":> " + c.text + t.get(code, lang);
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
            } else if (wsm.event.contentEquals("playerResponseInfo")) {
                String code = wsmsg.infoCode;
                String baseMessage = c.text + plugin.getName() + ":> " + c.text + t.get(code, lang);
                // switch (code) {

                // default:
                // break;
                // }
                player.sendTextMessage(baseMessage);
            } else {
                logger().warn("Unknown message type <" + wsm.event + ">");
            }
        }
    }

    /**
     * todo: check if we can move gsb and gson to elsewhere and dont recreate them
     * everytime
     *
     * this Method converts a WSMessage to JSON and sends it through WS
     */
    public void transmitMessageWS(Player player, WSMessage<?> wsmsg) {
        GsonBuilder gsb = new GsonBuilder();
        Gson gson = gsb.create();
        String lang = player.getSystemLanguage();

        try {
            if (wsc!=null && wsc.isConnected()) {
                String msg = gson.toJson(wsmsg);
                wsc.send(msg);
            } else {
                player.sendTextMessage(c.error + plugin.getName() + ":> " + c.text + t.get("MSG_WS_OFFLINE", lang));
            }
        } catch (Exception e) {
            player.sendTextMessage(c.error + plugin.getName() + ":>" + c.text + " " + e.getMessage());
            // this.initWebSocketClient();
        }
    }

}
