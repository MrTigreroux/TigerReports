package fr.mrtigreroux.tigerreports.objects;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.data.config.ConfigSound;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.Status;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;
import fr.mrtigreroux.tigerreports.utils.UserUtils;

/**
 * @author MrTigreroux
 */

public class Report {

	final int reportId;
	String status, appreciation;
	final String date, reportedUniqueId, signalmanUniqueId, reason;
	Map<String, String> advancedData = null;
	Map<Integer, Comment> comments = null;
	
	String reportedName = null;
	String signalmanName = null;
	
	public Report(int reportId, String status, String appreciation, String date, String reportedUniqueId, String signalmanUniqueId, String reason) {
		this.reportId = reportId;
		this.status = status;
		this.appreciation = appreciation;
		this.date = date;
		this.reportedUniqueId = reportedUniqueId;
		this.signalmanUniqueId = signalmanUniqueId;
		this.reason = reason;
	}
	
	public int getId() {
		return reportId;
	}

	public String getName() {
		return Message.REPORT_NAME.get().replace("_Id_", ""+reportId);
	}
	
	public String getReportedUniqueId() {
		return reportedUniqueId;
	}

	public String getSignalmanUniqueId() {
		return signalmanUniqueId;
	}
	
	public String getPlayerName(String type, boolean suffix) {
		boolean signalman = type.equals("Signalman");
		String name = signalman ? signalmanName : reportedName;
		if(name == null) name = UserUtils.getName(signalman ? signalmanUniqueId : reportedUniqueId);
		if(name != null) {
			if(signalman) signalmanName = name;
			else reportedName = name;
		}
		return name != null ? suffix ? name += Message.valueOf((UserUtils.isOnline(name) ? "ONLINE" : "OFFLINE")+"_SUFFIX").get() : name : Message.NOT_FOUND_MALE.get();
	}
	
	public String getDate() {
		return date != null ? date : Message.NOT_FOUND_FEMALE.get();
	}

	public void setStatus(Status status, boolean bungee) {
		this.status = status.getConfigWord();
		save();
		if(!bungee) {
			TigerReports.getBungeeManager().sendPluginNotification(status+" new_status "+reportId);
			TigerReports.getDb().updateAsynchronously("UPDATE reports SET status = ? WHERE report_id = ?", Arrays.asList(status.getConfigWord(), reportId));
		}
	}
	
	public Status getStatus() {
		return Status.getFrom(status);
	}
	
	public String getReason() {
		return reason != null ? MessageUtils.getMenuSentence(reason, Message.REPORT_DETAILS, "_Reason_", true) : Message.NOT_FOUND_FEMALE.get();
	}

	public String implementDetails(String message) {
		Status status = getStatus();
		return message.replace("_Status_", status.equals(Status.DONE) ? status.getWord(getProcessor())+Message.APPRECIATION_SUFFIX.get().replace("_Appreciation_", getAppreciation()) : status.getWord(null))
				.replace("_Date_", getDate()).replace("_Signalman_", getPlayerName("Signalman", true)).replace("_Reported_", getPlayerName("Reported", true)).replace("_Reason_", getReason());
	}
	
	public void setAdvancedData(Map<String, String> advancedData) {
		this.advancedData = advancedData;
	}
	
	public String implementData(String message, boolean advanced) {
		if(advancedData == null) return null;
		String defaultData;
		String reportedAdvancedData = "";
		try {
			String effects = "";
			String effectsList = advancedData.get("reported_effects");
			if(effectsList != null && effectsList.contains(":") && effectsList.contains("/")) {
				for(String effect : effectsList.split(",")) {
					String type = effect.split(":")[0].replace("_", " ");
					String duration = effect.split("/")[1];
					effects += Message.EFFECT.get().replace("_Type_", type.substring(0, 1)+type.substring(1).toLowerCase()).replace("_Amplifier_", effect.split(":")[1].replace("/"+duration, "")).replace("_Duration_", ""+Long.parseLong(duration)/20);
				}
			} else effects = Message.NONE_MALE.get();
			defaultData = Message.DEFAULT_DATA.get()
					.replace("_Gamemode_", MessageUtils.getGamemodeWord(advancedData.get("reported_gamemode"))).replace("_OnGround_", (advancedData.get("reported_on_ground").equals("true") ? Message.YES : Message.NO).get())
					.replace("_Sneak_", (advancedData.get("reported_sneak").equals("true") ? Message.YES : Message.NO).get()).replace("_Sprint_", (advancedData.get("reported_sprint").equals("true") ? Message.YES : Message.NO).get())
					.replace("_Health_", advancedData.get("reported_health")).replace("_Food_", advancedData.get("reported_food")).replace("_Effects_", effects);
			reportedAdvancedData = advanced == false ? "" : Message.ADVANCED_DATA_REPORTED.get().replace("_UUID_", MessageUtils.getMenuSentence(reportedUniqueId, Message.ADVANCED_DATA_REPORTED, "_UUID_", false)).replace("_IP_", advancedData.get("reported_ip"));
		} catch (Exception dataNotFound) {
			defaultData = Message.PLAYER_WAS_OFFLINE.get();
		}
		return message.replace("_Reported_", getPlayerName("Reported", true)).replace("_DefaultData_", defaultData).replace("_AdvancedData_", advanced == false ? "" : reportedAdvancedData+Message.ADVANCED_DATA_SIGNALMAN.get().replace("_Player_", getPlayerName("Signalman", true))
				.replace("_UUID_", MessageUtils.getMenuSentence(signalmanUniqueId, Message.ADVANCED_DATA_SIGNALMAN, "_UUID_", false))
				.replace("_IP_", advancedData.get("signalman_ip") != null ? advancedData.get("signalman_ip") : Message.NOT_FOUND_FEMALE.get()));
	}
	
