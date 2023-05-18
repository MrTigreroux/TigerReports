package fr.mrtigreroux.tigerreports.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import fr.mrtigreroux.tigerreports.data.config.ConfigFile;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.logs.Logger;
import fr.mrtigreroux.tigerreports.managers.BungeeManager;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.managers.VaultManager;
import fr.mrtigreroux.tigerreports.objects.users.User;

/**
 * @author MrTigreroux
 */

public class UserUtils {

	private static final Logger LOGGER = Logger.fromClass(UserUtils.class);

	private UserUtils() {}

	public static UUID getOnlinePlayerUniqueId(String name) {
		try {
			return Bukkit.getPlayer(name).getUniqueId();
		} catch (Exception offlinePlayer) {
			return null;
		}
	}

	public static boolean useDisplayName(boolean staff) {
		FileConfiguration configFile = ConfigFile.CONFIG.get();
		boolean displayForStaff = ConfigUtils.isEnabled(configFile, "Config.DisplayNameForStaff");
		boolean displayForPlayers = ConfigUtils.isEnabled(configFile, "Config.DisplayNameForPlayers");
		LOGGER.debug(() -> "useDisplayName(" + staff + "): displayForStaff = " + displayForStaff
		        + ", displayForPlayers = " + displayForPlayers);
		return (staff && displayForStaff) || (!staff && displayForPlayers);
	}

//	public interface NamesResultCallback {
//		void onNamesReceived(String[] names);
//	}

//	// TODO Unused
//	public static void getNamesAsynchronously(String[] uuids, Database db, TaskScheduler taskScheduler, UsersManager um,
//	        NamesResultCallback resultCallback) {
//		String[] names = new String[uuids.length];
//		getNamesAsynchronously(uuids, 0, names, db, taskScheduler, um, resultCallback);
//	}
//
//	// TODO Unused
//	private static void getNamesAsynchronously(String[] uuids, int index, String[] names, Database db,
//	        TaskScheduler taskScheduler, UsersManager um, NamesResultCallback resultCallback) {
//		if (index < uuids.length) {
//			getNameAsynchronously(uuids[index], db, taskScheduler, um, new UsersManager.NameResultCallback() {
//
//				@Override
//				public void onNameReceived(String name) {
//					names[index] = name;
//					getNamesAsynchronously(uuids, index + 1, names, db, taskScheduler, um, resultCallback);
//				}
//
//			});
//		} else {
//			resultCallback.onNamesReceived(names);
//		}
//	}

//	// TODO Unused
//	public static void getNameAsynchronously(String uuid, Database db, TaskScheduler taskScheduler, UsersManager um,
//	        UsersManager.NameResultCallback resultCallback) {
//		getNameAsynchronously(UUID.fromString(uuid), db, taskScheduler, um, resultCallback);
//	}

//	// TODO Unused
//	public static void getNameAsynchronously(UUID uuid, Database db, TaskScheduler taskScheduler, UsersManager um,
//	        UsersManager.NameResultCallback resultCallback) {
//		Player p = Bukkit.getPlayer(uuid);
//		if (p != null) {
//			resultCallback.onNameReceived(p.getName());
//		} else {
//			um.getNameAsynchronously(uuid, db, taskScheduler, resultCallback);
//		}
//	}

//	// TODO Unused
//	public static void getDisplayNameAsynchronously(String uuid, boolean staff, Database db,
//	        DisplayNameResultCallback resultCallback) {
//		try {
//			UUID uniqueId = UUID.fromString(uuid);
//			Player p = Bukkit.getPlayer(uniqueId);
//			TigerReports tr = TigerReports.getInstance();
//			getDisplayNameAsynchronously(uniqueId, p, staff, db, tr, tr.getVaultManager(), tr.getUsersManager(),
//			        resultCallback);
//		} catch (IllegalArgumentException invalidUniqueId) {
//			resultCallback.onDisplayNameReceived(uuid); // Allows to display old author display name of comments (now saved author is
//			// its uuid and not its display name)
//		}
//	}

//	public interface DisplayNameResultCallback {
//		void onDisplayNameReceived(String displayName);
//	}

//	// TODO Unused
//	public static void getDisplayNameAsynchronously(UUID uniqueId, Player p, boolean staff, Database db,
//	        TaskScheduler taskScheduler, VaultManager vm, UsersManager um, DisplayNameResultCallback resultCallback) {
//		Objects.requireNonNull(uniqueId);
//
//		OfflinePlayer offp = p != null ? p : Bukkit.getOfflinePlayer(uniqueId);
//
//		vm.getPlayerDisplayNameAsynchronously(offp, staff, taskScheduler, new VaultManager.DisplayNameResultCallback() {
//
//			@Override
//			public void onDisplayNameReceived(String displayName) {
//				if (displayName != null) {
//					resultCallback.onDisplayNameReceived(displayName);
//				} else {
//					um.getNameAsynchronously(uniqueId, offp, db, taskScheduler, new UsersManager.NameResultCallback() {
//
//						@Override
//						public void onNameReceived(String name) {
//							resultCallback.onDisplayNameReceived(name);
//						}
//
//					});
//				}
//			}
//
//		});
//	}

