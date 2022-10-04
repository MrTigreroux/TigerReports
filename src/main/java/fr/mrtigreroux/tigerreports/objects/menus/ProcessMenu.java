package fr.mrtigreroux.tigerreports.objects.menus;

import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.Appreciation;
import fr.mrtigreroux.tigerreports.data.constants.MenuItem;
import fr.mrtigreroux.tigerreports.data.constants.MenuRawItem;
import fr.mrtigreroux.tigerreports.data.constants.Permission;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.managers.BungeeManager;
import fr.mrtigreroux.tigerreports.managers.ReportsManager;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.managers.VaultManager;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.ReportUtils;

/**
 * @author MrTigreroux
 */

public class ProcessMenu extends ReportManagerMenu {

	private static final int[] GUI_POSITIONS = new int[] { 1, 2, 3, 4, 5, 6, 7, 10, 16, 19, 20, 21, 22, 23, 24, 25 };
	private final VaultManager vm;
	private final BungeeManager bm;

	public ProcessMenu(User u, int reportId, ReportsManager rm, Database db, TaskScheduler taskScheduler,
	        VaultManager vm, BungeeManager bm, UsersManager um) {
		super(u, 27, 0, Permission.STAFF, reportId, rm, db, taskScheduler, um);
		this.vm = vm;
		this.bm = bm;
	}

	@Override
	public Inventory onOpen() {
		Inventory inv = getInventory(Message.PROCESS_TITLE.get().replace("_Report_", r.getName()), false);

		ItemStack gui = MenuRawItem.GUI.create();
		for (int position : GUI_POSITIONS) {
			inv.setItem(position, gui);
		}

		inv.setItem(0, r.getItem(null, vm, bm));

		for (Appreciation appreciation : Appreciation.getValues()) {
			String displayName = appreciation.getDisplayName();
			inv.setItem(appreciation.getPosition(),
			        appreciation.getIcon()
			                .name(Message.PROCESS.get().replace("_Appreciation_", displayName))
			                .lore(Message.PROCESS_DETAILS.get()
			                        .replace("_Appreciation_", displayName)
			                        .split(ConfigUtils.getLineBreakSymbol()))
			                .create());

		}

		inv.setItem(18, MenuItem.CANCEL_PROCESS.get());

		return inv;
	}

	@Override
	public void onClick(ItemStack item, int slot, ClickType click) {
		if (slot == 18) {
			u.openReportMenu(r.getId(), rm, db, taskScheduler, vm, bm, um);
		} else {
			Appreciation appreciation = Appreciation.getAppreciationAtPosition(slot);
			if (appreciation != null) {
				if (appreciation == Appreciation.TRUE) {
					if (ReportUtils.punishmentsEnabled()) {
						u.openPunishmentMenu(1, r, rm, db, taskScheduler, vm, bm, um);
					} else {
						process(Appreciation.TRUE);
					}
				} else {
					process(appreciation);
				}
			}
		}
	}

	private void process(Appreciation appreciation) {
		u.afterProcessingAReport(true, rm, db, taskScheduler, vm, bm, um);
		r.process(u, appreciation, false, u.hasPermission(Permission.STAFF_ARCHIVE_AUTO), true, db, rm, vm, bm,
		        taskScheduler);
	}

}
