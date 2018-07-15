package fr.mrtigreroux.tigerreports.runnables;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.data.config.ConfigFile;
import fr.mrtigreroux.tigerreports.objects.menus.Menu;
import fr.mrtigreroux.tigerreports.objects.users.OnlineUser;

/**
 * @author MrTigreroux
 */

public class MenuUpdater implements Runnable {

	private static int taskId = -1;
	private static List<OnlineUser> users = new ArrayList<>(); 
	
	@Override
	public void run() {
		if(users.isEmpty())
			stop(false);
		else {
			for(OnlineUser u : users) {
				Menu menu = u.getOpenedMenu();
				if(menu != null)
					menu.update(false);
				else
					removeUser(u);
			}
		}
	}
	
	public static void addUser(OnlineUser u) {
		if(!users.contains(u))
			users.add(u);
		if(taskId == -1)
			start();
	}
	
	public static void removeUser(OnlineUser u) {
		users.remove(u);
		if(users.isEmpty())
			stop(false);
	}
	
	public static void start() {
		stop(false);
		int interval = ConfigFile.CONFIG.get().getInt("Config.MenuUpdatesInterval", 10)*20;
		if(interval > 0)
			taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(TigerReports.getInstance(), new MenuUpdater(), interval, interval);
	}
	
	public static void stop(boolean reset) {
		if(taskId != -1) {
			Bukkit.getScheduler().cancelTask(taskId);
			taskId = -1;
			if(reset)
				users.clear();
		}
	}

}
