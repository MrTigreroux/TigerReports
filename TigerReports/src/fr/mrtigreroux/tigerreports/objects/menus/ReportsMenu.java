package fr.mrtigreroux.tigerreports.objects.menus;

import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.MenuItem;
import fr.mrtigreroux.tigerreports.data.constants.Permission;
import fr.mrtigreroux.tigerreports.objects.Report;
import fr.mrtigreroux.tigerreports.objects.users.OnlineUser;
import fr.mrtigreroux.tigerreports.utils.ReportUtils;

/**
 * @author MrTigreroux
 */

public class ReportsMenu extends Menu implements UpdatedMenu {
	
	public ReportsMenu(OnlineUser u, int page) {
		super(u, 54, page, Permission.STAFF);
	}
	
	@Override
	public Inventory onOpen() {
		Inventory inv = getInventory(Message.REPORTS_TITLE.get().replace("_Page_", Integer.toString(page)), true);
		
		inv.setItem(4, MenuItem.REPORTS.get());
		if(Permission.STAFF_ARCHIVE.isOwned(u)) inv.setItem(8, MenuItem.ARCHIVED_REPORTS.getWithDetails(Message.ARCHIVED_REPORTS_DETAILS.get()));
		
		return inv;
	}
	
	@Override
	public void onUpdate(Inventory inv) {
		ReportUtils.addReports("reports", inv, page, Message.REPORT_SHOW_ACTION.get()+(Permission.STAFF_ARCHIVE.isOwned(u) ? Message.REPORT_ARCHIVE_ACTION.get() : "")+(Permission.STAFF_DELETE.isOwned(u) ? Message.REPORT_DELETE_ACTION.get() : ""));
	}

	@Override
	public void onClick(ItemStack item, int slot, ClickType click) {
		if(slot == 8 && Permission.STAFF_ARCHIVE.isOwned(u)) u.openArchivedReportsMenu(1, true);
		else if(slot >= 18 && slot <= size-9) {
			Report r = ReportUtils.getReport(getIndex(slot));
			if(r == null) update(false);
			else {
				if(click.equals(ClickType.MIDDLE) && Permission.STAFF_ARCHIVE.isOwned(u)) u.openConfirmationMenu(r, "ARCHIVE");
				else if(click.equals(ClickType.DROP) && Permission.STAFF_DELETE.isOwned(u)) u.openConfirmationMenu(r, "DELETE");
				else u.openReportMenu(r);
			}
		}
	}
	
}
