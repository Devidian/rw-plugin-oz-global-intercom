package de.omegazirkel.risingworld.globalintercom;

import de.omegazirkel.risingworld.GlobalIntercom;
import de.omegazirkel.risingworld.globalintercom.entities.GlobalIntercomPlayer;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ui.PluginInfoStatusProvider;
import net.risingworld.api.objects.Player;

public class GlobalIntercomPluginInfoStatusProvider implements PluginInfoStatusProvider {
	private final GlobalIntercom plugin;
	private final String pluginName;
	private final String pluginVersion;

	public GlobalIntercomPluginInfoStatusProvider(GlobalIntercom plugin, String pluginVersion) {
		this.plugin = plugin;
		this.pluginName = GlobalIntercom.name == null || GlobalIntercom.name.isBlank()
				? "OZGlobalIntercom"
				: GlobalIntercom.name;
		this.pluginVersion = pluginVersion == null ? "" : pluginVersion;
	}

	@Override
	public String getPluginName() {
		return pluginName;
	}

	@Override
	public String getInfo(Player player) {
		return t().get("TC_GLOBAL_INTERCOM_INFO_PANEL_INFO", player)
				.replace("PH_PLUGIN_NAME", pluginName)
				.replace("PH_PLUGIN_CMD", plugin.getCommandName());
	}

	@Override
	public String getStatus(Player player) {
		PluginSettings settings = GlobalIntercom.getSettings();
		GlobalIntercomPlayer giPlayer = plugin.getIntercomPlayer(player);
		String lang = player.getSystemLanguage();

		return t().get("TC_GLOBAL_INTERCOM_INFO_PANEL_STATUS", player)
				.replace("PH_VERSION", pluginVersion)
				.replace("PH_STATE_WS", state(GlobalIntercom.isRelayConnected(), "STATE_CONNECTED",
						"STATE_DISCONNECTED", lang))
				.replace("PH_DEFAULT_CHANNEL", settings == null ? "" : settings.defaultChannel)
				.replace("PH_LAST_CHANNEL", plugin.getPlayerLastChannel(player))
				.replace("PH_SAVE_STATUS", state(giPlayer != null && giPlayer.saveSettings, "STATE_ACTIVE",
						"STATE_INACTIVE", lang))
				.replace("PH_OVERRIDE_STATUS", state(giPlayer != null && giPlayer.override, "STATE_ON", "STATE_OFF",
						lang))
				.replace("PH_CHANNEL_LIST", giPlayer == null ? "" : giPlayer.getChannelList())
				.replace("PH_LANGUAGE", player.getLanguage() + " / " + player.getSystemLanguage())
				.replace("PH_USEDLANG", t().getLanguageUsed(lang))
				.replace("PH_LANG_AVAILABLE", t().getLanguageAvailable())
				.replace("PH_LOG_LEVEL", settings == null ? "" : settings.logLevel)
				.replace("PH_RELOAD_ON_CHANGE", booleanText(settings != null && settings.reloadOnChange, lang))
				.replace("PH_WELCOME_MESSAGE", booleanText(settings != null && settings.sendPluginWelcome, lang))
				.replace("PH_SCREENSHOTS", booleanText(settings != null && settings.allowScreenshots, lang))
				.replace("PH_MAX_SCREEN_WIDTH", settings == null ? "" : String.valueOf(settings.maxScreenWidth));
	}

	private static I18n t() {
		return I18n.getInstance(GlobalIntercom.name);
	}

	private static String state(boolean value, String trueKey, String falseKey, String lang) {
		return t().get(value ? trueKey : falseKey, lang);
	}

	private static String booleanText(boolean value, String lang) {
		return t().get(value ? "STATE_ON" : "STATE_OFF", lang);
	}
}
