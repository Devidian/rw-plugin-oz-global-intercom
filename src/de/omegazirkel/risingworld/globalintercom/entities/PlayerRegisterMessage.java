package de.omegazirkel.risingworld.globalintercom.entities;

import net.risingworld.api.objects.Player;

public class PlayerRegisterMessage extends PlayerMessage {
    public boolean register;

    public PlayerRegisterMessage(Player player) {
        super(player);
        this.register = true;
    }
}