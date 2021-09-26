package fr.mrtigreroux.tigerreports.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
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

	private static final List<String> ACTIONS = Arrays.asList("reload", "notify", "archive", "delete", "comment",
	        "archives", "archiveall", "deleteall", "user", "stopcooldown", "punish", "#1");
	private static final List<String> USER_ACTIONS = Arrays.asList("user", "u", "stopcooldown", "sc", "punish");
	private static final List<String> DELETEALL_ARGS = Arrays.asList("archived", "unarchived");

	private TigerReports tr;

	public ReportsCommand(TigerReports tr) {
		this.tr = tr;
	}

	@Override
	public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
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

		if (args.length > 2 && args[0].equalsIgnoreCase("comment") && Permission.STAFF.check(s)) {
			String reportId = args[1];
			Report r = tr.getReportsManager().getReportById(getReportId(reportId), false);
			if (r == null) {
				MessageUtils.sendErrorMessage(s, Message.INVALID_REPORT_ID.get().replace("_Id_", reportId));
				return true;
			}

			Player p = s instanceof Player ? (Player) s : null;
			String author = p != null ? p.getUniqueId().toString() : s.getName();
			StringBuilder sb = new StringBuilder();
			for (int argIndex = 2; argIndex < args.length; argIndex++)
				sb.append(args[argIndex]).append(" ");
			String message = sb.toString().trim();

			r.addComment(author, message, tr.getDb());
			if (p != null) {
				tr.getUsersManager().getOnlineUser(p).openDelayedlyCommentsMenu(r);
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
					u.openReportMenu(getReportId(args[0]));
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
				processCommandWithTarget(u, args[1], "user", 0);
				return true;
			case "stopcooldown":
			case "sc":
				processCommandWithTarget(u, args[1], "stopcooldown", 0);
				return true;
			default:
				break;
			}
			break;
		case 3:
			if (args[0].equalsIgnoreCase("punish")) {
				try {
					long punishSeconds = Long.parseLong(args[2]);
					if (punishSeconds > 0) {
						processCommandWithTarget(u, args[1], "punish", punishSeconds);
						return true;
					}
				} catch (Exception ex) {}
				MessageUtils.sendErrorMessage(s, Message.get("ErrorMessages.Invalid-time").replace("_Time_", args[2]));
				return true;
			} else {
				break;
			}
		default:
			break;
		}
		for (String line : Message.get("ErrorMessages.Invalid-syntax-reports").split(ConfigUtils.getLineBreakSymbol()))
			s.sendMessage(line);
		return true;
	}

	private int getReportId(String reportId) {
		return Integer.parseInt(reportId.replace("#", ""));
	}

	private Pair<Report, Boolean> getReportAndArchiveInfo(String reportId, CommandSender s) {
		try {
			return tr.getReportsManager().getReportByIdAndArchiveInfo(getReportId(reportId));
		} catch (Exception invalidIndex) {
			MessageUtils.sendErrorMessage(s, Message.INVALID_REPORT_ID.get().replace("_Id_", reportId));
			return null;
		}
	}

	private void processCommandWithTarget(OnlineUser u, String target, String command, long punishSeconds) {
		Bukkit.getScheduler().runTaskAsynchronously(tr, new Runnable() {

			@Override
			public void run() {
				String tuuid = UserUtils.getUniqueId(target);
				User tu = tr.getUsersManager().getUser(tuuid);
				boolean invalidTarget = tu == null || !UserUtils.isValid(tuuid, tr.getDb());
				Bukkit.getScheduler().runTask(tr, new Runnable() {

					@Override
					public void run() {
						if (invalidTarget) {
							MessageUtils.sendErrorMessage(u.getPlayer(),
							        Message.INVALID_PLAYER.get().replace("_Player_", target));
							return;
						}

						if (command.equalsIgnoreCase("stopcooldown")) {
							tu.stopCooldown(u.getUniqueId().toString(), false);
						} else if (command.equalsIgnoreCase("punish")) {
							tu.punish(punishSeconds, u.getUniqueId(), false);
						} else {
							u.openUserMenu(tu);
						}
					}

				});
			}

		});
		return;
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
