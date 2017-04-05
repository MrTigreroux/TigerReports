package fr.mrtigreroux.tigerreports.objects.menus;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import fr.mrtigreroux.tigerreports.TigerReports;
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

public class ArchivedReportsMenu extends Menu {
	
	public ArchivedReportsMenu(OnlineUser u, int page) {
		super(u, 54, page, -1, null, null);
	}
	
	@Override
	public void open(boolean sound) {
		if(!u.hasPermission(Permission.ARCHIVE)) return;
		
		Inventory inv = getInventory(Message.ARCHIVED_REPORTS_TITLE.get().replace("_Page_", ""+page), true);
		
		inv.setItem(4, MenuItem.ARCHIVED_REPORTS_ICON.get());
		int firstReport = 1;
		if(page >= 2) {
			inv.setItem(size-7, MenuItem.PAGE_SWITCH_PREVIOUS.get());
			firstReport += (page-1)*27;
		}
		
		int position = 18;
		List<Map<String, Object>> results = TigerReports.getDb().query("SELECT report_id,status,appreciation,date,reported_uuid,signalman_uuid,reason FROM archived_reports LIMIT 28 OFFSET ?", Arrays.asList(firstReport-1)).getResultList();
		for(Map<String, Object> result : results) {
			inv.setItem(position, ReportUtils.formatReport(result, false).getItem(Message.REPORT_RESTORE_ACTION.get()+(u.hasPermission(Permission.REMOVE) ? Message.REPORT_REMOVE_ACTION.get() : null)));
			if(position >= 46) break;
			else position++;
		}
		
		if(results.size() == 28) inv.setItem(size-3, MenuItem.PAGE_SWITCH_NEXT.get());
		p.openInventory(inv);
		if(sound) u.playSound(ConfigSound.MENU.get());
		u.setOpenedMenu(this);
	}

	@Override
	public void onClick(ItemStack item, int slot, ClickType click) {
		if(!u.hasPermission(Permission.ARCHIVE)) {
			p.closeInventory();
			return;
		}
		
		if(slot >= 18 && slot <= size-9) {
			Map<String, Object> result = TigerReports.getDb().query("SELECT * FROM archived_reports LIMIT 1 OFFSET ?", Arrays.asList(getIndex(slot))).getResult(0);
			Report r = ReportUtils.formatReport(result, true);
			if(click.equals(ClickType.DROP) && u.hasPermission(Permission.REMOVE)) u.openConfirmationMenu(r, "REMOVE_ARCHIVE");
			else {
				r.unarchive(p.getName(), false);
				p.closeInventory();
			}
		}
	}
	
}
