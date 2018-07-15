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

public class ConfirmationMenu extends ReportManagerMenu {
	
	private String action;
	
	public ConfirmationMenu(OnlineUser u, int reportId, String action) {
		super(u, 27, 0, Permission.STAFF, reportId);
		this.action = action;
	}
	
	@Override
	public Inventory onOpen() {
		String report = r.getName();
		String actionDisplayed = action.equals("DELETE_ARCHIVE") ? "DELETE" : action;
		Inventory inv = getInventory(Message.valueOf("CONFIRM_"+actionDisplayed+"_TITLE").get().replace("_Report_", report), false);
		
		ItemStack gui = new CustomItem().type(Material.STAINED_GLASS_PANE).damage((byte) 7).name(" ").create();
		for(int position : Arrays.asList(1, 2, 3, 5, 6, 7, 10, 12, 14, 16, 19, 20, 21, 23, 24, 25))
			inv.setItem(position, gui);
		
		inv.setItem(11, new CustomItem().type(Material.STAINED_CLAY).damage((byte) 5).name(Message.valueOf("CONFIRM_"+actionDisplayed).get()).lore(Message.valueOf("CONFIRM_"+actionDisplayed+"_DETAILS").get().replace("_Report_", report).split(ConfigUtils.getLineBreakSymbol())).create());
		inv.setItem(13, r.getItem(null));
		inv.setItem(15, new CustomItem().type(Material.STAINED_CLAY).damage((byte) 14).name(Message.valueOf("CANCEL_"+actionDisplayed).get()).lore(Message.valueOf("CANCEL_"+actionDisplayed+"_DETAILS").get().split(ConfigUtils.getLineBreakSymbol())).create());
		
		return inv;
	}

	@Override
	public void onClick(ItemStack item, int slot, ClickType click) {
		if(slot == 11) {
			if(!Permission.valueOf("STAFF_"+(action.equals("DELETE_ARCHIVE") ? "DELETE" : action)).isOwned(u)) {
				u.openReportMenu(r);
				return;
			}
			
			switch(action) {
				case "DELETE":
					r.delete(p.getName(), false); break;
				case "DELETE_ARCHIVE":
					r.deleteFromArchives(p.getName(), false); break;
				default:
					r.archive(p.getName(), false); break;
			}
			u.openDelayedlyReportsMenu();
		} else if(slot == 15) {
			if(action.equals("DELETE_ARCHIVE"))
				u.openArchivedReportsMenu(1, true);
			else
				u.openReportMenu(r);
		}
	}
	
}
