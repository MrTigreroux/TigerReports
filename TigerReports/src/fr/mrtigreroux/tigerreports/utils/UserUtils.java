package fr.mrtigreroux.tigerreports.utils;

import java.util.Arrays;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.mrtigreroux.tigerreports.data.Message;
import fr.mrtigreroux.tigerreports.managers.FilesManager;

/**
 * @author MrTigreroux
 */

public class UserUtils {

	@SuppressWarnings("deprecation")
	public static String getUniqueId(String name) {
		UUID uuid = null;
		String uuidString = null;
		try {
			uuid = Bukkit.getPlayerExact(name).getUniqueId();
			uuidString = uuid.toString();
		} catch(Exception offlinePlayer) {
			try {
				uuid = Bukkit.getOfflinePlayer(name).getUniqueId();
				uuidString = uuid.toString();
			} catch(Exception invalidPlayer) {
				Bukkit.getLogger().log(Level.WARNING, "TigerReports > L'UUID du pseudo <"+name+"> est introuvable.");
				return null;
			}
		}
		FilesManager.getData.set("Data."+uuid+".Name", name);
		FilesManager.saveData();
		return uuidString;
	}
	
	public static String getName(String uuid) {
		try {
			return Bukkit.getPlayer(UUID.fromString(uuid)).getName();
		} catch(Exception offlinePlayer) {
			for(String name : Arrays.asList(Bukkit.getOfflinePlayer(UUID.fromString(uuid)).getName(), FilesManager.getData.getString("Data."+uuid+".Name"))) if(name != null) return name;
			Bukkit.getLogger().log(Level.WARNING, "TigerReports > Le pseudo de l'UUID <"+uuid+"> est introuvable.");
			return null;
		}
	}
	
	public static boolean hasPermission(CommandSender s, String permission) {
		if(!s.hasPermission("tigerreports."+permission)) {
			s.sendMessage(Message.PERMISSION_COMMAND.get());
			if(s instanceof Player) {
				Player p = (Player) s;
				p.playSound(p.getLocation(), ConfigUtils.getErrorSound(), 1, 1);
			}
			return false;
		} else return true;
	}
	
	public static boolean isPlayer(CommandSender s) {
		if(!(s instanceof Player)) {
			s.sendMessage(Message.PLAYER_ONLY.get());
			return false;
		} else return true;
	}
	
	public static Player getPlayer(String name) {
		try {
			Player p = Bukkit.getPlayerExact(name);
			FilesManager.getData.set("Data."+p.getUniqueId()+".Name", p.getName());
			FilesManager.saveData();
			return p;
		} catch (Exception offlinePlayer) {
			return null;
		}
	}
	
	public static boolean isOnline(String name) {
		return getPlayer(name) != null;
	}
	
	public static int getAppreciation(String uuid, String appreciation) {
		return FilesManager.getData.get("Data."+uuid+".Appreciations."+appreciation) != null ? FilesManager.getData.getInt("Data."+uuid+".Appreciations."+appreciation) : 0;
	}
	
	public static void addAppreciation(String uuid, String appreciation, int value) {
		int score = getAppreciation(uuid, appreciation)+value;
		FilesManager.getData.set("Data."+uuid+".Appreciations."+appreciation, score > 0 ? score : 0);
		FilesManager.saveData();
	}
	
}
