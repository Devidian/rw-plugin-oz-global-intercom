package de.omegazirkel.risingworld.globalintercom;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import de.omegazirkel.risingworld.GlobalIntercom;
import de.omegazirkel.risingworld.tools.OZLogger;
import de.omegazirkel.risingworld.tools.settings.AdminSettingsEntry;
import de.omegazirkel.risingworld.tools.settings.AdminSettingsType;
import de.omegazirkel.risingworld.tools.settings.SettingsFileEditor;

public class PluginSettings {
	private static PluginSettings instance = null;

	private static GlobalIntercom plugin;

	private static OZLogger logger() {
		return GlobalIntercom.logger();
	}

	// Settings
	public String logLevel = "ALL";
	public boolean reloadOnChange = true;
	public boolean sendPluginWelcome = false;
	public boolean restartOnUpdate = true;
	public boolean joinDefault = false;
	public URI webSocketURI;
	public String defaultChannel = "global";

	public String colorOther = "<color=#3881f7>";
	public String colorSelf = "<color=#37f7da>";
	public String colorLocal = "<color=#FFFFFF>";

	public boolean allowScreenshots = true;
	public int maxScreenWidth = 1920;
	private Path settingsFile;
	private Properties currentSettings = new Properties();
	private Properties defaultSettings = new Properties();
	// end Settings

	public static PluginSettings getInstance(GlobalIntercom p) {
		plugin = p;
		return getInstance();
	}

	public static PluginSettings getInstance() {

		if (instance == null) {
			instance = new PluginSettings();
		}
		return instance;
	}

	private PluginSettings() {
	}

	public void initSettings() {
		initSettings((plugin.getPath() != null ? plugin.getPath() : ".") + "/settings.properties");
	}

	public void initSettings(String filePath) {
		settingsFile = Paths.get(filePath);
		Path defaultSettingsFile = settingsFile.resolveSibling("settings.default.properties");

		try {
			if (Files.notExists(settingsFile) && Files.exists(defaultSettingsFile)) {
				logger().info("settings.properties not found, copying from settings.default.properties...");
				Files.copy(defaultSettingsFile, settingsFile);
			}

			Properties settings = new Properties();
			Properties defaults = new Properties();
			if (Files.exists(defaultSettingsFile)) {
				try (FileInputStream in = new FileInputStream(defaultSettingsFile.toFile())) {
					defaults.load(new InputStreamReader(in, "UTF8"));
				}
			}
			if (Files.exists(settingsFile)) {
				try (FileInputStream in = new FileInputStream(settingsFile.toFile())) {
					settings.load(new InputStreamReader(in, "UTF8"));
				}
			} else {
				logger().warn(
						"⚠️ Neither settings.properties nor settings.default.properties found. Using default values.");
			}
			// fill global values
			logLevel = settings.getProperty("logLevel", "ALL");
			reloadOnChange = bool(settings, "reloadOnChange", true);
			restartOnUpdate = bool(settings, "restartOnUpdate", true);

			// motd settings
			sendPluginWelcome = bool(settings, "sendPluginWelcome", false);

			webSocketURI = new URI(settings.getProperty("webSocketURI", "wss://rw.gi.omega-zirkel.de/ws"));
			defaultChannel = settings.getProperty("defaultChannel", "global");
			joinDefault = bool(settings, "joinDefault", true);
			colorOther = settings.getProperty("colorOther", "<color=#3881f7>");
			colorSelf = settings.getProperty("colorSelf", "<color=#37f7da>");
			colorLocal = settings.getProperty("colorLocal", "<color=#FFFFFF>");

			allowScreenshots = bool(settings, "allowScreenshots", true);
			maxScreenWidth = Integer.parseInt(settings.getProperty("maxScreenWidth", "1920"));

			logger().info(plugin.getName() + " Plugin settings loaded");

			logger().info("Sending welcome message on login is: " + String.valueOf(sendPluginWelcome));
			logger().info("Loglevel is set to " + logLevel);
			logger().setLevel(logLevel);
			currentSettings = settings;
			defaultSettings = defaults;

		} catch (IOException ex) {
			logger().error("IOException on initSettings: " + ex.getMessage());
			ex.printStackTrace();
		} catch (NumberFormatException ex) {
			logger().error("NumberFormatException on initSettings: " + ex.getMessage());
			ex.printStackTrace();
		} catch (URISyntaxException ex) {
			logger().error("URISyntaxException on initSettings: " + ex.getMessage());
			ex.printStackTrace();
		}
	}

