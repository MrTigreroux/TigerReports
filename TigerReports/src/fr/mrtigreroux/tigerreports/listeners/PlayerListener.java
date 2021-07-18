package fr.mrtigreroux.tigerreports.listeners;

import java.util.Arrays;
import java.util.List;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.*;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerCommandEvent;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.commands.HelpCommand;
import fr.mrtigreroux.tigerreports.data.config.ConfigFile;
import fr.mrtigreroux.tigerreports.data.constants.Permission;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.objects.Comment;
import fr.mrtigreroux.tigerreports.objects.Report;
import fr.mrtigreroux.tigerreports.objects.users.*;
import fr.mrtigreroux.tigerreports.runnables.ReportsNotifier;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;

/**
 * @author MrTigreroux
 */

public class PlayerListener implements Listener {

	private final List<String> HELP_COMMANDS = Arrays.asList("tigerreport", "helptigerreport", "reportshelp", "report?",
	        "reports?");

	@SuppressWarnings("deprecation")
	@EventHandler
	private void onPlayerJoin(PlayerJoinEvent e) {
		Player p = e.getPlayer();
		TigerReports tr = TigerReports.getInstance();
		OnlineUser u = tr.getUsersManager().getOnlineUser(p);
		FileConfiguration configFile = ConfigFile.CONFIG.get();

		Bukkit.getScheduler().runTaskLater(tr, new Runnable() {

			@Override
			public void run() {
				tr.getDb().updateUserName(p.getUniqueId().toString(), p.getName());
				u.updateImmunity(Permission.REPORT_EXEMPT.isOwned(u) ? "always" : null, false);
			}

		}, 20);

		Bukkit.getScheduler().runTaskLater(tr, new Runnable() {

			@Override
			public void run() {
				u.sendNotifications();
				if (Permission.STAFF.isOwned(u)
				        && ConfigUtils.isEnabled(configFile, "Config.Notifications.Staff.Connection")) {
					ReportsNotifier.sendReportsNotification(p);
				}
			}

		}, configFile.getInt("Config.Notifications.Delay", 2) * 20);

		if (Permission.MANAGE.isOwned(u)) {
			String newVersion = tr.getWebManager().getNewVersion();
			if (newVersion != null) {
				boolean english = ConfigUtils.getInfoLanguage().equalsIgnoreCase("English");
				p.sendMessage("\u00A77[\u00A76TigerReports\u00A77] "
				        + (english ? "\u00A7eThe plugin \u00A76TigerReports \u00A7ehas been updated."
				                : "\u00A7eLe plugin \u00A76TigerReports \u00A7ea \u00E9t\u00E9 mis à jour."));
				BaseComponent updateMessage = new TextComponent(
				        english ? "The new version \u00A77" + newVersion + " \u00A7eis available on: "
				                : "La nouvelle version \u00A77" + newVersion + " \u00A7eest disponible ici: ");
				updateMessage.setColor(ChatColor.YELLOW);
				BaseComponent button = new TextComponent(
				        english ? "\u00A77[\u00A7aOpen page\u00A77]" : "\u00A77[\u00A7aOuvrir la page\u00A77]");
				button.setColor(ChatColor.GREEN);
				button.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
				        new ComponentBuilder((english ? "\u00A76Left click \u00A77to open the plugin page\n\u00A77of"
				                : "\u00A76Clic gauche \u00A77pour ouvrir la page\n\u00A77du plugin")
				                + " \u00A7eTigerReports\u00A77.").create()));
				button.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,
				        "https://www.spigotmc.org/resources/tigerreports.25773/"));
				updateMessage.addExtra(button);
				p.spigot().sendMessage(updateMessage);
			}
		}

		tr.getBungeeManager().processPlayerConnection(p.getName());
	}

	@EventHandler(priority = EventPriority.LOWEST)
	private void onPlayerQuit(PlayerQuitEvent e) {
		TigerReports tr = TigerReports.getInstance();
		Player p = e.getPlayer();
		String uuid = p.getUniqueId().toString();
		UsersManager userManager = tr.getUsersManager();
		User u = userManager.getSavedUser(uuid);

		if (u != null) {
			List<String> lastMessages = u.lastMessages;
			if (lastMessages.isEmpty()) {
				userManager.removeUser(uuid);
			} else {
				OfflineUser ou = new OfflineUser(uuid);
				ou.lastMessages = lastMessages;
				userManager.saveUser(ou);
			}
		}

		tr.getBungeeManager().processPlayerDisconnection(p.getName());
	}

	@EventHandler(priority = EventPriority.LOWEST)
	private void onPlayerChat(AsyncPlayerChatEvent e) {
		TigerReports tr = TigerReports.getInstance();
		OnlineUser u = tr.getUsersManager().getOnlineUser(e.getPlayer());
		Comment c = u.getEditingComment();
		if (c != null) {
			String message = e.getMessage();
			Report r = c.getReport();
			if (c.getId() == null) {
				tr.getDb()
				        .insertAsynchronously(
				                "INSERT INTO tigerreports_comments (report_id,status,date,author,message) VALUES (?,?,?,?,?)",
				                Arrays.asList(r.getId(), "Private", MessageUtils.getNowDate(),
				                        u.getUniqueId().toString(), message));
			} else {
				c.addMessage(message);
			}
			u.setEditingComment(null);
			u.openDelayedlyCommentsMenu(r);
			e.setCancelled(true);
			return;
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	private void onPlayerChat2(AsyncPlayerChatEvent e) {
		OnlineUser u = TigerReports.getInstance().getUsersManager().getOnlineUser(e.getPlayer());
		u.updateLastMessages(e.getMessage());

		ConfigurationSection config = ConfigFile.CONFIG.get();
		String path = "Config.ChatReport.";
		String playerName = u.getPlayer().getName();
		if (ConfigUtils.isEnabled(config, path + "Enabled")) {
			Object message = MessageUtils.getAdvancedMessage(
			        MessageUtils.translateColorCodes(config.getString(path + "Message"))
			                .replace("_DisplayName_", u.getDisplayName())
			                .replace("_Name_", playerName)
			                .replace("_Message_", e.getMessage()),
			        "_ReportButton_", MessageUtils.translateColorCodes(config.getString(path + "ReportButton.Text")),
			        MessageUtils.translateColorCodes(config.getString(path + "ReportButton.Hover"))
			                .replace("_Player_", playerName),
			        "/tigerreports:report " + playerName + " " + config.getString(path + "ReportButton.Reason"));
			boolean isTextComponent = message instanceof TextComponent;
			for (Player p : Bukkit.getOnlinePlayers()) {
				if (isTextComponent) {
					p.spigot().sendMessage((TextComponent) message);
				} else {
					p.sendMessage((String) message);
				}
			}
			MessageUtils
			        .sendConsoleMessage(isTextComponent ? ((TextComponent) message).toLegacyText() : (String) message);
			e.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	private void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent e) {
		String command = e.getMessage().substring(1);
		if (checkHelpCommand(command, e.getPlayer())) {
			e.setCancelled(true);
		} else if (ConfigFile.CONFIG.get().getStringList("Config.CommandsHistory").contains(command.split(" ")[0])) {
			TigerReports.getInstance().getUsersManager().getOnlineUser(e.getPlayer()).updateLastMessages("/" + command);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	private void onServerCommandPreprocess(ServerCommandEvent e) {
		if (checkHelpCommand(e.getCommand(), e.getSender()))
			e.setCommand("tigerreports");
	}

	private boolean checkHelpCommand(String command, CommandSender s) {
		if (command.equalsIgnoreCase("report help")) {
			HelpCommand.onCommand(s);
			return true;
		}

		command = command.toLowerCase().replace(" ", "");
		if (!command.startsWith("tigerreports:report")) {
			for (String helpCommand : HELP_COMMANDS) {
				if (command.startsWith(helpCommand)) {
					HelpCommand.onCommand(s);
					return true;
				}
			}
		}
		return false;
	}

}
