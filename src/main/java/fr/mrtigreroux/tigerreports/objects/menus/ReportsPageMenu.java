package fr.mrtigreroux.tigerreports.objects.menus;

import org.bukkit.inventory.Inventory;

import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.Permission;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.logs.Logger;
import fr.mrtigreroux.tigerreports.managers.ReportsManager;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.objects.CustomItem;
import fr.mrtigreroux.tigerreports.objects.reports.ReportsCharacteristics;
import fr.mrtigreroux.tigerreports.objects.reports.ReportsPage;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;

/**
 * @author MrTigreroux
 */
public abstract class ReportsPageMenu extends Menu implements ReportsPage.ReportsPageListener, UpdatedMenu {

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
		reportsPage = rm.getAndListenReportsPage(reportsCharacteristics, page, this, db, taskScheduler, um);
	}

	public abstract CustomItem getPageDisplayerItem();

	@Override
	public void onUpdate(Inventory inv) {
		inv.setItem(4,
		        getPageDisplayerItem().details(Message.PAGE_INFO.get().replace("_Page_", Integer.toString(page)))
		                .amount(page)
		                .create());
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

		reportsPage = rm.getAndListenReportsPage(oldReportsPage.characteristics.reportsCharacteristics, newPage, this,
		        db, taskScheduler, um);

		super.onPageChange(oldPage, newPage);
	}

	@Override
	public void onClose() {
		LOGGER.info(() -> this + ": onClose(): remove listener of page " + page);
		reportsPage.removeListener(this, rm);
		super.onClose();
	}

}
