package fr.mrtigreroux.tigerreports.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.data.config.ConfigSound;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.Pair;
import fr.mrtigreroux.tigerreports.data.constants.Permission;
import fr.mrtigreroux.tigerreports.managers.BungeeManager;
import fr.mrtigreroux.tigerreports.objects.Report;
import fr.mrtigreroux.tigerreports.objects.users.OnlineUser;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.runnables.MenuUpdater;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;
import fr.mrtigreroux.tigerreports.utils.UserUtils;

/**
 * @author MrTigreroux
 */

public class ReportsCommand implements TabExecutor {

	private static final List<String> ACTIONS = Arrays.asList("reload", "notify", "archive", "delete", "archives",
	        "archiveall", "deleteall", "user", "stopcooldown", "#1");
	private static final List<String> USER_ACTIONS = Arrays.asList("user", "u", "stopcooldown", "sc");
	private static final List<String> DELETEALL_ARGS = Arrays.asList("archived", "unarchived");

	@Override
	public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
		TigerReports tr = TigerReports.getInstance();
		if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
			if (Permission.MANAGE.check(s)) {
				MenuUpdater.stop(true);
				tr.unload();
				tr.load();
				tr.getReportsManager().clearReports();
				tr.getUsersManager().clearUsers();
				tr.initializeDatabase();
				BungeeManager bm = tr.getBungeeManager();
				bm.collectServerName();
				bm.collectOnlinePlayers();

				if (s instanceof Player) {
					s.sendMessage(Message.RELOAD.get());
				} else {
					MessageUtils.sendConsoleMessage(Message.RELOAD.get());
				}
			}
			return true;
		}

		if (!UserUtils.checkPlayer(s) || !Permission.STAFF.check(s))
			return true;
		Player p = (Player) s;
		OnlineUser u = tr.getUsersManager().getOnlineUser(p);

		switch (args.length) {
		case 0:
			u.openReportsMenu(1, true);
			return true;
		case 1:
			switch (args[0].toLowerCase()) {
			case "canceledit":
				u.cancelComment();
				return true;
			case "notify":
				boolean newState = !u.acceptsNotifications();
				u.setStaffNotifications(newState);
				p.sendMessage(Message.STAFF_NOTIFICATIONS.get()
				        .replace("_State_", (newState ? Message.ACTIVATED : Message.DISABLED).get()));
				return true;
			case "archiveall":
				if (Permission.STAFF_ARCHIVE.check(s)) {
					tr.getDb()
					        .updateAsynchronously(
					                "UPDATE tigerreports_reports SET archived = ? WHERE archived = ? AND status LIKE 'Done%'",
					                Arrays.asList(1, 0));
					MessageUtils.sendStaffMessage(Message.STAFF_ARCHIVEALL.get().replace("_Player_", p.getName()),
					        ConfigSound.STAFF.get());
				}
				return true;
			case "archives":
				if (Permission.STAFF_ARCHIVE.check(s))
					u.openArchivedReportsMenu(1, true);
				return true;
			default:
				try {
					u.openReportMenu(Integer.parseInt(args[0].replace("#", "")));
				} catch (Exception invalidIndex) {
					MessageUtils.sendErrorMessage(s, Message.INVALID_REPORT_ID.get().replace("_Id_", args[0]));
				}
				return true;
			}
		case 2:
			switch (args[0].toLowerCase()) {
			case "deleteall":
				String reportsType = args[1];
				boolean unarchived = reportsType != null && reportsType.equalsIgnoreCase("unarchived");
				if ((unarchived || reportsType.equalsIgnoreCase("archived"))) {
					if (Permission.STAFF_DELETE.check(s)) {
						tr.getDb()
						        .updateAsynchronously("DELETE FROM tigerreports_reports WHERE archived = ?",
						                Collections.singletonList(unarchived ? 0 : 1));
						MessageUtils.sendStaffMessage(
						        Message.get("Messages.Staff-deleteall-" + (unarchived ? "un" : "") + "archived")
						                .replace("_Player_", p.getName()),
						        ConfigSound.STAFF.get());
					}
					return true;
				}
				break;
			case "delete":
				if (Permission.STAFF_DELETE.check(s)) {
					Pair<Report, Boolean> info = getReportAndArchiveInfo(args[1], s);
					if (info == null)
						return true;
					if (info.a) {
						info.r.deleteFromArchives(p.getUniqueId().toString(), false);
					} else {
						info.r.delete(p.getUniqueId().toString(), false);
					}
				}
				return true;
			case "archive":
				if (Permission.STAFF_ARCHIVE.check(s)) {
					Pair<Report, Boolean> info2 = getReportAndArchiveInfo(args[1], s);
					if (info2 != null && !info2.a)
						info2.r.archive(p.getUniqueId().toString(), false);
				}
				return true;
			case "user":
			case "u":
				User tu = getTarget(args[1], s);
				if (tu != null)
					u.openUserMenu(tu);
				return true;
			case "stopcooldown":
			case "sc":
				User tu2 = getTarget(args[1], s);
				if (tu2 != null)
					tu2.stopCooldown(p.getUniqueId().toString(), false);
				return true;
			default:
				break;
			}
			break;
		default:
			break;
		}
		for (String line : Message.get("ErrorMessages.Invalid-syntax-reports").split(ConfigUtils.getLineBreakSymbol()))
			s.sendMessage(line);
		return true;
	}

	private Pair<Report, Boolean> getReportAndArchiveInfo(String reportId, CommandSender s) {
		try {
			return TigerReports.getInstance()
			        .getReportsManager()
			        .getReportByIdAndArchiveInfo(Integer.parseInt(reportId.replace("#", "")));
		} catch (Exception invalidIndex) {
			MessageUtils.sendErrorMessage(s, Message.INVALID_REPORT_ID.get().replace("_Id_", reportId));
			return null;
		}
	}

	private User getTarget(String target, CommandSender s) {
		String tuuid = UserUtils.getUniqueId(target);
		TigerReports tr = TigerReports.getInstance();
		User tu = tr.getUsersManager().getUser(tuuid);
		if (tu == null || !UserUtils.isValid(tuuid, tr.getDb())) {
			MessageUtils.sendErrorMessage(s, Message.INVALID_PLAYER.get().replace("_Player_", target));
			return null;
		}
		return tu;
	}

	@Override
	public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] args) {
		switch (args.length) {
		case 1:
			return StringUtil.copyPartialMatches(args[0].toLowerCase(), ACTIONS, new ArrayList<>());
		case 2:
			switch (args[0].toLowerCase()) {
			case "deleteall":
				return StringUtil.copyPartialMatches(args[1].toLowerCase(), DELETEALL_ARGS, new ArrayList<>());
			case "archive":
			case "delete":
				return StringUtil.copyPartialMatches(args[1].toLowerCase(), Collections.singletonList("#1"),
				        new ArrayList<>());
			default:
				return USER_ACTIONS.contains(args[0].toLowerCase()) && s instanceof Player
				        ? StringUtil.copyPartialMatches(args[1], UserUtils.getOnlinePlayersForPlayer((Player) s, false),
				                new ArrayList<>())
				        : new ArrayList<>();
			}
		default:
			return new ArrayList<>();
		}
	}

}
