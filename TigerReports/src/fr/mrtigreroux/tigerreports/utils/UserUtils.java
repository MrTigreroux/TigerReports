package fr.mrtigreroux.tigerreports.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.data.config.Message;

/**
 * @author MrTigreroux
 */

public class UserUtils {

	public static String getUniqueId(String name) {
		try {
			return Bukkit.getPlayerExact(name).getUniqueId().toString();
		} catch (Exception offlinePlayer) {
			return TigerReports.getInstance().getUsersManager().getUniqueId(name);
		}
	}
	
	public static String getName(String uuid) {
		UUID uniqueId = UUID.fromString(uuid);
		try {
			return Bukkit.getPlayer(uniqueId).getName();
		} catch (Exception offlinePlayer) {
			return TigerReports.getInstance().getUsersManager().getName(uuid, uniqueId);
		}
	}
	
	public static boolean checkPlayer(CommandSender s) {
		if(!(s instanceof Player)) {
			s.sendMessage(Message.PLAYER_ONLY.get());
			return false;
		} else {
			return true;
		}
	}
	
	public static Player getPlayer(String name) {
		try {
			return Bukkit.getPlayerExact(name);
		} catch (Exception offlinePlayer) {
			return null;
		}
	}
	
	public static boolean isValid(String uuid) {
		return TigerReports.getInstance().getDb().query("SELECT uuid FROM tigerreports_users WHERE uuid = ?", Collections.singletonList(uuid)).getResult(0, "uuid") != null;
	}
	
	public static boolean isOnline(String name) {
		return getPlayer(name) != null;
	}
	
	public static List<String> getOnlinePlayers() {
		List<String> players = new ArrayList<>();
		for(Player p : Bukkit.getOnlinePlayers())
			players.add(p.getName());
		return players;
	}
	
}
