package de.omegazirkel.risingworld.globalintercom;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import de.omegazirkel.risingworld.globalintercom.entities.ChatMessage;
import de.omegazirkel.risingworld.globalintercom.entities.GlobalIntercomPlayer;
import de.omegazirkel.risingworld.globalintercom.entities.WSMessage;

public sealed interface IncomingRelayMessage permits IncomingRelayMessage.Broadcast, IncomingRelayMessage.PlayerEvent {

    String event();

    static IncomingRelayMessage parse(String json) {
        Gson gson = new Gson();
        WSMessage<?> envelope = gson.fromJson(json, WSMessage.class);
        if (envelope == null || envelope.event == null || envelope.event.isBlank()) {
            throw new IllegalArgumentException("Relay message has no event");
        }
        if ("broadcastMessage".equals(envelope.event)) {
            Type type = new TypeToken<WSMessage<ChatMessage>>() {
            }.getType();
            ChatMessage payload = gson.<WSMessage<ChatMessage>>fromJson(json, type).payload;
            if (payload == null) {
                throw new IllegalArgumentException("Broadcast message has no payload");
            }
            return new Broadcast(envelope.event, payload.chatContent, payload.chatChannel, payload.playerName,
                    payload.playerUID, payload.sourceName, payload.sourceIP, payload.sourceVersion, payload.attachment);
        }

        Type type = new TypeToken<WSMessage<GlobalIntercomPlayer>>() {
        }.getType();
        WSMessage<GlobalIntercomPlayer> message = gson.fromJson(json, type);
        if (message.payload == null) {
            throw new IllegalArgumentException("Player event has no payload");
        }
        return new PlayerEvent(envelope.event, new PlayerSnapshot(message.payload), message.subject, message.infoCode,
                message.successCode, message.errorCode);
    }

    record Broadcast(String event, String content, String channel, String playerName, String playerUid,
            String sourceName, String sourceIp, String sourceVersion, String attachment)
            implements IncomingRelayMessage {
        public Broadcast {
            content = safe(content);
            channel = safe(channel);
            playerName = safe(playerName);
            playerUid = safe(playerUid);
            sourceName = safe(sourceName);
            sourceIp = safe(sourceIp);
            sourceVersion = safe(sourceVersion);
            attachment = safe(attachment);
        }
    }

    final class PlayerEvent implements IncomingRelayMessage {
        public final String event;
        public final PlayerSnapshot player;
        public final String subject;
        public final String infoCode;
        public final String successCode;
        public final String errorCode;

        PlayerEvent(String event, PlayerSnapshot player, String subject, String infoCode, String successCode,
                String errorCode) {
            this.event = event;
            this.player = player;
            this.subject = safe(subject);
            this.infoCode = safe(infoCode);
            this.successCode = safe(successCode);
            this.errorCode = safe(errorCode);
        }

        @Override
        public String event() {
            return event;
        }
    }

    record PlayerSnapshot(String id, String id64, String name, boolean saveSettings, List<String> channels,
            boolean online, boolean override) {
        PlayerSnapshot(GlobalIntercomPlayer player) {
            this(safe(player._id), safe(player.id64), safe(player.name), player.saveSettings,
                    player.channels == null ? List.of() : List.copyOf(player.channels), player.online, player.override);
        }

        GlobalIntercomPlayer toEntity() {
            GlobalIntercomPlayer entity = new GlobalIntercomPlayer();
            entity._id = id;
            entity.id64 = id64;
            entity.name = name;
            entity.saveSettings = saveSettings;
            entity.channels = new ArrayList<>(channels);
            entity.online = online;
            entity.override = override;
            return entity;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
