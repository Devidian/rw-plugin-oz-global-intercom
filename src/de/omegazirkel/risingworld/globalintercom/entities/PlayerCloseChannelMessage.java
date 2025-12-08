package de.omegazirkel.risingworld.globalintercom.entities;

import net.risingworld.api.objects.Player;

public class PlayerCloseChannelMessage extends PlayerMessage {
    public String channel;

    public PlayerCloseChannelMessage(Player player) {
        super(player);
    }
}