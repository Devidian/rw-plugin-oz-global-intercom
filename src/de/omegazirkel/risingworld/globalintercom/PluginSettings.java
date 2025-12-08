package de.omegazirkel.risingworld.globalintercom;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import de.omegazirkel.risingworld.GlobalIntercom;
import de.omegazirkel.risingworld.tools.OZLogger;

public class PluginSettings {
	private static PluginSettings instance = null;

	private static GlobalIntercom plugin;

	private static OZLogger logger() {
		return OZLogger.getInstance("OZ.GlobalIntercom.Settings");
	}

	// Settings
	public String logLevel = "ALL";
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
		Path settingsFile = Paths.get(filePath);
		Path defaultSettingsFile = settingsFile.resolveSibling("settings.default.properties");

		try {
			if (Files.notExists(settingsFile) && Files.exists(defaultSettingsFile)) {
				logger().info("settings.properties not found, copying from settings.default.properties...");
				Files.copy(defaultSettingsFile, settingsFile);
			}

			Properties settings = new Properties();
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

			// motd settings
			sendPluginWelcome = settings.getProperty("sendPluginWelcome", "false").contentEquals("true");

			webSocketURI = new URI(settings.getProperty("webSocketURI", "wss://rw.gi.omega-zirkel.de/ws"));
			defaultChannel = settings.getProperty("defaultChannel", "global");
			joinDefault = settings.getProperty("joinDefault", "true").contentEquals("true");
			colorOther = settings.getProperty("colorOther", "<color=#3881f7>");
			colorSelf = settings.getProperty("colorSelf", "<color=#37f7da>");
			colorLocal = settings.getProperty("colorLocal", "<color=#FFFFFF>");

			allowScreenshots = settings.getProperty("allowScreenshots", "true").contentEquals("true");
			maxScreenWidth = Integer.parseInt(settings.getProperty("maxScreenWidth", "1920"));

			logger().info(plugin.getName() + " Plugin settings loaded");

			logger().info("Sending welcome message on login is: " + String.valueOf(sendPluginWelcome));
			logger().info("Loglevel is set to " + logLevel);
			logger().setLevel(logLevel);

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
}
