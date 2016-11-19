package fr.mrtigreroux.tigerreports.managers;

import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.listeners.InventoryListener;
import fr.mrtigreroux.tigerreports.listeners.PlayerListener;
import fr.mrtigreroux.tigerreports.listeners.SignListener;

/**
 * @author MrTigreroux
 */

public class ListenersManager {

	public static TigerReports main = TigerReports.getInstance();
	
	public static void registerListeners() {
		PluginManager pm = Bukkit.getServer().getPluginManager();
		pm.registerEvents(new InventoryListener(), main);
		pm.registerEvents(new SignListener(), main);
		pm.registerEvents(new PlayerListener(), main);
	}
	
}
