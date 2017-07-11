package fr.mrtigreroux.tigerreports.objects.menus;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

public class ArchivedReportsMenu extends Menu {
	
	public ArchivedReportsMenu(OnlineUser u, int page) {
		super(u, 54, page, Permission.ARCHIVE, -1, null, null);
	}
	
	@Override
	public void onOpen() {
		Inventory inv = getInventory(Message.ARCHIVED_REPORTS_TITLE.get().replace("_Page_", ""+page), true);
		
		inv.setItem(0, MenuItem.REPORTS.getWithDetails(Message.REPORTS_DETAILS.get()));
		inv.setItem(4, MenuItem.ARCHIVED_REPORTS.get());
		int firstReport = 1;
		if(page >= 2) {
			inv.setItem(size-7, MenuItem.PAGE_SWITCH_PREVIOUS.get());
			firstReport += (page-1)*27;
		}
		
		int position = 18;
		List<Map<String, Object>> results = TigerReports.getDb().query("SELECT report_id,status,appreciation,date,reported_uuid,signalman_uuid,reason FROM archived_reports LIMIT 28 OFFSET ?", Collections.singletonList(firstReport - 1)).getResultList();
		for(Map<String, Object> result : results) {
			inv.setItem(position, ReportUtils.formatReport(result, false).getItem(Message.REPORT_RESTORE_ACTION.get()+(Permission.REMOVE.check(u) ? Message.REPORT_REMOVE_ACTION.get() : null)));
			if(position >= 46) break;
			else position++;
		}
		
		if(results.size() == 28) inv.setItem(size-3, MenuItem.PAGE_SWITCH_NEXT.get());
		p.openInventory(inv);
	}

	@Override
	public void onClick(ItemStack item, int slot, ClickType click) {
		if(slot == 0) u.openReportsMenu(1, true);
		else if(slot >= 18 && slot <= size-9) {
			Report r = ReportUtils.formatReport(TigerReports.getDb().query("SELECT * FROM archived_reports LIMIT 1 OFFSET ?", Collections.singletonList(getIndex(slot) - 1)).getResult(0), true);
			if(r == null) {
				open(true);
				return;
			}
			if(click.equals(ClickType.DROP) && Permission.REMOVE.check(u)) u.openConfirmationMenu(r, "REMOVE_ARCHIVE");
			else {
				r.unarchive(p.getName(), false);
				p.closeInventory();
			}
		}
	}
	
}
