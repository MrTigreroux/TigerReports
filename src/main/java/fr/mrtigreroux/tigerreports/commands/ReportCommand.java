package fr.mrtigreroux.tigerreports.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import fr.mrtigreroux.tigerreports.data.config.ConfigFile;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.Permission;
import fr.mrtigreroux.tigerreports.data.constants.Statistic;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.logs.Logger;
import fr.mrtigreroux.tigerreports.managers.BungeeManager;
import fr.mrtigreroux.tigerreports.managers.ReportsManager;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.managers.VaultManager;
import fr.mrtigreroux.tigerreports.objects.reports.Report;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.tasks.ResultCallback;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.DatetimeUtils;
import fr.mrtigreroux.tigerreports.utils.LogUtils;
import fr.mrtigreroux.tigerreports.utils.ReportUtils;
import fr.mrtigreroux.tigerreports.utils.UserUtils;

/**
 * @author MrTigreroux
 */

public class ReportCommand implements TabExecutor {

	private static final Logger LOGGER = Logger.fromClass(ReportCommand.class);
	private static final int MAX_DEFAULT_REASONS_AMOUNT = 1000;

	private final TaskScheduler taskScheduler;
	private final ReportsManager rm;
	private final Database db;
	private final BungeeManager bm;
	private final VaultManager vm;
	private final UsersManager um;

	public ReportCommand(TaskScheduler tr, ReportsManager rm, Database db, BungeeManager bm, VaultManager vm,
	        UsersManager um) {
		taskScheduler = tr;
		this.rm = rm;
		this.db = db;
		this.bm = bm;
		this.vm = vm;
		this.um = um;
	}

	@Override
	public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
		if (!UserUtils.checkPlayer(s) || (ReportUtils.permissionRequiredToReport() && !Permission.REPORT.check(s))) {
			return true;
		}

		FileConfiguration configFile = ConfigFile.CONFIG.get();
		if (args.length == 0
		        || (args.length == 1 && !ConfigUtils.exists(configFile, "Config.DefaultReasons.Reason1"))) {
			s.sendMessage(Message.INVALID_SYNTAX_REPORT.get());
			return true;
		}

		Player p = (Player) s;
		User u = um.getOnlineUser(p);
		if (u == null) {
			LogUtils.logUnexpectedOfflineUser(LOGGER, "onCommand()", p);
			return true;
		}
		LOGGER.info(
		        () -> "user = " + u + ", user name = " + u.getName() + ", p = u.getPlayer() ? " + (p == u.getPlayer()));

