package fr.mrtigreroux.tigerreports.listeners;

import java.util.Arrays;
import java.util.List;

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
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.logs.Logger;
import fr.mrtigreroux.tigerreports.managers.BungeeManager;
import fr.mrtigreroux.tigerreports.managers.ReportsManager;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.managers.VaultManager;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.tasks.runnables.ReportsNotifier;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * @author MrTigreroux
 */

public class PlayerListener implements Listener {

	private static final List<String> HELP_COMMANDS = Arrays.asList("tigerreport", "helptigerreport", "reportshelp",
	        "report?", "reports?");

	private final ReportsManager rm;
	private final Database db;
	private final TigerReports tr;
	private final BungeeManager bm;
	private final VaultManager vm;
	private final UsersManager um;

	public PlayerListener(ReportsManager rm, Database db, TigerReports tr, BungeeManager bm, VaultManager vm,
	        UsersManager um) {
		this.rm = rm;
		this.db = db;
		this.tr = tr;
		this.bm = bm;
		this.vm = vm;
		this.um = um;
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	private void onPlayerJoin(PlayerJoinEvent e) {
		Player p = e.getPlayer();
		User u = um.getOnlineUser(p);
		Logger.EVENTS.info(() -> "onPlayerJoin(): " + u.getName());
		FileConfiguration configFile = ConfigFile.CONFIG.get();

		tr.runTaskDelayedly(1000L, new Runnable() {

			@Override
			public void run() {
				u.updateBasicData(db, bm, um);
			}

		});

		tr.runTaskDelayedly(configFile.getInt("Config.Notifications.Delay", 2) * 1000L, new Runnable() {

			@Override
			public void run() {
				u.sendNotifications(rm, db, tr, vm, bm, um);
				if (u.hasPermission(Permission.STAFF)
				        && ConfigUtils.isEnabled(configFile, "Config.Notifications.Staff.Connection")) {
					ReportsNotifier.sendReportsNotification(p, db, tr);
				}
			}

		});

		if (u.hasPermission(Permission.MANAGE)) {
			String newVersion = tr.getNewVersion();
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

		bm.processPlayerConnection(p.getName());
	}

	@EventHandler(priority = EventPriority.LOWEST)
	private void onPlayerQuit(PlayerQuitEvent e) {
		Player p = e.getPlayer();
		Logger.EVENTS.info(() -> "onPlayerQuit(): " + p.getName());
		um.processUserDisconnection(p.getUniqueId(), vm);
		bm.processPlayerDisconnection(p.getName());
	}

	@EventHandler(priority = EventPriority.LOWEST)
	private void onPlayerChat(AsyncPlayerChatEvent e) {
		User u = um.getOnlineUser(e.getPlayer());
		Logger.EVENTS.info(() -> "onPlayerChat(): " + u.getName());
		if (u.isEditingComment()) {
			tr.runTask(() -> {
				u.terminateEditingComment(e.getMessage(), rm, db, tr, um, bm, vm);
			});
			e.setCancelled(true);
		} else if (u.isProcessPunishingWithStaffReason()) {
			tr.runTask(() -> {
				u.terminateProcessPunishingWithStaffReason(e.getMessage(), rm, db, tr, vm, bm);
			});
			e.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	private void onPlayerChat2(AsyncPlayerChatEvent e) {
		User u = um.getOnlineUser(e.getPlayer());
		Logger.EVENTS.info(() -> "onPlayerChat2(): " + u.getName());
		u.updateLastMessages(e.getMessage());

		ConfigurationSection config = ConfigFile.CONFIG.get();
		String path = "Config.ChatReport.";
		String playerName = u.getPlayer().getName();
		if (ConfigUtils.isEnabled(config, path + "Enabled")) {
			Object message = MessageUtils.getAdvancedMessage(
			        MessageUtils.translateColorCodes(config.getString(path + "Message"))
			                .replace("_DisplayName_", u.getDisplayName(vm))
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
			um.getOnlineUser(e.getPlayer()).updateLastMessages("/" + command);
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
