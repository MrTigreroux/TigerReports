package fr.mrtigreroux.tigerreports.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

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
import fr.mrtigreroux.tigerreports.data.constants.Status;
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
import fr.mrtigreroux.tigerreports.utils.CollectionUtils;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.DatetimeUtils;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;
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
		LOGGER.info(
		        () -> "user = " + u + ", user name = " + u.getName() + ", p = u.getPlayer() ? " + (p == u.getPlayer()));

		u.getCooldownAsynchronously(db, taskScheduler, new ResultCallback<String>() {

			@Override
			public void onResultReceived(String cooldown) {
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

				UUID ruuid = UserUtils.getUniqueId(reportedName);
				LOGGER.info(() -> "reported uuid = " + ruuid);
				um.getUserAsynchronously(ruuid, db, taskScheduler, new ResultCallback<User>() {

					@Override
					public void onResultReceived(User ru) {
						LOGGER.info(() -> "reported user = " + ru.getName() + ", is online: " + ru.isOnline());
						ru.checkExistsAsynchronously(db, taskScheduler, um, new ResultCallback<Boolean>() {

							@Override
							public void onResultReceived(Boolean reportedExists) {
								if (!reportedExists) {
									LOGGER.info(() -> "reported user does not exists");
									u.sendErrorMessage(Message.INVALID_PLAYER.get().replace("_Player_", reportedName));
									return;
								}

								processReportCommand(args, u, ru, reportOneself, configFile);
							}

						});
					}

				});
			}

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

				String freason = reason;
				String date = DatetimeUtils.getNowDate();

				String reporterUUID = u.getUniqueId().toString();

				LOGGER.info(() -> "processReportCommand(): checking if similar existing report");
				taskScheduler.runTaskAsynchronously(new Runnable() {

					@Override
					public void run() {
						if (ReportUtils.stackReports()) {
							Map<String, Object> reportData = db.query(
							        "SELECT report_id,status,appreciation,date,reported_uuid,reporter_uuid,reason FROM tigerreports_reports WHERE status NOT LIKE ? AND reported_uuid = ? AND archived = ? AND LOWER(reason) = LOWER(?) LIMIT 1",
							        Arrays.asList(Status.DONE.getRawName() + "%", ru.getUniqueId().toString(), 0,
							                freason))
							        .getResult(0);
							if (reportData != null) {
								LOGGER.info(() -> "processReportCommand(): found a similar report: "
								        + CollectionUtils.toString(reportData));
								try {
									String reportReporterUUID = (String) reportData.get("reporter_uuid");
									if (reportReporterUUID.contains(reporterUUID.toString())) {
										taskScheduler.runTask(new Runnable() {

											@Override
											public void run() {
												u.sendErrorMessage(
												        Message.get("ErrorMessages.Player-already-reported-by-you")
												                .replace("_Player_", ru.getName())
												                .replace("_Reason_", freason));
											}

										});
										return;
									}

									reportReporterUUID += "," + reporterUUID;
									reportData.put("reporter_uuid", reportReporterUUID);

									int reportId = (int) reportData.get("report_id");
									if (ConfigUtils.isEnabled(configFile, "Config.UpdateDateOfStackedReports")) {
										reportData.put("date", date);
										db.update(
										        "UPDATE tigerreports_reports SET reporter_uuid = ?, date = ? WHERE report_id = ?",
										        Arrays.asList(reportReporterUUID, date, reportId));
									} else {
										db.update(
										        "UPDATE tigerreports_reports SET reporter_uuid = ? WHERE report_id = ?",
										        Arrays.asList(reportReporterUUID, reportId));
									}
									rm.updateAndGetReport(reportId, reportData, false, false, false, db, taskScheduler,
									        um, new ResultCallback<Report>() {

										        @Override
										        public void onResultReceived(Report r) {
											        finalReportCommandProcess(r, false, u, ru, db, configFile);
										        }

									        });
									return;
								} catch (Exception invalidReport) {}
							}
						}

						LOGGER.info(() -> "processReportCommand(): creating a new report...");
						final boolean maxReportsReached = ReportUtils.getTotalReports(db) + 1 > ReportUtils
						        .getMaxReports();

						taskScheduler.runTask(new Runnable() {

							@Override
							public void run() {
								Map<String, Object> reportData = new HashMap<>();
								reportData.put("status", Status.WAITING.getRawName());
								reportData.put("appreciation", "None");
								reportData.put("date", date);
								reportData.put("reported_uuid", ru.getUniqueId().toString());
								reportData.put("reporter_uuid", reporterUUID);
								reportData.put("reason", freason);
								if (!maxReportsReached) {
									reportData.put("reporter_ip", p.getAddress().getAddress().toString());
									reportData.put("reporter_location",
									        MessageUtils.formatConfigLocation(p.getLocation(), bm));
									reportData.put("reporter_messages", u.getLastMessages());

									boolean missingData = !ReportUtils.collectAndFillReportedData(ru, bm, reportData);
									if (missingData) {
										reportData.put("reported_messages", ru.getLastMessages());
									}

									StringBuilder queryColumnsName = new StringBuilder();
									StringBuilder queryColumnsValue = new StringBuilder();
									List<Object> queryParams = new ArrayList<>();
									for (Entry<String, Object> data : reportData.entrySet()) {
										if (queryColumnsName.length() > 0) {
											queryColumnsName.append(",");
											queryColumnsValue.append(",");
										}
										queryColumnsName.append('`').append(data.getKey()).append('`');
										queryColumnsValue.append("?");
										queryParams.add(data.getValue());
									}

									String query = "INSERT INTO tigerreports_reports (" + queryColumnsName
									        + ") VALUES (" + queryColumnsValue + ")";

									db.insertAsynchronously(query, queryParams, taskScheduler,
									        new ResultCallback<Integer>() {

										        @Override
										        public void onResultReceived(Integer reportId) {
											        reportData.put("report_id", reportId);
											        Report.asynchronouslyFrom(reportData, false, false, db,
											                taskScheduler, um, new ResultCallback<Report>() {

												                @Override
												                public void onResultReceived(Report r) {
													                finalReportCommandProcess(r, missingData, u, ru, db,
													                        configFile);
												                }

											                });
										        }

									        });
								} else {
									LOGGER.info(() -> "processReportCommand(): max reports reached");
									reportData.put("report_id", -1);
									Report.asynchronouslyFrom(reportData, false, false, db, taskScheduler, um,
									        new ResultCallback<Report>() {

										        @Override
										        public void onResultReceived(Report r) {
											        if (r != null) {
												        finalReportCommandProcess(r, false, u, ru, db, configFile);
											        } else {
												        LOGGER.error(ConfigUtils.getInfoMessage(
												                "An error occurred while creating a report (the max reports amount is reached)",
												                "Une erreur est survenue pendant la creation d'un signalement (le nombre maximum de signalements est atteint)"));
											        }
										        }

									        });
								}

							}
						});

					}

				});

			}

		});
	}

	private String getReason(String[] args) {
		StringBuilder sb = new StringBuilder();
		for (int argIndex = 1; argIndex < args.length; argIndex++) {
			sb.append(args[argIndex]).append(" ");
		}
		return sb.toString().trim();
	}

	private void finalReportCommandProcess(Report r, boolean missingData, User u, User ru, Database db,
	        FileConfiguration configFile) {
		LOGGER.info(() -> "finalReportCommandProcess(): report id = " + r.getId());
		String server = bm.getServerName();

		ReportUtils.sendReport(r, server, true, db, vm, bm);
		String reason = r.getReason(false);
		String date = r.getDate();
		u.sendMessage(Message.REPORT_SENT.get()
		        .replace("_Player_", r.getPlayerName(Report.ParticipantType.REPORTED, false, true, vm, bm))
		        .replace("_Reason_", reason));
		bm.sendPluginNotificationToAll(server, "new_report", Boolean.toString(missingData), r.getBasicDataAsString());

		u.startCooldown(ReportUtils.getCooldown(), db, null);
		ru.startImmunity(false, db, null, um);
		u.changeStatistic(Statistic.REPORTS, 1, db, null);
		ru.changeStatistic(Statistic.REPORTED_TIMES, 1, db, null);
		bm.sendUsersDataChanged(u.getUniqueId().toString(), ru.getUniqueId().toString());

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
