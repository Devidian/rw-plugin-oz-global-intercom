package de.omegazirkel.risingworld.globalintercom.entities;

import net.risingworld.api.objects.Player;

public class PlayerLeaveChannelMessage extends PlayerMessage {
    public String channel;

    public PlayerLeaveChannelMessage(Player player) {
        super(player);
    }
}