	public List<AdminSettingsEntry> adminSettingsEntries() {
		return Arrays.asList(
				AdminSettingsEntry.group("logging", "Logging", "Logging output and verbosity."),
				entry("logLevel", "Log level", "Controls GlobalIntercom logging verbosity.", AdminSettingsType.STRING),
				AdminSettingsEntry.group("runtime", "Runtime", "Runtime reload and maintenance behavior."),
				entry("reloadOnChange", "Reload on change",
						"Documents that GlobalIntercom settings reload when settings.properties changes.",
						AdminSettingsType.BOOLEAN),
				entry("restartOnUpdate", "Restart on update",
						"Documents that GlobalIntercom should restart after plugin updates.",
						AdminSettingsType.BOOLEAN),
				AdminSettingsEntry.group("relay", "Relay", "WebSocket relay connection and default channel behavior."),
				entry("webSocketURI", "WebSocket URI", "Relay server WebSocket endpoint.", AdminSettingsType.STRING),
				entry("defaultChannel", "Default channel", "Default global intercom channel.",
						AdminSettingsType.STRING),
				entry("joinDefault", "Join default channel", "Automatically joins players to the default channel.",
						AdminSettingsType.BOOLEAN),
				AdminSettingsEntry.group("chatColors", "Chat colors", "RichText colors used for chat output."),
				entry("colorOther", "Other-player color", "Color for messages from other players.",
						AdminSettingsType.STRING),
				entry("colorSelf", "Own-message color", "Color for messages from the receiving player.",
						AdminSettingsType.STRING),
				entry("colorLocal", "Local-chat color", "Color applied to local chat messages.",
						AdminSettingsType.STRING),
				AdminSettingsEntry.group("playerMessages", "Player messages", "Messages sent directly to players."),
				entry("sendPluginWelcome", "Welcome message",
						"Shows a short GlobalIntercom message when a player joins.", AdminSettingsType.BOOLEAN),
				AdminSettingsEntry.group("screenshots", "Screenshots", "Screenshot attachment options for relayed chat."),
				entry("allowScreenshots", "Allow screenshots", "Allows screenshot posting from chat commands.",
						AdminSettingsType.BOOLEAN),
				entry("maxScreenWidth", "Max screenshot width", "Maximum screenshot width in pixels.", maxScreenWidth,
						AdminSettingsType.INTEGER));
	}

	private AdminSettingsEntry entry(String key, String label, String description, AdminSettingsType type) {
		return entry(key, label, description, currentSettings.getProperty(key, defaultSettings.getProperty(key, "")),
				type);
	}

	private AdminSettingsEntry entry(String key, String label, String description, Object value, AdminSettingsType type) {
		return new AdminSettingsEntry(
				key,
				label,
				description,
				String.valueOf(value),
				defaultSettings.getProperty(key, ""),
				type,
				false,
				newValue -> SettingsFileEditor.writeValue(settingsPath(), key, newValue));
	}

	private Path settingsPath() {
		return settingsFile != null ? settingsFile
				: Paths.get((plugin.getPath() != null ? plugin.getPath() : ".") + "/settings.properties");
	}

	private boolean bool(Properties settings, String key, boolean fallback) {
		return settings.getProperty(key, String.valueOf(fallback)).contentEquals("true");
	}
}
