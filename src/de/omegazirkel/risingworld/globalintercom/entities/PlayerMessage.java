package de.omegazirkel.risingworld.globalintercom.entities;

import net.risingworld.api.objects.Player;

public abstract class PlayerMessage {
    public String playerUID; // should be long but JavaScript cant handle this (yet)
    public String playerName;

    public PlayerMessage(Player player) {
        // Player stuff
        this.playerName = player.getName();
        this.playerUID = "" + player.getUID();
    }
}