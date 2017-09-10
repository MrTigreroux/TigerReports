package fr.mrtigreroux.tigerreports.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;


import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.objects.users.*;

/**
 * @author MrTigreroux
 */

public class UserUtils {

	@SuppressWarnings("deprecation")
	public static String getUniqueId(String name) {
		try {
			return Bukkit.getPlayerExact(name).getUniqueId().toString();
		} catch (Exception offlinePlayer) {
			try {
				String uuid = ((uuid = TigerReports.LastUniqueIdFound.get(name)) != null ? uuid : Bukkit.getOfflinePlayer(name).getUniqueId().toString());
				if(uuid != null) TigerReports.LastUniqueIdFound.put(name, uuid);
				return uuid;
			} catch (Exception invalidPlayer) {
				Bukkit.getLogger().log(Level.WARNING, ConfigUtils.getInfoMessage("UUID of pseudo <"+name+"> not found.", "L'UUID du pseudo <"+name+"> n'a pas ete trouve."));
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
				String name = ((name = TigerReports.LastNameFound.get(uuid)) != null ? name : Bukkit.getOfflinePlayer(uniqueId).getName());
				if(name == null) {
					try {
						name = (String) TigerReports.getDb().query("SELECT name FROM users WHERE uuid = ?", Arrays.asList(uuid)).getResult(0, "name");
					} catch (Exception nameNotFound) {}
				}
				if(name != null) {
					TigerReports.LastNameFound.put(uuid, name);
					return name;
				}
			}
			Bukkit.getLogger().log(Level.WARNING, ConfigUtils.getInfoMessage("Pseudo of UUID <"+uuid+"> not found.", "Le pseudo de l'UUID <"+uuid+"> n'a pas ete trouve."));
			return null;
		}
	}
	
	public static boolean checkPlayer(CommandSender s) {
		if(!(s instanceof Player)) {
			s.sendMessage(Message.PLAYER_ONLY.get());
			return false;
		} else return true;
	}
	
	public static Player getPlayer(String name) {
		try {
			return Bukkit.getPlayerExact(name);
		} catch (Exception offlinePlayer) {
			return null;
		}
	}
	
	public static boolean isValid(String uuid) {
		return TigerReports.getDb().query("SELECT uuid FROM users WHERE uuid = ?", Collections.singletonList(uuid)).getResult(0, "uuid") != null;
	}
	
	public static boolean isOnline(String name) {
		return getPlayer(name) != null;
	}
	
	public static User getUser(String uuid) {
		if(uuid == null) return null;
		User u = TigerReports.Users.get(uuid);
		if(u == null) {
			try {
				u = new OnlineUser(Bukkit.getPlayer(UUID.fromString(uuid)));
			} catch (Exception offlinePlayer) {
				u = new OfflineUser(uuid);
			}
			u.save();
		}
		return u;
	}
	
	public static OnlineUser getOnlineUser(Player p) {
		User u = TigerReports.Users.get(p.getUniqueId().toString());
		if(u == null || !(u instanceof OnlineUser)) {
			u = new OnlineUser(p);
			u.save();
		}
		return (OnlineUser) u;
	}
	
	public static List<String> getOnlinePlayers(String ignored) {
		List<String> players = new ArrayList<>();
		for(Player p : Bukkit.getOnlinePlayers()) {
			String name = p.getName();
			if(!name.equals(ignored)) players.add(name);
		}
		return players;
	}
	
}
