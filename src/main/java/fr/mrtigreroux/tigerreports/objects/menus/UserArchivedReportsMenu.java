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

public class UserArchivedReportsMenu extends UserReportsPageMenu {

	public UserArchivedReportsMenu(User u, int page, User tu, ReportsManager rm, Database db,
	        TaskScheduler taskScheduler, VaultManager vm, BungeeManager bm, UsersManager um) {
		super("Menus.User-archived-reports", u, page, tu, new ReportsCharacteristics(tu.getUniqueId(), null, true), rm,
		        db, taskScheduler, vm, bm, um);
	}

}
