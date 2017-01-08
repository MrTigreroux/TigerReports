package fr.mrtigreroux.tigerreports.data;

import java.util.Arrays;

import org.bukkit.Sound;

/**
 * @author MrTigreroux
 */

public enum ConfigSound {

	MENU("ITEM_PICKUP", "ENTITY_ITEM_PICKUP"),
	ERROR("ITEM_BREAK", "ENTITY_ITEM_BREAK"),
	REPORT("BAT_DEATH", "ENTITY_BAT_DEATH"),
	STAFF("ITEM_PICKUP", "ENTITY_ITEM_PICKUP"),
	TELEPORT("ENDERMAN_TELEPORT", "ENTITY_ENDERMEN_TELEPORT");
	
	private String oldSound;
	private String newSound;
	
	ConfigSound(String oldSound, String newSound) {
		this.oldSound = oldSound;
		this.newSound = newSound;
	}
	
	public String getConfigName() {
		return this.name().substring(0, 1)+this.name().substring(1).toLowerCase()+"Sound";
	}
	
	public Sound get() {
		String path = "Config."+getConfigName();
		String configSound = ConfigFile.CONFIG.get().getString(path) != null ? ConfigFile.CONFIG.get().getString(path).toUpperCase() : "";
		for(String sound : Arrays.asList(configSound, oldSound, newSound)) {
			try {
				return Sound.valueOf(sound);
			} catch (Exception invalidSound) {}
		}
		return null;
	}
	
}
