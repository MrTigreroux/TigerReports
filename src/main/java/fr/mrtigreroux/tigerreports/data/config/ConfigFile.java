package fr.mrtigreroux.tigerreports.data.config;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.logs.Logger;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;

/**
 * @author MrTigreroux
 */

public enum ConfigFile {

	CONFIG,
	MESSAGES;

	private File file = null;
	private FileConfiguration config = null;

	ConfigFile() {}

	public void load(TigerReports tr) {
		file = new File(tr.getDataFolder(), name().toLowerCase() + ".yml");
		if (!file.exists())
			reset(tr);
		config = YamlConfiguration.loadConfiguration(file);

		try {
			Reader defaultConfigStream = new InputStreamReader(tr.getResource(file.getName()), "UTF8");
			YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(defaultConfigStream);
			if (this == CONFIG) {
				defaultConfig.set("Config.DefaultReasons", null);
				defaultConfig.set("Config.Punishments", null);
				defaultConfig.set("Config.Punishments.Enabled", true);
				defaultConfig.set("Config.Punishments.DefaultReasons", true);
			}

			config.setDefaults(defaultConfig);
		} catch (Exception ex) {
			Logger.CONFIG.error(ConfigUtils.getInfoMessage("An error has occurred while loading config files:",
			        "Une erreur est survenue en chargeant les fichiers de configuration:"), ex);
		}
	}

	public FileConfiguration get() {
		return config;
	}

	public void save(TigerReports tr) {
		try {
			get().save(file);
		} catch (Exception ex) {
			load(tr);
		}
	}

	public void reset(TigerReports tr) {
		tr.saveResource(file.getName(), false);
		Logger logger = Logger.CONFIG;
		logger.warn(() -> MessageUtils.LINE);
		logger.warn(() -> this != CONFIG && ConfigUtils.getInfoLanguage().equalsIgnoreCase("English")
		        ? "The file " + file.getName() + " has been reset."
		        : "Le fichier " + file.getName() + " a ete reinitialise.");
		logger.warn(() -> MessageUtils.LINE);
	}

}
