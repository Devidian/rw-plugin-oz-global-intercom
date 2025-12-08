package de.omegazirkel.risingworld.globalintercom.entities;

import net.risingworld.api.objects.Player;

public class PlayerOverrideChangeMessage extends PlayerMessage {
    public boolean override = true;

    public PlayerOverrideChangeMessage(Player player) {
        super(player);
    }
}