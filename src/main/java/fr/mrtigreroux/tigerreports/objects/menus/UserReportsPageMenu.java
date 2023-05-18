package fr.mrtigreroux.tigerreports.objects.menus;

import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.Permission;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.managers.BungeeManager;
import fr.mrtigreroux.tigerreports.managers.ReportsManager;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.managers.VaultManager;
import fr.mrtigreroux.tigerreports.objects.CustomItem;
import fr.mrtigreroux.tigerreports.objects.reports.Report;
import fr.mrtigreroux.tigerreports.objects.reports.ReportsCharacteristics;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.tasks.ResultCallback;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;

/**
 * @author MrTigreroux
 */

public abstract class UserReportsPageMenu extends ReportsPageMenu implements UpdatedMenu {

	final User tu;
	private final String tag;
	protected final VaultManager vm;
	protected final BungeeManager bm;

	public UserReportsPageMenu(String tag, User u, int page, User tu, ReportsCharacteristics reportsCharacteristics,
	        ReportsManager rm, Database db, TaskScheduler taskScheduler, VaultManager vm, BungeeManager bm,
	        UsersManager um) {
		super(u, 54, page, Permission.STAFF, reportsCharacteristics, rm, db, taskScheduler, um);
		this.tag = tag;
		this.tu = tu;
		this.vm = vm;
		this.bm = bm;
	}

	@Override
	public CustomItem getPageDisplayerItem() {
		return new CustomItem().type(Material.BOOKSHELF)
		        .name(Message.get(tag).replace("_Target_", tu.getDisplayName(vm, false)));
	}

	@Override
	public Inventory onOpen() {
		String name = tu.getName();
		String displayName = tu.getDisplayName(vm, false);
		Inventory inv = getInventory(
		        Message.get(tag + "-title").replace("_Target_", name).replace("_Page_", Integer.toString(page)), true);

		inv.setItem(0,
		        new CustomItem().skullOwner(name)
		                .name(Message.USER.get().replace("_Target_", displayName))
		                .lore(Message.get("Menus.User-details")
		                        .replace("_Target_", displayName)
		                        .split(ConfigUtils.getLineBreakSymbol()))
		                .create());

		return inv;
	}

	@Override
	public void onUpdate(Inventory inv) {
		super.onUpdate(inv);

		String actionsBefore;
		boolean archiveAction;
		String actionsAfter;

		if (reportsPage.characteristics.reportsCharacteristics.archived) {
			actionsBefore = Message.REPORT_RESTORE_ACTION.get()
			        + (u.hasPermission(Permission.STAFF_DELETE) ? Message.REPORT_DELETE_ACTION.get() : "");
			archiveAction = false;
			actionsAfter = "";
		} else {
			actionsBefore = Message.REPORT_SHOW_ACTION.get();
			archiveAction = u.hasPermission(Permission.STAFF_ARCHIVE);
			actionsAfter = u.hasPermission(Permission.STAFF_DELETE) ? Message.REPORT_DELETE_ACTION.get() : "";
		}

		rm.fillInventoryWithReportsPage(inv, reportsPage, actionsBefore, archiveAction, actionsAfter, vm, bm);
	}

	@Override
	public void onClick(ItemStack item, int slot, ClickType click) {
		if (slot == 0) {
			u.openUserMenu(tu, rm, db, taskScheduler, vm, um);
		} else if (slot >= 18 && slot <= size - 10) {
			int reportId = reportsPage.getReportIdAtIndex(slot - 18);

			if (reportId == -1) {
				update(false);
			} else {
				rm.getReportByIdAsynchronously(reportId, false, true, db, taskScheduler, um,
				        new ResultCallback<Report>() {

					        @Override
					        public void onResultReceived(Report r) {
						        if (r.isArchived()) {
							        processClickOnArchivedReport(r, click);
						        } else {
							        processClickOnCurrentReport(r, click);
						        }
					        }

				        });
			}
		}
	}

	private void processClickOnCurrentReport(Report r, ClickType click) {
		if (r == null) {
			update(false);
		} else {
			if ((click == ClickType.RIGHT || click == ClickType.SHIFT_RIGHT) && u.canArchive(r)) {
				u.openConfirmationMenu(r, ConfirmationMenu.Action.ARCHIVE, rm, db, taskScheduler, vm, bm, um);
			} else if (click == ClickType.DROP && u.hasPermission(Permission.STAFF_DELETE)) {
				u.openConfirmationMenu(r, ConfirmationMenu.Action.DELETE, rm, db, taskScheduler, vm, bm, um);
			} else {
				u.openReportMenu(r, rm, db, taskScheduler, vm, bm, um);
			}
		}
	}

	private void processClickOnArchivedReport(Report r, ClickType click) {
		if (r == null) {
			update(true);
		} else if (click == ClickType.DROP && u.hasPermission(Permission.STAFF_DELETE)) {
			u.openConfirmationMenu(r, ConfirmationMenu.Action.DELETE_ARCHIVE, rm, db, taskScheduler, vm, bm, um);
		} else {
			r.unarchive(u, false, db, rm, vm, bm);
		}
	}

}
