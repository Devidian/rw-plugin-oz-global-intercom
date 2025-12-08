package de.omegazirkel.risingworld.globalintercom.entities;

import net.risingworld.api.objects.Player;

public class PlayerJoinChannelMessage extends PlayerMessage {
    public String channel;
    public String password;

    public PlayerJoinChannelMessage(Player player) {
        super(player);
    }
}