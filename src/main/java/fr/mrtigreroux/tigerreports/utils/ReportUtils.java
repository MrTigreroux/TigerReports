package fr.mrtigreroux.tigerreports.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import com.google.common.primitives.Ints;

import fr.mrtigreroux.tigerreports.bungee.BungeeManager;
import fr.mrtigreroux.tigerreports.data.config.ConfigFile;
import fr.mrtigreroux.tigerreports.data.config.ConfigSound;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.Appreciation;
import fr.mrtigreroux.tigerreports.data.constants.Status;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.events.NewReportEvent;
import fr.mrtigreroux.tigerreports.logs.Logger;
import fr.mrtigreroux.tigerreports.managers.ReportsManager;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.managers.VaultManager;
import fr.mrtigreroux.tigerreports.objects.reports.Report;
import fr.mrtigreroux.tigerreports.objects.reports.Report.AdvancedData;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.tasks.ResultCallback;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * @author MrTigreroux
 */

public class ReportUtils {

	private static final Logger LOGGER = Logger.fromClass(ReportUtils.class);

	private ReportUtils() {}

	/**
	 * Check if a similar report already exists (same reported and reason). If it does exist, check if the reporter is not already a reporter of that existing report. If it is not the case, then the
	 * existing report is updated with the new reporter.
	 * 
	 * @param reporterUUID
	 * @param reportedUUID
	 * @param reason
	 * @param date
	 * @param updateDate
	 * @param taskScheduler
	 * @param db
	 * @param rm
	 * @param um
	 * @param resultCallback return false if reporter is already a reporter of the similar existing report, else the updated report if found and stack succeeded, else null.
	 */
	public static void stackReportAsynchronously(String reporterUUID, String reportedUUID, String reason, String date,
	        boolean updateDate, TaskScheduler taskScheduler, Database db, ReportsManager rm, UsersManager um,
	        ResultCallback<Object> resultCallback) {
		LOGGER.info(() -> "stackReportAsynchronously(): checking if similar existing report");
		taskScheduler.runTaskAsynchronously(() -> {
			Map<String, Object> reportData = db.query(
			        "SELECT report_id,status,appreciation,date,reported_uuid,reporter_uuid,reason FROM tigerreports_reports WHERE status NOT LIKE ? AND reported_uuid = ? AND archived = ? AND LOWER(reason) = LOWER(?) LIMIT 1",
			        Arrays.asList(Status.DONE.getConfigName() + "%", reportedUUID, 0, reason)).getResult(0);
			if (reportData != null) {
				LOGGER.info(() -> "stackReportAsynchronously(): found a similar report: "
				        + CollectionUtils.toString(reportData));
				try {
					String reportReporterUUID = (String) reportData.get(Report.REPORTER_UUID);
					if (reportReporterUUID.contains(reporterUUID.toString())) {
						taskScheduler.runTask(() -> {
							LOGGER.info(() -> "stackReportAsynchronously(): player already reporter");
							resultCallback.onResultReceived(Boolean.FALSE);
						});
						return;
					}

					reportReporterUUID += Report.REPORTERS_SEPARATOR + reporterUUID;
					reportData.put(Report.REPORTER_UUID, reportReporterUUID);

					int reportId = (int) reportData.get(Report.REPORT_ID);
					if (updateDate) {
						reportData.put("date", date);
						db.update("UPDATE tigerreports_reports SET reporter_uuid = ?, date = ? WHERE report_id = ?",
						        Arrays.asList(reportReporterUUID, date, reportId));
					} else {
						db.update("UPDATE tigerreports_reports SET reporter_uuid = ? WHERE report_id = ?",
						        Arrays.asList(reportReporterUUID, reportId));
					}
					taskScheduler.runTask(() -> {
						LOGGER.info(() -> "stackReportAsynchronously(): update and send report");
						rm.updateAndGetReport(reportId, reportData, false, false, db, taskScheduler, um,
						        (r) -> resultCallback.onResultReceived(r));
					});
					return;
				} catch (Exception invalidReport) {
					taskScheduler.runTask(() -> {
						LOGGER.info(() -> "stackReportAsynchronously(): invalid report");
						resultCallback.onResultReceived(null);
					});
				}
			} else {
				taskScheduler.runTask(() -> {
					LOGGER.info(() -> "stackReportAsynchronously(): non-existent report");
					resultCallback.onResultReceived(null);
				});
			}
		});
	}

	public static class CreatedReport {

		public final Report r;
		public final boolean missingData;

