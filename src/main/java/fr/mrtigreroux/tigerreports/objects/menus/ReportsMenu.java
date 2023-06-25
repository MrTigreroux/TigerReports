package fr.mrtigreroux.tigerreports.objects.menus;

import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.MenuItem;
import fr.mrtigreroux.tigerreports.data.constants.Permission;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.logs.Logger;
import fr.mrtigreroux.tigerreports.managers.BungeeManager;
import fr.mrtigreroux.tigerreports.managers.ReportsManager;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.managers.VaultManager;
import fr.mrtigreroux.tigerreports.objects.CustomItem;
import fr.mrtigreroux.tigerreports.objects.reports.ReportsCharacteristics;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;

/**
 * @author MrTigreroux
 */

public class ReportsMenu extends ReportsPageMenu implements UpdatedMenu {

	private static final Logger LOGGER = Logger.fromClass(ReportsMenu.class);

	private final VaultManager vm;
	private final BungeeManager bm;

	public ReportsMenu(User u, int page, ReportsManager rm, Database db, TaskScheduler taskScheduler, VaultManager vm,
	        BungeeManager bm, UsersManager um) {
		super(u, 54, page, Permission.STAFF, ReportsCharacteristics.CURRENT_REPORTS, rm, db, taskScheduler, um);
		this.vm = vm;
		this.bm = bm;
	}

	@Override
	public CustomItem getPageDisplayerItem() {
		return MenuItem.REPORTS.getCustomItem();
	}

	@Override
	public Inventory onOpen() {
		Inventory inv = getInventory(Message.REPORTS_TITLE.get(), true);

		if (u.hasPermission(Permission.STAFF_ARCHIVE)) {
			inv.setItem(8, MenuItem.ARCHIVED_REPORTS.getWithDetails(Message.ARCHIVED_REPORTS_DETAILS.get()));
		}

		return inv;
	}

	@Override
	public void onUpdate(Inventory inv) {
		super.onUpdate(inv);
		LOGGER.info(() -> this + ": onUpdate()");
		rm.fillInventoryWithReportsPage(inv, reportsPage, Message.REPORT_SHOW_ACTION.get(),
		        u.hasPermission(Permission.STAFF_ARCHIVE),
		        u.hasPermission(Permission.STAFF_DELETE) ? Message.REPORT_DELETE_ACTION.get() : "", vm, bm);
	}

	@Override
	public void onClick(ItemStack item, int slot, ClickType click) {
		if (slot == 8 && u.hasPermission(Permission.STAFF_ARCHIVE)) {
			u.openArchivedReportsMenu(1, true, rm, db, taskScheduler, vm, bm, um);
		} else if (slot >= 18 && slot <= size - 10) {
			int reportId = reportsPage.getReportIdAtIndex(slot - 18);

			if (reportId == -1) {
				update(false);
			} else {
				rm.getReportByIdAsynchronously(reportId, false, true, db, taskScheduler, um, (r) -> {
					if ((click == ClickType.RIGHT || click == ClickType.SHIFT_RIGHT) && u.canArchive(r)) {
						u.openConfirmationMenu(r, ConfirmationMenu.Action.ARCHIVE, rm, db, taskScheduler, vm, bm, um);
					} else if (click == ClickType.DROP && u.hasPermission(Permission.STAFF_DELETE)) {
						u.openConfirmationMenu(r, ConfirmationMenu.Action.DELETE, rm, db, taskScheduler, vm, bm, um);
					} else {
						u.openReportMenu(r, rm, db, taskScheduler, vm, bm, um);
					}
				});

			}
		}
	}

}
