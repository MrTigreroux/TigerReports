package fr.mrtigreroux.tigerreports.runnables;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.objects.menus.Menu;
import fr.mrtigreroux.tigerreports.objects.menus.UpdatedMenu;
import fr.mrtigreroux.tigerreports.objects.users.OnlineUser;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;

/**
 * @author MrTigreroux
 */

public class MenuUpdater implements Runnable {

	private static int taskId = -1;
	private static List<OnlineUser> users = new ArrayList<>(); 
	
	@Override
	public void run() {
		if(users.isEmpty()) stop(false);
		else {
			for(OnlineUser u : users) {
				Menu menu = u.getOpenedMenu();
				if(menu != null) {
					InventoryView invView = u.getPlayer().getOpenInventory();
					if(menu instanceof UpdatedMenu && invView != null) {
						Inventory inv = invView.getTopInventory();
						if(inv != null) {
							((UpdatedMenu) menu).onUpdate(inv);
							return;
						}
					}
				}
				removeUser(u);
			}
		}
	}
	
	public static void addUser(OnlineUser u) {
		if(!users.contains(u)) users.add(u);
		if(taskId == -1) start();
	}
	
	public static void removeUser(OnlineUser u) {
		users.remove(u);
		if(users.isEmpty()) stop(false);
	}
	
	public static void start() {
		stop(false);
		int interval = ConfigUtils.getMenuUpdatesInterval();
		if(interval > 0) taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(TigerReports.getInstance(), new MenuUpdater(), interval, interval);
	}
	
	public static void stop(boolean reset) {
		if(taskId != -1) {
			Bukkit.getScheduler().cancelTask(taskId);
			taskId = -1;
			if(reset) users.clear();
		}
	}

}
