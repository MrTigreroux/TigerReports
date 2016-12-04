package fr.mrtigreroux.tigerreports.objects.menus;

import java.util.Arrays;

import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import fr.mrtigreroux.tigerreports.data.Message;
import fr.mrtigreroux.tigerreports.data.Permission;
import fr.mrtigreroux.tigerreports.objects.CustomItem;
import fr.mrtigreroux.tigerreports.objects.Report;
import fr.mrtigreroux.tigerreports.objects.User;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;

/**
 * @author MrTigreroux
 */

public class ConfirmationMenu extends Menu {
	
	public ConfirmationMenu(User u, Report r, String action) {
		super(u, 27, 0, r, action, null);
	}
	
	@Override
	public void open(boolean sound) {
		String report = r.getName();
		Inventory inv = getInventory(Message.valueOf("CONFIRM_"+action+"_TITLE").get().replace("_Report_", report), false);
		
		ItemStack gui = new CustomItem().type(Material.STAINED_GLASS_PANE).damage((byte) 7).name("").create();
		for(int position : Arrays.asList(1, 2, 3, 5, 6, 7, 10, 12, 14, 16, 19, 20, 21, 23, 24, 25)) inv.setItem(position, gui);
		
		inv.setItem(11, new CustomItem().type(Material.STAINED_CLAY).damage((byte) 5).name(Message.valueOf("CONFIRM_"+action).get()).lore(Message.valueOf("CONFIRM_"+action+"_DETAILS").get().replace("_Report_", report).split(ConfigUtils.getLineBreakSymbol())).create());
		inv.setItem(13, r.getItem(null));
		inv.setItem(15, new CustomItem().type(Material.STAINED_CLAY).damage((byte) 14).name(Message.valueOf("CANCEL_"+action).get()).lore(Message.valueOf("CANCEL_"+action+"_DETAILS").get().split(ConfigUtils.getLineBreakSymbol())).create());
		
		p.openInventory(inv);
		if(sound) p.playSound(p.getLocation(), ConfigUtils.getMenuSound(), 1, 1);
		u.setOpenedMenu(this);
	}

	@Override
	public void onClick(ItemStack item, int slot, ClickType click) {
		if(slot == 11) {
			if(!u.hasPermission(Permission.valueOf(action))) {
				u.openReportMenu(r);
				return;
			}
			if(action.equals("REMOVE")) r.remove();
			else r.archive();
			MessageUtils.sendStaffMessage(Message.valueOf("STAFF_"+action).get().replace("_Player_", p.getName()).replace("_Report_", r.getName()), ConfigUtils.getStaffSound());
			u.openReportsMenu(1, false);
		} else if(slot == 15) u.openReportMenu(r);
	}
	
}