		u.getCooldownAsynchronously(db, taskScheduler, (cooldown) -> {
			LOGGER.info(() -> "user cooldown = " + cooldown);
			if (cooldown != null) {
				LOGGER.info(() -> "under cooldown, cancelled");
				u.sendErrorMessage(Message.COOLDOWN.get().replace("_Time_", cooldown));
				return;
			}

			final String reportedName = args[0];
			boolean reportOneself = reportedName.equalsIgnoreCase(p.getName());
			if (reportOneself && !u.hasPermission(Permission.MANAGE)) {
				LOGGER.info(() -> "report oneself no permission");
				u.sendErrorMessage(Message.REPORT_ONESELF.get());
				return;
			}

			um.getUserByNameAsynchronously(reportedName, db, taskScheduler, (ru) -> {
				if (ru == null) {
					LOGGER.info(() -> "reported user does not exist");
					u.sendErrorMessage(Message.INVALID_PLAYER.get().replace("_Player_", reportedName));
					return;
				}
				LOGGER.info(() -> "reported user = " + ru.getName() + ", is online: " + ru.isOnline());

				processReportCommand(args, u, ru, reportOneself, configFile);
			});
		});
		return true;
	}

	private void processReportCommand(String[] args, User u, User ru, boolean reportOneself,
	        FileConfiguration configFile) {
		Player p = u.getPlayer();
		Player rp = ru.getPlayer();

		LOGGER.info(() -> "processReportCommand()");
		if (ReportUtils.onlinePlayerRequired() && ((rp != null && !p.canSee(rp)) || !ru.isOnlineInNetwork(bm))) {
			LOGGER.info(() -> "processReportCommand(): reported offline");
			u.sendErrorMessage(Message.REPORTED_OFFLINE.get().replace("_Player_", ru.getName()));
			return;
		}

		ru.getImmunityAsynchronously(db, taskScheduler, um, bm, new ResultCallback<String>() {

			@Override
			public void onResultReceived(String reportedImmunity) {
				LOGGER.info(() -> "processReportCommand(): reportedImmunity = " + reportedImmunity);
				if (reportedImmunity != null && !reportOneself) {
					if (User.IMMUNITY_ALWAYS.equals(reportedImmunity)) {
						u.sendErrorMessage(Message.PERMISSION_REPORT.get().replace("_Player_", ru.getName()));
					} else {
						u.sendErrorMessage(Message.PLAYER_ALREADY_REPORTED.get()
						        .replace("_Player_", ru.getName())
						        .replace("_Time_", reportedImmunity));
					}
					return;
				}

				if (args.length == 1) {
					LOGGER.info(() -> "processReportCommand(): no reason, open reason menu");
					u.openReasonMenu(1, ru, db, vm);
					return;
				}

				String reason = getReason(args);
				if (reason.length() < ReportUtils.getMinCharacters()) {
					u.sendErrorMessage(Message.TOO_SHORT_REASON.get().replace("_Reason_", reason));
					return;
				}
				String lineBreak = ConfigUtils.getLineBreakSymbol();
				if (lineBreak.length() >= 1) {
					reason = reason.replace(lineBreak, lineBreak.substring(0, 1));
				}

				if (!ConfigUtils.isEnabled(configFile, "Config.CustomReasons")) {
					for (int reasonIndex = 1; reasonIndex <= MAX_DEFAULT_REASONS_AMOUNT; reasonIndex++) {
						String defaultReasonPath = "Config.DefaultReasons.Reason" + reasonIndex;
						String defaultReason = configFile.getString(defaultReasonPath + ".Name");
						if (defaultReason == null) {
							if (configFile.get(defaultReasonPath) != null) {
								continue;
							}
							u.openReasonMenu(1, ru, db, vm);
							return;
						} else if (reason.equalsIgnoreCase(defaultReason)) {
							reason = defaultReason;
							break;
						}
					}
				}

				final String freason = reason;
				final String date = DatetimeUtils.getNowDatetime();

				if (ReportUtils.stackReports()) {
					LOGGER.info(() -> "processReportCommand(): stackReports, checking if similar existing report");
					ReportUtils.stackReportAsynchronously(u.getUniqueId().toString(), ru.getUniqueId().toString(),
					        freason, date, ConfigUtils.isEnabled(configFile, "Config.UpdateDateOfStackedReports"),
					        taskScheduler, db, rm, um, (result) -> {
						        if (result != null) {
							        if (result instanceof Boolean) {
								        if (Boolean.FALSE.equals(result)) {
									        LOGGER.info(
									                () -> "processReportCommand(): stackReport failed, player already reporter");
									        u.sendErrorMessage(
									                Message.get("ErrorMessages.Player-already-reported-by-you")
									                        .replace("_Player_", ru.getName())
									                        .replace("_Reason_", freason));
									        return;
								        }
							        } else if (result instanceof Report) {
								        LOGGER.info(() -> "processReportCommand(): stackReport succeeded");
								        finishReportCommandProcess((Report) result, false, u, ru, db, configFile);
								        return;
							        }
						        }

						        LOGGER.info(() -> "processReportCommand(): stackReport failed, create new report");
						        createNewReportThenFinish(u, ru, configFile, freason, date);
					        });
				} else {
					LOGGER.info(() -> "processReportCommand(): !stackReports, create new report");
					createNewReportThenFinish(u, ru, configFile, freason, date);
				}
			}

		});
	}

	private void createNewReportThenFinish(User u, User ru, FileConfiguration configFile, String freason, String date) {
		ReportUtils.checkMaxReportsReachedAsynchronously(taskScheduler, db, (maxReportsReached) -> {
			ReportUtils.createReportAsynchronously(u, ru, freason, date, maxReportsReached, taskScheduler, db, bm, um,
			        (ReportUtils.CreatedReport cr) -> {
				        if (cr.r != null) {
					        finishReportCommandProcess(cr.r, cr.missingData, u, ru, db, configFile);
				        } else {
					        LOGGER.error(ConfigUtils.getInfoMessage("An error occurred while creating a report",
					                "Une erreur est survenue pendant la creation d'un signalement")
					                + " (maxReportsReached = " + maxReportsReached + ")");
				        }
			        });
		});
	}

	private String getReason(String[] args) {
		StringBuilder sb = new StringBuilder();
		for (int argIndex = 1; argIndex < args.length; argIndex++) {
			sb.append(args[argIndex]).append(" ");
		}
		return sb.toString().trim();
	}

	private void finishReportCommandProcess(Report r, boolean missingData, User u, User ru, Database db,
	        FileConfiguration configFile) {
		LOGGER.info(() -> "finalReportCommandProcess(): report id = " + r.getId());
		String server = bm.getServerName();

		ReportUtils.sendReport(r, server, true, db, vm, bm);
		String reason = r.getReason(false);
		String date = r.getDate();
		u.sendMessage(Message.REPORT_SENT.get()
		        .replace("_Player_", r.getPlayerName(Report.ParticipantType.REPORTED, false, true, vm, bm))
		        .replace("_Reason_", reason));
		bm.sendPluginNotificationToAll(false, server, BungeeManager.NotificationType.NEW_REPORT,
		        Boolean.toString(missingData), r.getBasicDataAsString());

		u.startCooldown(ReportUtils.getCooldown(), db, null);
		ru.startImmunity(false, db, null, um);
		u.changeStatistic(Statistic.REPORTS, 1, db, null);
		ru.changeStatistic(Statistic.REPORTED_TIMES, 1, db, null);
		bm.sendUsersDataChangedNotification(u.getUniqueId().toString(), ru.getUniqueId().toString());

		String reportIdStr = Integer.toString(r.getId());
		for (String command : configFile.getStringList("Config.AutoCommands")) {
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
			        command.replace("_Id_", reportIdStr)
			                .replace("_Server_", server)
			                .replace("_Date_", date)
			                .replace("_Reporter_", u.getName())
			                .replace("_Reported_", ru.getName())
			                .replace("_Reason_", reason));
		}
	}

	@Override
	public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] args) {
		return args.length == 1 && s instanceof Player ? StringUtil.copyPartialMatches(args[0],
		        UserUtils.getOnlinePlayersForPlayer((Player) s, true, um, bm), new ArrayList<>()) : new ArrayList<>();
	}

}
