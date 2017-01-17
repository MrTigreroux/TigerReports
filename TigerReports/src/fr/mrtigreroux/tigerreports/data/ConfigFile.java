package fr.mrtigreroux.tigerreports.data;

import java.io.File;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.libs.jline.internal.InputStreamReader;

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
		
		try {
			Reader defaultConfigStream = new InputStreamReader(TigerReports.getInstance().getResource(file.getName()), "UTF8");
			YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(defaultConfigStream);
	        config.setDefaults(defaultConfig);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
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
