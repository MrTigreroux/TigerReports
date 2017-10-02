package fr.mrtigreroux.tigerreports.data.config;

import java.util.Arrays;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * @author MrTigreroux
 */

public enum ConfigSound {

	MENU("ITEM_PICKUP", "ENTITY_ITEM_PICKUP"),
	ERROR("ITEM_BREAK", "ENTITY_ITEM_BREAK"),
	REPORT("BAT_DEATH", "ENTITY_BAT_DEATH"),
	STAFF("ITEM_PICKUP", "ENTITY_ITEM_PICKUP"),
	TELEPORT("ENDERMAN_TELEPORT", "ENTITY_ENDERMEN_TELEPORT");
	
	private final String oldSound;
	private final String newSound;
	
	ConfigSound(String oldSound, String newSound) {
		this.oldSound = oldSound;
		this.newSound = newSound;
	}
	
	public String getConfigName() {
		return name().charAt(0)+name().substring(1).toLowerCase()+"Sound";
	}
	
	public Sound get() {
		String path = "Config."+getConfigName();
		String configSound = ConfigFile.CONFIG.get().getString(path);
		if(configSound != null && !configSound.equalsIgnoreCase("none")) {
			for(String sound : Arrays.asList(configSound.toUpperCase(), oldSound, newSound)) {
				try {
					return Sound.valueOf(sound);
				} catch (Exception invalidSound) {}
			}
		}
		return null;
	}
	
	public void play(Player p) {
		p.playSound(p.getLocation(), get(), 1, 1);
	}
	
}
