package fr.mrtigreroux.tigerreports.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.data.ConfigSound;
import fr.mrtigreroux.tigerreports.data.Message;
import fr.mrtigreroux.tigerreports.data.Permission;
import fr.mrtigreroux.tigerreports.data.Status;
import fr.mrtigreroux.tigerreports.objects.Report;
import fr.mrtigreroux.tigerreports.objects.User;
import fr.mrtigreroux.tigerreports.runnables.ReportsNotifier;
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
				TigerReports.loadFiles();
				ReportsNotifier.start();
				s.sendMessage(Message.RELOAD.get());
			}
			return true;
		}
		
		if(!UserUtils.checkPlayer(s)) return true;
		if(!s.hasPermission(Permission.STAFF.get())) {
			MessageUtils.sendErrorMessage(s, Message.PERMISSION_COMMAND.get());
			return true;
		}
		Player p = (Player) s;
		User u = UserUtils.getUser(p);
		
		if(args.length == 0) u.openReportsMenu(1, true);
		else if(args.length == 1) {
			if(args[0].equalsIgnoreCase("notify")) {
				boolean acceptsNotifications = u.acceptsNotifications();
				u.setNotifications(!acceptsNotifications);
				p.sendMessage(Message.NOTIFICATIONS.get().replace("_State_", (acceptsNotifications ? Message.DISABLED : Message.ACTIVATED).get()));
			} else if(args[0].equalsIgnoreCase("archiveall")) {
				if(!u.hasPermission(Permission.ARCHIVE)) MessageUtils.sendErrorMessage(s, Message.PERMISSION_COMMAND.get());
				else {
					for(int reportNumber = 1; reportNumber <= ReportUtils.getTotalReports(); reportNumber++) {
						Report r = new Report(reportNumber);
						if(r.getStatus() == Status.DONE) {
							r.archive();
							reportNumber--;
						}
					}
					MessageUtils.sendStaffMessage(Message.STAFF_ARCHIVEALL.get().replace("_Player_", p.getName()), ConfigSound.STAFF.get());
				}
			} else {
				try {
					u.openReportMenu(new Report(Integer.parseInt(args[0].replace("#", ""))));
				} catch(Exception invalidNumber) {
					MessageUtils.sendErrorMessage(s, Message.INVALID_REPORTNUMBER.get().replace("_Number_", args[0]));
				}
			}
		} else if(args.length == 2) {
			if(args[0].equalsIgnoreCase("stopcooldown") || args[0].equalsIgnoreCase("sc")) {
				Player t = UserUtils.getPlayer(args[1]);
				if(t == null) {
					MessageUtils.sendErrorMessage(s, Message.PLAYER_OFFLINE.get().replace("_Player_", args[1]));
					return true;
				}
				UserUtils.getUser(t).stopCooldown(p.getName());
			} else if(args[0].equalsIgnoreCase("user")) {
				String target = UserUtils.getUniqueId(args[1]);
				if(!UserUtils.isValid(target)) {
					MessageUtils.sendErrorMessage(s, Message.INVALID_PLAYER.get().replace("_Player_", args[1]));
					return true;
				}
				u.openUserMenu(UserUtils.getUniqueId(args[1]));
			} else s.sendMessage(Message.INVALID_SYNTAX.get().replace("_Command_", "/"+label+" "+Message.REPORTS_SYNTAX.get()));
		} else s.sendMessage(Message.INVALID_SYNTAX.get().replace("_Command_", "/"+label+" "+Message.REPORTS_SYNTAX.get()));
		return true;
	}

}
