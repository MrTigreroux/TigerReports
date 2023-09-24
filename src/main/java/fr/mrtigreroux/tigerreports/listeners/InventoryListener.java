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

import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.logs.Logger;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.utils.LogUtils;

/**
 * @author MrTigreroux
 */
public class InventoryListener implements Listener {

	private Database db;
	private UsersManager um;

	public InventoryListener(Database db, UsersManager um) {
		this.db = db;
		this.um = um;
	}

	@EventHandler(priority = EventPriority.LOW)
	private void onInventoryDrag(InventoryDragEvent e) {
		Logger.EVENTS.info(() -> "onInventoryDrag(): " + e.getWhoClicked().getName());
		if (checkMenuAction(e.getWhoClicked(), e.getInventory()) != null) {
			e.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	private void onInventoryClick(InventoryClickEvent e) {
		Logger.EVENTS.info(() -> "onInventoryClick(): " + e.getWhoClicked().getName());
		Inventory inv = e.getClickedInventory();
		User u = checkMenuAction(e.getWhoClicked(), inv);
		if (u != null) {
			if (inv.getType() == InventoryType.CHEST) {
				e.setCancelled(true);
				if (e.getCursor().getType() == Material.AIR) {
					u.getOpenedMenu().click(e.getCurrentItem(), e.getSlot(), e.getClick());
				}
			} else if (inv.getType() == InventoryType.PLAYER
			        && (e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
			                || e.getAction() == InventoryAction.COLLECT_TO_CURSOR)) {
				e.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	private void onInventoryClose(InventoryCloseEvent e) {
		if (!(e.getPlayer() instanceof Player)) {
			return;
		}

		Logger.EVENTS.debug(() -> "onInventoryClose(): get online user");
		Player p = (Player) e.getPlayer();
		User u = um.getOnlineUser(p);
		if (u != null) {
			Logger.EVENTS.info(() -> "onInventoryClose(): " + u.getName());
			u.setOpenedMenu(null);
			try {
				db.startClosing();
			} catch (Exception ignored) {}
		} else {
			LogUtils.logUnexpectedOfflineUser(Logger.EVENTS, "onInventoryClose()", p);
		}
	}

	private User checkMenuAction(HumanEntity whoClicked, Inventory inv) {
		if (!(whoClicked instanceof Player) || inv == null) {
			return null;
		}
		Player p = (Player) whoClicked;
		User u = um.getOnlineUser(p);
		if (u == null) {
			LogUtils.logUnexpectedOfflineUser(Logger.EVENTS, "checkMenuAction()", p);
			return null;
		}
		return u.getOpenedMenu() != null ? u : null;
	}

}
