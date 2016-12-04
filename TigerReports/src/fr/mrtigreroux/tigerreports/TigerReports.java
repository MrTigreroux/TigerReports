package fr.mrtigreroux.tigerreports;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import fr.mrtigreroux.tigerreports.data.ConfigFile;
import fr.mrtigreroux.tigerreports.managers.CommandsManager;
import fr.mrtigreroux.tigerreports.managers.FilesManager;
import fr.mrtigreroux.tigerreports.managers.ListenersManager;
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
		FilesManager.loadFiles();
		CommandsManager.registerCommands();
		ListenersManager.registerListeners();
		for(Player p : Bukkit.getOnlinePlayers()) ConfigFile.DATA.get().set("Data."+p.getUniqueId()+".Name", p.getName());
		ConfigFile.DATA.save();
	}
	
	@Override
	public void onDisable() {
		for(Player p : Bukkit.getOnlinePlayers()) if(UserUtils.getUser(p).getOpenedMenu() != null) p.closeInventory();
	}
	
	public static TigerReports getInstance() {
		return instance;
	}
	
}
