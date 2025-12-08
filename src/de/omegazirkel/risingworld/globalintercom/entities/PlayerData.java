package de.omegazirkel.risingworld.globalintercom.entities;

import java.util.List;

import net.risingworld.api.objects.Player;

public class PlayerData {

	public String playerName;
	public String playerUID;// should be long but JavaScript cant handle this (yet)
	public List<String> channelList;

	public PlayerData(Player player) {
		// Player stuff
		this.playerName = player.getName();
		this.playerUID = "" + player.getUID();
	}
}