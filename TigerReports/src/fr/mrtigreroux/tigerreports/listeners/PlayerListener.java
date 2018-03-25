package fr.mrtigreroux.tigerreports.listeners;

import java.util.Arrays;
import java.util.List;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.*;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.server.ServerCommandEvent;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.commands.HelpCommand;
import fr.mrtigreroux.tigerreports.data.config.ConfigFile;
import fr.mrtigreroux.tigerreports.data.constants.Permission;
import fr.mrtigreroux.tigerreports.objects.users.OfflineUser;
import fr.mrtigreroux.tigerreports.objects.users.OnlineUser;
import fr.mrtigreroux.tigerreports.runnables.ReportsNotifier;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.UserUtils;

/**
 * @author MrTigreroux
 */

public class PlayerListener implements Listener {

	private final static List<String> helpCommands = Arrays.asList("tigerreport", "helptigerreport", "reportshelp", "report?", "reports?");
	
	@EventHandler
	private void onPlayerJoin(PlayerJoinEvent e) {
		Player p = e.getPlayer();
		OnlineUser u = UserUtils.getOnlineUser(p);
		for(String notification : u.getNotifications()) u.sendNotification(notification, false);
		if(Permission.STAFF.isOwned(u) && ConfigUtils.isEnabled(ConfigFile.CONFIG.get(), "Config.ReportsNotifications.Connection")) {
			String reportsNotifications = ReportsNotifier.getReportsNotification();
			if(reportsNotifications != null) p.sendMessage(reportsNotifications);
		}
		
		TigerReports.getDb().updateAsynchronously("REPLACE INTO tigerreports_users (uuid,name) VALUES (?,?);", Arrays.asList(p.getUniqueId().toString(), p.getName()));
		u.updateImmunity(Permission.REPORT_EXEMPT.isOwned(u) ? "always" : null, false);
		
		if(Permission.MANAGE.isOwned(u)) {
			String newVersion = TigerReports.getWebManager().getNewVersion();
			if(newVersion != null) {
				boolean english = ConfigUtils.getInfoLanguage().equalsIgnoreCase("English");
				p.sendMessage("§7[§6TigerReports§7] "+(english ? "§eThe plugin §6TigerReports §ehas been updated." : "§eLe plugin §6TigerReports §ea été mis à jour."));
				BaseComponent updateMessage = new TextComponent(english ? "New version §7"+newVersion+" §eis available on: " : "La nouvelle version §7"+newVersion+" §eest disponible ici: ");
				updateMessage.setColor(ChatColor.YELLOW);
				BaseComponent button = new TextComponent(english ? "§7[§aOpen page§7]" : "§7[§aOuvrir la page§7]");
				button.setColor(ChatColor.GREEN);
				button.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder((english ? "§6Left click §7to open plugin page\n§7of" : "§6Clic gauche §7pour ouvrir la page\n§7du plugin")+" §eTigerReports§7.").create()));
				button.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://www.spigotmc.org/resources/tigerreports.25773/"));
				updateMessage.addExtra(button);
				p.spigot().sendMessage(updateMessage);
			}
		}
		
		TigerReports.getBungeeManager().collectServerName();
	}
	
	@EventHandler
	private void onPlayerQuit(PlayerQuitEvent e) {
		String uuid = e.getPlayer().getUniqueId().toString();
		if(TigerReports.Users.containsKey(uuid)) {
			List<String> lastMessages = TigerReports.Users.get(uuid).lastMessages;
			TigerReports.Users.remove(uuid);
			if(!lastMessages.isEmpty()) {
				OfflineUser ou = new OfflineUser(uuid);
				ou.lastMessages = lastMessages;
				ou.save();
			}
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	private void onPlayerLogin(PlayerLoginEvent e) {
		String error = TigerReports.getWebManager().check(e.getPlayer().getUniqueId().toString());
		if(error != null) e.disallow(Result.KICK_OTHER, error);
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onPlayerChat(AsyncPlayerChatEvent e) {
		UserUtils.getOnlineUser(e.getPlayer()).updateLastMessages(e.getMessage());
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	private void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent e) {
		String command = e.getMessage();
		if(checkHelpCommand(command.substring(1), e.getPlayer())) e.setCancelled(true);
		else if(ConfigFile.CONFIG.get().getStringList("Config.CommandsHistory").contains(command.split(" ")[0])) UserUtils.getOnlineUser(e.getPlayer()).updateLastMessages(command);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	private void onServerCommandPreprocess(ServerCommandEvent e) {
		if(checkHelpCommand(e.getCommand(), e.getSender())) e.setCommand("tigerreports");
	}
	
	private boolean checkHelpCommand(String command, CommandSender s) {
		if(command.equalsIgnoreCase("report help")) {
			HelpCommand.onCommand(s);
			return true;
		}
		command = command.toLowerCase().replace(" ", "");
		if(!command.startsWith("tigerreports:report")) {
			for(String helpCommand : helpCommands) {
				if(command.startsWith(helpCommand)) {
					HelpCommand.onCommand(s);
					return true;
				}
			}
		}
		return false;
	}
	
}
