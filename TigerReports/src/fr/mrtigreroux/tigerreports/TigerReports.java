package fr.mrtigreroux.tigerreports;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import fr.mrtigreroux.tigerreports.commands.ReportCommand;
import fr.mrtigreroux.tigerreports.commands.ReportsCommand;
import fr.mrtigreroux.tigerreports.data.config.ConfigFile;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.data.database.MySQL;
import fr.mrtigreroux.tigerreports.data.database.SQLite;
import fr.mrtigreroux.tigerreports.listeners.*;
import fr.mrtigreroux.tigerreports.managers.*;
import fr.mrtigreroux.tigerreports.objects.Report;
import fr.mrtigreroux.tigerreports.objects.users.OnlineUser;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.runnables.ReportsNotifier;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;

/**
 * @author MrTigreroux
 * Published on: 30/06/2016
 */

public class TigerReports extends JavaPlugin {

	private static TigerReports instance;
	
	private WebManager webManager;
	private Database database;
	private BungeeManager bungeeManager;

	public Map<String, User> users = new HashMap<>();
	public Map<String, String> lastNameFound = new HashMap<>();
	public Map<String, String> lastUniqueIdFound = new HashMap<>();
	public Map<Integer, Report> reports = new HashMap<>();
	
	public TigerReports() {}

	public void load() {
		for(ConfigFile configFiles : ConfigFile.values()) configFiles.load();
		
		ReportsNotifier.start();
	}
	
	@Override
	public void onEnable() {
		instance = this;
		load();
		
		PluginManager pm = Bukkit.getServer().getPluginManager();
		pm.registerEvents(new InventoryListener(), this);
		pm.registerEvents(new PlayerListener(), this);
		pm.registerEvents(new SignListener(), this);
		
		getCommand("report").setExecutor(new ReportCommand());
		getCommand("reports").setExecutor(new ReportsCommand());
		
		webManager = new WebManager(this);
		webManager.initialize();
		
		PluginDescriptionFile desc = getDescription();
		if(!desc.getName().equals("TigerReports") || desc.getAuthors().size() > 1 || !desc.getAuthors().contains("MrTigreroux")) {
			MessageUtils.logSevere(ConfigUtils.getInfoMessage("The file plugin.yml has been edited without authorization.", "Le fichier plugin.yml a ete modifie sans autorisation."));
			Bukkit.shutdown();
		}

		bungeeManager = new BungeeManager(this);
		bungeeManager.initialize();
		bungeeManager.collectServerName();

		initializeDatabase();
	}
	
	public void unload() {
		database.closeConnection();
		for(User u : users.values()) {
			if(u instanceof OnlineUser) {
				OnlineUser ou = (OnlineUser) u;
				if(ou.getOpenedMenu() != null) ou.getPlayer().closeInventory();
			}
		}
	}
	
	@Override
	public void onDisable() {
		unload();
	}
	
	public static TigerReports getInstance() {
		return instance;
	}
	
	public Database getDb() {
		return database;
	}
	
	public WebManager getWebManager() {
		return webManager;
	}
	
	public BungeeManager getBungeeManager() {
		return bungeeManager;
	}
	
	public void initializeDatabase() {
		Bukkit.getScheduler().runTaskAsynchronously(instance, new Runnable() {
			
			@Override
			public void run() {
				Logger logger = Bukkit.getLogger();
				try {
					FileConfiguration config = ConfigFile.CONFIG.get();
					MySQL mysql = new MySQL(config.getString("MySQL.Host"), config.getInt("MySQL.Port"), config.getString("MySQL.Database"), config.getString("MySQL.Username"), config.getString("MySQL.Password"));
					mysql.check();
					database = mysql;
					logger.info(ConfigUtils.getInfoMessage("The plugin is using a MySQL database.", "Le plugin utilise une base de donnees MySQL."));
				} catch (Exception invalidMySQL) {
					database = new SQLite();
					logger.info(ConfigUtils.getInfoMessage("The plugin is using the SQLite (default) database.", "Le plugin utilise la base de donnees SQLite (par defaut)."));
				}
				database.initialize();
			}
			
		});
	}
	
}
