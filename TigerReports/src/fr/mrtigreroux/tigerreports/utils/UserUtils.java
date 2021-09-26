package fr.mrtigreroux.tigerreports.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.database.Database;

/**
 * @author MrTigreroux
 */

public class UserUtils {

	public static String getUniqueId(String name) {
		try {
			return Bukkit.getPlayer(name).getUniqueId().toString();
		} catch (Exception offlinePlayer) {
			return TigerReports.getInstance().getUsersManager().getUniqueId(name);
		}
	}

	public static String getName(String uuid) {
		UUID uniqueId = UUID.fromString(uuid);
		Player p = Bukkit.getPlayer(uniqueId);
		return p != null ? p.getName() : TigerReports.getInstance().getUsersManager().getName(uuid, uniqueId);
	}

	public static String getDisplayName(String uuid, boolean staff) {
		try {
			UUID uniqueId = UUID.fromString(uuid);
			Player p = Bukkit.getPlayer(uniqueId);
			OfflinePlayer offp = null;

			TigerReports tr = TigerReports.getInstance();
			String name = null;

			if (p != null) {
				name = tr.getVaultManager().getPlayerDisplayName(p, staff);
				offp = p;
			} else {
				if (uniqueId != null) {
					offp = Bukkit.getOfflinePlayer(uniqueId);
					if (offp != null) {
						name = tr.getVaultManager().getPlayerDisplayName(offp, staff);
					}
				}
			}
			return name != null ? name : tr.getUsersManager().getName(uuid, uniqueId, offp);
		} catch (Exception invalidUniqueId) {
			return uuid; // Allows to display old author display name of comments (now saved author is
			             // its uuid and not its display name)
		}
	}

	public static boolean checkPlayer(CommandSender s) {
		if (!(s instanceof Player)) {
			s.sendMessage(Message.PLAYER_ONLY.get());
			return false;
		} else {
			return true;
		}
	}

	public static Player getPlayerFromUniqueId(String uuid) {
		try {
			return Bukkit.getPlayer(UUID.fromString(uuid));
		} catch (Exception offlinePlayer) {
			return null;
		}
	}

	public static boolean isValid(String uuid, Database db) {
		return db.query("SELECT uuid FROM tigerreports_users WHERE uuid = ?", Collections.singletonList(uuid))
		        .getResult(0, "uuid") != null;
	}

	public static boolean isOnline(String name) {
		return Bukkit.getPlayer(name) != null ? true : TigerReports.getInstance().getBungeeManager().isOnline(name);
	}

	/**
	 * Return the players that player p can see (not vanished). Doesn't take in consideration the vanished players on a different server, who are therefore considered as online for the player p, because
	 * no official check can be used.
	 * 
	 * @param p            - Viewer of players
	 * @param hideExempted - Hide players owing tigerreports.report.exempt permission
	 */
	public static List<String> getOnlinePlayersForPlayer(Player p, boolean hideExempted) {
		TigerReports tr = TigerReports.getInstance();
		List<String> players = tr.getBungeeManager().getOnlinePlayers();

		if (players == null) {
			players = new ArrayList<>();
			for (Player plr : Bukkit.getOnlinePlayers()) {
				if (p.canSee(plr))
					players.add(plr.getName());
			}
		}

		if (hideExempted)
			players.removeAll(tr.getUsersManager().getExemptedPlayers());

		return players;
	}

}
