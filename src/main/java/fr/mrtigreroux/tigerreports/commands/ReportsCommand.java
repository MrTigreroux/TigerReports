package fr.mrtigreroux.tigerreports.commands;

import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.data.config.ConfigSound;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.Permission;
import fr.mrtigreroux.tigerreports.objects.users.OnlineUser;
import fr.mrtigreroux.tigerreports.objects.users.User;
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
			if(Permission.MANAGE.check(s)) {
				TigerReports.Reports.clear();
				TigerReports.Users.clear();
				TigerReports.loadFiles();
				TigerReports.initializeDatabase();
				ReportsNotifier.start();
				if(!(s instanceof Player)) MessageUtils.sendConsoleMessage(Message.RELOAD.get());
				else s.sendMessage(Message.RELOAD.get());
			}
			return true;
		}
		
		if(!UserUtils.checkPlayer(s)) return true;
		if(!Permission.STAFF.check(s)) return true;
		Player p = (Player) s;
		OnlineUser u = UserUtils.getOnlineUser(p);
		
		if(args.length == 0) u.openReportsMenu(1, true);
		else if(args.length == 1) {
			if(args[0].equalsIgnoreCase("notify")) {
				boolean newState = !u.acceptsNotifications();
				u.setStaffNotifications(newState);
				p.sendMessage(Message.STAFF_NOTIFICATIONS.get().replace("_State_", (newState ? Message.ACTIVATED : Message.DISABLED).get()));
			} else if(args[0].equalsIgnoreCase("archiveall")) {
				if(Permission.ARCHIVE.check(s)) {
					for(Map<String, Object> result : TigerReports.getDb().query("SELECT * FROM reports", null).getResultList()) {
						String status = (String) result.get("status");
						if(status == null) continue;
						else if(status.startsWith("Done")) ReportUtils.formatReport(result, true).archive(null, false);
					}
					MessageUtils.sendStaffMessage(Message.STAFF_ARCHIVEALL.get().replace("_Player_", p.getName()), ConfigSound.STAFF.get());
				}
			} else if(args[0].equalsIgnoreCase("archives")) {
				if(Permission.ARCHIVE.check(s)) u.openArchivedReportsMenu(1, true);
			} else {
				try {
					u.openReportMenu(ReportUtils.getReportById(Integer.parseInt(args[0].replace("#", ""))));
				} catch (Exception invalidIndex) {
					MessageUtils.sendErrorMessage(s, Message.INVALID_REPORT_ID.get().replace("_Id_", args[0]));
				}
			}
		} else if(args.length == 2) {
			User tu = UserUtils.getUser(UserUtils.getUniqueId(args[1]));
			if(tu == null) {
				MessageUtils.sendErrorMessage(s, Message.INVALID_PLAYER.get().replace("_Player_", args[1]));
				return true;
			}
			if(args[0].equalsIgnoreCase("stopcooldown") || args[0].equalsIgnoreCase("sc")) tu.stopCooldown(p.getName(), false);
			else if(args[0].equalsIgnoreCase("user")) u.openUserMenu(tu);
			else s.sendMessage(Message.INVALID_SYNTAX.get().replace("_Command_", "/"+label+" "+Message.REPORTS_SYNTAX.get()));
		} else s.sendMessage(Message.INVALID_SYNTAX.get().replace("_Command_", "/"+label+" "+Message.REPORTS_SYNTAX.get()));
		return true;
	}

}
