package fr.mrtigreroux.tigerreports.objects.menus;

import java.util.Collections;

import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.MenuItem;
import fr.mrtigreroux.tigerreports.data.constants.Permission;
import fr.mrtigreroux.tigerreports.objects.Report;
import fr.mrtigreroux.tigerreports.objects.users.OnlineUser;
import fr.mrtigreroux.tigerreports.utils.ReportUtils;

/**
 * @author MrTigreroux
 */

public class ArchivedReportsMenu extends Menu implements UpdatedMenu {
	
	public ArchivedReportsMenu(OnlineUser u, int page) {
		super(u, 54, page, Permission.ARCHIVE, -1, null, null);
	}
	
	@Override
	protected Inventory onOpen() {
		Inventory inv = getInventory(Message.ARCHIVED_REPORTS_TITLE.get().replace("_Page_", ""+page), true);
		
		inv.setItem(0, MenuItem.REPORTS.getWithDetails(Message.REPORTS_DETAILS.get()));
		inv.setItem(4, MenuItem.ARCHIVED_REPORTS.get());
		
		return inv;
	}
	
	@Override
	public void onUpdate(Inventory inv) {
		if(!check()) return;
		ReportUtils.addReports("archived_reports", inv, page, Message.REPORT_RESTORE_ACTION.get()+(Permission.REMOVE.isOwned(u) ? Message.REPORT_REMOVE_ACTION.get() : ""));
	}

	@Override
	public void onClick(ItemStack item, int slot, ClickType click) {
		if(slot == 0) u.openReportsMenu(1, true);
		else if(slot >= 18 && slot <= size-9) {
			Report r = ReportUtils.formatReport(TigerReports.getDb().query("SELECT * FROM archived_reports LIMIT 1 OFFSET ?", Collections.singletonList(getIndex(slot)-1)).getResult(0), true);
			if(r == null) {
				open(true);
				return;
			}
			if(click.equals(ClickType.DROP) && Permission.REMOVE.isOwned(u)) u.openConfirmationMenu(r, "REMOVE_ARCHIVE");
			else {
				r.unarchive(p.getName(), false);
				p.closeInventory();
			}
		}
	}
	
}
