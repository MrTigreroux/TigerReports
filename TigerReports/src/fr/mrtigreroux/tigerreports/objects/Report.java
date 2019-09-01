package fr.mrtigreroux.tigerreports.objects;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.data.config.ConfigSound;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.Status;
import fr.mrtigreroux.tigerreports.events.ProcessReportEvent;
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

	public Report(int reportId, String status, String appreciation, String date, String reportedUniqueId, String reporterUniqueId, String reason) {
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
		return reporters[reporters.length-1];
	}

	public boolean isStackedReport() {
		return reporterUniqueId.contains(",");
	}

	public String getPlayerName(String type, boolean suffix, boolean color) {
		return getPlayerName(type.equals("Reported") ? reportedUniqueId : getReportersUniqueIds()[0], type, suffix, color);
	}

	public String getPlayerName(String uuid, String type, boolean suffix, boolean color) {
		String name = UserUtils.getName(uuid);
		return name != null ? (color ? Message.valueOf(type.toUpperCase()+"_NAME").get().replace("_Player_", name) : name)+(suffix ? Message.valueOf(
				(UserUtils.isOnline(name) ? "ONLINE" : "OFFLINE")+"_SUFFIX").get() : "") : Message.NOT_FOUND_MALE.get();
	}

	public String getReportersNames(int first) {
		String[] reporters = getReportersUniqueIds();
		String name = Message.REPORTERS_NAMES.get();
		StringBuilder names = new StringBuilder();
		for (int i = first; i < reporters.length; i++)
			names.append(name.replace("_Player_", getPlayerName(reporters[i], "Reporter", true, true)));
		return names.toString();
	}

	public String getDate() {
		return date != null ? date : Message.NOT_FOUND_FEMALE.get();
	}

	public void setStatus(Status status, boolean bungee) {
		this.status = status.getConfigWord();
		if (!bungee) {
			TigerReports.getInstance().getBungeeManager().sendPluginNotification(status+" new_status "+reportId);
			TigerReports.getInstance()
					.getDb()
					.updateAsynchronously("UPDATE tigerreports_reports SET status = ? WHERE report_id = ?", Arrays.asList(status.getConfigWord(),
							reportId));
		}
	}

	public Status getStatus() {
		return Status.getFrom(status);
	}

	public String getReason(boolean menu) {
		return reason != null	? menu ? MessageUtils.getMenuSentence(reason, Message.REPORT_DETAILS, "_Reason_", true) : reason
								: Message.NOT_FOUND_FEMALE.get();
	}

	public String implementDetails(String message, boolean menu) {
		Status status = getStatus();
		String reportersNames = isStackedReport() ? getReportersNames(0) : getPlayerName("Reporter", true, true);

		return message.replace("_Status_", status.equals(Status.DONE) ? status.getWord(getProcessor())+Message.APPRECIATION_SUFFIX.get()
				.replace("_Appreciation_", getAppreciation(false)) : status.getWord(null))
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
							.replace("_Type_", type.charAt(0)+type.substring(1).toLowerCase())
							.replace("_Amplifier_", effect.split(":")[1].replace("/"+duration, ""))
							.replace("_Duration_", Long.toString(Long.parseLong(duration)/20)));
				}
				effects = effectsLines.toString();
			} else {
				effects = Message.NONE_MALE.get();
			}
			defaultData = Message.DEFAULT_DATA.get()
					.replace("_Gamemode_", MessageUtils.getGamemodeWord(advancedData.get("reported_gamemode")))
					.replace("_OnGround_", (advancedData.get("reported_on_ground").equals("1") ? Message.YES : Message.NO).get())
					.replace("_Sneak_", (advancedData.get("reported_sneak").equals("1") ? Message.YES : Message.NO).get())
					.replace("_Sprint_", (advancedData.get("reported_sprint").equals("1") ? Message.YES : Message.NO).get())
					.replace("_Health_", advancedData.get("reported_health"))
					.replace("_Food_", advancedData.get("reported_food"))
					.replace("_Effects_", effects);
			reportedAdvancedData = !advanced	? ""
												: Message.ADVANCED_DATA_REPORTED.get()
														.replace("_UUID_", MessageUtils.getMenuSentence(reportedUniqueId,
																Message.ADVANCED_DATA_REPORTED, "_UUID_", false))
														.replace("_IP_", advancedData.get("reported_ip"));
		} catch (Exception dataNotFound) {
			defaultData = Message.PLAYER_WAS_OFFLINE.get();
		}

		return message.replace("_Reported_", getPlayerName("Reported", true, true))
				.replace("_DefaultData_", defaultData)
				.replace("_AdvancedData_", !advanced	? ""
														: reportedAdvancedData+Message.ADVANCED_DATA_REPORTER.get()
																.replace("_Player_", getPlayerName("Reporter", true, true))
																.replace("_UUID_", MessageUtils.getMenuSentence(reporterUniqueId,
																		Message.ADVANCED_DATA_REPORTER, "_UUID_", false))
																.replace("_IP_", advancedData.get("reporter_ip") != null ? advancedData.get(
																		"reporter_ip") : Message.NOT_FOUND_FEMALE.get()));
	}

	public String getOldLocation(String type) {
		if (advancedData == null)
			return null;
		return advancedData.get(type.toLowerCase()+"_location");
	}

	public String[] getMessagesHistory(String type) {
		String messages = advancedData.get(type.toLowerCase()+"_messages");
		return messages != null ? messages.split("#next#") : new String[0];
	}

	public ItemStack getItem(String actions) {
		Status status = getStatus();
		return new CustomItem().type(status.getMaterial())
				.amount(getReportersUniqueIds().length)
				.hideFlags(true)
				.glow(status.equals(Status.WAITING))
				.name(Message.REPORT.get().replace("_Report_", getName()))
				.lore(implementDetails(Message.REPORT_DETAILS.get(), true).replace("_Actions_", actions != null ? actions : "")
						.split(ConfigUtils.getLineBreakSymbol()))
				.create();
	}

	public String getText() {
		return Message.REPORT.get().replace("_Report_", getName())+"\n"+implementDetails(Message.REPORT_DETAILS.get(), false).replace("_Actions_",
				"");
	}

	public void process(String uuid, String staff, String appreciation, boolean bungee, boolean auto) {
		processing(uuid, staff, appreciation, bungee, auto, (auto ? Message.STAFF_PROCESS_AUTO : Message.STAFF_PROCESS).get()
				.replace("_Appreciation_", Message.valueOf(appreciation.toUpperCase()).get()), "process");
	}

	public void processPunishing(String uuid, String staff, boolean bungee, boolean auto, String punishment) {
		processing(uuid, staff, "True", bungee, auto, (auto ? Message.STAFF_PROCESS_PUNISH_AUTO : Message.STAFF_PROCESS_PUNISH).get()
				.replace("_Punishment_", punishment)
				.replace("_Reported_", getPlayerName("Reported", false, false)), "process_punish");
	}

	private void processing(String uuid, String staff, String appreciation, boolean bungee, boolean auto, String staffMessage, String bungeeAction) {
		this.status = Status.DONE.getConfigWord()+" by "+uuid;
		this.appreciation = appreciation;
		if (staff != null) {
			MessageUtils.sendStaffMessage(MessageUtils.getAdvancedMessage(staffMessage.replace("_Player_", staff), "_Report_", getName(), getText(),
					null), ConfigSound.STAFF.get());
		}

		TigerReports plugin = TigerReports.getInstance();
		UsersManager um = plugin.getUsersManager();
		BungeeManager bm = plugin.getBungeeManager();

		if (!bungee) {
			bm.sendPluginNotification(uuid+"/"+staff+" "+bungeeAction+" "+reportId+" "+appreciation+" "+auto);
			plugin.getDb()
					.update("UPDATE tigerreports_reports SET status = ?,appreciation = ?,archived = ? WHERE report_id = ?", Arrays.asList(status,
							appreciation, auto ? 1 : 0, reportId));

			um.getUser(uuid).changeStatistic("processed_reports", 1);

			for (String ruuid : getReportersUniqueIds()) {
				User ru = um.getUser(ruuid);
				ru.changeStatistic(appreciation.toLowerCase()+"_appreciations", 1);

				if (ConfigUtils.playersNotifications()) {
					if (ru instanceof OnlineUser) {
						((OnlineUser) ru).sendReportNotification(this, true);
					} else if (!bm.isOnline(ru.getName())) {
						List<String> notifications = ru.getNotifications();
						notifications.add(Integer.toString(getId()));
						ru.setNotifications(notifications);
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
			Bukkit.getServer().getPluginManager().callEvent(new ProcessReportEvent(this, staff));
		} catch (Exception ignored) {}
	}

	public String getProcessor() {
		String processor = null;
		if (status != null && status.startsWith(Status.DONE.getConfigWord()+" by "))
			processor = UserUtils.getName(status.replaceFirst(Status.DONE.getConfigWord()+" by ", ""));
		return processor != null ? processor : Message.NOT_FOUND_MALE.get();
	}

	public String getAppreciation(boolean config) {
		if (config)
			return appreciation;
		try {
			return appreciation != null && !appreciation.equalsIgnoreCase("None")	? Message.valueOf(appreciation.toUpperCase()).get()
																					: Message.NONE_FEMALE.get();
		} catch (Exception invalidAppreciation) {
			return Message.NONE_FEMALE.get();
		}
	}

	public Comment getCommentById(int commentId) {
		return formatComment(TigerReports.getInstance()
				.getDb()
				.query("SELECT * FROM tigerreports_comments WHERE report_id = ? AND comment_id = ?", Arrays.asList(reportId, commentId))
				.getResult(0));
	}

	public Comment getComment(int commentIndex) {
		return formatComment(TigerReports.getInstance()
				.getDb()
				.query("SELECT * FROM tigerreports_comments WHERE report_id = ? LIMIT 1 OFFSET ?", Arrays.asList(reportId, commentIndex-1))
				.getResult(0));
	}

	public Comment formatComment(Map<String, Object> result) {
		return new Comment(this, (int) result.get("comment_id"), (String) result.get("status"), (String) result.get("date"), (String) result.get(
				"author"), (String) result.get("message"));
	}

	public void delete(String staff, boolean bungee) {
		TigerReports.getInstance().getReportsManager().removeReport(reportId);
		if (staff != null)
			MessageUtils.sendStaffMessage(MessageUtils.getAdvancedMessage(Message.STAFF_DELETE.get().replace("_Player_", staff), "_Report_",
					getName(), getText(), null), ConfigSound.STAFF.get());
		if (!bungee) {
			TigerReports.getInstance().getBungeeManager().sendPluginNotification(staff+" delete "+reportId);
			TigerReports.getInstance()
					.getDb()
					.updateAsynchronously("DELETE FROM tigerreports_reports WHERE report_id = ?", Collections.singletonList(reportId));
			TigerReports.getInstance()
					.getDb()
					.updateAsynchronously("DELETE FROM tigerreports_comments WHERE report_id = ?", Collections.singletonList(reportId));
		}
	}

	public void archive(String staff, boolean bungee) {
		if (staff != null)
			MessageUtils.sendStaffMessage(MessageUtils.getAdvancedMessage(Message.STAFF_ARCHIVE.get().replace("_Player_", staff), "_Report_",
					getName(), getText(), null), ConfigSound.STAFF.get());
		if (!bungee) {
			TigerReports.getInstance().getBungeeManager().sendPluginNotification(staff+" archive "+reportId);
			TigerReports.getInstance()
					.getDb()
					.updateAsynchronously("UPDATE tigerreports_reports SET archived = ? WHERE report_id = ?", Arrays.asList(1, reportId));
		}
	}

	public void unarchive(String staff, boolean bungee) {
		if (staff != null)
			MessageUtils.sendStaffMessage(MessageUtils.getAdvancedMessage(Message.STAFF_RESTORE.get().replace("_Player_", staff), "_Report_",
					getName(), getText(), null), ConfigSound.STAFF.get());
		if (!bungee) {
			TigerReports.getInstance().getBungeeManager().sendPluginNotification(staff+" unarchive "+reportId);
			TigerReports.getInstance()
					.getDb()
					.updateAsynchronously("UPDATE tigerreports_reports SET archived = ? WHERE report_id = ?", Arrays.asList(0, reportId));
		}
	}

	public void deleteFromArchives(String staff, boolean bungee) {
		if (staff != null)
			MessageUtils.sendStaffMessage(MessageUtils.getAdvancedMessage(Message.STAFF_DELETE_ARCHIVE.get().replace("_Player_", staff), "_Report_",
					getName(), getText(), null), ConfigSound.STAFF.get());
		if (!bungee) {
			TigerReports.getInstance().getBungeeManager().sendPluginNotification(staff+" delete_archive "+reportId);
			TigerReports.getInstance()
					.getDb()
					.updateAsynchronously("DELETE FROM tigerreports_reports WHERE report_id = ?", Collections.singletonList(reportId));
		}
	}

}
