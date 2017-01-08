package fr.mrtigreroux.tigerreports.objects.menus;

import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import fr.mrtigreroux.tigerreports.data.ConfigSound;
import fr.mrtigreroux.tigerreports.data.MenuItem;
import fr.mrtigreroux.tigerreports.data.Message;
import fr.mrtigreroux.tigerreports.data.Permission;
import fr.mrtigreroux.tigerreports.objects.Report;
import fr.mrtigreroux.tigerreports.objects.User;
import fr.mrtigreroux.tigerreports.utils.ReportUtils;

/**
 * @author MrTigreroux
 */

public class ReportsMenu extends Menu {
	
	public ReportsMenu(User u, int page) {
		super(u, 54, page, null, null, null);
	}
	
	@Override
	public void open(boolean sound) {
		Inventory inv = getInventory(Message.REPORTS_TITLE.get().replace("_Page_", ""+page), true);
		
		inv.setItem(4, MenuItem.REPORTS_ICON.get());
		int firstReport = 1;
		if(page >= 2) {
			inv.setItem(size-7, MenuItem.PAGE_SWITCH_PREVIOUS.get());
			firstReport = ((page-1)*27)+1;
		}
		int totalReports = ReportUtils.getTotalReports();
		for(int reportNumber = firstReport; reportNumber <= firstReport+26; reportNumber++) {
			if(reportNumber > totalReports) break;
			inv.setItem(reportNumber-firstReport+18, new Report(reportNumber).getItem(Message.REPORT_SHOW_ACTION.get()+(u.hasPermission(Permission.REMOVE) ? Message.REPORT_REMOVE_ACTION.get() : null)));
		}
		
		if(firstReport+26 < totalReports) inv.setItem(size-3, MenuItem.PAGE_SWITCH_NEXT.get());
		p.openInventory(inv);
		if(sound) u.playSound(ConfigSound.MENU.get());
		u.setOpenedMenu(this);
	}

	@Override
	public void onClick(ItemStack item, int slot, ClickType click) {
		if(slot >= 18 && slot <= size-9) {
			Report r = new Report(slot-18+((page-1)*27)+1);
			if(click.equals(ClickType.DROP) && u.hasPermission(Permission.REMOVE)) u.openConfirmationMenu(r, "REMOVE");
			else u.openReportMenu(r);
		}
	}
	
}
