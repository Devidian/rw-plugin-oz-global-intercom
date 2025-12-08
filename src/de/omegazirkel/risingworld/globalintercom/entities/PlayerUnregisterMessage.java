package de.omegazirkel.risingworld.globalintercom.entities;

import net.risingworld.api.objects.Player;

public class PlayerUnregisterMessage extends PlayerMessage {
    public boolean unregister;

    public PlayerUnregisterMessage(Player player) {
        super(player);
        this.unregister = true;
    }
}