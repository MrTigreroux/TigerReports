package fr.mrtigreroux.tigerreports.objects;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import fr.mrtigreroux.tigerreports.data.ConfigFile;
import fr.mrtigreroux.tigerreports.data.Message;
import fr.mrtigreroux.tigerreports.data.Status;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;
import fr.mrtigreroux.tigerreports.utils.ReportUtils;
import fr.mrtigreroux.tigerreports.utils.UserUtils;

/**
 * @author MrTigreroux
 */

public class Report {

	int reportNumber;
	String path;
	String name;
	String reportedName = null;
	String signalmanName = null;
	String date = null;
	String reason = null;
	
	public Report(int reportNumber) {
		this.reportNumber = reportNumber;
		this.path = "Reports.Report"+reportNumber;
		this.name = Message.REPORT_NAME.get().replace("_Number_", ""+reportNumber);
	}
	
	public int getNumber() {
		return reportNumber;
	}

	public String getConfigPath() {
		return path;
	}

	public boolean exist() {
		return ConfigFile.REPORTS.get().get(path) != null;
	}

	public String getName() {
		return name;
	}
	
	public String getPlayerName(String type, boolean suffix) {
		boolean signalman = type.equals("Signalman");
		String name = signalman ? signalmanName : reportedName;
		if(name == null) name = UserUtils.getName(ConfigFile.REPORTS.get().getString(path+"."+type+".UUID"));
		if(name != null) {
			if(signalman) signalmanName = name;
			else reportedName = name;
		}
		return name != null ? suffix ? name += Message.valueOf((UserUtils.isOnline(name) ? "ONLINE" : "OFFLINE")+"_SUFFIX").get() : name : Message.NOT_FOUND_MALE.get();
	}
	
	public String getDate() {
		if(date == null) date = ConfigFile.REPORTS.get().getString(path+".Date");
		return date != null ? date.replace("-", ":") : Message.NOT_FOUND_FEMALE.get();
	}
	
	public Location getOldLocation(String type) {
		String configLoc = ConfigFile.REPORTS.get().getString(path+"."+type+".Location");
		if(configLoc == null) return null;
		try {
			String world = configLoc.split(":")[0];
			String[] coords = configLoc.replace(world+":", "").split("/");
			return new Location(Bukkit.getWorld(world), Double.parseDouble(coords[0]), Double.parseDouble(coords[1]), Double.parseDouble(coords[2]), Float.parseFloat(coords[3]), Float.parseFloat(coords[4]));
		} catch (Exception invalidLocation) {
			return null;
		}
	}

	public void setStatus(Status status) {
		ConfigFile.REPORTS.get().set(path+".Status", status.getConfigWord());
		ConfigFile.REPORTS.save();
	}
	
	public Status getStatus() {
		String status = ConfigFile.REPORTS.get().getString(path+".Status");
		try {
			if(status != null && status.startsWith(Status.DONE.getConfigWord())) return Status.DONE;
			return status != null ? Status.valueOf(status.toUpperCase().replace(" ", "_")) : Status.WAITING;
		} catch (Exception invalidStatus) {
			return Status.WAITING;
		}
	}
	
	public String getReason() {
		if(reason == null) reason = MessageUtils.getMenuSentence(ConfigFile.REPORTS.get().getString(path+".Reason"), Message.REPORT_DETAILS, "_Reason_", true);
		return reason;
	}

	public String implementDetails(String message) {
		Status status = getStatus();
		return message.replace("_Status_", status.equals(Status.DONE) ? status.getWord(getProcessor())+Message.APPRECIATION_SUFFIX.get().replace("_Appreciation_", getAppreciation()) : status.getWord(null))
				.replace("_Date_", getDate()).replace("_Signalman_", getPlayerName("Signalman", true)).replace("_Reported_", getPlayerName("Reported", true))
				.replace("_Reason_", getReason());
	}
	
