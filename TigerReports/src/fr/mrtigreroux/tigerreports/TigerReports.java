package fr.mrtigreroux.tigerreports;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import fr.mrtigreroux.tigerreports.commands.ReportCommand;
import fr.mrtigreroux.tigerreports.commands.ReportsCommand;
import fr.mrtigreroux.tigerreports.data.config.ConfigFile;
import fr.mrtigreroux.tigerreports.data.config.Message;
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

/**
 * @author MrTigreroux
 * Finished on: 30/06/2016
 */

public class TigerReports extends JavaPlugin {

	private static TigerReports instance;
	private static BungeeManager bungeeManager;
	private static WebManager webManager;
	private static Database database;

	public static Map<String, User> Users = new HashMap<>();
	public static Map<String, String> LastNameFound = new HashMap<>();
	public static Map<String, String> LastUniqueIdFound = new HashMap<>();
	public static Map<Integer, Report> Reports = new HashMap<>();

	public static void load() {
		for(ConfigFile configFiles : ConfigFile.values()) configFiles.load();
		
		InventoryListener.menuTitles.clear();
		for(Message message : Arrays.asList(Message.REASON_TITLE, Message.REPORTS_TITLE, Message.REPORT_TITLE, Message.COMMENTS_TITLE, Message.CONFIRM_ARCHIVE_TITLE, Message.CONFIRM_REMOVE_TITLE, Message.PROCESS_TITLE, Message.USER_TITLE, Message.ARCHIVED_REPORTS_TITLE))
			InventoryListener.menuTitles.add(message.get().replace("_Page_", "").replace("_Report_", "").replace("_Target_", ""));

		ReportsNotifier.start();
	}
	
	@Override
	public void onEnable() {
		instance = this;
		load();
		
		PluginManager pm = Bukkit.getServer().getPluginManager();
		pm.registerEvents(new InventoryListener(), this);
		pm.registerEvents(new SignListener(), this);
		pm.registerEvents(new PlayerListener(), this);
		
		getCommand("report").setExecutor(new ReportCommand());
		getCommand("reports").setExecutor(new ReportsCommand());
		
		webManager = new WebManager(this);
		webManager.checkUpdate();
		webManager.saveStatistics();
		
		if(getDescription().getAuthors().size() > 1 || !getDescription().getAuthors().contains("MrTigreroux")) {
			Logger logger = Bukkit.getLogger();
    		logger.log(Level.SEVERE, "------------------------------------------------------");
    		if(ConfigUtils.getInfoLanguage().equalsIgnoreCase("English")) {
	    		logger.log(Level.SEVERE, "[TigerReports] An user tried to appropriate");
	    		logger.log(Level.SEVERE, "the plugin TigerReports as his plugin.");
    		} else {
    			logger.log(Level.SEVERE, "[TigerReports] Un utilisateur a tente de s'approprier");
        		logger.log(Level.SEVERE, "le plugin TigerReports.");
    		}
    		logger.log(Level.SEVERE, "------------------------------------------------------");
			Bukkit.shutdown();
		}

		bungeeManager = new BungeeManager(this);
		bungeeManager.initialize();

		initializeDatabase();
	}
	
	public static void unload() {
		database.closeConnection();
		for(User u : Users.values()) {
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
	
	public static Database getDb() {
		return database;
	}
	
	public static BungeeManager getBungeeManager() {
		return bungeeManager;
	}
	
	public static WebManager getWebManager() {
		return webManager;
	}
	
	public static void initializeDatabase() {
		Bukkit.getScheduler().runTaskAsynchronously(instance, new Runnable() {
			@Override
			public void run() {
				Logger logger = Bukkit.getLogger();
				try {
					MySQL mysql = new MySQL(ConfigFile.CONFIG.get().getString("MySQL.Host"), ConfigFile.CONFIG.get().getInt("MySQL.Port"), ConfigFile.CONFIG.get().getString("MySQL.Database"), ConfigFile.CONFIG.get().getString("MySQL.Username"), ConfigFile.CONFIG.get().getString("MySQL.Password"));
					mysql.check();
					database = mysql;
					logger.log(Level.INFO, ConfigUtils.getInfoMessage("Plugin is using MySQL database.", "Le plugin utilise une base de donnees MySQL."));
				} catch (Exception invalidMySQL) {
					database = new SQLite();
					logger.log(Level.INFO, ConfigUtils.getInfoMessage("Plugin is using SQLite (default) database.", "Le plugin utilise une base de donnees SQLite (par defaut)."));
				}
				database.initialize();
			}
		});
	}
	
}
