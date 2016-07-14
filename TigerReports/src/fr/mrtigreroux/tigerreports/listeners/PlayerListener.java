package fr.mrtigreroux.tigerreports.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import fr.mrtigreroux.tigerreports.objects.User;
import fr.mrtigreroux.tigerreports.utils.UserUtils;

public class PlayerListener implements Listener {

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		Player p = e.getPlayer();
		User u = new User(p);
		for(String notification : UserUtils.getNotifications(p.getUniqueId().toString())) u.sendNotification(notification);
	}
	
}