	public String implementData(String message, boolean advanced) {
		String reportedPath = path+".Reported";
		String signalmanPath = path+".Signalman";
		String effects = "";
		try {
			String effectsList = ConfigFile.REPORTS.get().getString(reportedPath+".Effects");
			if(effectsList != null && effectsList.contains(":") && effectsList.contains("/")) {
				for(String effect : effectsList.split(",")) {
					String type = effect.split(":")[0].replace("_", " ");
					String duration = effect.split("/")[1];
					effects += Message.EFFECT.get().replace("_Type_", type.substring(0, 1)+type.substring(1, type.length()).toLowerCase()).replace("_Amplifier_", effect.split(":")[1].replace("/"+duration, "")).replace("_Duration_", ""+Long.parseLong(duration)/20);
				}
			} else effects = Message.NONE_MALE.get();
			return message.replace("_Reported_", getPlayerName("Reported", true)).replace("_DefaultData_", Message.DEFAULT_DATA.get()
					.replace("_Gamemode_", MessageUtils.getGamemodeWord(ConfigFile.REPORTS.get().getString(reportedPath+".Gamemode"))).replace("_OnGround_", (ConfigFile.REPORTS.get().getBoolean(reportedPath+".OnGround") ? Message.YES : Message.NO).get())
					.replace("_Sneak_", (ConfigFile.REPORTS.get().getBoolean(reportedPath+".Sneak") ? Message.YES : Message.NO).get()).replace("_Sprint_", (ConfigFile.REPORTS.get().getBoolean(reportedPath+".Sprint") ? Message.YES : Message.NO).get())
					.replace("_Health_", ConfigFile.REPORTS.get().getString(reportedPath+".Health")).replace("_Food_", ConfigFile.REPORTS.get().getString(reportedPath+".Food")).replace("_Effects_", effects))
					.replace("_AdvancedData_", advanced == false ? "" : Message.ADVANCED_DATA_REPORTED.get().replace("_UUID_", MessageUtils.getMenuSentence(ConfigFile.REPORTS.get().getString(reportedPath+".UUID"), Message.ADVANCED_DATA_SIGNALMAN, "_UUID_", false))
							.replace("_IP_", ConfigFile.REPORTS.get().getString(reportedPath+".IP"))+Message.ADVANCED_DATA_SIGNALMAN.get().replace("_Player_", getPlayerName("Signalman", true))
							.replace("_UUID_", MessageUtils.getMenuSentence(ConfigFile.REPORTS.get().getString(signalmanPath+".UUID"), Message.ADVANCED_DATA_SIGNALMAN, "_UUID_", false)).replace("_IP_", ConfigFile.REPORTS.get().getString(signalmanPath+".IP")));
		} catch (Exception dataNotFound) {
			return message.replace("_Reported_", getPlayerName("Reported", true)).replace("_DefaultData_", Message.PLAYER_WAS_OFFLINE.get()).replace("_AdvancedData_", advanced == false ? "" : Message.ADVANCED_DATA_SIGNALMAN.get().replace("_Player_", getPlayerName("Signalman", true))
					.replace("_UUID_", MessageUtils.getMenuSentence(ConfigFile.REPORTS.get().getString(signalmanPath+".UUID"), Message.ADVANCED_DATA_SIGNALMAN, "_UUID_", false)).replace("_IP_", ConfigFile.REPORTS.get().getString(signalmanPath+".IP")));
		}
	}
	
