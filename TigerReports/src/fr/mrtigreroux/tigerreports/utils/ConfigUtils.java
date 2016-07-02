package fr.mrtigreroux.tigerreports.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;

import fr.mrtigreroux.tigerreports.managers.FilesManager;

/**
 * @author MrTigreroux
 */

public class ConfigUtils {
	
	private final static Set<String> ActivationWords = new HashSet<String>(Arrays.asList("true", "t" ,"on", "o", "enabled", "yes", "y", "activated", "act", "a"));
	
	public static boolean isEnabled(ConfigurationSection config, String path) {
		return config.get(path) != null && ActivationWords.contains(config.getString(path));
	}
	
	public static char getColorCharacter() {
		return FilesManager.getConfig.contains("Config.ColorCharacter") && FilesManager.getConfig.getString("Config.ColorCharacter") != null && FilesManager.getConfig.getString("Config.ColorCharacter").length() >= 1 ? FilesManager.getConfig.getString("Config.ColorCharacter").charAt(0) : '&';
	}
	
	public static String getLineBreakSymbol() {
		return FilesManager.getConfig.get("Config.LineBreakSymbol") != null ? FilesManager.getConfig.getString("Config.LineBreakSymbol") : "//";
	}

	public static Sound getSound(String type, String default1, String default2) {
		String path = "Config."+type;
		String configSound = FilesManager.getConfig.getString(path) != null ? FilesManager.getConfig.getString(path).toUpperCase() : "";
		for(String sound : Arrays.asList(configSound, default1, default2)) {
			try {
				return Sound.valueOf(sound);
			} catch (Exception invalidSound) {}
		}
		return null;
	}
	
	public static Sound getMenuSound() {
		return getSound("MenuSound", "ITEM_PICKUP", "ENTITY_ITEM_PICKUP");
	}

	public static Sound getErrorSound() {
		return getSound("ErrorSound", "ITEM_BREAK", "ENTITY_ITEM_BREAK");
	}

	public static Sound getReportSound() {
		return getSound("ReportSound", "BAT_DEATH", "ENTITY_BAT_DEATH");
	}

	public static Sound getTeleportSound() {
		return getSound("TeleportSound", "ENDERMAN_TELEPORT", "ENTITY_ENDERMEN_TELEPORT");
	}

	public static Sound getStaffSound() {
		return getSound("StaffSound", "ITEM_PICKUP", "ENTITY_ITEM_PICKUP");
	}

	/**
	 * @deprecated
	 */
	public static short getDamage(ConfigurationSection config, String path) {
		try {
			String icon = config.getString(path);
			return icon != null && icon.startsWith("Material-") && icon.contains(":") ? Short.parseShort(icon.split(":")[1]) : 0;
		} catch(Exception NoDamage) {
			return 0;
		}
	}
	
}
