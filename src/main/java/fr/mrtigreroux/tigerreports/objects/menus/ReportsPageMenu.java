package fr.mrtigreroux.tigerreports.objects.menus;

import fr.mrtigreroux.tigerreports.data.constants.Permission;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.logs.Logger;
import fr.mrtigreroux.tigerreports.managers.ReportsManager;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.objects.reports.ReportsCharacteristics;
import fr.mrtigreroux.tigerreports.objects.reports.ReportsPage;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;

/**
 * @author MrTigreroux
 */
public abstract class ReportsPageMenu extends Menu implements ReportsPage.ReportsPageListener {

	private static final Logger LOGGER = Logger.fromClass(ReportsPageMenu.class);

	protected final ReportsManager rm;
	protected final Database db;
	protected final TaskScheduler taskScheduler;
	protected final UsersManager um;
	protected ReportsPage reportsPage;

	public ReportsPageMenu(User u, int size, int page, Permission permission,
	        ReportsCharacteristics reportsCharacteristics, ReportsManager rm, Database db, TaskScheduler taskScheduler,
	        UsersManager um) {
		super(u, size, page, permission);
		this.rm = rm;
		this.db = db;
		this.taskScheduler = taskScheduler;
		this.um = um;
		reportsPage = rm.getAndListenReportsPage(reportsCharacteristics, page, true, this, db, taskScheduler, um);
	}

	@Override
	public void onReportsPageChange(int page) {
		if (page == this.page) { // Could be an event fired before page change.
			LOGGER.info(() -> this + ": onReportsPageChanged(" + page + ")");
			update(false);
		}
	}

	@Override
	public void onPageChange(int oldPage, int newPage) {
		LOGGER.info(() -> this + ": onPageChange(" + oldPage + ", " + newPage + "): remove listener of page " + page
		        + ", and listen to page " + newPage);
		ReportsPage oldReportsPage = reportsPage;
		oldReportsPage.removeListener(this, rm);

		reportsPage = rm.getAndListenReportsPage(oldReportsPage.characteristics.reportsCharacteristics, newPage, true,
		        this, db, taskScheduler, um);

		super.onPageChange(oldPage, newPage);
	}

	@Override
	public void onClose() {
		LOGGER.info(() -> this + ": onClose(): remove listener of page " + page);
		reportsPage.removeListener(this, rm);
		super.onClose();
	}

}
