package fr.mrtigreroux.tigerreports.listeners;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerCommandEvent;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.commands.HelpCommand;
import fr.mrtigreroux.tigerreports.data.config.ConfigFile;
import fr.mrtigreroux.tigerreports.data.constants.Permission;
import fr.mrtigreroux.tigerreports.objects.users.OnlineUser;
import fr.mrtigreroux.tigerreports.runnables.ReportsNotifier;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.UserUtils;

/**
 * @author MrTigreroux
 */

@SuppressWarnings("deprecation")
public class PlayerListener implements Listener {

	private final static Set<String> helpCommands = new HashSet<String>(Arrays.asList("/tigerreports", "/helptigerreports", "/helptigerreport", "/tigerreport", "/tigerreporthelp", "/tigerreportshelp"));
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		Player p = e.getPlayer();
		OnlineUser u = UserUtils.getOnlineUser(p);
		for(String notification : u.getNotifications()) u.sendNotification(notification, false);
		if(Permission.STAFF.check(u) && ConfigUtils.isEnabled(ConfigFile.CONFIG.get(), "Config.ReportsNotifications.Connection")) {
			String reportsNotifications = ReportsNotifier.getReportsNotification();
			if(reportsNotifications != null) p.sendMessage(reportsNotifications);
		}
		
		TigerReports.getDb().updateAsynchronously("REPLACE INTO users (uuid,name) VALUES (?,?);", Arrays.asList(p.getUniqueId().toString(), p.getName()));
		u.updateImmunity(Permission.EXEMPT.check(u) ? "always" : null, false);
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent e) {
		TigerReports.Users.remove(e.getPlayer().getUniqueId().toString());
	}
	
	@EventHandler
	public void onPlayerChat(PlayerChatEvent e) {
		UserUtils.getOnlineUser(e.getPlayer()).updateLastMessages(e.getMessage());
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent e) {
		if(checkHelpCommand(e.getMessage(), e.getPlayer())) e.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onServerCommandPreprocess(ServerCommandEvent e) {
		checkHelpCommand("/"+e.getCommand(), e.getSender());
	}
	
	private boolean checkHelpCommand(String command, CommandSender s) {
		command = command.replace(" ", "");
		for(String helpCommand : helpCommands) {
			if(command.startsWith(helpCommand)) {
				HelpCommand.onCommand(s);
				return true;
			}
		}
		return false;
	}
	
}
