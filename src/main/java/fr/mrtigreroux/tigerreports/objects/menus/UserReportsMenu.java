package fr.mrtigreroux.tigerreports.objects.menus;

import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.managers.BungeeManager;
import fr.mrtigreroux.tigerreports.managers.ReportsManager;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.managers.VaultManager;
import fr.mrtigreroux.tigerreports.objects.reports.ReportsCharacteristics;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;

/**
 * @author MrTigreroux
 */

public class UserReportsMenu extends UserReportsPageMenu {

	public UserReportsMenu(User u, int page, User tu, ReportsManager rm, Database db, TaskScheduler taskScheduler,
	        VaultManager vm, BungeeManager bm, UsersManager um) {
		super("Menus.User-reports", u, page, tu, new ReportsCharacteristics(tu.getUniqueId(), null, false), rm, db,
		        taskScheduler, vm, bm, um);
	}

}
