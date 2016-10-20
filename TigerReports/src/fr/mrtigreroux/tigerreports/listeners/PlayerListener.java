package fr.mrtigreroux.tigerreports.listeners;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.ServerCommandEvent;

import fr.mrtigreroux.tigerreports.commands.HelpCommand;
import fr.mrtigreroux.tigerreports.managers.FilesManager;
import fr.mrtigreroux.tigerreports.objects.User;
import fr.mrtigreroux.tigerreports.utils.UserUtils;

@SuppressWarnings("deprecation")
public class PlayerListener implements Listener {

	private final static Set<String> helpCommands = new HashSet<String>(Arrays.asList("/tigerreports", "/tr", "/helptigerreports", "/helptigerreport", "/tigerreport", "/tigerreporthelp", "/tigerreportshelp"));
	
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
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent e) {
		if(helpCommands.contains(e.getMessage().replaceAll(" ", ""))) HelpCommand.onCommand(e.getPlayer());
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onServerCommandPreprocess(ServerCommandEvent e) {
		if(helpCommands.contains("/"+e.getCommand().replaceAll(" ", ""))) HelpCommand.onCommand(e.getSender());
	}
	
}
