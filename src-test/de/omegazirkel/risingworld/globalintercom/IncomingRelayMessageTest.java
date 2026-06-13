package de.omegazirkel.risingworld.globalintercom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class IncomingRelayMessageTest {

    @Test
    public void parsesBroadcastIntoImmutableValues() {
        IncomingRelayMessage incoming = IncomingRelayMessage.parse("""
                {
                  "event": "broadcastMessage",
                  "payload": {
                    "chatContent": "hello",
                    "chatChannel": "global",
                    "playerName": "Maik",
                    "playerUID": "123"
                  }
                }
                """);

        IncomingRelayMessage.Broadcast broadcast = (IncomingRelayMessage.Broadcast) incoming;
        assertEquals("hello", broadcast.content());
        assertEquals("global", broadcast.channel());
        assertEquals("Maik", broadcast.playerName());
        assertEquals("123", broadcast.playerUid());
    }

    @Test
    public void parsesPlayerEventAndCopiesChannels() {
        IncomingRelayMessage incoming = IncomingRelayMessage.parse("""
                {
                  "event": "playerJoinChannel",
                  "subject": "global",
                  "payload": {
                    "_id": "123",
                    "id64": "123",
                    "name": "Maik",
                    "saveSettings": true,
                    "channels": ["global"],
                    "online": true,
                    "override": false
                  }
                }
                """);

        IncomingRelayMessage.PlayerEvent event = (IncomingRelayMessage.PlayerEvent) incoming;
        assertEquals("global", event.subject);
        assertEquals("123", event.player.id());
        assertEquals("global", event.player.channels().get(0));
        expectThrows(UnsupportedOperationException.class, () -> event.player.channels().add("other"));
    }

    @Test
    public void rejectsMessagesWithoutEvent() {
        IllegalArgumentException error = expectThrows(IllegalArgumentException.class,
                () -> IncomingRelayMessage.parse("{\"payload\":{}}"));
        assertTrue(error.getMessage().contains("event"));
    }

    private static <T extends Throwable> T expectThrows(Class<T> expected, Runnable action) {
        try {
            action.run();
        } catch (Throwable error) {
            if (expected.isInstance(error)) {
                return expected.cast(error);
            }
            throw new AssertionError("Unexpected exception type", error);
        }
        fail("Expected " + expected.getName());
        return null;
    }
}
