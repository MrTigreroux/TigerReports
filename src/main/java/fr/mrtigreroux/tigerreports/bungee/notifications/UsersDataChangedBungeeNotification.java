package fr.mrtigreroux.tigerreports.bungee.notifications;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import fr.mrtigreroux.tigerreports.bungee.BungeeManager;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.managers.ReportsManager;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.managers.VaultManager;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;
import fr.mrtigreroux.tigerreports.utils.CheckUtils;

/**
 * @author MrTigreroux
 */
public class UsersDataChangedBungeeNotification extends BungeeNotification {

	public final String[] usersUniqueId;

	public UsersDataChangedBungeeNotification(long creationTime, String... usersUniqueId) {
		super(creationTime);
		this.usersUniqueId = CheckUtils.notEmpty(usersUniqueId);
	}

	@Override
	public boolean isEphemeral() {
		return true;
	}

	@Override
	public void onReceive(Database db, TaskScheduler ts, UsersManager um, ReportsManager rm, VaultManager vm,
	        BungeeManager bm) {
		long remainingDelay = RECENT_MAX_DELAY - getElapsedTime(bm);
		if (remainingDelay > 0L) {
			// Wait for the database to be updated
			ts.runTaskDelayedly(remainingDelay, () -> {
				updateUsersData(db, ts, um);
			});
		} else {
			updateUsersData(db, ts, um);
		}
	}

	private void updateUsersData(Database db, TaskScheduler ts, UsersManager um) {
		List<UUID> usersUUID = new ArrayList<>();
		for (int i = 0; i < usersUniqueId.length; i++) {
			try {
				usersUUID.add(UUID.fromString(usersUniqueId[i]));
			} catch (IllegalArgumentException ignored) {}
		}
		um.updateDataOfUsersWhenPossible(usersUUID, db, ts);
	}

}
