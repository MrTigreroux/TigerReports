package fr.mrtigreroux.tigerreports.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.mrtigreroux.tigerreports.data.ConfigFile;
import fr.mrtigreroux.tigerreports.data.ConfigSound;
import fr.mrtigreroux.tigerreports.data.Message;
import fr.mrtigreroux.tigerreports.objects.User;

/**
 * @author MrTigreroux
 */

public class UserUtils {
	
	public static HashMap<UUID, User> Users = new HashMap<>();
	public static HashMap<String, Long> LastTimeReported = new HashMap<>();
	public static HashMap<UUID, String> LastNameFound = new HashMap<>();
	public static HashMap<String, String> LastUniqueIdFound = new HashMap<>();

	@SuppressWarnings("deprecation")
	public static String getUniqueId(String name) {
		try {
			return Bukkit.getPlayerExact(name).getUniqueId().toString();
		} catch (Exception offlinePlayer) {
			try {
				String uuid = ((uuid = LastUniqueIdFound.get(name)) != null ? uuid : Bukkit.getOfflinePlayer(name).getUniqueId().toString());
				if(uuid != null) LastUniqueIdFound.put(name, uuid);
				return uuid;
			} catch (Exception invalidPlayer) {
				Bukkit.getLogger().log(Level.WARNING, "[TigerReports] UUID of pseudo <"+name+"> not found.");
				return null;
			}
		}
	}
	
	public static String getName(String uuid) {
		UUID uniqueId = UUID.fromString(uuid);
		try {
			return Bukkit.getPlayer(uniqueId).getName();
		} catch (Exception offlinePlayer) {
			if(uniqueId != null) {
				String name = ((name = LastNameFound.get(uniqueId)) != null ? name : ((name = ConfigFile.DATA.get().getString("Data."+uuid+".Name")) != null ? name : ((name = Bukkit.getOfflinePlayer(uniqueId).getName()))));
				if(name != null) {
					LastNameFound.put(uniqueId, name);
					return name;
				}
			}
			Bukkit.getLogger().log(Level.WARNING, "[TigerReports] Pseudo of UUID <"+uuid+"> not found.");
			return null;
		}
	}
	
	public static boolean checkPermission(CommandSender s, String permission) {
		if(!s.hasPermission("tigerreports."+permission)) {
			s.sendMessage(Message.PERMISSION_COMMAND.get());
			if(s instanceof Player) {
				Player p = (Player) s;
				p.playSound(p.getLocation(), ConfigSound.ERROR.get(), 1, 1);
			}
			return false;
		} else return true;
	}
	
	public static boolean checkPlayer(CommandSender s) {
		if(!(s instanceof Player)) {
			s.sendMessage(Message.PLAYER_ONLY.get());
			return false;
		} else return true;
	}
	
	public static Player getPlayer(String name) {
		try {
			Player p = Bukkit.getPlayerExact(name);
			ConfigFile.DATA.get().set("Data."+p.getUniqueId()+".Name", p.getName());
			ConfigFile.DATA.save();
			return p;
		} catch (Exception offlinePlayer) {
			return null;
		}
	}
	
	public static boolean isValid(String uuid) {
		return ConfigFile.DATA.get().get("Data."+uuid+".Name") != null;
	}
	
	public static boolean isOnline(String name) {
		return getPlayer(name) != null;
	}

	public static int getStat(String uuid, String stat) {
		return ConfigFile.DATA.get().get("Data."+uuid+".Statistics."+stat) != null ? ConfigFile.DATA.get().getInt("Data."+uuid+".Statistics."+stat) : 0;
	}
	
	public static void changeStat(String uuid, String stat, int value) {
		int score = getStat(uuid, stat)+value;
		ConfigFile.DATA.get().set("Data."+uuid+".Statistics."+stat, score > 0 ? score : 0);
		ConfigFile.DATA.save();
	}
	
	public static List<String> getNotifications(String uuid) {
		return ConfigFile.DATA.get().get("Data."+uuid+".Notifications") != null ? ConfigFile.DATA.get().getStringList("Data."+uuid+".Notifications") : new ArrayList<String>();
	}
	
	public static void setNotifications(String uuid, List<String> notifications) {
		ConfigFile.DATA.get().set("Data."+uuid+".Notifications", notifications);
		ConfigFile.DATA.save();
	}
	
	public static User getUser(Player p) {
		UUID uuid = p.getUniqueId();
		User u = Users.get(uuid);
		if(u == null) {
			u = new User(p);
			Users.put(uuid, u);
		}
		return u;
	}
	
}
