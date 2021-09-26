package fr.mrtigreroux.tigerreports.objects;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.data.config.ConfigFile;
import fr.mrtigreroux.tigerreports.data.config.ConfigSound;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.Statistic;
import fr.mrtigreroux.tigerreports.data.constants.Status;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.events.ProcessReportEvent;
import fr.mrtigreroux.tigerreports.events.ReportStatusChangeEvent;
import fr.mrtigreroux.tigerreports.managers.BungeeManager;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.objects.users.OnlineUser;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;
import fr.mrtigreroux.tigerreports.utils.UserUtils;

/**
 * @author MrTigreroux
 */

public class Report {

	private final int reportId;
	private String status, appreciation;
	private final String date, reportedUniqueId, reporterUniqueId, reason;
	private Map<String, String> advancedData = null;

	public Report(int reportId, String status, String appreciation, String date, String reportedUniqueId,
	        String reporterUniqueId, String reason) {
		this.reportId = reportId;
		this.status = status;
		this.appreciation = appreciation;
		this.date = date;
		this.reportedUniqueId = reportedUniqueId;
		this.reporterUniqueId = reporterUniqueId;
		this.reason = reason;
	}

	public int getId() {
		return reportId;
	}

	public String getName() {
		return Message.REPORT_NAME.get().replace("_Id_", Integer.toString(reportId));
	}

	public String getReportedUniqueId() {
		return reportedUniqueId;
	}

	public String getReporterUniqueId() {
		return isStackedReport() ? getReportersUniqueIds()[0] : reporterUniqueId;
	}

	public String[] getReportersUniqueIds() {
		return reporterUniqueId.split(",");
	}

	public String getLastReporterUniqueId() {
		String[] reporters = getReportersUniqueIds();
		return reporters[reporters.length - 1];
	}

	public boolean isStackedReport() {
		return reporterUniqueId.contains(",");
	}

	public String getPlayerName(String type, boolean onlineSuffix, boolean color) {
		return getPlayerName(type.equals("Reported") ? reportedUniqueId : getReportersUniqueIds()[0], type,
		        onlineSuffix, color);
	}

	public String getPlayerName(String uuid, String type, boolean onlineSuffix, boolean color) {
		String name = UserUtils.getName(uuid);
		return name != null ? (color
		        ? Message.valueOf(type.toUpperCase() + "_NAME")
		                .get()
		                .replace("_Player_", UserUtils.getDisplayName(uuid, false))
		        : name)
		        + (onlineSuffix ? Message.valueOf((UserUtils.isOnline(name) ? "ONLINE" : "OFFLINE") + "_SUFFIX").get()
		                : "")
		        : Message.NOT_FOUND_MALE.get();
	}

	public String getReportersNames(int first, boolean onlineSuffix) {
		String[] reporters = getReportersUniqueIds();
		String name = Message.REPORTERS_NAMES.get();
		StringBuilder names = new StringBuilder();
		for (int i = first; i < reporters.length; i++) {
			names.append(name.replace("_Player_", getPlayerName(reporters[i], "Reporter", onlineSuffix, true)));
		}
		return names.toString();
	}

	public String getDate() {
		return date != null ? date : Message.NOT_FOUND_FEMALE.get();
	}

	public void setStatus(Status status, String staffUuid, boolean bungee) {
		TigerReports tr = TigerReports.getInstance();
		this.status = status.getConfigWord() + (status == Status.IN_PROGRESS ? "-" + staffUuid : "");
		if (!bungee) {
			tr.getBungeeManager().sendPluginNotification(status + " new_status " + reportId + " " + staffUuid);
			tr.getDb()
			        .updateAsynchronously("UPDATE tigerreports_reports SET status = ? WHERE report_id = ?",
			                Arrays.asList(this.status, reportId));
		}

		try {
			Bukkit.getServer().getPluginManager().callEvent(new ReportStatusChangeEvent(this, status.getConfigWord()));
		} catch (Exception ignored) {}
	}

	public Status getStatus() {
		return Status.getFrom(status);
	}

	public String getReason(boolean menu) {
		return reason != null
		        ? menu ? MessageUtils.getMenuSentence(reason, Message.REPORT_DETAILS, "_Reason_", true) : reason
		        : Message.NOT_FOUND_FEMALE.get();
	}

	public String getAppreciation(boolean config) {
		int pos = appreciation.indexOf('/');
		String appreciationWord = pos != -1 ? appreciation.substring(0, pos) : appreciation;
		if (config)
			return appreciationWord;
		try {
			return appreciation != null && !appreciation.equalsIgnoreCase("None")
			        ? Message.valueOf(appreciationWord.toUpperCase()).get()
			        : Message.NONE_FEMALE.get();
		} catch (Exception invalidAppreciation) {
			return Message.NONE_FEMALE.get();
		}
	}

