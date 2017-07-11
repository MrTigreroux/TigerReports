package fr.mrtigreroux.tigerreports.listeners;

import java.util.Arrays;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.objects.users.OnlineUser;
import fr.mrtigreroux.tigerreports.utils.UserUtils;

/**
 * @author MrTigreroux
 */

public class InventoryListener implements Listener {

	private boolean isReportMenu(Inventory inv) {
		String title = inv.getTitle();
		for(Message message : Arrays.asList(Message.REASON_TITLE, Message.REPORTS_TITLE, Message.REPORT_TITLE, Message.COMMENTS_TITLE, Message.CONFIRM_ARCHIVE_TITLE, Message.CONFIRM_REMOVE_TITLE, Message.PROCESS_TITLE, Message.USER_TITLE, Message.ARCHIVED_REPORTS_TITLE))
			if(title.startsWith(message.get().replace("_Page_", "").replace("_Report_", "").replace("_Target_", ""))) return true;
		return false;
	}
	
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onInventoryDrag(InventoryDragEvent e) {
		if(!(e.getWhoClicked() instanceof Player)) return;
		OnlineUser u = UserUtils.getOnlineUser((Player) e.getWhoClicked());
		Inventory inv = e.getInventory();
		if(inv != null && inv.getType() == InventoryType.CHEST && u.getOpenedMenu() != null && isReportMenu(inv)) e.setCancelled(true);
	}
	
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onInventoryClick(InventoryClickEvent e) {
		if(!(e.getWhoClicked() instanceof Player)) return;
		OnlineUser u = UserUtils.getOnlineUser((Player) e.getWhoClicked());
		Inventory inv = e.getClickedInventory();
		if(inv != null && inv.getType() == InventoryType.CHEST && u.getOpenedMenu() != null && isReportMenu(inv)) {
			e.setCancelled(true);
			u.getOpenedMenu().click(e.getCurrentItem(), e.getSlot(), e.getClick());
		}
	}
	
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onInventoryClose(InventoryCloseEvent e) {
		UserUtils.getOnlineUser((Player) e.getPlayer()).setOpenedMenu(null);
		try {
			TigerReports.getDb().startClosing();
		} catch (Exception ex) {}
	}
	
}
