package fr.mrtigreroux.tigerreports.objects.menus;

import java.util.Arrays;

import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.Permission;
import fr.mrtigreroux.tigerreports.objects.CustomItem;
import fr.mrtigreroux.tigerreports.objects.users.OnlineUser;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;

/**
 * @author MrTigreroux
 */

public class ConfirmationMenu extends Menu implements ReportManagement {
	
	public ConfirmationMenu(OnlineUser u, int reportId, String action) {
		super(u, 27, 0, Permission.STAFF, reportId, action, null);
	}
	
	@Override
	public void onOpen() {
		String report = r.getName();
		final boolean removeArchive = action.equals("REMOVE_ARCHIVE");
		if(removeArchive) action = "REMOVE";
		Inventory inv = getInventory(Message.valueOf("CONFIRM_"+action+"_TITLE").get().replace("_Report_", report), false);
		
		ItemStack gui = new CustomItem().type(Material.STAINED_GLASS_PANE).damage((byte) 7).name("").create();
		for(int position : Arrays.asList(1, 2, 3, 5, 6, 7, 10, 12, 14, 16, 19, 20, 21, 23, 24, 25)) inv.setItem(position, gui);
		
		inv.setItem(11, new CustomItem().type(Material.STAINED_CLAY).damage((byte) 5).name(Message.valueOf("CONFIRM_"+action).get()).lore(Message.valueOf("CONFIRM_"+action+"_DETAILS").get().replace("_Report_", report).split(ConfigUtils.getLineBreakSymbol())).create());
		inv.setItem(13, r.getItem(null));
		inv.setItem(15, new CustomItem().type(Material.STAINED_CLAY).damage((byte) 14).name(Message.valueOf("CANCEL_"+action).get()).lore(Message.valueOf("CANCEL_"+action+"_DETAILS").get().split(ConfigUtils.getLineBreakSymbol())).create());
		
		if(removeArchive) action = "REMOVE_ARCHIVE";
		p.openInventory(inv);
	}

	@Override
	public void onClick(ItemStack item, int slot, ClickType click) {
		if(slot == 11) {
			if(!Permission.valueOf(action.equals("REMOVE_ARCHIVE") ? "REMOVE" : action).check(u)) {
				u.openReportMenu(r);
				return;
			}
			
			if(action.equals("REMOVE")) r.remove(p.getName(), false);
			else if(action.equals("REMOVE_ARCHIVE")) r.removeFromArchives(p.getName(), false);
			else r.archive(p.getName(), false);
			p.closeInventory();
		} else if(slot == 15) {
			if(action.equals("REMOVE_ARCHIVE")) u.openArchivedReportsMenu(1, true);
			else u.openReportMenu(r);
		}
	}
	
}
