package fr.mrtigreroux.tigerreports.objects.menus;

import java.util.Arrays;

import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import fr.mrtigreroux.tigerreports.data.MenuItem;
import fr.mrtigreroux.tigerreports.data.Message;
import fr.mrtigreroux.tigerreports.data.Status;
import fr.mrtigreroux.tigerreports.objects.CustomItem;
import fr.mrtigreroux.tigerreports.objects.User;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;
import fr.mrtigreroux.tigerreports.utils.ReportUtils;
import fr.mrtigreroux.tigerreports.utils.UserUtils;

/**
 * @author MrTigreroux
 */

public class ProcessMenu extends Menu {
	
	public ProcessMenu(User u, int reportNumber) {
		super(u, 27, 0, reportNumber);
	}
	
	@Override
	public void open(boolean sound) {
		Inventory inv = getInventory(Message.PROCESS_TITLE.get().replaceAll("_Report_", ReportUtils.getName(reportNumber)), false);
		
		ItemStack gui = new CustomItem().type(Material.STAINED_GLASS_PANE).damage((byte) 7).name("").create();
		for(int position : Arrays.asList(1, 2, 3, 4, 5, 6, 7, 10, 16, 19, 20, 21, 22, 23, 24, 25)) inv.setItem(position, gui);
		
		inv.setItem(0, ReportUtils.getItem(reportNumber, null));
		
		for(String appreciation : Arrays.asList("TRUE", "UNCERTAIN", "FALSE")) {
			String appreciationWord = Message.valueOf(appreciation).get();
			inv.setItem(appreciation.equals("TRUE") ? 11 : appreciation.equals("UNCERTAIN") ? 13 : 15, new CustomItem().type(Material.STAINED_CLAY)
					.damage((byte) (appreciation.equals("TRUE") ? 5 : appreciation.equals("UNCERTAIN") ? 4 : 14)).name(Message.PROCESS.get().replaceAll("_Appreciation_", appreciationWord))
					.lore(Message.PROCESS_DETAILS.get().replaceAll("_Appreciation_", appreciationWord).split(ConfigUtils.getLineBreakSymbol())).create());
		}
		
		inv.setItem(18, MenuItem.CANCEL_APPRECIATION.get());
		
		p.openInventory(inv);
		if(sound) p.playSound(p.getLocation(), ConfigUtils.getMenuSound(), 1, 1);
		u.setOpenedMenu(this);
	}

	@Override
	public void onClick(ItemStack item, int slot, ClickType click) {
		if(slot == 11 || slot == 13 || slot == 15) {
			String appreciation = slot == 11 ? "True" : slot == 13 ? "Uncertain" : "False";
			ReportUtils.setAppreciation(reportNumber, appreciation);
			ReportUtils.setStatus(reportNumber, Status.DONE);
			UserUtils.addAppreciation(UserUtils.getUniqueId(ReportUtils.getPlayerName("Signalman", reportNumber, false)), appreciation, 1);
			MessageUtils.sendStaffMessage(Message.STAFF_PROCESS.get().replaceAll("_Player_", p.getName()).replaceAll("_Report_", ReportUtils.getName(reportNumber)).replaceAll("_Appreciation_", Message.valueOf(appreciation.toUpperCase()).get()), ConfigUtils.getStaffSound());
			u.openReportsMenu(1, false);
		} else if(slot == MenuItem.CANCEL_APPRECIATION.getPosition()) u.openReportMenu(reportNumber);
	}
	
}
