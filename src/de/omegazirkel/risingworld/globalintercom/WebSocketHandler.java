package de.omegazirkel.risingworld.globalintercom;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.omegazirkel.risingworld.GlobalIntercom;
import de.omegazirkel.risingworld.globalintercom.entities.GlobalIntercomPlayer;
import de.omegazirkel.risingworld.globalintercom.entities.PlayerJoinChannelMessage;
import de.omegazirkel.risingworld.globalintercom.entities.WSMessage;
import de.omegazirkel.risingworld.tools.Colors;
import de.omegazirkel.risingworld.tools.ExternalDataConsent;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.OZLogger;
import de.omegazirkel.risingworld.tools.WSClientEndpoint;
import de.omegazirkel.risingworld.tools.privacy.ErasureMessageSigner;
import net.risingworld.api.Server;
import net.risingworld.api.objects.Player;

public class WebSocketHandler implements de.omegazirkel.risingworld.tools.WebSocketHandler {
    GlobalIntercom plugin = null;
    static WSClientEndpoint wsc;
    static final Colors c = Colors.getInstance();
    static final PluginSettings s = PluginSettings.getInstance();
    private final Map<String, Boolean> erasureRequests = new ConcurrentHashMap<>();
    private volatile String erasureProbeId;
    private volatile boolean relayErasureSupported;

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
        relayErasureSupported = false;
        logger().info("🔌 Websocket Connected");
        ServerRegistrationMessage registration = new ServerRegistrationMessage();
        String shortName = Server.getOption("Server_ShortName");
        registration.shortName = shortName == null || shortName.isBlank() ? Server.getName() : shortName.trim();
        transmitMessageWS(new WSMessage<>("serverRegister", registration));
        if (hasErasureCredentials()) {
            erasureProbeId = java.util.UUID.randomUUID().toString();
            try {
                sendErasureMessage("erasureProbe", "intercom-erasure-probe", erasureProbeId, "_");
            } catch (java.security.GeneralSecurityException ex) {
                logger().warn("Could not sign Intercom erasure probe");
            }
        }
        plugin.dispatchServer(() -> plugin.resyncOnlinePlayersWithRelay());
    }

    @Override
    public void onTextMessage(String message) {
        try {
            var envelope = JsonParser.parseString(message).getAsJsonObject();
            if (envelope.has("event") && "playerErased".equals(envelope.get("event").getAsString())) {
                erasureRequests.replace(envelope.getAsJsonObject("payload").get("requestId").getAsString(), true);
                return;
            }
            if (envelope.has("event") && "erasureAvailable".equals(envelope.get("event").getAsString())) {
                relayErasureSupported = envelope.getAsJsonObject("payload").get("requestId").getAsString().equals(erasureProbeId);
                return;
            }
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
        GlobalIntercomPlayer previousPlayer = GlobalIntercom.playerMap.get(giPlayer._id);
        GlobalIntercom.playerMap.put(giPlayer._id, giPlayer);
        Player player = Server.getPlayerByUID(giPlayer.id64);
        if (player == null) {
            logger().debug("Ignoring WebSocket event <" + event + "> for offline player " + giPlayer.id64);
            return;
        }
        if (!ExternalDataConsent.isGranted(player, ExternalDataConsent.Purpose.INTERCOM)) {
            GlobalIntercom.playerMap.remove(giPlayer._id);
            return;
        }
        String lang = de.omegazirkel.risingworld.OZTools.getPlayerLanguage(player);

            if (event.contentEquals("directContactMessage")) {
                // Not yet implemented
            }
            else if (event.contentEquals("playerOnline")) {
                if (!giPlayer.saveSettings && s.joinDefault && !giPlayer.isInChannel(s.defaultChannel)) {
                    PlayerJoinChannelMessage msg = new PlayerJoinChannelMessage(player);
                    msg.channel = s.defaultChannel;
                    transmitMessageWS(player, new WSMessage<>("playerJoinChannel", msg));
                    // event.getPlayer().setAttribute("gi." + defaultChannel, true);
                    // String lang = event.getPlayer().getSystemLanguage();
                }
                rejoinPreviousChannels(player, giPlayer, previousPlayer);
            } else if (event.contentEquals("playerOffline")) {
                // Currently nothing to do here
            } else if (event.contentEquals("playerOverrideChange")) {
                {
                    boolean newVal = wsmsg.subject.contentEquals("true");
                    String msg = c.okay + plugin.getName() + ":> " + c.text + t().get("msg.cmd.override.state", lang);

                    if (newVal) {
                        msg = msg.replace("PH_STATE", c.okay + t().get("state.on", lang) + c.text);
                    } else {
                        msg = msg.replace("PH_STATE", c.error + t().get("state.off", lang) + c.text);
                    }

                    player.sendTextMessage(msg);
                }
            } else if (event.contentEquals("playerJoinChannel")) {
                String chName = wsmsg.subject;
                player.sendTextMessage(
                        c.okay + plugin.getName() + ":> " + c.text
                                + t().get("msg.join", lang).replace("PH_CHANNEL", chName));
            } else if (event.contentEquals("playerLeaveChannel")) {
                String chName = wsmsg.subject;
                player.sendTextMessage(c.warning + plugin.getName() + ":> " + c.text
                        + t().get("msg.leave", lang).replace("PH_CHANNEL", chName));
            } else if (event.contentEquals("playerCreateChannel")) {
                String chName = wsmsg.subject;
                player.sendTextMessage(
                        c.okay + plugin.getName() + ":> " + c.text
                                + t().get("relay.create.success", lang).replace("PH_CHANNEL", chName));
            } else if (event.contentEquals("playerResponseError")) {
                String code = wsmsg.errorCode;
                String baseMessage = c.error + plugin.getName() + ":> " + c.text + t().get(RelayTranslationKey.fromCode(code), lang);
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
                String baseMessage = c.okay + plugin.getName() + ":> " + c.text + t().get(RelayTranslationKey.fromCode(code), lang);
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
                String baseMessage = c.text + plugin.getName() + ":> " + c.text + t().get(RelayTranslationKey.fromCode(code), lang);
                // switch (code) {

                // default:
                // break;
                // }
                player.sendTextMessage(baseMessage);
            } else {
                logger().warn("Unknown message type <" + event + ">");
            }
    }

    private void rejoinPreviousChannels(Player player, GlobalIntercomPlayer giPlayer, GlobalIntercomPlayer previousPlayer) {
        if (previousPlayer == null || previousPlayer.channels == null) {
            return;
        }
        for (String channel : previousPlayer.channels) {
            if (channel == null || channel.isBlank() || giPlayer.isInChannel(channel)) {
                continue;
            }
            PlayerJoinChannelMessage msg = new PlayerJoinChannelMessage(player);
            msg.channel = channel;
            transmitMessageWS(player, new WSMessage<>("playerJoinChannel", msg));
        }
    }

    /**
     * todo: check if we can move gsb and gson to elsewhere and dont recreate them
     * everytime
     *
     * this Method converts a WSMessage to JSON and sends it through WS
     */
    public void transmitMessageWS(Player player, WSMessage<?> wsmsg) {
        if (!de.omegazirkel.risingworld.tools.ExternalDataConsent.isGranted(
                player, de.omegazirkel.risingworld.tools.ExternalDataConsent.Purpose.INTERCOM)) return;
        String lang = de.omegazirkel.risingworld.OZTools.getPlayerLanguage(player);

        try {
            if (!transmitMessageWS(wsmsg)) {
                player.sendTextMessage(c.error + plugin.getName() + ":> " + c.text + t().get("msg.ws.offline", lang));
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

    private boolean hasErasureCredentials() {
        String id = System.getenv("INTERCOM_ERASURE_SERVER_ID");
        String key = System.getenv("INTERCOM_ERASURE_KEY");
        return id != null && id.matches("[a-zA-Z0-9._-]{1,80}")
                && key != null && key.matches("[a-f0-9]{64}");
    }

    public boolean erasureReady() {
        return relayErasureSupported && wsc != null && wsc.isConnected() && hasErasureCredentials();
    }

    public boolean erasePlayer(String requestId, String uid) throws java.security.GeneralSecurityException {
        if (erasureRequests.remove(requestId, true)) return true;
        if (!erasureReady()) return false;
        erasureRequests.putIfAbsent(requestId, false);
        transmitMessageWS(new WSMessage<>("playerOffline", Map.of("playerUID", uid)));
        sendErasureMessage("erasePlayer", "intercom-player-erasure", requestId, uid);
        return false;
    }

    private void sendErasureMessage(String event, String domain, String requestId, String uid)
            throws java.security.GeneralSecurityException {
        String serverId = System.getenv("INTERCOM_ERASURE_SERVER_ID");
        long issuedAt = System.currentTimeMillis();
        String signature = ErasureMessageSigner.sign(System.getenv("INTERCOM_ERASURE_KEY"),
                domain, serverId, requestId, uid, Long.toString(issuedAt));
        transmitMessageWS(new WSMessage<>(event, Map.of("serverId", serverId, "uid", uid,
                "requestId", requestId, "issuedAt", issuedAt, "signature", signature)));
    }

}