	public String getOldLocation(String type) {
		if(advancedData == null) return null;
		return advancedData.get(type.toLowerCase()+"_location");
	}
	
	public String getMessagesHistory(String type) {
		String messagesHistory = advancedData.get(type.toLowerCase()+"_messages");
		if(messagesHistory == null || messagesHistory.isEmpty()) return Message.NONE_MALE.get();
		else return "§7- §r"+messagesHistory.replace("#next#", ConfigUtils.getLineBreakSymbol()+"§7- §r");
	}
	
	public ItemStack getItem(String actions) {
		Status status = getStatus();
		return new CustomItem().type(status.getMaterial()).hideFlags(true).glow(status.equals(Status.WAITING)).name(Message.REPORT.get().replace("_Report_", getName()))
				.lore(implementDetails(Message.REPORT_DETAILS.get()).replace("_Actions_", actions != null ? actions : "").split(ConfigUtils.getLineBreakSymbol())).create();
	}
	
	public String getText() {
		return Message.REPORT.get().replace("_Report_", getName())+"\n"+implementDetails(Message.REPORT_DETAILS.get()).replace("_Actions_", "");
	}
	
	public void process(String uuid, String player, String appreciation, boolean bungee) {
		this.status = Status.DONE.getConfigWord()+" by "+uuid;
		this.appreciation = appreciation;
		save();
		if(player != null) MessageUtils.sendStaffMessage(Message.STAFF_PROCESS.get().replace("_Player_", player).replace("_Report_", getName()).replace("_Appreciation_", Message.valueOf(appreciation.toUpperCase()).get()), ConfigSound.STAFF.get());
		if(!bungee) {
			TigerReports.getBungeeManager().sendPluginNotification(uuid+"/"+player+" process "+reportId+" "+appreciation);
			TigerReports.getDb().update("UPDATE reports SET status = ?,appreciation = ? WHERE report_id = ?", Arrays.asList(status, appreciation, reportId));
			UserUtils.getUser(uuid).changeStatistic("processed_reports", 1, false);
			UserUtils.getUser(signalmanUniqueId).changeStatistic(appreciation.toLowerCase()+"_appreciations", 1, false);
		}
	}
	
	public String getProcessor() {
		String processor = null;
		if(status != null && status.startsWith(Status.DONE.getConfigWord()+" by ")) processor = UserUtils.getName(status.replaceFirst(Status.DONE.getConfigWord()+" by ", ""));
		return processor != null ? processor : Message.NOT_FOUND_MALE.get();
	}

	public String getAppreciation() {
		try {
			return appreciation != null && !appreciation.equalsIgnoreCase("None") ? Message.valueOf(appreciation.toUpperCase()).get() : Message.NONE_FEMALE.get();
		} catch (Exception invalidAppreciation) {
			return Message.NONE_FEMALE.get();
		}
	}
	
	public void checkComments() {
		if(comments == null || comments.isEmpty()) TigerReports.getDb().createCommentsTable(reportId);
	}
	
	public Map<Integer, Comment> getComments() {
		if(comments != null) return comments;
		comments = new HashMap<Integer, Comment>(); ;
		if(!TigerReports.getDb().existsTable("report"+reportId+"_comments")) return comments;
		for(Map<String, Object> rows : TigerReports.getDb().query("SELECT * FROM report"+reportId+"_comments", null).getResultList()) {
			int commentId = (int) rows.get("comment_id");
			comments.put(commentId, new Comment(this, commentId, (String) rows.get("status"), (String) rows.get("date"), (String) rows.get("author"), (String) rows.get("message")));
		}
		save();
		return comments;
	}
	
