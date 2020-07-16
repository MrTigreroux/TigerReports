package fr.mrtigreroux.tigerreports.data.config;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import fr.mrtigreroux.tigerreports.TigerReports;
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

	public void load() {
		file = new File("plugins/TigerReports", toString().toLowerCase()+".yml");
		if (!file.exists())
			reset();
		config = YamlConfiguration.loadConfiguration(file);

		try {
			Reader defaultConfigStream = new InputStreamReader(TigerReports.getInstance().getResource(file.getName()), "UTF8");
			YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(defaultConfigStream);
			if (this == CONFIG) {
				defaultConfig.set("Config.DefaultReasons", null);
				defaultConfig.set("Config.Punishments", null);
				defaultConfig.set("Config.Punishments.Enabled", true);
				defaultConfig.set("Config.Punishments.DefaultReasons", true);
			}

			config.setDefaults(defaultConfig);
		} catch (Exception ex) {
			Bukkit.getLogger()
					.log(Level.SEVERE, ConfigUtils.getInfoMessage("An error has occurred while loading config files:",
							"Une erreur est survenue en chargeant les fichiers de configuration:"), ex);
		}
	}

	public FileConfiguration get() {
		return config;
	}

	public void save() {
		try {
			get().save(file);
		} catch (Exception ex) {
			load();
		}
	}

	public void reset() {
		TigerReports.getInstance().saveResource(file.getName(), false);
		Bukkit.getLogger().warning(MessageUtils.LINE);
		Bukkit.getLogger()
				.warning(this != CONFIG && ConfigUtils.getInfoLanguage().equalsIgnoreCase("English") ? "[TigerReports] The file "+file.getName()
						+" has been reset." : "[TigerReports] Le fichier "+file.getName()+" a ete reinitialise.");
		Bukkit.getLogger().warning(MessageUtils.LINE);
	}

}
