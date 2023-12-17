package fr.mrtigreroux.tigerreports.tasks.runnables;

import fr.mrtigreroux.tigerreports.data.config.ConfigFile;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.logs.Logger;
import fr.mrtigreroux.tigerreports.managers.ReportsManager;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;

/**
 * @author MrTigreroux
 */

public class MenuUpdater implements Runnable {

	private static final Logger LOGGER = Logger.fromClass(MenuUpdater.class);

	private static final long USERS_DATA_UPDATE_INTERVAL = 15 * 60 * 1000;
	private static int taskId = -1;
	private long lastUsersDataUpdateTime = 0;
	private final ReportsManager rm;
	private final Database db;
	private final TaskScheduler taskScheduler;
	private final UsersManager um;

	public MenuUpdater(ReportsManager rm, Database db, TaskScheduler taskScheduler, UsersManager um) {
		super();
		this.rm = rm;
		this.db = db;
		this.taskScheduler = taskScheduler;
		this.um = um;
	}

	@Override
	public void run() {
		LOGGER.info(() -> "------------ MenuUpdater execution ------------");
		long now = System.currentTimeMillis();
		try {
			if (now - lastUsersDataUpdateTime > USERS_DATA_UPDATE_INTERVAL) {
				LOGGER.debug(() -> "update users manager");
				um.updateData(db, taskScheduler);
				lastUsersDataUpdateTime = now;
			}
		} catch (IllegalStateException underCooldown) {
			// Ignored
		}

		try {
			LOGGER.debug(() -> "update reports manager");
			boolean updateWasNeeded = rm.updateData(db, taskScheduler, um);

			if (!updateWasNeeded) {
				stop(taskScheduler);
			}
		} catch (IllegalStateException underCooldown) {
			// Ignored
		}
		LOGGER.info(() -> "---------- MenuUpdater execution end ----------");
	}

	public static void startIfNeeded(ReportsManager rm, Database db, TaskScheduler taskScheduler, UsersManager um) {
		if (taskId != -1) {
			return; // Already started.
		}
		LOGGER.info(() -> "-------------- MenuUpdater start --------------");

		long interval = ConfigFile.CONFIG.get().getInt("Config.MenuUpdatesInterval", 10) * 1000L;
		if (interval > 0) {
			taskId = taskScheduler.runTaskRepeatedly(interval, interval, new MenuUpdater(rm, db, taskScheduler, um));
		}
	}

	public static void stop(TaskScheduler taskScheduler) {
		if (taskId != -1) {
			LOGGER.info(() -> "-------------- MenuUpdater stop ---------------");
			taskScheduler.cancelTask(taskId);
			taskId = -1;
		}
	}

}
