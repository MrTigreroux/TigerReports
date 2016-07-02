package fr.mrtigreroux.tigerreports.commands;

import java.util.UUID;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.mrtigreroux.tigerreports.data.Message;
import fr.mrtigreroux.tigerreports.data.Permission;
import fr.mrtigreroux.tigerreports.data.Status;
import fr.mrtigreroux.tigerreports.data.UserData;
import fr.mrtigreroux.tigerreports.managers.FilesManager;
import fr.mrtigreroux.tigerreports.objects.User;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;
import fr.mrtigreroux.tigerreports.utils.ReportUtils;
import fr.mrtigreroux.tigerreports.utils.UserUtils;

/**
 * @author MrTigreroux
 */

public class ReportsCommand implements CommandExecutor {
	
	@Override
	public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
		if(args.length == 1 && args[0].equalsIgnoreCase("reload")) {
			if(!s.hasPermission(Permission.MANAGE.get())) MessageUtils.sendErrorMessage(s, Message.PERMISSION_COMMAND.get());
			else {
				FilesManager.loadConfig(FilesManager.getConfig, FilesManager.config);
				FilesManager.loadConfig(FilesManager.getMessages, FilesManager.messages);
				FilesManager.loadConfig(FilesManager.getReports, FilesManager.reports);
				FilesManager.loadConfig(FilesManager.getData, FilesManager.data);
				s.sendMessage(Message.RELOAD.get());
			}
			return true;
		}
		
		if(!UserUtils.isPlayer(s)) return true;
		Player p = (Player) s;
		User u = new User(p);
		if(!u.hasPermission(Permission.STAFF)) {
			MessageUtils.sendErrorMessage(s, Message.PERMISSION_COMMAND.get());
			return true;
		}
		
		if(args.length == 0) u.openReportsMenu(1, true);
		else if(args.length == 1) {
			if(args[0].equalsIgnoreCase("notify")) {
				UUID uuid = p.getUniqueId();
				if(UserData.NotificationsDisabled.contains(uuid)) {
					UserData.NotificationsDisabled.remove(uuid);
					p.sendMessage(Message.NOTIFICATIONS.get().replaceAll("_State_", Message.ACTIVATED.get()));
				} else {
					UserData.NotificationsDisabled.add(uuid);
					p.sendMessage(Message.NOTIFICATIONS.get().replaceAll("_State_", Message.DISABLED.get()));
				}
			} else if(args[0].equalsIgnoreCase("archiveall")) {
				if(!u.hasPermission(Permission.ARCHIVE)) MessageUtils.sendErrorMessage(s, Message.PERMISSION_COMMAND.get());
				else {
					for(int reportNumber = 1; reportNumber <= ReportUtils.getTotalReports(); reportNumber++)
						if(ReportUtils.getStatus(reportNumber) == Status.DONE) {
							ReportUtils.archive(reportNumber);
							reportNumber--;
						}
					MessageUtils.sendStaffMessage(Message.STAFF_ARCHIVEALL.get().replaceAll("_Player_", p.getName()), ConfigUtils.getStaffSound());
				}
			} else {
				try {
					u.openReportMenu(Integer.parseInt(args[0].replaceAll("#", "")));
				} catch(Exception invalidNumber) {
					MessageUtils.sendErrorMessage(s, Message.INVALID_REPORTNUMBER.get().replaceAll("_Number_", args[0]));
				}
			}
		} else if(args.length == 2) {
			Player t = UserUtils.getPlayer(args[1]);
			if(t == null) {
				MessageUtils.sendErrorMessage(s, Message.INVALID_PLAYER.get().replaceAll("_Player_", args[1]));
				return true;
			}
			
			if(args[0].equalsIgnoreCase("stopcooldown")) new User(t).stopCooldown(p.getName());
			else if(args[0].equalsIgnoreCase("user")) u.openUserMenu(t.getUniqueId().toString());
		} else s.sendMessage(Message.INVALID_SYNTAX.get().replaceAll("_Command_", "/"+label+" (reload / notify / archiveall / #<numéro du signalement> / stopcooldown) (joueur)"));
		return true;
	}

}