	public void remove(String player, boolean bungee) {
		TigerReports.Reports.remove(reportId);
		if(player != null) MessageUtils.sendStaffMessage(MessageUtils.getAdvancedMessage(Message.STAFF_REMOVE.get().replace("_Player_", player), "_Report_", getName(), getText(), null), ConfigSound.STAFF.get());
		if(!bungee) {
			TigerReports.getBungeeManager().sendPluginNotification(player+" remove "+reportId);
			TigerReports.getDb().updateAsynchronously("DELETE FROM reports WHERE report_id = ?", Arrays.asList(reportId));
		}
	}
	
	public void archive(String player, boolean bungee) {
		if(player != null) MessageUtils.sendStaffMessage(MessageUtils.getAdvancedMessage(Message.STAFF_ARCHIVE.get().replace("_Player_", player), "_Report_", getName(), getText(), null), ConfigSound.STAFF.get());
		if(!bungee) {
			TigerReports.getBungeeManager().sendPluginNotification(player+" archive "+reportId);
			TigerReports.getDb().updateAsynchronously("REPLACE INTO archived_reports (report_id,status,appreciation,date,reported_uuid,signalman_uuid,reason,reported_ip,reported_location,reported_messages,reported_gamemode,reported_on_ground,reported_sneak,reported_sprint,reported_health,reported_food,reported_effects,signalman_ip,signalman_location,signalman_messages) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);",
					Arrays.asList(reportId, status, appreciation, date, reportedUniqueId, signalmanUniqueId, reason, advancedData.get("reported_ip"), advancedData.get("reported_location"), advancedData.get("reported_messages"), advancedData.get("reported_gamemode"), advancedData.get("reported_on_ground"), advancedData.get("reported_sneak"), advancedData.get("reported_sprint"), advancedData.get("reported_health"), advancedData.get("reported_food"), advancedData.get("reported_effects"), advancedData.get("signalman_ip"), advancedData.get("signalman_location"), advancedData.get("signalman_messages")));
		}
		remove(null, bungee);
	}
	
	public void unarchive(String player, boolean bungee) {
		MessageUtils.sendStaffMessage(MessageUtils.getAdvancedMessage(Message.STAFF_RESTORE.get().replace("_Player_", player), "_Report_", getName(), getText(), null), ConfigSound.STAFF.get());
		save();
		if(!bungee) {
			TigerReports.getBungeeManager().sendPluginNotification(player+" unarchive "+reportId);
			Bukkit.getScheduler().runTaskAsynchronously(TigerReports.getInstance(), new Runnable() {
				@Override
				public void run() {
					TigerReports.getDb().update("REPLACE INTO reports (report_id,status,appreciation,date,reported_uuid,signalman_uuid,reason,reported_ip,reported_location,reported_messages,reported_gamemode,reported_on_ground,reported_sneak,reported_sprint,reported_health,reported_food,reported_effects,signalman_ip,signalman_location,signalman_messages) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);",
							Arrays.asList(reportId, status, appreciation, date, reportedUniqueId, signalmanUniqueId, reason, (String) advancedData.get("reported_ip"), 
							(String) advancedData.get("reported_location"), (String) advancedData.get("reported_messages"), (String) advancedData.get("reported_gamemode"), (String) advancedData.get("reported_on_ground"), (String) advancedData.get("reported_sneak"), (String) advancedData.get("reported_sprint"), (String) advancedData.get("reported_health"), (String) advancedData.get("reported_food"),
							(String) advancedData.get("reported_effects"), (String) advancedData.get("signalman_ip"), (String) advancedData.get("signalman_location"), (String) advancedData.get("signalman_messages")));
					TigerReports.getDb().update("DELETE FROM archived_reports WHERE report_id = ?", Arrays.asList(reportId));
				}
			});
		}
	}
	
	public void removeFromArchives(String player, boolean bungee) {
		MessageUtils.sendStaffMessage(MessageUtils.getAdvancedMessage(Message.STAFF_REMOVE_ARCHIVE.get().replace("_Player_", player), "_Report_", getName(), getText(), null), ConfigSound.STAFF.get());
		if(!bungee) {
			TigerReports.getBungeeManager().sendPluginNotification(player+" remove_archive "+reportId);
			TigerReports.getDb().updateAsynchronously("DELETE FROM archived_reports WHERE report_id = ?", Arrays.asList(reportId));
		}
	}
	
	public void save() {
		TigerReports.Reports.put(reportId, this);
	}
	
}
