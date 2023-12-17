package fr.mrtigreroux.tigerreports.bungee.notifications;

import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;

import fr.mrtigreroux.tigerreports.bungee.BungeeManager;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.managers.ReportsManager;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.managers.VaultManager;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.objects.users.User.SavedMessage;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;
import fr.mrtigreroux.tigerreports.utils.CheckUtils;

/**
 * @author MrTigreroux
 */
public class PlayerOnlineBungeeNotification extends BungeeNotification {

	public final String playerName;
	public final String playerUniqueId;
	public final boolean online;
	public final String serverName;
	public final String lastMessagesStartDatetime;

	public PlayerOnlineBungeeNotification(long creationTime, String playerName, UUID playerUUID, boolean online, String serverName, String lastMessagesStartDatetime) {
		this(creationTime, playerName, playerUUID.toString(), online, serverName, lastMessagesStartDatetime);
	}

	public PlayerOnlineBungeeNotification(long creationTime, String playerName, String playerUniqueId, boolean online, String serverName, String lastMessagesStartDatetime) {
		super(creationTime);
		this.playerName = CheckUtils.notEmpty(playerName);
		this.playerUniqueId = CheckUtils.notEmpty(playerUniqueId);
		this.online = online;
		this.serverName = serverName;
		this.lastMessagesStartDatetime = lastMessagesStartDatetime;
	}

	@Override
	public boolean isEphemeral() {
		return true;
	}

	@Override
	public void onReceive(Database db, TaskScheduler ts, UsersManager um, ReportsManager rm, VaultManager vm, BungeeManager bm) {
		bm.setServerLastNotificationTime(serverName, creationTime);

		if (isNotifiable(bm) && Bukkit.getPlayer(playerName) == null) { // prevents changing local online status of an online player on the current server
			UUID playerUUID = UUID.fromString(playerUniqueId);
			bm.setPlayerOnlineLocally(playerName, online, serverName);

			if (online && bm.isValidDifferentServerName(serverName)) {
				User cachedUser = um.getCachedUser(playerUUID);
				if (cachedUser != null) {
					List<SavedMessage> lastMessages = cachedUser.getLastMessagesAfterDatetime(lastMessagesStartDatetime);
					if (CheckUtils.isNotEmpty(lastMessages)) {
						bm.sendPluginNotificationToServer(serverName, new PlayerLastMessagesBungeeNotification(bm.getNetworkCurrentTime(), playerUUID, lastMessages));
					}
				}
			}
		} // else ignore notif because player is online in current server or because notif is too old (probably was sent during a period of time where there were no online player in the current server, after that period, a player joined, triggered the collection of all online players connected in the network and allowed the reception of that current notif that can be ignored because pointless)
	}

}