	public String getStaffOfStatus(String prefix) {
		String staff = null;
		if (status != null && status.startsWith(prefix))
			staff = UserUtils.getDisplayName(status.replaceFirst(prefix, ""), true);
		return staff != null ? staff : Message.NOT_FOUND_MALE.get();
	}

	public String getProcessor() {
		return getStaffOfStatus(Status.DONE.getConfigWord() + " by ");
	}

	public String getProcessingStaff() {
		return getStaffOfStatus(Status.IN_PROGRESS.getConfigWord() + "-");
	}

	public String getPunishment() {
		int pos = appreciation.indexOf('/');
		return pos != -1 ? appreciation.substring(pos + 1) : Message.NONE_FEMALE.get();
	}

	public String implementDetails(String message, boolean menu) {
		Status status = getStatus();
		String reportersNames = isStackedReport() ? getReportersNames(0, true) : getPlayerName("Reporter", true, true);
		String suffix = getAppreciation(true).equalsIgnoreCase("true")
		        ? Message.get("Words.Done-suffix.True-appreciation").replace("_Punishment_", getPunishment())
		        : Message.get("Words.Done-suffix.Other-appreciation").replace("_Appreciation_", getAppreciation(false));
		return message
		        .replace("_Status_",
		                status.equals(Status.DONE) ? status.getWord(getProcessor()) + suffix
		                        : status.equals(Status.IN_PROGRESS) ? status.getWord(getProcessingStaff(), true)
		                                : status.getWord(null))
		        .replace("_Date_", getDate())
		        .replace("_Reporters_", reportersNames)
		        .replace("_Reported_", getPlayerName("Reported", true, true))
		        .replace("_Reason_", getReason(menu));
	}

	public void setAdvancedData(Map<String, String> advancedData) {
		this.advancedData = advancedData;
	}

