package fr.mrtigreroux.tigerreports.utils;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import fr.mrtigreroux.tigerreports.data.config.ConfigFile;

/**
 * @author MrTigreroux
 */

public class ConfigUtils {

	private final static List<String> ACTIVATION_WORDS = Arrays.asList("true", "t", "on", "enabled", "yes", "y",
	        "activated", "a");

	public static boolean isEnabled(ConfigurationSection config, String path) {
		return config.get(path) != null && ACTIVATION_WORDS.contains(config.getString(path));
	}
	
	public static boolean isEnabled(String path) {
		return isEnabled(ConfigFile.CONFIG.get(), path);
	}

	public static char getColorCharacter() {
		return ConfigFile.CONFIG.get().getString("Config.ColorCharacter", "&").charAt(0);
	}

	public static String getLineBreakSymbol() {
		return ConfigFile.CONFIG.get().getString("Config.LineBreakSymbol", "//");
	}

	public static String getInfoLanguage() {
		return ConfigFile.CONFIG.get().getString("Config.InfoLanguage", "French");
	}

	public static String getInfoMessage(String english, String french) {
		return "[TigerReports] " + (getInfoLanguage().equalsIgnoreCase("English") ? english : french);
	}

	public static boolean exist(ConfigurationSection config, String path) {
		return config.get(path) != null;
	}

	public static Material getMaterial(ConfigurationSection config, String path) {
		String icon = config.getString(path);
		return icon != null && icon.startsWith("Material-")
		        ? Material.matchMaterial(icon.split("-")[1].toUpperCase().replace(":" + getDamage(config, path), ""))
		        : null;
	}

	public static short getDamage(ConfigurationSection config, String path) {
		try {
			String icon = config.getString(path);
			return icon != null && icon.startsWith("Material-") && icon.contains(":")
			        ? Short.parseShort(icon.split(":")[1])
			        : 0;
		} catch (Exception noDamage) {
			return 0;
		}
	}

	public static String getSkull(ConfigurationSection config, String path) {
		String icon = config.getString(path);
		return icon != null && icon.startsWith("Skull-") ? icon.split("-")[1] : null;
	}

	public static boolean playersNotifications() {
		return isEnabled(ConfigFile.CONFIG.get(), "Config.Notifications.Players");
	}

	public static ZoneId getZoneId() {
		String time = ConfigFile.CONFIG.get().getString("Config.Time");
		if (!time.equals("default")) {
			try {
				return ZoneId.of(time);
			} catch (Exception ex) {}
		}
		return ZoneId.systemDefault();
	}

}
