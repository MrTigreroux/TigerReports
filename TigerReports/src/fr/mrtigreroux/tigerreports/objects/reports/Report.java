package fr.mrtigreroux.tigerreports.objects.reports;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.data.config.ConfigFile;
import fr.mrtigreroux.tigerreports.data.config.ConfigSound;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.Statistic;
import fr.mrtigreroux.tigerreports.data.constants.Status;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.data.database.QueryResult;
import fr.mrtigreroux.tigerreports.events.ProcessReportEvent;
import fr.mrtigreroux.tigerreports.events.ReportStatusChangeEvent;
import fr.mrtigreroux.tigerreports.logs.Logger;
import fr.mrtigreroux.tigerreports.managers.BungeeManager;
import fr.mrtigreroux.tigerreports.managers.ReportsManager;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.managers.VaultManager;
import fr.mrtigreroux.tigerreports.objects.Comment;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.tasks.ResultCallback;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.DatetimeUtils;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;

/**
 * @author MrTigreroux
 */

public class Report {

	private static final Logger LOGGER = Logger.fromClass(Report.class);

	private static final char APPRECIATION_PUNISHMENT_SEPARATOR = '/';
	private static final String REPORTERS_SEPARATOR = ",";
	private static final String EFFECTS_SEPARATOR = ",";
	private static final String STATUS_IN_PROGRESS_SEPARATOR = "-";
	private static final String STATUS_DONE_SEPARATOR = " by ";
	private static final String DATA_SEPARATOR = "##";
	private static final List<String> REPORT_BASIC_DATA_KEYS = Arrays.asList("report_id", "status", "appreciation",
	        "date", "reported_uuid", "reporter_uuid", "reason", "archived");

	private final int reportId;
	private StatusDetails statusDetails;
	private String appreciation;
	private String date;
	private final String reason;
	private final User reported;
	private List<User> reporters;
	private Map<String, String> advancedData = null;
	private boolean archived;

	/**
	 * 
	 * @param reportData       containing archived field
	 * @param saveAdvancedData
	 * @return
	 */
	public static void asynchronouslyFrom(Map<String, Object> reportData, boolean saveAdvancedData, Database db,
	        TaskScheduler taskScheduler, UsersManager um, ResultCallback<Report> resultCallback) {
		if (reportData == null) {
			resultCallback.onResultReceived(null);
			return;
		}

		boolean archived = ((int) Objects.requireNonNull(reportData.get("archived"))) == 1;

		asynchronouslyFrom(reportData, archived, saveAdvancedData, db, taskScheduler, um, resultCallback);
	}

	public static void asynchronouslyFrom(Map<String, Object> reportData, boolean archived, boolean saveAdvancedData,
	        Database db, TaskScheduler taskScheduler, UsersManager um, ResultCallback<Report> resultCallback) {
		if (reportData == null) {
			resultCallback.onResultReceived(null);
			return;
		}

		String reportedUUID = (String) reportData.get("reported_uuid");

		um.getUserAsynchronously(reportedUUID, db, taskScheduler, new ResultCallback<User>() {

			@Override
			public void onResultReceived(User reported) {
				if (reported == null) {
					resultCallback.onResultReceived(null);
					return;
				}

				String configReporter = (String) reportData.get("reporter_uuid");
				String[] reportersUUID = configReporter.split(REPORTERS_SEPARATOR);
				um.getUsersAsynchronously(reportersUUID, db, taskScheduler, new ResultCallback<List<User>>() {

					@Override
					public void onResultReceived(List<User> reporters) {
						if (reporters == null || reporters.isEmpty()) {
							resultCallback.onResultReceived(null);
							return;
						}

						String configStatus = (String) reportData.get("status");
						StatusDetails.asynchronouslyFrom(configStatus, db, taskScheduler, um,
						        new ResultCallback<Report.StatusDetails>() {

							        @Override
							        public void onResultReceived(StatusDetails sd) {
								        Report r = new Report((int) reportData.get("report_id"), sd,
								                (String) reportData.get("appreciation"),
								                (String) reportData.get("date"), reported, reporters,
								                (String) reportData.get("reason"), archived);

								        if (saveAdvancedData) {
									        r.extractAndSaveAdvancedData(reportData);
								        }

								        resultCallback.onResultReceived(r);
							        }
						        });

					}

				});

			}

		});
	}

	public Report(int reportId, StatusDetails statusDetails, String appreciation, String date, User reported,
	        List<User> reporters, String reason) {
		this(reportId, statusDetails, appreciation, date, reported, reporters, reason, false);
	}