	public String implementData(String message, boolean advanced) {
		if (advancedData == null)
			return null;

		String defaultData;
		String reportedAdvancedData = "";
		try {
			String effects;
			String effectsList = advancedData.get("reported_effects");
			if (effectsList != null && effectsList.contains(":") && effectsList.contains("/")) {
				StringBuilder effectsLines = new StringBuilder();
				for (String effect : effectsList.split(",")) {
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
			                        MessageUtils.getMenuSentence(reportedUniqueId, Message.ADVANCED_DATA_REPORTED,
			                                "_UUID_", false))
			                .replace("_IP_", advancedData.get("reported_ip"));
		} catch (Exception dataNotFound) {
			defaultData = Message.PLAYER_WAS_OFFLINE.get();
		}

		return message.replace("_Reported_", getPlayerName("Reported", true, true))
		        .replace("_DefaultData_", defaultData)
		        .replace("_AdvancedData_",
		                !advanced ? ""
		                        : reportedAdvancedData + Message.ADVANCED_DATA_REPORTER.get()
		                                .replace("_Player_", getPlayerName("Reporter", true, true))
		                                .replace("_UUID_",
		                                        MessageUtils.getMenuSentence(reporterUniqueId,
		                                                Message.ADVANCED_DATA_REPORTER, "_UUID_", false))
		                                .replace("_IP_",
		                                        advancedData.get("reporter_ip") != null
		                                                ? advancedData.get("reporter_ip")
		                                                : Message.NOT_FOUND_FEMALE.get()));
	}

	public String getOldLocation(String type) {
		if (advancedData == null)
			return null;
		return advancedData.get(type.toLowerCase() + "_location");
	}

	public String[] getMessagesHistory(String type) {
		String messages = advancedData.get(type.toLowerCase() + "_messages");
		return messages != null ? messages.split("#next#") : new String[0];
	}

	public ItemStack getItem(String actions) {
		Status status = getStatus();
		return status.getIcon()
		        .amount(getReportersUniqueIds().length)
		        .hideFlags(true)
		        .glow(status.equals(Status.WAITING))
		        .name(Message.REPORT.get().replace("_Report_", getName()))
		        .lore(implementDetails(Message.REPORT_DETAILS.get(), true)
		                .replace("_Actions_", actions != null ? actions : "")
		                .split(ConfigUtils.getLineBreakSymbol()))
		        .create();
	}

	public String getText() {
		return implementDetails(Message.get("Messages.Report-details")
		        .replace("_Report_", Message.REPORT.get().replace("_Report_", getName())), false);
	}

	public void process(String staffUuid, String appreciation, boolean bungee, boolean auto, boolean notifyStaff) {
		processing(staffUuid, appreciation, bungee, auto,
		        (auto ? Message.STAFF_PROCESS_AUTO : Message.STAFF_PROCESS).get()
		                .replace("_Appreciation_", Message.valueOf(appreciation.toUpperCase()).get()),
		        "process", notifyStaff, null);
	}

	public void processPunishing(String staffUuid, boolean bungee, boolean auto, String punishment,
	        boolean notifyStaff) {
		processing(staffUuid, "True/" + punishment, bungee, auto,
		        (auto ? Message.STAFF_PROCESS_PUNISH_AUTO : Message.STAFF_PROCESS_PUNISH).get()
		                .replace("_Punishment_", punishment)
		                .replace("_Reported_", getPlayerName("Reported", false, true)),
		        "process_punish", notifyStaff, null);
	}

	public void processAbusive(String staffUuid, boolean bungee, boolean auto, long punishSeconds,
	        boolean notifyStaff) {
		String time = MessageUtils.convertToSentence(punishSeconds);
		processing(staffUuid, "False", bungee, auto,
		        Message.get("Messages.Staff-process-abusive").replace("_Time_", time), "process_abusive", notifyStaff,
		        Long.toString(punishSeconds));

		String[] usersUuid = getReportersUniqueIds();
		TigerReports tr = TigerReports.getInstance();
		if (!bungee) {
			tr.getUsersManager().startCooldownForUsers(usersUuid, punishSeconds, tr.getDb());
			ConfigUtils.processCommands(ConfigFile.CONFIG.get(), "Config.AbusiveReport.Commands", this,
			        UserUtils.getPlayerFromUniqueId(staffUuid));
		}

		UsersManager um = tr.getUsersManager();
		String cooldown = MessageUtils.getRelativeDate(punishSeconds);
		for (String uuid : usersUuid) {
			User u = um.getUser(uuid);
			if (u != null && u instanceof OnlineUser) {
				((OnlineUser) u).setCooldown(cooldown);
				u.sendMessage(Message.PUNISHED.get().replace("_Time_", time));
			}
		}
	}

	private void processing(String staffUuid, String appreciation, boolean bungee, boolean auto, String staffMessage,
	        String bungeeAction, boolean notifyStaff, String bungeeExtraData) {
		this.status = Status.DONE.getConfigWord() + " by " + staffUuid;
		this.appreciation = appreciation;

		if (notifyStaff) {
			MessageUtils.sendStaffMessage(MessageUtils.getAdvancedMessage(
			        staffMessage.replace("_Player_", UserUtils.getDisplayName(staffUuid, true)), "_Report_", getName(),
			        getText(), null), ConfigSound.STAFF.get());
		}

		TigerReports tr = TigerReports.getInstance();
		UsersManager um = tr.getUsersManager();
		BungeeManager bm = tr.getBungeeManager();
		Database db = tr.getDb();

		if (!bungee) {
			bm.sendPluginNotification(
			        String.join(" ", staffUuid, bungeeAction, "" + reportId, auto ? "1" : "0", appreciation)
			                + bungeeExtraData != null ? " " + bungeeExtraData : "");
			db.update("UPDATE tigerreports_reports SET status = ?,appreciation = ?,archived = ? WHERE report_id = ?",
			        Arrays.asList(status, appreciation, auto ? 1 : 0, reportId));

			um.getUser(staffUuid).changeStatistic(Statistic.PROCESSED_REPORTS, 1, db);

			for (String ruuid : getReportersUniqueIds()) {
				User ru = um.getUser(ruuid);
				ru.changeStatistic(getAppreciation(true).toLowerCase() + "_appreciations", 1, db);

				if (ConfigUtils.playersNotifications()) {
					if (ru instanceof OnlineUser) {
						((OnlineUser) ru).sendReportNotification(this, true);
					} else if (!bm.isOnline(ru.getName())) {
						Bukkit.getScheduler().runTaskAsynchronously(tr, new Runnable() {

							@Override
							public void run() {
								List<String> notifications = ru.getNotifications();
								notifications.add(Integer.toString(getId()));
								ru.setNotifications(notifications);
							}

						});
					}
				}
			}
		} else {
			if (ConfigUtils.playersNotifications()) {
				for (String ruuid : getReportersUniqueIds()) {
					User ru = um.getUser(ruuid);
					if (ru instanceof OnlineUser)
						((OnlineUser) ru).sendReportNotification(this, true);
				}
			}
		}

		try {
			Bukkit.getServer().getPluginManager().callEvent(new ProcessReportEvent(this, UserUtils.getName(staffUuid)));
		} catch (Exception ignored) {}
	}

	public void addComment(User u, String message, Database db) {
		addComment(u.getUniqueId().toString(), message, db);
	}

	public void addComment(String author, String message, Database db) {
		db.insertAsynchronously(
		        "INSERT INTO tigerreports_comments (report_id,status,date,author,message) VALUES (?,?,?,?,?)",
		        Arrays.asList(reportId, "Private", MessageUtils.getNowDate(), author, message));
	}

	public Comment getCommentById(int commentId) {
		return getCommentFrom(TigerReports.getInstance()
		        .getDb()
		        .query("SELECT * FROM tigerreports_comments WHERE report_id = ? AND comment_id = ?",
		                Arrays.asList(reportId, commentId))
		        .getResult(0));
	}

	public Comment getComment(int commentIndex) {
		return getCommentFrom(TigerReports.getInstance()
		        .getDb()
		        .query("SELECT * FROM tigerreports_comments WHERE report_id = ? LIMIT 1 OFFSET ?",
		                Arrays.asList(reportId, commentIndex - 1))
		        .getResult(0));
	}

	public Comment getCommentFrom(Map<String, Object> result) {
		return new Comment(this, (int) result.get("comment_id"), (String) result.get("status"),
		        (String) result.get("date"), (String) result.get("author"), (String) result.get("message"));
	}

	public void delete(String staffUuid, boolean bungee) {
		TigerReports tr = TigerReports.getInstance();
		tr.getReportsManager().removeReport(reportId);
		if (staffUuid != null)
			MessageUtils.sendStaffMessage(MessageUtils.getAdvancedMessage(
			        Message.STAFF_DELETE.get().replace("_Player_", UserUtils.getDisplayName(staffUuid, true)),
			        "_Report_", getName(), getText(), null), ConfigSound.STAFF.get());
		if (!bungee) {
			tr.getBungeeManager().sendPluginNotification(staffUuid + " delete " + reportId);
			tr.getDb()
			        .updateAsynchronously("DELETE FROM tigerreports_reports WHERE report_id = ?",
			                Collections.singletonList(reportId));
			tr.getDb()
			        .updateAsynchronously("DELETE FROM tigerreports_comments WHERE report_id = ?",
			                Collections.singletonList(reportId));
		}
	}

	public void archive(String staffUuid, boolean bungee) {
		TigerReports tr = TigerReports.getInstance();
		if (staffUuid != null)
			MessageUtils.sendStaffMessage(MessageUtils.getAdvancedMessage(
			        Message.STAFF_ARCHIVE.get().replace("_Player_", UserUtils.getDisplayName(staffUuid, true)),
			        "_Report_", getName(), getText(), null), ConfigSound.STAFF.get());
		if (!bungee) {
			tr.getBungeeManager().sendPluginNotification(staffUuid + " archive " + reportId);
			tr.getDb()
			        .updateAsynchronously("UPDATE tigerreports_reports SET archived = ? WHERE report_id = ?",
			                Arrays.asList(1, reportId));
		}
	}

	public void unarchive(String staffUuid, boolean bungee) {
		TigerReports tr = TigerReports.getInstance();
		if (staffUuid != null)
			MessageUtils.sendStaffMessage(MessageUtils.getAdvancedMessage(
			        Message.STAFF_RESTORE.get().replace("_Player_", UserUtils.getDisplayName(staffUuid, true)),
			        "_Report_", getName(), getText(), null), ConfigSound.STAFF.get());
		if (!bungee) {
			tr.getBungeeManager().sendPluginNotification(staffUuid + " unarchive " + reportId);
			tr.getDb()
			        .updateAsynchronously("UPDATE tigerreports_reports SET archived = ? WHERE report_id = ?",
			                Arrays.asList(0, reportId));
		}
	}

	public void deleteFromArchives(String staffUuid, boolean bungee) {
		TigerReports tr = TigerReports.getInstance();
		if (staffUuid != null)
			MessageUtils.sendStaffMessage(MessageUtils.getAdvancedMessage(
			        Message.STAFF_DELETE_ARCHIVE.get().replace("_Player_", UserUtils.getDisplayName(staffUuid, true)),
			        "_Report_", getName(), getText(), null), ConfigSound.STAFF.get());
		if (!bungee) {
			tr.getBungeeManager().sendPluginNotification(staffUuid + " delete_archive " + reportId);
			tr.getDb()
			        .updateAsynchronously("DELETE FROM tigerreports_reports WHERE report_id = ?",
			                Collections.singletonList(reportId));
		}
	}

}
