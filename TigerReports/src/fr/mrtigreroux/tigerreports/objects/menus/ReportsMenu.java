package fr.mrtigreroux.tigerreports.objects.menus;

import java.util.List;

import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import fr.mrtigreroux.tigerreports.data.config.ConfigSound;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.MenuItem;
import fr.mrtigreroux.tigerreports.data.constants.Permission;
import fr.mrtigreroux.tigerreports.objects.Report;
import fr.mrtigreroux.tigerreports.objects.users.OnlineUser;
import fr.mrtigreroux.tigerreports.utils.ReportUtils;

/**
 * @author MrTigreroux
 */

public class ReportsMenu extends Menu {
	
	public ReportsMenu(OnlineUser u, int page) {
		super(u, 54, page, -1, null, null);
	}
	
	@Override
	public void open(boolean sound) {
		Inventory inv = getInventory(Message.REPORTS_TITLE.get().replace("_Page_", ""+page), true);
		
		inv.setItem(4, MenuItem.REPORTS.get());
		if(u.hasPermission(Permission.ARCHIVE)) inv.setItem(MenuItem.ARCHIVED_REPORTS.getPosition(), MenuItem.ARCHIVED_REPORTS.getWithDetails(Message.ARCHIVED_REPORTS_DETAILS.get()));
		int firstReport = 1;
		if(page >= 2) {
			inv.setItem(size-7, MenuItem.PAGE_SWITCH_PREVIOUS.get());
			firstReport += (page-1)*27;
		}
		
		List<Report> reports = ReportUtils.getReports(firstReport, firstReport+27);
		int position = 18;
		for(Report r : reports) {
			inv.setItem(position, r.getItem(Message.REPORT_SHOW_ACTION.get()+(u.hasPermission(Permission.REMOVE) ? Message.REPORT_REMOVE_ACTION.get() : "")));
			if(position >= 46) break;
			else position++;
		}
		
		if(reports.size() == 28) inv.setItem(size-3, MenuItem.PAGE_SWITCH_NEXT.get());
		p.openInventory(inv);
		if(sound) u.playSound(ConfigSound.MENU.get());
		u.setOpenedMenu(this);
	}

	@Override
	public void onClick(ItemStack item, int slot, ClickType click) {
		if(slot == MenuItem.ARCHIVED_REPORTS.getPosition() && u.hasPermission(Permission.ARCHIVE)) u.openArchivedReportsMenu(1, true);
		else if(slot >= 18 && slot <= size-9) {
			Report r = ReportUtils.getReport(getIndex(slot));
			if(click.equals(ClickType.DROP) && u.hasPermission(Permission.REMOVE)) u.openConfirmationMenu(r, "REMOVE");
			else u.openReportMenu(r);
		}
	}
	
}
