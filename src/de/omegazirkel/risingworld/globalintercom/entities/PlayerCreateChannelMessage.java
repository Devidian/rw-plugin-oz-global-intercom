package de.omegazirkel.risingworld.globalintercom.entities;

import net.risingworld.api.objects.Player;

public class PlayerCreateChannelMessage extends PlayerMessage {
    public String channel;
    public String password;

    public PlayerCreateChannelMessage(Player player) {
        super(player);
    }
}