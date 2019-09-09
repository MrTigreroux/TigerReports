package fr.mrtigreroux.tigerreports.objects.menus;

import org.bukkit.Bukkit;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.Permission;
import fr.mrtigreroux.tigerreports.objects.Report;
import fr.mrtigreroux.tigerreports.objects.users.OnlineUser;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.utils.ReportUtils;

/**
 * @author MrTigreroux
 */

public class UserReportsMenu extends UserManagerMenu implements UpdatedMenu {

	public UserReportsMenu(OnlineUser u, int page, User tu) {
		super("Menus.User-reports", u, page, tu);
	}

	@Override
	public void onUpdate(Inventory inv) {
		ReportUtils.addReports(tu.getUniqueId(), false, inv, page, Message.REPORT_SHOW_ACTION.get()+(Permission.STAFF_ARCHIVE.isOwned(u)
																																			? Message.REPORT_ARCHIVE_ACTION
																																					.get()
																																			: "")
				+(Permission.STAFF_DELETE.isOwned(u) ? Message.REPORT_DELETE_ACTION.get() : ""));
	}

	@Override
	public void onClick(ItemStack item, int slot, ClickType click) {
		if (slot == 0) {
			u.openUserMenu(tu);
		} else if (slot >= 18 && slot <= size-9) {
			TigerReports tr = TigerReports.getInstance();
			Bukkit.getScheduler().runTaskAsynchronously(tr, new Runnable() {

				@Override
				public void run() {
					Report r = getReport(false, getConfigIndex(slot));
					Bukkit.getScheduler().runTask(tr, new Runnable() {

						@Override
						public void run() {
							if (r == null) {
								update(false);
							} else {
								if (click.equals(ClickType.MIDDLE) && Permission.STAFF_ARCHIVE.isOwned(u)) {
									u.openConfirmationMenu(r, "ARCHIVE");
								} else if (click.equals(ClickType.DROP) && Permission.STAFF_DELETE.isOwned(u)) {
									u.openConfirmationMenu(r, "DELETE");
								} else {
									u.openReportMenu(r);
								}
							}
						}

					});
				}

			});
		}
	}

}