	public String getMessagesHistory(String type) {
		String messagesHistory = ConfigFile.REPORTS.get().getString(path+"."+type+".Messages");
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
	
	public void setDone(UUID uuid, String appreciation) {
		ConfigFile.REPORTS.get().set(path+".Status", Status.DONE.getConfigWord()+" by "+uuid);
		ConfigFile.REPORTS.get().set(path+".Appreciation", appreciation);
		UserUtils.changeStat(uuid.toString(), "ProcessedReports", 1);
		ConfigFile.REPORTS.save();
	}
	
	public String getProcessor() {
		String status = ConfigFile.REPORTS.get().getString(path+".Status");
		String processor = null;
		if(status != null && status.startsWith(Status.DONE.getConfigWord()+" by ")) processor = UserUtils.getName(status.replaceFirst(Status.DONE.getConfigWord()+" by ", ""));
		return processor != null ? processor : Message.NOT_FOUND_MALE.get();
	}

	public String getAppreciation() {
		String appreciation = ConfigFile.REPORTS.get().getString(path+".Appreciation");
		try {
			return appreciation != null && !appreciation.equalsIgnoreCase("None") ? Message.valueOf(appreciation.toUpperCase()).get() : Message.NONE_FEMALE.get();
		} catch (Exception invalidAppreciation) {
			return Message.NONE_FEMALE.get();
		}
	}

	public int getTotalComments() {
		int totalComments = 0;
		String path = this.path+".Comments.Comment";
		for(int commentNumber = 1; commentNumber <= ReportUtils.getMaxReports(); commentNumber++) {
			if(ConfigFile.REPORTS.get().get(path+commentNumber) == null) break;
			else totalComments++;
		}
		return totalComments;
	}
	
	public void remove() {
		ConfigFile.REPORTS.get().set(path, null);
		ConfigFile.REPORTS.get().set(path, "archived");
		for(int reportsNumber = reportNumber+1; reportsNumber <= ReportUtils.getTotalReports(); reportsNumber++) {
			String path = ReportUtils.getConfigPath(reportsNumber);
			if(!ReportUtils.exist(reportsNumber)) break;
			for(String data : ConfigFile.REPORTS.get().getConfigurationSection(ReportUtils.getConfigPath(reportsNumber)).getKeys(false)) ConfigFile.REPORTS.get().set(ReportUtils.getConfigPath(reportsNumber-1)+"."+data, ConfigFile.REPORTS.get().get(path+"."+data));
			ConfigFile.REPORTS.get().set(path, null);
			if(!ReportUtils.exist(reportsNumber+1)) break;
			ConfigFile.REPORTS.get().set(path, "moved");
		}
		if(ConfigFile.REPORTS.get().getString(path).equals("archived")) ConfigFile.REPORTS.get().set(path, null);
		ConfigFile.REPORTS.save();
	}
	
	public void archive() {
		ConfigFile.REPORTS.get().set("Archives."+ConfigFile.REPORTS.get().getString(path+".Date"), "Archived_on:"+MessageUtils.getNowDate()+",Status:"+ConfigFile.REPORTS.get().getString(path+".Status")+",Appreciation:"+ConfigFile.REPORTS.get().getString(path+".Appreciation")+
				",REPORTED:_LastName:"+UserUtils.getName(ConfigFile.REPORTS.get().getString(path+".Reported.UUID"))+",UUID:"+ConfigFile.REPORTS.get().getString(path+".Reported.UUID")+",IP:"+ConfigFile.REPORTS.get().getString(path+".Reported.IP")+
				",Gamemode:"+ConfigFile.REPORTS.get().getString(path+".Reported.Gamemode")+",Location:"+ConfigFile.REPORTS.get().getString(path+".Reported.Location")+",OnGround:"+ConfigFile.REPORTS.get().getString(path+".Reported.OnGround")+
				",Health:"+ConfigFile.REPORTS.get().getString(path+".Reported.Health")+",Food:"+ConfigFile.REPORTS.get().getString(path+".Reported.Food")+",Effects:"+ConfigFile.REPORTS.get().getString(path+".Reported.Effects")+
				",Sneak:"+ConfigFile.REPORTS.get().getString(path+".Reported.Sneak")+",Sprint:"+ConfigFile.REPORTS.get().getString(path+".Reported.Sprint")+",SIGNALMAN:_LastName:"+UserUtils.getName(ConfigFile.REPORTS.get().getString(path+".Signalman.UUID"))+
				",UUID:"+ConfigFile.REPORTS.get().getString(path+".Signalman.UUID")+",IP:"+ConfigFile.REPORTS.get().getString(path+".Signalman.IP")+",Gamemode:"+ConfigFile.REPORTS.get().getString(path+".Signalman.Gamemode")+
				",Location:"+ConfigFile.REPORTS.get().getString(path+".Signalman.Location"));
		remove();
	}
	
}
