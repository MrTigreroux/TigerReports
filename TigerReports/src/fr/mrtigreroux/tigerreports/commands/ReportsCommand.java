package fr.mrtigreroux.tigerreports.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.data.config.ConfigSound;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.Permission;
import fr.mrtigreroux.tigerreports.objects.users.OnlineUser;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.runnables.MenuUpdater;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;
import fr.mrtigreroux.tigerreports.utils.ReportUtils;
import fr.mrtigreroux.tigerreports.utils.UserUtils;

/**
 * @author MrTigreroux
 */

public class ReportsCommand implements TabExecutor {
	
	private final List<String> Actions = Arrays.asList("reload", "notify", "archiveall", "archives", "deleteall", "user", "stopcooldown", "#1");
	private final List<String> UserActions = Arrays.asList("user", "u", "stopcooldown", "sc");
	
	@Override
	public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
		if(args.length == 1 && args[0].equalsIgnoreCase("reload")) {
			if(Permission.MANAGE.check(s)) {
				MenuUpdater.stop(true);
				TigerReports.Reports.clear();
				TigerReports.Users.clear();
				TigerReports.unload();
				TigerReports.load();
				TigerReports.initializeDatabase();
				if(!(s instanceof Player)) MessageUtils.sendConsoleMessage(Message.RELOAD.get());
				else s.sendMessage(Message.RELOAD.get());
			}
			return true;
		}
		
		if(!UserUtils.checkPlayer(s) || !Permission.STAFF.check(s)) return true;
		Player p = (Player) s;
		OnlineUser u = UserUtils.getOnlineUser(p);
		
		switch(args.length) {
			case 0: u.openReportsMenu(1, true); break;
			case 1:
				switch(args[0].toLowerCase()) {
					case "notify":
						boolean newState = !u.acceptsNotifications();
						u.setStaffNotifications(newState);
						p.sendMessage(Message.STAFF_NOTIFICATIONS.get().replace("_State_", (newState ? Message.ACTIVATED : Message.DISABLED).get()));
						break;
					case "archiveall":
						if(Permission.STAFF_ARCHIVE.check(s)) {
							for(Map<String, Object> result : TigerReports.getDb().query("SELECT * FROM tigerreports_reports", null).getResultList()) {
								String status = (String) result.get("status");
								if(status != null && status.startsWith("Done")) ReportUtils.formatReport(result, true).archive(null, false);
							}
							MessageUtils.sendStaffMessage(Message.STAFF_ARCHIVEALL.get().replace("_Player_", p.getName()), ConfigSound.STAFF.get());
						}
						break;
					case "archives": if(Permission.STAFF_ARCHIVE.check(s)) u.openArchivedReportsMenu(1, true); break;
					case "deleteall":
						if(Permission.STAFF_DELETE.check(s)) {
							TigerReports.getDb().update("DELETE FROM archived_reports;", null);
							MessageUtils.sendStaffMessage(Message.STAFF_DELETEALL.get().replace("_Player_", p.getName()), ConfigSound.STAFF.get());
						}
						break;
					default:
						try {
							u.openReportMenu(ReportUtils.getReportById(Integer.parseInt(args[0].replace("#", ""))));
						} catch (Exception invalidIndex) {
							MessageUtils.sendErrorMessage(s, Message.INVALID_REPORT_ID.get().replace("_Id_", args[0]));
						}
						break;
				}
				break;
			case 2:
				User tu = UserUtils.getUser(UserUtils.getUniqueId(args[1]));
				if(tu == null) {
					MessageUtils.sendErrorMessage(s, Message.INVALID_PLAYER.get().replace("_Player_", args[1]));
					return true;
				}
				switch(args[0].toLowerCase()) {
					case "user": case "u": u.openUserMenu(tu); break;
					case "stopcooldown": case "sc": tu.stopCooldown(p.getName(), false); break;
					default: s.sendMessage(Message.INVALID_SYNTAX.get().replace("_Command_", "/"+label+" "+Message.REPORTS_SYNTAX.get())); break;
				}
				break;
			default: s.sendMessage(Message.INVALID_SYNTAX.get().replace("_Command_", "/"+label+" "+Message.REPORTS_SYNTAX.get())); break;
		}
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] args) {
		switch(args.length) {
			case 1: return StringUtil.copyPartialMatches(args[0], Actions, new ArrayList<>());
			case 2: return UserActions.contains(args[0].toLowerCase()) ? StringUtil.copyPartialMatches(args[1], UserUtils.getOnlinePlayers(""), new ArrayList<>()) : new ArrayList<>();
			default: return new ArrayList<>();
		}
	}

}
