package fr.mrtigreroux.tigerreports.data;

import java.io.File;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import fr.mrtigreroux.tigerreports.TigerReports;

/**
 * @author MrTigreroux
 */

public enum ConfigFile {

	CONFIG, DATA, MESSAGES, REPORTS;
	
	private File file = null;
	private FileConfiguration config = null;
	
	ConfigFile() {}
	
	public void load() {
		file = new File("plugins/TigerReports", toString().toLowerCase()+".yml");
		if(!file.exists()) reset();
		config = YamlConfiguration.loadConfiguration(file);
	}
	
	public FileConfiguration get() {
		return config;
	}
	
	public void save() {
		try {
			get().save(file);
		} catch(Exception error) {
			load();
		}
	}
	
	public void reset() {
		TigerReports.getInstance().saveResource(file.getName(), false);
		Bukkit.getLogger().log(Level.WARNING, "------------------------------------------------------");
		Bukkit.getLogger().log(Level.WARNING, "[TigerReports] File "+file.getName()+" has been reset.");
		Bukkit.getLogger().log(Level.WARNING, "------------------------------------------------------");
	}
	
}