		public CreatedReport(Report r, boolean missingData) {
			this.r = r;
			this.missingData = missingData;
		}

	}

	public static void createReportAsynchronously(User u, User ru, String reason, String date,
	        boolean maxReportsReached, TaskScheduler taskScheduler, Database db, BungeeManager bm, UsersManager um,
	        ResultCallback<CreatedReport> resultCallback) {
		LOGGER.debug(() -> "createReportAsynchronously()");
		Map<String, Object> reportData = new HashMap<>();
		reportData.put(Report.STATUS, Status.WAITING.getConfigName());
		reportData.put(Report.APPRECIATION, Appreciation.NONE.getConfigName());
		reportData.put(Report.DATE, date);
		reportData.put(Report.REPORTED_UUID, ru.getUniqueId().toString());
		reportData.put(Report.REPORTER_UUID, u.getUniqueId().toString());
		reportData.put(Report.REASON, reason);
		if (!maxReportsReached) {
			LOGGER.debug(() -> "createReportAsynchronously(): maxReportsReached = false");
			reportData.put(Report.AdvancedData.REPORTER_IP, u.getIPAddress());
			Player reporter = u.getPlayer();
			if (reporter != null) {
				reportData.put(Report.AdvancedData.REPORTER_LOCATION,
				        SerializationUtils.serializeLocation(reporter.getLocation(), bm));
			}
			reportData.put(Report.AdvancedData.REPORTER_MESSAGES,
			        Report.AdvancedData.serializeMessages(u.getLastMessages()));

			boolean missingData = !ReportUtils.collectAndFillReportedData(ru, bm, reportData);
			if (missingData) {
				LOGGER.debug(() -> "createReportAsynchronously(): missingData = " + missingData);
				reportData.put(Report.AdvancedData.REPORTED_MESSAGES,
				        Report.AdvancedData.serializeMessages(ru.getLastMessages()));
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

			String query = "INSERT INTO tigerreports_reports (" + queryColumnsName + ") VALUES (" + queryColumnsValue
			        + ")";

			LOGGER.debug(() -> "createReportAsynchronously(): insert in db...");
			db.insertAsynchronously(query, queryParams, taskScheduler, new ResultCallback<Integer>() {

				@Override
				public void onResultReceived(Integer reportId) {
					reportData.put(Report.REPORT_ID, reportId);
					Report.asynchronouslyFrom(reportData, false, false, db, taskScheduler, um, (r) -> {
						LOGGER.debug(() -> "onResultReceived(): send report result");
						resultCallback.onResultReceived(new CreatedReport(r, missingData));
					});
				}

			});
		} else {
			LOGGER.info(() -> "createReportAsynchronously(): max reports reached");
			reportData.put(Report.REPORT_ID, -1);
			Report.asynchronouslyFrom(reportData, false, false, db, taskScheduler, um, (r) -> {
				LOGGER.debug(() -> "onResultReceived(): send report result");
				resultCallback.onResultReceived(new CreatedReport(r, false));
			});
		}
	}

	/**
	 * @param ru the online reported user instance
	 * @return true if reported data has been successfully collected.
	 */
	@SuppressWarnings("deprecation")
	public static boolean collectAndFillReportedData(User ru, BungeeManager bm, Map<String, Object> data) {
		Player rp = ru.getPlayer();
		if (rp == null) {
			return false;
		}
		try {
			data.put(AdvancedData.REPORTED_IP, ru.getIPAddress());
			data.put(AdvancedData.REPORTED_LOCATION, SerializationUtils.serializeLocation(rp.getLocation(), bm));
			data.put(AdvancedData.REPORTED_MESSAGES, AdvancedData.serializeMessages(ru.getLastMessages()));
			data.put(AdvancedData.REPORTED_GAMEMODE, AdvancedData.serializeGamemode(rp.getGameMode()));
			data.put(AdvancedData.REPORTED_ON_GROUND,
			        !rp.getLocation().getBlock().getRelative(BlockFace.DOWN).getType().equals(Material.AIR) ? 1 : 0);
			data.put(AdvancedData.REPORTED_SNEAK, rp.isSneaking() ? 1 : 0);
			data.put(AdvancedData.REPORTED_SPRINT, rp.isSprinting() ? 1 : 0);
			data.put(AdvancedData.REPORTED_HEALTH,
			        (int) Math.round(rp.getHealth()) + "/" + (int) Math.round(rp.getMaxHealth()));
			data.put(AdvancedData.REPORTED_FOOD, rp.getFoodLevel());
			data.put(AdvancedData.REPORTED_EFFECTS, AdvancedData.serializeConfigEffects(rp.getActivePotionEffects()));
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	@SuppressWarnings("deprecation")
	public static void sendReport(Report r, String server, boolean notify, Database db, VaultManager vm,
	        BungeeManager bm) {
		if (r.isStackedReport() && !ConfigUtils.isEnabled("Config.NotifyStackedReports")) {
			return;
		}

		try {
			Bukkit.getServer().getPluginManager().callEvent(new NewReportEvent(server, r));
		} catch (Exception ignored) {}

		if (r.isArchived() || !notify) {
			return;
		}

		int reportId = r.getId();

		BaseComponent alert = new TextComponent("");
		alert.setColor(ChatColor.valueOf(MessageUtils.getLastColor(Message.ALERT.get(), "_Reason_").name()));
		if (reportId == -1) {
			MessageUtils.sendStaffMessage(
			        Message.STAFF_MAX_REPORTS_REACHED.get().replace("_Amount_", Integer.toString(getMaxReports())),
			        ConfigSound.STAFF.get());
		} else {
			alert.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/reports #" + reportId));
			BaseComponent hoverTC = new TextComponent("");
			MessageUtils.APPEND_TEXT_WITH_TRANSLATED_COLOR_CODES_TO_COMPONENT_BUILDER_METHOD.accept(hoverTC,
			        Message.ALERT_DETAILS.get()
			                .replace("_Report_", r.getName())
			                .replace(ConfigUtils.getLineBreakSymbol(), "\n"));
			alert.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[] { hoverTC }));
		}

		String[] lines = Message.ALERT.get()
		        .replace("_Server_", MessageUtils.getServerName(server))
		        .replace("_Reporter_",
		                r.getPlayerName(r.getLastReporter(), Report.ParticipantType.REPORTER, false, true, vm, bm))
		        .replace("_Reported_",
		                r.getPlayerName(Report.ParticipantType.REPORTED, !ReportUtils.onlinePlayerRequired(), true, vm,
		                        bm))
		        .replace("_Reason_", r.getReason(false))
		        .split(ConfigUtils.getLineBreakSymbol());

		for (String line : lines) {
			BaseComponent alertLine = alert.duplicate();
			MessageUtils.APPEND_TEXT_WITH_TRANSLATED_COLOR_CODES_TO_COMPONENT_BUILDER_METHOD.accept(alertLine, line);
			MessageUtils.sendStaffMessage(alertLine, ConfigSound.REPORT.get());
		}
	}

	public static void checkMaxReportsReachedAsynchronously(TaskScheduler taskScheduler, Database db,
	        ResultCallback<Boolean> resultCallback) {
		taskScheduler.runTaskAsynchronously(() -> {
			int totalReports = ReportUtils.getTotalReports(db);
			taskScheduler.runTask(() -> {
				resultCallback.onResultReceived(totalReports + 1 > ReportUtils.getMaxReports());
			});
		});
	}

	public static int getTotalReports(Database db) {
		Object o = db.query("SELECT COUNT(report_id) AS total FROM tigerreports_reports", null).getResult(0, "total");
		return o instanceof Integer ? (int) o : Ints.checkedCast((long) o);
	}

	public static int getMaxReports() {
		return ConfigFile.CONFIG.get().getInt("Config.MaxReports", 100);
	}

	public static boolean permissionRequiredToReport() {
		return ConfigUtils.isEnabled("Config.PermissionRequired");
	}

	public static boolean onlinePlayerRequired() {
		return ConfigUtils.isEnabled("Config.ReportOnline");
	}

	public static int getMinCharacters() {
		return ConfigFile.CONFIG.get().getInt("Config.MinCharacters", 4);
	}

	public static long getCooldown() {
		return ConfigFile.CONFIG.get().getLong("Config.ReportCooldown", 300);
	}

	public static long getAbusiveReportCooldown() {
		return ConfigFile.CONFIG.get().getLong("Config.AbusiveReport.Cooldown", 3600);
	}

	public static boolean onlyDoneArchives() {
		return ConfigUtils.isEnabled("Config.OnlyDoneArchives");
	}

	public static boolean stackReports() {
		return ConfigUtils.isEnabled("Config.StackReports");
	}

	public static boolean punishmentsEnabled() {
		return ConfigUtils.isEnabled("Config.Punishments.Enabled");
	}

	public static int getMessagesHistory() {
		return ConfigFile.CONFIG.get().getInt("Config.MessagesHistory", 5);
	}

}
