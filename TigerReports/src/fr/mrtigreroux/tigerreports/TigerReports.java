package fr.mrtigreroux.tigerreports;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import fr.mrtigreroux.tigerreports.commands.ReportCommand;
import fr.mrtigreroux.tigerreports.commands.ReportsCommand;
import fr.mrtigreroux.tigerreports.data.config.ConfigFile;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.data.database.MySQL;
import fr.mrtigreroux.tigerreports.data.database.SQLite;
import fr.mrtigreroux.tigerreports.listeners.*;
import fr.mrtigreroux.tigerreports.managers.BungeeManager;
import fr.mrtigreroux.tigerreports.objects.Report;
import fr.mrtigreroux.tigerreports.objects.users.OnlineUser;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.runnables.ReportsNotifier;

/**
 * @author MrTigreroux
 * Finished on: 30/06/2016
 */

public class TigerReports extends JavaPlugin {

	private static TigerReports instance;
	private static Database database;
	private static BungeeManager bungeeManager;

	public static HashMap<String, User> Users = new HashMap<>();
	public static HashMap<String, String> LastNameFound = new HashMap<>();
	public static HashMap<String, String> LastUniqueIdFound = new HashMap<>();

	public static HashMap<Integer, Report> Reports = new HashMap<>();
	
	@Override
	public void onEnable() {
		instance = this;
		loadFiles();
		
		PluginManager pm = Bukkit.getServer().getPluginManager();
		pm.registerEvents(new InventoryListener(), this);
		pm.registerEvents(new SignListener(), this);
		pm.registerEvents(new PlayerListener(), this);
		
		getCommand("report").setExecutor(new ReportCommand());
		getCommand("reports").setExecutor(new ReportsCommand());
		
		try {
			Class.forName("fr.mrtigreroux.tigerreports.utils.UserUtils");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		ReportsNotifier.start();
		
		try {
			HttpURLConnection connection = (HttpURLConnection) new URL("http://www.spigotmc.org/api/general.php").openConnection();
			connection.setDoOutput(true);
			connection.setRequestMethod("POST");
			connection.getOutputStream().write(("key=98BE0FE67F88AB82B4C197FAF1DC3B69206EFDCC4D3B80FC83A00037510B99B4&resource=25773").getBytes("UTF-8"));
			String version = new BufferedReader(new InputStreamReader(connection.getInputStream())).readLine();
			if(!getDescription().getVersion().equals(version)) {
        		Bukkit.getLogger().log(Level.WARNING, "------------------------------------------------------");
        		Bukkit.getLogger().log(Level.WARNING, "[TigerReports] The plugin has been updated !");
        		Bukkit.getLogger().log(Level.WARNING, "New version "+version+" is now available on:");
        		Bukkit.getLogger().log(Level.WARNING, "https://www.spigotmc.org/resources/tigerreports.25773/");
        		Bukkit.getLogger().log(Level.WARNING, "------------------------------------------------------");
			}
		} catch (Exception noUpdate) {}
		
		if(getDescription().getAuthors().size() > 1 || !getDescription().getAuthors().contains("MrTigreroux")) {
    		Bukkit.getLogger().log(Level.SEVERE, "------------------------------------------------------");
    		Bukkit.getLogger().log(Level.SEVERE, "[TigerReports] An user tried to appropriate");
    		Bukkit.getLogger().log(Level.SEVERE, "the plugin TigerReports as his plugin.");
    		Bukkit.getLogger().log(Level.SEVERE, "------------------------------------------------------");
			Bukkit.shutdown();
		}

		bungeeManager = new BungeeManager(this);
		bungeeManager.initialize();
		
		initializeDatabase();
	}
	
	@Override
	public void onDisable() {
		database.closeConnection();
		for(User u : Users.values()) {
			if(u instanceof OnlineUser) {
				OnlineUser ou = (OnlineUser) u;
				if(ou.getOpenedMenu() != null) ou.getPlayer().closeInventory();
			}
		}
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
	
	public static void loadFiles() {
		for(ConfigFile configFiles : ConfigFile.values()) configFiles.load();
	}
	
	public static void initializeDatabase() {
		Bukkit.getScheduler().runTaskAsynchronously(instance, new Runnable() {
			@Override
			public void run() {
				try {
					MySQL mysql = new MySQL(ConfigFile.CONFIG.get().getString("MySQL.Host"), ConfigFile.CONFIG.get().getInt("MySQL.Port"), ConfigFile.CONFIG.get().getString("MySQL.Database"), ConfigFile.CONFIG.get().getString("MySQL.Username"), ConfigFile.CONFIG.get().getString("MySQL.Password"));
					mysql.check();
					database = mysql;
		    		Bukkit.getLogger().log(Level.INFO, "[TigerReports] Plugin is using MySQL database.");
				} catch (Exception invalidMySQL) {
					database = new SQLite();
		    		Bukkit.getLogger().log(Level.INFO, "[TigerReports] Plugin is using SQLite (default) database.");
				}
				database.initialize();
			}
		});
	}
	
}
