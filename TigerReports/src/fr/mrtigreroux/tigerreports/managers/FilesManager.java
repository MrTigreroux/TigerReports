package fr.mrtigreroux.tigerreports.managers;

import java.io.File;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import fr.mrtigreroux.tigerreports.TigerReports;

/**
 * @author MrTigreroux
 */

public class FilesManager {

	private static TigerReports main = TigerReports.getInstance();
	private static String folder = "plugins/TigerReports";
	public static File config = new File(folder, "config.yml");
	public static FileConfiguration getConfig = YamlConfiguration.loadConfiguration(config);
	public static File messages = new File(folder, "messages.yml");
	public static FileConfiguration getMessages = YamlConfiguration.loadConfiguration(messages);
	public static File reports = new File(folder, "reports.yml");
	public static FileConfiguration getReports = YamlConfiguration.loadConfiguration(reports);
	public static File data = new File(folder, "data.yml");
	public static FileConfiguration getData = YamlConfiguration.loadConfiguration(data);
	
	
	public static void checkFiles() {
		if(!config.exists()) resetConfig(getConfig, config);
		if(!messages.exists()) resetConfig(getMessages, messages);
		if(!reports.exists()) resetConfig(getReports, reports);
		if(!data.exists()) resetConfig(getData, data);
	}
	
	public static void resetConfig(FileConfiguration fileConfig, File file) {
		main.saveResource(file.getName(), false);
		try {
			fileConfig.load(file);
		} catch (Exception error) {}
		Bukkit.getLogger().log(Level.WARNING, "----------------------------------------------------------------");
		Bukkit.getLogger().log(Level.WARNING, "TigerReports > Le fichier "+file.getName()+" a ete reinitialise.");
		Bukkit.getLogger().log(Level.WARNING, "----------------------------------------------------------------");
	}

	public static void loadConfig(FileConfiguration fileConfig, File file) {
		try {
			fileConfig.load(file);
		} catch (Exception FileNotFound) {
			if(!file.exists()) resetConfig(fileConfig, file);
			return;
		}
	}
	
	public static void saveConfig(FileConfiguration fileConfig, File file) {
		try {
			fileConfig.save(file);
		} catch (Exception FileNotFound) {
			if(!file.exists()) resetConfig(fileConfig, file);
			return;
		}
	}

	public static void saveConfig() {
		saveConfig(getConfig, config);
	}
	
	public static void saveMessages() {
		saveConfig(getMessages, messages);
	}
	
	public static void saveReports() {
		saveConfig(getReports, reports);
	}

	public static void saveData() {
		saveConfig(getData, data);
	}
	
}
