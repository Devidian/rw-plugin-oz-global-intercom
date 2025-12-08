package de.omegazirkel.risingworld.globalintercom.entities;

import net.risingworld.api.objects.Player;

public class PlayerRemoveChannelMessage extends PlayerMessage {
    public String channel;

    public PlayerRemoveChannelMessage(Player player) {
        super(player);
    }
}