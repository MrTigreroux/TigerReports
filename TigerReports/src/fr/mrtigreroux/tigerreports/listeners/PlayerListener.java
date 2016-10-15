package fr.mrtigreroux.tigerreports.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import fr.mrtigreroux.tigerreports.managers.FilesManager;
import fr.mrtigreroux.tigerreports.objects.User;
import fr.mrtigreroux.tigerreports.utils.UserUtils;

@SuppressWarnings("deprecation")
public class PlayerListener implements Listener {

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		Player p = e.getPlayer();
		User u = new User(p);
		String uuid = p.getUniqueId().toString();
		for(String notification : UserUtils.getNotifications(uuid)) u.sendNotification(notification);

		FilesManager.getData.set("Data."+uuid+".Name", p.getName());
		FilesManager.saveData();
	}
	
	@EventHandler
	public void onPlayerChat(PlayerChatEvent e) {
		new User(e.getPlayer()).updateLastMessages(e.getMessage());
	}
	
}
