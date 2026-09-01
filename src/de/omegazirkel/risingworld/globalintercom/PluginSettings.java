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
import de.omegazirkel.risingworld.tools.settings.JsonSettingsFile;
import de.omegazirkel.risingworld.tools.settings.SettingsFileEditor;

public class PluginSettings {
	private static PluginSettings instance = null;

	private static GlobalIntercom plugin;

	private static OZLogger logger() {
		return GlobalIntercom.logger();
	}

	// Settings
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
		initSettings(JsonSettingsFile.worldSettingsFile(plugin.getPath() != null ? plugin.getPath() : ".").toString());
	}

	public void initSettings(String filePath) {
		settingsFile = Paths.get(filePath);
		Path defaultSettingsFile = settingsFile.resolveSibling("settings.default.json");
		Path legacySettingsFile = settingsFile.resolveSibling("settings.properties");

		try {
			if (JsonSettingsFile.migrateLegacyProperties(legacySettingsFile, settingsFile))
				logger().info("Migrated legacy settings.properties to " + settingsFile.getFileName());
			if (Files.notExists(settingsFile) && Files.exists(defaultSettingsFile))
				JsonSettingsFile.writeFlatAtomically(settingsFile, JsonSettingsFile.loadFlat(defaultSettingsFile));
			JsonSettingsFile.normalizePaths(settingsFile);

			Properties settings = new Properties();
			Properties defaults = new Properties();
			defaults = loadSettings(defaultSettingsFile);
			settings = loadSettings(settingsFile);
			if (settings.isEmpty()) {
				logger().warn(
						"⚠️ Neither settings.properties nor settings.default.properties found. Using default values.");
			}
			// fill global values
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
				AdminSettingsEntry.group("runtime", "Runtime", "Maintenance behavior."),
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
				newValue -> SettingsFileEditor.writeValue(settingsPath(), JsonSettingsFile.canonicalPath(key), newValue));
	}

	private Path settingsPath() {
		return settingsFile != null ? settingsFile
				: JsonSettingsFile.worldSettingsFile(plugin.getPath() != null ? plugin.getPath() : ".");
	}

	private Properties loadSettings(Path file) throws IOException {
		if (!file.getFileName().toString().endsWith(".properties")) {
			Properties properties = JsonSettingsFile.loadProperties(file);
			JsonSettingsFile.addCompatibilityAliases(properties);
			return properties;
		}
		Properties properties = new Properties();
		if (Files.exists(file)) try (FileInputStream input = new FileInputStream(file.toFile())) {
			properties.load(new InputStreamReader(input, "UTF8"));
		}
		return properties;
	}

	private boolean bool(Properties settings, String key, boolean fallback) {
		return settings.getProperty(key, String.valueOf(fallback)).contentEquals("true");
	}
}