	public static boolean checkPlayer(CommandSender s) {
		if (!(s instanceof Player)) {
			s.sendMessage(Message.PLAYER_ONLY.get());
			return false;
		} else {
			return true;
		}
	}

	public static Player getPlayerFromUniqueId(UUID uuid) {
		try {
			return Bukkit.getPlayer(uuid);
		} catch (Exception offlinePlayer) {
			return null;
		}
	}

//	// TODO Unused
//	public static void checkUserExistsAsynchronously(UUID uuid, Database db, TaskScheduler taskScheduler,
//	        ResultCallback<Boolean> resultCallback) {
//		checkUserExistsAsynchronously(uuid, Bukkit.getPlayer(uuid), db, taskScheduler, resultCallback);
//	}

//	// TODO Unused
//	public static void checkUserExistsAsynchronously(UUID uuid, Player p, Database db, TaskScheduler taskScheduler,
//	        ResultCallback<Boolean> resultCallback) {
//		if (p != null) {
//			resultCallback.onResultReceived(true);
//			db.updateUserName(p.getUniqueId().toString(), p.getName());
//		} else {
//			db.queryAsynchronously("SELECT uuid FROM tigerreports_users WHERE uuid = ?",
//			        Collections.singletonList(uuid.toString()), taskScheduler, new ResultCallback<QueryResult>() {
//
//				        @Override
//				        public void onResultReceived(QueryResult qr) {
//					        Object o = qr.getResult(0, "uuid");
//					        resultCallback.onResultReceived(o != null);
//				        }
//
//			        });
//		}
//	}

	/**
	 * Returns the players that player p can see (not vanished). Doesn't take in consideration vanished players on a different server, who are therefore considered as online for the player p, because no
	 * official check can be used.
	 * 
	 * @param p            - Viewer of players
	 * @param hideExempted - Hide players owning tigerreports.report.exempt permission
	 */
	public static List<String> getOnlinePlayersForPlayer(Player p, boolean hideExempted, UsersManager um,
	        BungeeManager bm) {
		List<String> players = bm.getOnlinePlayers();

		if (players == null) {
			players = new ArrayList<>();
			for (Player plr : Bukkit.getOnlinePlayers()) {
				if (p.canSee(plr)) {
					players.add(plr.getName());
				}
			}
		}

		if (hideExempted) {
			players.removeAll(um.getExemptedPlayers());
		}

		return players;
	}

	public static String getStaffDisplayName(User u, VaultManager vm) {
		String name;
		if (u == null) {
			name = Message.NOT_FOUND_MALE.get();
		} else {
			name = u.getDisplayName(vm, true);
		}
		return name;
	}

}
