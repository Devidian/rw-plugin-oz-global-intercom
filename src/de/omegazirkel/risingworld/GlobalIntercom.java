package de.omegazirkel.risingworld;

import java.nio.file.Path;
import java.util.Map;

import de.omegazirkel.risingworld.globalintercom.PluginSettings;
import de.omegazirkel.risingworld.globalintercom.entities.GlobalIntercomPlayer;
import de.omegazirkel.risingworld.tools.FileChangeListener;
import de.omegazirkel.risingworld.tools.OZLogger;
import net.risingworld.api.events.EventMethod;
import net.risingworld.api.events.Listener;
import net.risingworld.api.events.player.PlayerChatEvent;
import net.risingworld.api.events.player.PlayerCommandEvent;
import net.risingworld.api.events.player.PlayerConnectEvent;
import net.risingworld.api.events.player.PlayerDisconnectEvent;
import net.risingworld.api.events.player.PlayerSpawnEvent;

/** Rising World entry point; relay behavior lives in {@link GlobalIntercomRuntime}. */
public final class GlobalIntercom extends GlobalIntercomRuntime implements Listener, FileChangeListener {
    public static final String pluginCMD = GlobalIntercomRuntime.pluginCMD;
    public static final Map<String, GlobalIntercomPlayer> playerMap = GlobalIntercomRuntime.playerMap;

    public static OZLogger logger() { return GlobalIntercomRuntime.logger(); }
    public static PluginSettings getSettings() { return GlobalIntercomRuntime.getSettings(); }
    public static boolean isRelayConnected() { return GlobalIntercomRuntime.isRelayConnected(); }

    @Override
    public void onEnable() {
        super.onEnable();
        registerEventListener(this);
    }

    @Override public void onDisable() { super.onDisable(); }
    @Override public void onJarChanged(Path file) { super.onJarChanged(file); }
    @Override public void onSettingsChanged(Path file) { super.onSettingsChanged(file); }

    @Override @EventMethod
    public void onPlayerConnect(PlayerConnectEvent event) { super.onPlayerConnect(event); }
    @Override @EventMethod
    public void onPlayerDisconnect(PlayerDisconnectEvent event) { super.onPlayerDisconnect(event); }
    @Override @EventMethod
    public void onPlayerCommand(PlayerCommandEvent event) { super.onPlayerCommand(event); }
    @Override @EventMethod
    public void onPlayerChat(PlayerChatEvent event) { super.onPlayerChat(event); }
    @Override @EventMethod
    public void onPlayerSpawn(PlayerSpawnEvent event) { super.onPlayerSpawn(event); }
}
