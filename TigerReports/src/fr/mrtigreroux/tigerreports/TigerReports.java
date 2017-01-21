package fr.mrtigreroux.tigerreports;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import fr.mrtigreroux.tigerreports.commands.ReportCommand;
import fr.mrtigreroux.tigerreports.commands.ReportsCommand;
import fr.mrtigreroux.tigerreports.data.ConfigFile;
import fr.mrtigreroux.tigerreports.listeners.InventoryListener;
import fr.mrtigreroux.tigerreports.listeners.PlayerListener;
import fr.mrtigreroux.tigerreports.listeners.SignListener;
import fr.mrtigreroux.tigerreports.runnables.ReportsNotifier;
import fr.mrtigreroux.tigerreports.utils.UserUtils;

/**
 * @author MrTigreroux
 * Finished on: 30/06/2016
 */

public class TigerReports extends JavaPlugin {

	public static TigerReports instance;
	
	@Override
	public void onEnable() {
		instance = this;
		loadFiles();
		
		PluginManager pm = Bukkit.getServer().getPluginManager();
		pm.registerEvents(new InventoryListener(), this);
		pm.registerEvents(new SignListener(), this);
		pm.registerEvents(new PlayerListener(), this);
		
		this.getCommand("report").setExecutor(new ReportCommand());
		this.getCommand("reports").setExecutor(new ReportsCommand());
		
		for(Player p : Bukkit.getOnlinePlayers()) ConfigFile.DATA.get().set("Data."+p.getUniqueId()+".Name", p.getName());
		ConfigFile.DATA.save();
		
		ReportsNotifier.start();
		
		try {
			HttpURLConnection con = (HttpURLConnection) new URL("http://www.spigotmc.org/api/general.php").openConnection();
			con.setDoOutput(true);
			con.setRequestMethod("POST");
			con.getOutputStream().write(("key=98BE0FE67F88AB82B4C197FAF1DC3B69206EFDCC4D3B80FC83A00037510B99B4&resource=25773").getBytes("UTF-8"));
			String version = new BufferedReader(new InputStreamReader(con.getInputStream())).readLine();
			if(!this.getDescription().getVersion().equals(version)) {
        		Bukkit.getLogger().log(Level.WARNING, "------------------------------------------------------");
        		Bukkit.getLogger().log(Level.WARNING, "[TigerReports] The plugin has been updated !");
        		Bukkit.getLogger().log(Level.WARNING, "New version "+version+" is now available on:");
        		Bukkit.getLogger().log(Level.WARNING, "https://www.spigotmc.org/resources/tigerreports.25773/");
        		Bukkit.getLogger().log(Level.WARNING, "------------------------------------------------------");
			}
		} catch (Exception noUpdate) {
			;
		}
		
		if(this.getDescription().getAuthors().size() > 1 || !this.getDescription().getAuthors().contains("MrTigreroux")) {
    		Bukkit.getLogger().log(Level.WARNING, "------------------------------------------------------");
    		Bukkit.getLogger().log(Level.WARNING, "[TigerReports] An user tried to appropriate");
    		Bukkit.getLogger().log(Level.WARNING, "the plugin TigerReports as his plugin.");
    		Bukkit.getLogger().log(Level.WARNING, "------------------------------------------------------");
			Bukkit.shutdown();
		}
	}
	
	@Override
	public void onDisable() {
		for(Player p : Bukkit.getOnlinePlayers()) if(UserUtils.getUser(p).getOpenedMenu() != null) p.closeInventory();
	}
	
	public static TigerReports getInstance() {
		return instance;
	}
	
	public static void loadFiles() {
		for(ConfigFile configFiles : ConfigFile.values()) configFiles.load();
	}
	
}
