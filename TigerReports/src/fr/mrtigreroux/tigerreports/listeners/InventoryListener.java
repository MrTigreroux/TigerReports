package fr.mrtigreroux.tigerreports.listeners;

import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.objects.users.OnlineUser;
import fr.mrtigreroux.tigerreports.runnables.MenuUpdater;

/**
 * @author MrTigreroux
 */

public class InventoryListener implements Listener {
	
	@EventHandler(priority = EventPriority.LOW)
	private void onInventoryDrag(InventoryDragEvent e) {
		if(checkMenuAction(e.getWhoClicked(), e.getInventory()) != null)
			e.setCancelled(true);
	}
	
	@EventHandler(priority = EventPriority.LOW)
	private void onInventoryClick(InventoryClickEvent e) {
		Inventory inv = e.getClickedInventory();
		OnlineUser u = checkMenuAction(e.getWhoClicked(), inv);
		if(u != null) {
			if(inv.getType() == InventoryType.CHEST) {
				e.setCancelled(true);
				if(e.getCursor().getType() == Material.AIR)
					u.getOpenedMenu().click(e.getCurrentItem(), e.getSlot(), e.getClick());
			} else if(inv.getType() == InventoryType.PLAYER && (e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY || e.getAction() == InventoryAction.COLLECT_TO_CURSOR)) {
				e.setCancelled(true);
			}
		}
	}
	
	@EventHandler(priority = EventPriority.LOW)
	private void onInventoryClose(InventoryCloseEvent e) {
		OnlineUser u = TigerReports.getInstance().getUsersManager().getOnlineUser((Player) e.getPlayer());
		MenuUpdater.removeUser(u);
		u.setOpenedMenu(null);
		try {
			TigerReports.getInstance().getDb().startClosing();
		} catch (Exception ignored) {}
	}
	
	private OnlineUser checkMenuAction(HumanEntity whoClicked, Inventory inv) {
		if(!(whoClicked instanceof Player) || inv == null)
			return null;
		OnlineUser u = TigerReports.getInstance().getUsersManager().getOnlineUser((Player) whoClicked);
		return u.getOpenedMenu() != null ? u : null;
	}
	
}
