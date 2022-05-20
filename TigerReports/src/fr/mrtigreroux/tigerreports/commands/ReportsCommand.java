package fr.mrtigreroux.tigerreports.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.data.config.ConfigSound;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.Permission;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.managers.BungeeManager;
import fr.mrtigreroux.tigerreports.managers.ReportsManager;
import fr.mrtigreroux.tigerreports.managers.UpdatesManager;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.managers.VaultManager;
import fr.mrtigreroux.tigerreports.objects.reports.Report;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.tasks.ResultCallback;
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

	private final ReportsManager rm;
	private final Database db;
	private final TigerReports tr;
	private final BungeeManager bm;
	private final VaultManager vm;
	private final UsersManager um;

	public ReportsCommand(ReportsManager rm, Database db, TigerReports tr, BungeeManager bm, VaultManager vm,
	        UsersManager um) {
		this.rm = rm;
		this.db = db;
		this.tr = tr;
		this.bm = bm;
		this.vm = vm;
		this.um = um;
	}

	@Override
	public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
		if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
			if (Permission.MANAGE.check(s)) {
				tr.unload(false);
				tr.load();

				if (s instanceof Player) {
					s.sendMessage(Message.RELOAD.get());
				} else {
					MessageUtils.sendConsoleMessage(Message.RELOAD.get());
				}
			}
			return true;
		}

		if (args.length > 2 && args[0].equalsIgnoreCase("comment") && Permission.STAFF.check(s)) {
			String reportIdStr = args[1];
			int reportId = getReportIdOrSendError(args[1], s);
			if (reportId < 0) {
				return true;
			}
			rm.getReportByIdAsynchronously(reportId, false, true, true, db, tr, um, new ResultCallback<Report>() {

				@Override
				public void onResultReceived(Report r) {
					if (r == null) {
						MessageUtils.sendErrorMessage(s, Message.INVALID_REPORT_ID.get().replace("_Id_", reportIdStr));
						return;
					}

					Player p = s instanceof Player ? (Player) s : null;
					String author = p != null ? p.getUniqueId().toString() : s.getName();
					StringBuilder sb = new StringBuilder();
					for (int argIndex = 2; argIndex < args.length; argIndex++) {
						sb.append(args[argIndex]).append(" ");
					}
					String message = sb.toString().trim();

					r.addComment(author, message, db, tr, ResultCallback.NOTHING);
					if (p != null) {
						um.getOnlineUser(p).openCommentsMenu(1, r, rm, db, tr, um, bm, vm);
					}
				}

			});
			return true;
		}

		if (args.length == 2 && args[0].equals("update_data") && Permission.MANAGE.check(s)) {
			UpdatesManager.runUpdatesInstructions(args[1], tr, tr, db);
			UpdatesManager.updateLastVersionUsed(tr);
			tr.updateNeedUpdatesInstructions(false);
			if (s instanceof Player) {
				ConfigSound.STAFF.play((Player) s);
			}
			s.sendMessage("\u00A77[\u00A76TigerReports\u00A77] " + ConfigUtils.getInfoMessage(
			        "\u00A7eData should have been \u00A7aupdated\u00A7e. Check if there is an error in the logs/console.",
			        "\u00A7eLes donn\u00E9es devraient avoir \u00E9t\u00E9 \u00A7amises \u00E0 jour\u00A7e. V\u00E9rifiez qu'il n'y ait pas d'erreur dans les logs/console."));
			return true;
		}

		if (!UserUtils.checkPlayer(s) || !Permission.STAFF.check(s)) {
			return true;
		}
		Player p = (Player) s;
		User u = um.getOnlineUser(p);

		switch (args.length) {
		case 0:
			u.openReportsMenu(1, true, rm, db, tr, vm, bm, um);
			return true;
		case 1:
			switch (args[0].toLowerCase()) {
			case "canceledit":
				u.cancelEditingComment();
				u.cancelProcessPunishingWithStaffReason();
				return true;
			case "notify":
				boolean newState = !u.acceptsNotifications();
				u.setStaffNotifications(newState);
				p.sendMessage(Message.STAFF_NOTIFICATIONS.get()
				        .replace("_State_", (newState ? Message.ACTIVATED : Message.DISABLED).get()));
				return true;
			case "archiveall":
				if (Permission.STAFF_ARCHIVE.check(s)) {
					db.updateAsynchronously(
					        "UPDATE tigerreports_reports SET archived = ? WHERE archived = ? AND status LIKE 'Done%'",
					        Arrays.asList(1, 0));
					MessageUtils.sendStaffMessage(
					        Message.STAFF_ARCHIVEALL.get().replace("_Player_", u.getDisplayName(vm, true)),
					        ConfigSound.STAFF.get());
				}
				return true;
			case "archives":
				if (Permission.STAFF_ARCHIVE.check(s)) {
					u.openArchivedReportsMenu(1, true, rm, db, tr, vm, bm, um);
				}
				return true;
			default:
				int reportId = getReportIdOrSendError(args[0], s);
				if (reportId >= 0) {
					u.openReportMenu(reportId, rm, db, tr, vm, bm, um);
				}
				return true;
			}
		case 2:
			switch (args[0].toLowerCase()) {
			case "deleteall":
				String reportsType = args[1];
				boolean unarchived = reportsType != null && reportsType.equalsIgnoreCase("unarchived");
				if (unarchived || (reportsType != null && reportsType.equalsIgnoreCase("archived"))) {
					if (Permission.STAFF_DELETE.check(s)) {
						db.updateAsynchronously("DELETE FROM tigerreports_reports WHERE archived = ?",
						        Collections.singletonList(unarchived ? 0 : 1));
						MessageUtils
						        .sendStaffMessage(
						                Message.get("Messages.Staff-deleteall-" + (unarchived ? "un" : "") + "archived")
						                        .replace("_Player_", u.getDisplayName(vm, true)),
						                ConfigSound.STAFF.get());
					}
					return true;
				}
				break;
			case "delete":
				if (Permission.STAFF_DELETE.check(s)) {
					getReportAndArchiveInfo(args[1], s, db, new ResultCallback<Report>() {

						@Override
						public void onResultReceived(Report r) {
							if (r == null) {
								return;
							}
							r.delete(u, false, db, tr, rm, vm, bm);
						}
					});
				}
				return true;
			case "archive":
				if (Permission.STAFF_ARCHIVE.check(s)) {
					getReportAndArchiveInfo(args[1], s, db, new ResultCallback<Report>() {

						@Override
						public void onResultReceived(Report r) {
							if (r != null && !r.isArchived()) {
								r.archive(u, false, db);
							}
						}
					});
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
				MessageUtils.sendErrorMessage(s, Message.INVALID_TIME.get().replace("_Time_", args[2]));
				return true;
			} else {
				break;
			}
		default:
			break;
		}
		for (String line : Message.INVALID_SYNTAX_REPORTS.get().split(ConfigUtils.getLineBreakSymbol())) {
			s.sendMessage(line);
		}
		return true;
	}

	private int getReportIdOrSendError(String reportId, CommandSender s) {
		int id = -1;
		try {
			id = Integer.parseInt(reportId.replace("#", ""));
		} catch (NumberFormatException ex) {
			MessageUtils.sendErrorMessage(s, Message.INVALID_REPORT_ID.get().replace("_Id_", reportId));
		}
		return id;
	}

	private void getReportAndArchiveInfo(String reportId, CommandSender s, Database db,
	        ResultCallback<Report> resultCallback) {
		int id = getReportIdOrSendError(reportId, s);
		if (id >= 0) {
			rm.getReportByIdAsynchronously(id, false, false, true, db, tr, um, resultCallback);
		} else {
			resultCallback.onResultReceived(null);
		}
	}

	private void processCommandWithTarget(User u, String target, String command, long punishSeconds) {
		UUID tuuid = UserUtils.getUniqueId(target);
		UserUtils.checkUserExistsAsynchronously(tuuid, db, tr, new ResultCallback<Boolean>() {

			@Override
			public void onResultReceived(Boolean targetExists) {
				if (!targetExists) {
					u.sendErrorMessage(Message.INVALID_PLAYER.get().replace("_Player_", target));
					return;
				}

				um.getUserAsynchronously(tuuid, db, tr, new ResultCallback<User>() {

					@Override
					public void onResultReceived(User tu) {
						if (tu == null) {
							u.sendErrorMessage(Message.INVALID_PLAYER.get().replace("_Player_", target));
							return;
						}

						if (command.equalsIgnoreCase("stopcooldown")) {
							tu.stopCooldown(u, false, db, bm);
						} else if (command.equalsIgnoreCase("punish")) {
							tu.punish(punishSeconds, u, false, db, bm, vm);
						} else {
							u.openUserMenu(tu, rm, db, tr, vm, um);
						}
					}
				});

			}
		});
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
				        ? StringUtil.copyPartialMatches(args[1],
				                UserUtils.getOnlinePlayersForPlayer((Player) s, false, um, bm), new ArrayList<>())
				        : new ArrayList<>();
			}
		default:
			return new ArrayList<>();
		}
	}

}