	public Report(int reportId, StatusDetails statusDetails, String appreciation, String date, User reported,
	        List<User> reporters, String reason, boolean archived) {
		this.reportId = reportId;
		this.statusDetails = Objects.requireNonNull(statusDetails);
		this.appreciation = appreciation;
		this.date = date;
		this.reported = Objects.requireNonNull(reported);
		this.reporters = Objects.requireNonNull(reporters);
		this.reason = reason;
		this.archived = archived;
	}

	public static class StatusDetails {

		Status status;
		User staff;

		private StatusDetails(Status status, User staff) {
			this.status = status;
			this.staff = staff;
		}

		@Override
		public String toString() {
			String configStatus = status.getRawName();
			if (status == Status.IN_PROGRESS) {
				configStatus += STATUS_IN_PROGRESS_SEPARATOR;
			} else if (status == Status.DONE) {
				configStatus += STATUS_DONE_SEPARATOR;
			} else {
				return configStatus;
			}
			return configStatus + staff.getUniqueId();
		}

		@Override
		public int hashCode() {
			return Objects.hash(staff, status);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof StatusDetails)) {
				return false;
			}
			StatusDetails other = (StatusDetails) obj;
			return Objects.equals(staff, other.staff) && status == other.status;
		}

		public static StatusDetails from(Status status, User staff) {
			if (status != Status.IN_PROGRESS && status != Status.DONE) {
				staff = null;
			}
			return new StatusDetails(status, staff);
		}

		public static void asynchronouslyFrom(String configStatus, Database db, TaskScheduler taskScheduler,
		        UsersManager um, ResultCallback<StatusDetails> resultCallback) {
			Status status = Status.from(configStatus);
			String statusPrefix = null;
			UUID staffUUID = null;
			if (status == Status.IN_PROGRESS) {
				statusPrefix = Status.IN_PROGRESS.getRawName() + STATUS_IN_PROGRESS_SEPARATOR;
			} else if (status == Status.DONE) {
				statusPrefix = Status.DONE.getRawName() + STATUS_DONE_SEPARATOR;
			}

			if (statusPrefix != null) {
				staffUUID = UUID.fromString(configStatus.replaceFirst(statusPrefix, ""));
			}

			if (staffUUID != null) {
				um.getUserAsynchronously(staffUUID, db, taskScheduler, new ResultCallback<User>() {

					@Override
					public void onResultReceived(User u) {
						resultCallback.onResultReceived(new StatusDetails(status, u));
					}

				});
			} else {
				resultCallback.onResultReceived(new StatusDetails(status, null));
			}
		}

	}

	public interface ReportListener {
		void onReportDataChange(Report r);

		void onReportDelete(int reportId);
	}

	public int getId() {
		return reportId;
	}

	public String getName() {
		return Message.REPORT_NAME.get().replace("_Id_", Integer.toString(reportId));
	}

	public User getReported() {
		return reported;
	}

	public User getReporter() {
		return reporters.get(0);
	}

	public List<User> getReporters() {
		return reporters;
	}

	public User getLastReporter() {
		return reporters.get(reporters.size() - 1);
	}

	public int getReportersAmount() {
		return reporters.size();
	}

	public UUID getReportedUniqueId() {
		return reported.getUniqueId();
	}

	public UUID getReporterUniqueId() {
		return getReporter().getUniqueId();
	}

	public boolean isStackedReport() {
		return reporters.size() > 1;
	}

	public enum ParticipantType {

		REPORTED("reported", Message.REPORTED_NAME),
		REPORTER("reporter", Message.REPORTER_NAME);

		final String configName;
		final Message nameMsg;

		ParticipantType(String configName, Message name) {
			this.configName = configName;
			nameMsg = name;
		}

		public String getName() {
			return nameMsg.get();
		}

	}

	public String getPlayerName(ParticipantType type, boolean onlineSuffix, boolean color, VaultManager vm,
	        BungeeManager bm) {
		return getPlayerName(type == ParticipantType.REPORTED ? reported : getReporter(), type, onlineSuffix, color, vm,
		        bm);
	}

	public String getPlayerName(User u, ParticipantType type, boolean onlineSuffix, boolean color, VaultManager vm,
	        BungeeManager bm) {
		String name;

		if (color) {
			name = type.getName().replace("_Player_", u.getDisplayName(vm, false));
		} else {
			name = u.getName();
		}

		if (name == null) {
			return Message.NOT_FOUND_MALE.get();
		}

		if (onlineSuffix) {
			name += (u.isOnlineInNetwork(bm) ? Message.ONLINE_SUFFIX : Message.OFFLINE_SUFFIX).get();
		}

		return name;
	}

	public String getReportersNames(int first, boolean onlineSuffix, VaultManager vm, BungeeManager bm) {
		String name = Message.REPORTERS_NAMES.get();
		StringBuilder names = new StringBuilder();
		for (int i = first; i < reporters.size(); i++) {
			User reporter = reporters.get(i);
			names.append(name.replace("_Player_",
			        getPlayerName(reporter, ParticipantType.REPORTER, onlineSuffix, true, vm, bm)));
		}
		return names.toString();
	}

	public String getReportersUUIDStr() {
		StringBuilder uuids = new StringBuilder();
		for (User reporter : reporters) {
			if (uuids.length() > 0) {
				uuids.append(REPORTERS_SEPARATOR);
			}
			uuids.append(reporter.getUniqueId().toString());
		}
		return uuids.toString();
	}

	public List<UUID> getReportersUUID() {
		List<UUID> uuids = new ArrayList<>();
		for (User reporter : reporters) {
			uuids.add(reporter.getUniqueId());
		}
		return uuids;
	}

	public String getDate() {
		return date != null ? date : Message.NOT_FOUND_FEMALE.get();
	}

	public void setStatus(StatusDetails statusDetails, boolean bungee, Database db, ReportsManager rm,
	        BungeeManager bm) {
		updateStatusDetailsWithBroadcast(statusDetails, rm);

		if (!bungee) {
			String configStatus = getConfigStatus();
			bm.sendPluginNotificationToAll(configStatus + " new_status " + reportId);
			db.updateAsynchronously("UPDATE tigerreports_reports SET status = ? WHERE report_id = ?",
			        Arrays.asList(configStatus, reportId));
		}

		try {
			Bukkit.getServer().getPluginManager().callEvent(new ReportStatusChangeEvent(this, bungee));
		} catch (Exception ignored) {}
	}

	public Status getStatus() {
		return statusDetails.status;
	}

	public String getConfigStatus() {
		return statusDetails.toString();
	}

	public String getStatusWithDetails(VaultManager vm) {
		Status status = getStatus();
		if (status == Status.DONE) {
			String suffix = getAppreciation(true).equalsIgnoreCase("true")
			        ? Message.get("Words.Done-suffix.True-appreciation").replace("_Punishment_", getPunishment())
			        : Message.get("Words.Done-suffix.Other-appreciation")
			                .replace("_Appreciation_", getAppreciation(false));
			String processorName = getInvolvedStaffDisplayName(getProcessorStaff(), vm);
			return status.getWord(processorName) + suffix;
		} else if (status == Status.IN_PROGRESS) {
			String processingName = getInvolvedStaffDisplayName(getProcessingStaff(), vm);
			return status.getWord(processingName, true);
		} else {
			return status.getWord(null);
		}
	}

	public String getReason(boolean menu) {
		return reason != null
		        ? menu ? MessageUtils.getMenuSentence(reason, Message.REPORT_DETAILS, "_Reason_", true) : reason
		        : Message.NOT_FOUND_FEMALE.get();
	}

	public String getAppreciation(boolean config) {
		int pos = appreciation != null ? appreciation.indexOf(APPRECIATION_PUNISHMENT_SEPARATOR) : -1;
		String appreciationWord = pos != -1 ? appreciation.substring(0, pos) : appreciation;
		if (config) {
			return appreciationWord;
		}
		try {
			return appreciation != null && !appreciation.equalsIgnoreCase("None")
			        ? Message.valueOf(appreciationWord.toUpperCase()).get()
			        : Message.NONE_FEMALE.get();
		} catch (Exception invalidAppreciation) {
			return Message.NONE_FEMALE.get();
		}
	}

	public User getProcessorStaff() {
		return getStatus() == Status.DONE ? statusDetails.staff : null;
	}

	public User getProcessingStaff() {
		return getStatus() == Status.IN_PROGRESS ? statusDetails.staff : null;
	}

	public String getInvolvedStaffDisplayName(User u, VaultManager vm) {
		String name;
		if (u == null) {
			name = Message.NOT_FOUND_MALE.get();
		} else {
			name = u.getDisplayName(vm, true);
		}
		return name;
	}

	public String getPunishment() {
		int pos = appreciation != null ? appreciation.indexOf(APPRECIATION_PUNISHMENT_SEPARATOR) : -1;
		return pos != -1 && appreciation != null ? appreciation.substring(pos + 1) : Message.NONE_FEMALE.get();
	}

	public String implementDetails(String message, boolean menu, VaultManager vm, BungeeManager bm) {
		String reportersNames = isStackedReport() ? getReportersNames(0, true, vm, bm)
		        : getPlayerName(ParticipantType.REPORTER, true, true, vm, bm);

		return message.replace("_Status_", getStatusWithDetails(vm))
		        .replace("_Date_", getDate())
		        .replace("_Reporters_", reportersNames)
		        .replace("_Reported_", getPlayerName(ParticipantType.REPORTED, true, true, vm, bm))
		        .replace("_Reason_", getReason(menu));
	}

	public boolean hasAdvancedData() {
		return advancedData != null;
	}

	private void setAdvancedData(Map<String, String> advancedData) {
		if (advancedData != null && !advancedData.isEmpty()) {
			this.advancedData = advancedData;
		}
	}

	private void clearAdvancedData() {
		advancedData = null;
	}

	void extractAndSaveAdvancedData(Map<String, Object> reportData) {
		if (isArchived()) {
			return;
		}
		Map<String, String> advancedData = new HashMap<>();
		Set<String> advancedKeys = new HashSet<>(reportData.keySet());
		advancedKeys.removeAll(REPORT_BASIC_DATA_KEYS);
		for (String key : advancedKeys) {
			Object data = reportData.get(key);
			advancedData.put(key, data != null ? data.toString() : null);
		}
		setAdvancedData(advancedData);
	}

	public String implementData(String message, boolean advanced, VaultManager vm, BungeeManager bm) {
		if (advancedData == null) {
			return null;
		}

		String defaultData;
		String reportedAdvancedData = "";
		try {
			String effects;
			String effectsList = advancedData.get("reported_effects");
			if (effectsList != null && effectsList.contains(":") && effectsList.contains("/")) {
				StringBuilder effectsLines = new StringBuilder();
				for (String effect : effectsList.split(EFFECTS_SEPARATOR)) {
					String type = effect.split(":")[0].replace("_", " ");
					String duration = effect.split("/")[1];
					effectsLines.append(Message.EFFECT.get()
					        .replace("_Type_", type.charAt(0) + type.substring(1).toLowerCase())
					        .replace("_Amplifier_", effect.split(":")[1].replace("/" + duration, ""))
					        .replace("_Duration_", Long.toString(Long.parseLong(duration) / 20)));
				}
				effects = effectsLines.toString();
			} else {
				effects = Message.NONE_MALE.get();
			}
			defaultData = Message.DEFAULT_DATA.get()
			        .replace("_Gamemode_", MessageUtils.getGamemodeWord(advancedData.get("reported_gamemode")))
			        .replace("_OnGround_",
			                (advancedData.get("reported_on_ground").equals("1") ? Message.YES : Message.NO).get())
			        .replace("_Sneak_",
			                (advancedData.get("reported_sneak").equals("1") ? Message.YES : Message.NO).get())
			        .replace("_Sprint_",
			                (advancedData.get("reported_sprint").equals("1") ? Message.YES : Message.NO).get())
			        .replace("_Health_", advancedData.get("reported_health"))
			        .replace("_Food_", advancedData.get("reported_food"))
			        .replace("_Effects_", effects);
			reportedAdvancedData = !advanced ? ""
			        : Message.ADVANCED_DATA_REPORTED.get()
			                .replace("_UUID_",
			                        MessageUtils.getMenuSentence(getReportedUniqueId().toString(),
			                                Message.ADVANCED_DATA_REPORTED, "_UUID_", false))
			                .replace("_IP_", advancedData.get("reported_ip"));
		} catch (Exception dataNotFound) {
			defaultData = Message.PLAYER_WAS_OFFLINE.get();
		}

		return message.replace("_Reported_", getPlayerName(ParticipantType.REPORTED, true, true, vm, bm))
		        .replace("_DefaultData_", defaultData)
		        .replace("_AdvancedData_", !advanced ? ""
		                : reportedAdvancedData + Message.ADVANCED_DATA_REPORTER.get()
		                        .replace("_Player_", getPlayerName(ParticipantType.REPORTER, true, true, vm, bm))
		                        .replace("_UUID_",
		                                MessageUtils.getMenuSentence(getReporterUniqueId().toString(),
		                                        Message.ADVANCED_DATA_REPORTER, "_UUID_", false))
		                        .replace("_IP_",
		                                advancedData.get("reporter_ip") != null ? advancedData.get("reporter_ip")
		                                        : Message.NOT_FOUND_FEMALE.get()));
	}

	public String getOldLocation(ParticipantType type) {
		if (advancedData == null) {
			return null;
		}
		return advancedData.get(type.configName + "_location");
	}

	public String[] getMessagesHistory(ParticipantType type) {
		String messages = advancedData.get(type.configName + "_messages");
		return messages != null ? messages.split("#next#") : new String[0];
	}

	public ItemStack getItem(String actions, VaultManager vm, BungeeManager bm) {
		Status status = getStatus();
		return status.getIcon()
		        .amount(getReportersAmount())
		        .hideFlags(true)
		        .glow(status == Status.WAITING)
		        .name(Message.REPORT.get().replace("_Report_", getName()))
		        .lore(implementDetails(Message.REPORT_DETAILS.get(), true, vm, bm)
		                .replace("_Actions_", actions != null ? actions : "")
		                .split(ConfigUtils.getLineBreakSymbol()))
		        .create();
	}

	public String getText(VaultManager vm, BungeeManager bm) {
		return implementDetails(Message.get("Messages.Report-details")
		        .replace("_Report_", Message.REPORT.get().replace("_Report_", getName())), false, vm, bm);
	}

	public boolean isArchived() {
		return archived;
	}

	// TODO: create Appreciation object
	public void process(User staff, String appreciation, boolean bungee, boolean autoArchive, boolean notifyStaff,
	        Database db) {
		processing(staff, appreciation, bungee, autoArchive,
		        (autoArchive ? Message.STAFF_PROCESS_AUTO : Message.STAFF_PROCESS).get()
		                .replace("_Appreciation_", Message.valueOf(appreciation.toUpperCase()).get()),
		        "process", notifyStaff, null, db);
	}

	public void processWithPunishment(User staff, boolean bungee, boolean autoArchive, String punishment,
	        boolean notifyStaff, Database db, VaultManager vm, BungeeManager bm) {
		processing(staff, "True" + APPRECIATION_PUNISHMENT_SEPARATOR + punishment, bungee, autoArchive,
		        (autoArchive ? Message.STAFF_PROCESS_PUNISH_AUTO : Message.STAFF_PROCESS_PUNISH).get()
		                .replace("_Punishment_", punishment)
		                .replace("_Reported_", getPlayerName(ParticipantType.REPORTED, false, true, vm, bm)),
		        "process_punish", notifyStaff, null, db);
	}

	public void processAbusive(User staff, boolean bungee, boolean archive, long punishSeconds, boolean notifyStaff,
	        Database db) {
		String time = DatetimeUtils.convertToSentence(punishSeconds);
		processing(staff, "False", bungee, archive, Message.STAFF_PROCESS_ABUSIVE.get().replace("_Time_", time),
		        "process_abusive", notifyStaff, Long.toString(punishSeconds), db);

		TigerReports tr = TigerReports.getInstance();
		UsersManager um = tr.getUsersManager();
		if (!bungee) {
			BungeeManager bm = tr.getBungeeManager();
			um.startCooldownForUsers(reporters, punishSeconds, db, bm);
			if (staff != null) {
				Player p = staff.getPlayer();
				if (p != null) {
					VaultManager vm = tr.getVaultManager();
					ConfigUtils.processCommands(ConfigFile.CONFIG.get(), "Config.AbusiveReport.Commands", this, p, vm,
					        bm);
				}
			}
		}

		String punishedMsg = Message.PUNISHED.get().replace("_Time_", time);
		for (User reporter : reporters) {
			if (reporter != null) {
				reporter.sendMessage(punishedMsg);
			}
		}
	}

	private void processing(User staff, String appreciation, boolean bungee, boolean archive, String staffMessage,
	        String bungeeAction, boolean notifyStaff, String bungeeExtraData, Database db) {
		boolean changed = false;
		changed |= updateStatusDetails(new StatusDetails(Status.DONE, staff));
		changed |= updateAppreciation(appreciation);
		changed |= updateArchived(archive);

		TigerReports tr = TigerReports.getInstance();
		if (changed) {
			tr.getReportsManager().broadcastReportDataChanged(this);
		}

		BungeeManager bm = tr.getBungeeManager();
		VaultManager vm = tr.getVaultManager();

		if (notifyStaff && staff != null) {
			MessageUtils.sendStaffMessage(
			        MessageUtils.getAdvancedMessage(staffMessage.replace("_Player_", staff.getDisplayName(vm, true)),
			                "_Report_", getName(), getText(vm, bm), null),
			        ConfigSound.STAFF.get());
		}

		if (!bungee) {
			if (staff != null) {
				db.updateAsynchronously(
				        "UPDATE tigerreports_reports SET status = ?,appreciation = ?,archived = ? WHERE report_id = ?",
				        Arrays.asList(getConfigStatus(), appreciation, archive ? 1 : 0, reportId));

				staff.changeStatistic(Statistic.PROCESSED_REPORTS, 1, db, null);

				String appreciationStatisticConfigName = getAppreciation(true).toLowerCase() + "_appreciations";
				String[] uuidOfChangedStatsUsers = new String[reporters.size() + 1];
				uuidOfChangedStatsUsers[0] = staff.getUniqueId().toString();
				int i = 1;
				for (User reporter : reporters) {
					// TODO: group stat change
					reporter.changeStatistic(appreciationStatisticConfigName, 1, false, db, null);
					uuidOfChangedStatsUsers[i] = reporter.getUniqueId().toString();
					if (ConfigUtils.playersNotifications()) {
						if (reporter.isOnline()) {
							reporter.sendReportNotification(this, true, db, vm, bm);
						} else if (!bm.isOnline(reporter.getName())) {
							reporter.addReportNotification(getId(), db, tr);
						}
					}
					i++;
				}
				bm.sendPluginNotificationToAll(
				        String.join(BungeeManager.MESSAGE_DATA_SEPARATOR, staff.getUniqueId().toString(), bungeeAction,
				                "" + reportId, archive ? "1" : "0", appreciation) + bungeeExtraData != null
				                        ? " " + bungeeExtraData
				                        : "");
				bm.sendUsersDataChanged(uuidOfChangedStatsUsers);
			}
		} else {
			if (ConfigUtils.playersNotifications()) {
				for (User reporter : reporters) {
					if (reporter != null) {
						reporter.sendReportNotification(this, true, db, vm, bm);
					}
				}
			}
		}

		Bukkit.getServer()
		        .getPluginManager()
		        .callEvent(new ProcessReportEvent(Report.this, staff != null ? staff.getName() : null, bungee));
	}

	public void addComment(User u, String message, Database db, TaskScheduler taskScheduler,
	        ResultCallback<Integer> resultCallback) {
		addComment(u.getUniqueId().toString(), message, db, taskScheduler, resultCallback);
	}

	public void addComment(String author, String message, Database db, TaskScheduler taskScheduler,
	        ResultCallback<Integer> resultCallback) {
		db.insertAsynchronously(
		        "INSERT INTO tigerreports_comments (report_id,status,date,author,message) VALUES (?,?,?,?,?)",
		        Arrays.asList(reportId, "Private", DatetimeUtils.getNowDate(), author, message), taskScheduler,
		        resultCallback);
	}

	public void getCommentByIdAsynchronously(int commentId, Database db, TaskScheduler taskScheduler, UsersManager um,
	        ResultCallback<Comment> resultCallback) {
		db.queryAsynchronously("SELECT * FROM tigerreports_comments WHERE report_id = ? AND comment_id = ?",
		        Arrays.asList(reportId, commentId), taskScheduler, new ResultCallback<QueryResult>() {

			        @Override
			        public void onResultReceived(QueryResult qr) {
				        getCommentAsynchronouslyFrom(qr.getResult(0), db, taskScheduler, um, resultCallback);
			        }

		        });
	}

	public void getCommentAsynchronously(int commentIndex, Database db, TaskScheduler taskScheduler, UsersManager um,
	        ResultCallback<Comment> resultCallback) {
		db.queryAsynchronously("SELECT * FROM tigerreports_comments WHERE report_id = ? LIMIT 1 OFFSET ?",
		        Arrays.asList(reportId, commentIndex - 1), taskScheduler, new ResultCallback<QueryResult>() {

			        @Override
			        public void onResultReceived(QueryResult qr) {
				        getCommentAsynchronouslyFrom(qr.getResult(0), db, taskScheduler, um,
				                new ResultCallback<Comment>() {

					                @Override
					                public void onResultReceived(Comment c) {
						                resultCallback.onResultReceived(c);
					                }

				                });
			        }

		        });
	}

	public void getCommentAsynchronouslyFrom(Map<String, Object> result, Database db, TaskScheduler taskScheduler,
	        UsersManager um, ResultCallback<Comment> resultCallback) {
		if (result == null) {
			resultCallback.onResultReceived(null);
		} else {
			String authorUUID = (String) result.get("author");
			um.getUserAsynchronously(authorUUID, db, taskScheduler, new ResultCallback<User>() {

				@Override
				public void onResultReceived(User author) {
					resultCallback.onResultReceived(
					        new Comment(Report.this, (int) result.get("comment_id"), (String) result.get("status"),
					                (String) result.get("date"), author, (String) result.get("message")));
				}

			});
		}
	}

	public void delete(User staff, boolean bungee, Database db, TaskScheduler taskScheduler, ReportsManager rm,
	        VaultManager vm, BungeeManager bm) {
		if (staff != null) {
			MessageUtils.sendStaffMessage(MessageUtils.getAdvancedMessage(
			        (isArchived() ? Message.STAFF_DELETE_ARCHIVE : Message.STAFF_DELETE).get()
			                .replace("_Player_", staff.getDisplayName(vm, true)),
			        "_Report_", getName(), getText(vm, bm), null), ConfigSound.STAFF.get());

			if (!bungee) {
				bm.sendPluginNotificationToAll(staff.getUniqueId(), "delete", getBasicDataAsString());
				taskScheduler.runTaskAsynchronously(new Runnable() {

					@Override
					public void run() {
						List<Object> param = Collections.singletonList(reportId);
						db.update("DELETE FROM tigerreports_reports WHERE report_id = ?", param);
						db.update("DELETE FROM tigerreports_comments WHERE report_id = ?", param);
					}

				});
			}
		}

		rm.reportIsDeleted(reportId);
	}

	public void archive(User staff, boolean bungee, Database db) {
		TigerReports tr = TigerReports.getInstance();
		updateArchivedWithBroadcast(true, tr.getReportsManager());

		VaultManager vm = tr.getVaultManager();
		BungeeManager bm = tr.getBungeeManager();

		if (staff != null) {
			MessageUtils.sendStaffMessage(MessageUtils.getAdvancedMessage(
			        Message.STAFF_ARCHIVE.get().replace("_Player_", staff.getDisplayName(vm, true)), "_Report_",
			        getName(), getText(vm, bm), null), ConfigSound.STAFF.get());
			if (!bungee) {
				bm.sendPluginNotificationToAll(staff.getUniqueId(), "archive", reportId);
				db.updateAsynchronously("UPDATE tigerreports_reports SET archived = ? WHERE report_id = ?",
				        Arrays.asList(1, reportId));
			}
		}
	}

	public void unarchive(User staff, boolean bungee, Database db) {
		TigerReports tr = TigerReports.getInstance();
		updateArchivedWithBroadcast(false, tr.getReportsManager());

		VaultManager vm = tr.getVaultManager();
		BungeeManager bm = tr.getBungeeManager();

		if (staff != null) {
			MessageUtils.sendStaffMessage(MessageUtils.getAdvancedMessage(
			        Message.STAFF_RESTORE.get().replace("_Player_", staff.getDisplayName(vm, true)), "_Report_",
			        getName(), getText(vm, bm), null), ConfigSound.STAFF.get());

			if (!bungee) {
				bm.sendPluginNotificationToAll(staff.getUniqueId(), "unarchive", reportId);
				db.updateAsynchronously("UPDATE tigerreports_reports SET archived = ? WHERE report_id = ?",
				        Arrays.asList(0, reportId));
			}
		}
	}

	public void deleteFromArchives(User staff, boolean bungee, Database db, TaskScheduler taskScheduler,
	        ReportsManager rm, VaultManager vm, BungeeManager bm) {
		if (!isArchived()) {
			return;
		}

		delete(staff, bungee, db, taskScheduler, rm, vm, bm);
	}

	private boolean updateStatusDetails(StatusDetails newStatusDetails) {
		if (!Objects.equals(statusDetails, Objects.requireNonNull(newStatusDetails))) {
			statusDetails = newStatusDetails;
			return true;
		} else {
			return false;
		}
	}

	private void updateStatusDetails(String newStatusDetails, Database db, TaskScheduler taskScheduler, UsersManager um,
	        ResultCallback<Boolean> resultCallback) {
		if (!Objects.equals(statusDetails.toString(), newStatusDetails)) {
			StatusDetails.asynchronouslyFrom(newStatusDetails, db, taskScheduler, um,
			        new ResultCallback<Report.StatusDetails>() {

				        @Override
				        public void onResultReceived(StatusDetails sd) {
					        statusDetails = sd;
					        resultCallback.onResultReceived(true);
				        }

			        });
		} else {
			resultCallback.onResultReceived(false);
		}
	}

	private boolean updateAppreciation(String newAppreciation) {
		if (!Objects.equals(appreciation, newAppreciation)) {
			appreciation = newAppreciation;
			return true;
		} else {
			return false;
		}
	}

	private void updateReportersAndDate(String newReporters, String newDate, Database db, TaskScheduler taskScheduler,
	        UsersManager um, ResultCallback<Boolean> resultCallback) {
		String[] newReportersUUID = newReporters.split(REPORTERS_SEPARATOR);
		int newReportersUUIDLength = newReportersUUID.length;
		boolean changed = !Objects.equals(date, newDate);
		if (!changed) {
			for (int i = 0; i < reporters.size(); i++) {
				User reporter = reporters.get(i);
				if (i >= newReportersUUIDLength || !reporter.getUniqueId().toString().equals(newReportersUUID[i])) {
					changed = true;
					break;
				}
			}
		}

		if (changed) {
			um.getUsersAsynchronously(newReportersUUID, db, taskScheduler, new ResultCallback<List<User>>() {

				@Override
				public void onResultReceived(List<User> reporters) {
					Report.this.reporters = reporters;
					date = newDate;
					resultCallback.onResultReceived(true);
				}

			});
		} else {
			resultCallback.onResultReceived(false);
		}
	}

	private boolean updateArchived(boolean newArchived) {
		if (!Objects.equals(archived, newArchived)) {
			archived = newArchived;
			// Advanced data is fixed. It can be present or missing but cannot change. If the report is archived, advanced data is useless and we free
			// memory.
			if (isArchived()) {
				clearAdvancedData();
			}
			return true;
		} else {
			return false;
		}
	}

	private boolean updateStatusDetailsWithBroadcast(StatusDetails newStatusDetails, ReportsManager rm) {
		if (!Objects.equals(statusDetails, Objects.requireNonNull(newStatusDetails))) {
			statusDetails = newStatusDetails;
			rm.broadcastReportDataChanged(this);
			return true;
		} else {
			return false;
		}
	}

	private boolean updateArchivedWithBroadcast(boolean newArchived, ReportsManager rm) {
		if (!Objects.equals(archived, newArchived)) {
			archived = newArchived;
			rm.broadcastReportDataChanged(this);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(reportId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Report)) {
			return false;
		}
		Report other = (Report) obj;
		return reportId == other.reportId;
	}

	public void update(Map<String, Object> reportData, boolean saveAdvancedData, Database db,
	        TaskScheduler taskScheduler, UsersManager um, ResultCallback<Boolean> resultCallback) {
		boolean archived = false;
		if (reportData != null) {
			archived = (int) reportData.get("archived") == 1;
		}
		update(reportData, archived, saveAdvancedData, db, taskScheduler, um, resultCallback);
	}

	public void update(Map<String, Object> reportData, boolean archived, boolean saveAdvancedData, Database db,
	        TaskScheduler taskScheduler, UsersManager um, ResultCallback<Boolean> resultCallback) {
		int reportId = (int) reportData.get("report_id");
		if (this.reportId != reportId) {
			throw new IllegalArgumentException("Report data concerns another report");
		}

		String newStatusDetails = (String) reportData.get("status");
		updateStatusDetails(newStatusDetails, db, taskScheduler, um, new ResultCallback<Boolean>() {

			@Override
			public void onResultReceived(Boolean changed) {
				String newReporters = (String) reportData.get("reporter_uuid");
				updateReportersAndDate(newReporters, (String) reportData.get("date"), db, taskScheduler, um,
				        new ResultCallback<Boolean>() {

					        @Override
					        public void onResultReceived(Boolean changed2) {
						        changed2 |= changed;

						        changed2 |= updateAppreciation((String) reportData.get("appreciation"));
						        changed2 |= updateArchived(archived);

						        if (saveAdvancedData) {
							        extractAndSaveAdvancedData(reportData);
							        // changed2 doesn't take into account advanced data save
						        }

						        if (changed2) {
							        LOGGER.info(() -> "update(): report changed, new value:" + Report.this);
						        }
						        resultCallback.onResultReceived(changed);
					        }

				        });
			}

		});
	}

	@Override
	public String toString() {
		return "Report [reportId=" + reportId + ", statusDetails=" + statusDetails + ", appreciation=" + appreciation
		        + ", date=" + date + ", reason=" + reason + ", reported=" + reported + ", reporters=" + reporters
		        + ", advancedData=" + advancedData + ", archived=" + archived + "]";
	}

	public String getBasicDataAsString() {
		String[] basicData = new String[] { Integer.toString(reportId), getConfigStatus(), appreciation, date,
		        reported.getUniqueId().toString(), getReportersUUIDStr(), reason, Boolean.toString(archived) };
		for (int i = 0; i < basicData.length; i++) {
			basicData[i].replace(DATA_SEPARATOR, "");
		}
		return String.join(DATA_SEPARATOR, basicData);
	}

	public static Map<String, Object> parseBasicDataFromString(String dataAsString) {
		if (dataAsString == null || dataAsString.isEmpty()) {
			return null;
		}
		Map<String, Object> result = new HashMap<>();
		String[] data = dataAsString.split(DATA_SEPARATOR);
		if (data.length >= 8) {
			try {
				result.put("report_id", Integer.parseInt(data[0]));
			} catch (NumberFormatException ex) {
				return null;
			}
			result.put("status", data[1]);
			result.put("appreciation", data[2]);
			result.put("date", data[3]);
			result.put("reported_uuid", data[4]);
			result.put("reporter_uuid", data[5]);
			result.put("reason", data[6]);
			result.put("archived", Boolean.parseBoolean(data[7]));
		}
		return result;
	}

}
