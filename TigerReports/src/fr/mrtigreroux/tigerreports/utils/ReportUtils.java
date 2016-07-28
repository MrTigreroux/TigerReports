package fr.mrtigreroux.tigerreports.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import fr.mrtigreroux.tigerreports.data.Message;
import fr.mrtigreroux.tigerreports.data.Status;
import fr.mrtigreroux.tigerreports.managers.FilesManager;
import fr.mrtigreroux.tigerreports.objects.CustomItem;

/**
 * @author MrTigreroux
 */

public class ReportUtils {
	
	public static String getConfigPath(int reportNumber) {
		return "Reports.Report"+reportNumber;
	}

	public static String getName(int reportNumber) {
		return Message.REPORT_NAME.get().replaceAll("_Number_", ""+reportNumber);
	}
	
	public static String getPlayerName(String type, int reportNumber, boolean suffix) {
		String player = UserUtils.getName(FilesManager.getReports.getString(getConfigPath(reportNumber)+"."+type+".UUID"));
		return player != null ? suffix ? player += Message.valueOf((UserUtils.isOnline(player) ? "ONLINE" : "OFFLINE")+"_SUFFIX").get() : player : Message.NOT_FOUND_MALE.get();
	}

	public static void setStatus(int reportNumber, Status status) {
		FilesManager.getReports.set(getConfigPath(reportNumber)+".Status", status.getConfigWord());
		FilesManager.saveReports();
	}
	
	public static Status getStatus(int reportNumber) {
		String status = FilesManager.getReports.getString(getConfigPath(reportNumber)+".Status");
		try {
			return status != null ? Status.valueOf(status.toUpperCase()) : Status.WAITING;
		} catch (Exception invalidStatus) {
			return Status.WAITING;
		}
	}
	
	public static String getData(int reportNumber, boolean advanced) {
		String reportedPath = getConfigPath(reportNumber)+".Reported";
		String signalmanPath = getConfigPath(reportNumber)+".Signalman";
		String effects = "";
		try {
			String effectsList = FilesManager.getReports.getString(reportedPath+".Effects");
			if(effectsList != null && effectsList.contains(":") && effectsList.contains("/")) {
				for(String effect : effectsList.split(",")) {
					String type = effect.split(":")[0].replaceAll("_", " ");
					String duration = effect.split("/")[1];
					effects += Message.EFFECT.get().replaceAll("_Type_", type.substring(0, 1)+type.substring(1, type.length()).toLowerCase()).replaceAll("_Amplifier_", effect.split(":")[1].replaceAll("/"+duration, "")).replaceAll("_Duration_", ""+Long.parseLong(duration)/20);
				}
			} else effects = Message.NONE_MALE.get();
			return Message.DATA_DETAILS.get().replaceAll("_Reported_", ReportUtils.getPlayerName("Reported", reportNumber, true)).replaceAll("_DefaultData_", Message.DEFAULT_DATA.get()
					.replaceAll("_Gamemode_", MessageUtils.getGamemodeWord(FilesManager.getReports.getString(reportedPath+".Gamemode"))).replaceAll("_OnGround_", (FilesManager.getReports.getBoolean(reportedPath+".OnGround") ? Message.YES : Message.NO).get())
					.replaceAll("_Sneak_", (FilesManager.getReports.getBoolean(reportedPath+".Sneak") ? Message.YES : Message.NO).get()).replaceAll("_Sprint_", (FilesManager.getReports.getBoolean(reportedPath+".Sprint") ? Message.YES : Message.NO).get())
					.replaceAll("_Health_", FilesManager.getReports.getString(reportedPath+".Health")).replaceAll("_Food_", FilesManager.getReports.getString(reportedPath+".Food")).replaceAll("_Effects_", effects))
					.replaceAll("_AdvancedData_", advanced == false ? "" : Message.ADVANCED_DATA_REPORTED.get().replaceAll("_UUID_", MessageUtils.getMenuSentence(FilesManager.getReports.getString(reportedPath+".UUID"), Message.ADVANCED_DATA_SIGNALMAN, "_UUID_", false))
							.replaceAll("_IP_", FilesManager.getReports.getString(reportedPath+".IP"))+Message.ADVANCED_DATA_SIGNALMAN.get().replaceAll("_Player_", ReportUtils.getPlayerName("Signalman", reportNumber, true))
							.replaceAll("_UUID_", MessageUtils.getMenuSentence(FilesManager.getReports.getString(signalmanPath+".UUID"), Message.ADVANCED_DATA_SIGNALMAN, "_UUID_", false)).replaceAll("_IP_", FilesManager.getReports.getString(signalmanPath+".IP")));
		} catch (Exception dataNotFound) {
			return Message.DATA_DETAILS.get().replaceAll("_Reported_", ReportUtils.getPlayerName("Reported", reportNumber, true)).replaceAll("_DefaultData_", Message.PLAYER_WAS_OFFLINE.get()).replaceAll("_AdvancedData_", advanced == false ? "" : Message.ADVANCED_DATA_SIGNALMAN.get().replaceAll("_Player_", ReportUtils.getPlayerName("Signalman", reportNumber, true))
					.replaceAll("_UUID_", MessageUtils.getMenuSentence(FilesManager.getReports.getString(signalmanPath+".UUID"), Message.ADVANCED_DATA_SIGNALMAN, "_UUID_", false)).replaceAll("_IP_", FilesManager.getReports.getString(signalmanPath+".IP")));
		}
	}
	
	public static String getDate(int reportNumber) {
		String date = FilesManager.getReports.getString(getConfigPath(reportNumber)+".Date");
		return date != null ? date.replaceAll("-", ":") : Message.NOT_FOUND_FEMALE.get();
	}
	
	public static Location getOldLocation(String type, int reportNumber) {
		String configLoc = FilesManager.getReports.getString(getConfigPath(reportNumber)+"."+type+".Location");
		if(configLoc == null) return null;
		try {
			String world = configLoc.split(":")[0];
			String[] coords = configLoc.replaceAll(world+":", "").split("/");
			return new Location(Bukkit.getWorld(world), Double.parseDouble(coords[0]), Double.parseDouble(coords[1]), Double.parseDouble(coords[2]), Float.parseFloat(coords[3]), Float.parseFloat(coords[4]));
		} catch (Exception invalidLocation) {
			return null;
		}
	}
	
	public static int getNewReportNumber() {
		for(int reportNumber = 1; reportNumber <= getMaxReports(); reportNumber++) if(FilesManager.getReports.get(getConfigPath(reportNumber)) == null) return reportNumber;
		return -1;
	}

	public static int getNewCommentNumber(int reportNumber) {
		for(int commentNumber = 1; commentNumber <= 54; commentNumber++) if(FilesManager.getReports.get(getConfigPath(reportNumber)+".Comments.Comment"+commentNumber) == null) return commentNumber;
		return -1;
	}

	public static boolean exist(int reportNumber) {
		return FilesManager.getReports.get(getConfigPath(reportNumber)) != null;
	}
	
	public static boolean permissionRequired() {
		return ConfigUtils.isEnabled(FilesManager.getConfig, "Config.PermissionRequired");
	}
	
	public static boolean onlinePlayerRequired() {
		return ConfigUtils.isEnabled(FilesManager.getConfig, "Config.ReportOnline");
	}
	
	public static int getMinCharacters() {
		return FilesManager.getConfig.get("Config.MinCharacters") != null ? FilesManager.getConfig.getInt("Config.MinCharacters") : 4;
	}
	
	public static double getCooldown() {
		return FilesManager.getConfig.get("Config.ReportCooldown") != null ? FilesManager.getConfig.getDouble("Config.ReportCooldown") : 300;
	}
	
	public static long getPunishSeconds() {
		return FilesManager.getConfig.get("Config.PunishSeconds") != null ? FilesManager.getConfig.getLong("Config.PunishSeconds") : 3600;
	}

	public static int getMaxReports() {
		return FilesManager.getConfig.get("Config.MaxReports") != null ? FilesManager.getConfig.getInt("Config.MaxReports") : 100;
	}
	
	public static int getTotalReports() {
		int totalReports = 0;
		for(int reportNumber = 1; reportNumber <= getMaxReports(); reportNumber++) {
			if(!exist(reportNumber)) break;
			else totalReports++;
		}
		return totalReports;
	}

	public static int getTotalComments(int reportNumber) {
		int totalComments = 0;
		String path = getConfigPath(reportNumber)+".Comments.Comment";
		for(int commentNumber = 1; commentNumber <= getMaxReports(); commentNumber++) {
			if(FilesManager.getReports.get(path+commentNumber) == null) break;
			else totalComments++;
		}
		return totalComments;
	}

	public static void setAppreciation(int reportNumber, String appreciation) {
		FilesManager.getReports.set(getConfigPath(reportNumber)+".Appreciation", appreciation);
		FilesManager.saveReports();
	}

	public static String getAppreciation(int reportNumber) {
		String appreciation = FilesManager.getReports.getString(getConfigPath(reportNumber)+".Appreciation");
		try {
			return appreciation != null && !appreciation.equalsIgnoreCase("None") ? Message.valueOf(appreciation.toUpperCase()).get() : Message.NONE_FEMALE.get();
		} catch (Exception invalidAppreciation) {
			return Message.NONE_FEMALE.get();
		}
	}
	
	public static ItemStack getItem(int reportNumber, String actions) {
		Status status = getStatus(reportNumber);
		return new CustomItem().type(status.getMaterial()).hideFlags(true).glow(status.equals(Status.WAITING)).name(Message.REPORT.get().replaceAll("_Report_", getName(reportNumber)))
				.lore(implementDetails(reportNumber, Message.REPORT_DETAILS.get()).replaceAll("_Actions_", actions != null ? actions : "").split(ConfigUtils.getLineBreakSymbol())).create();
	}
	
	public static String implementDetails(int reportNumber, String message) {
		Status status = getStatus(reportNumber);
		return message.replaceAll("_Status_", status.getWord()+(status.equals(Status.DONE) ? Message.APPRECIATION_SUFFIX.get().replaceAll("_Appreciation_", getAppreciation(reportNumber)) : ""))
				.replaceAll("_Date_", getDate(reportNumber)).replaceAll("_Signalman_", getPlayerName("Signalman", reportNumber, true)).replaceAll("_Reported_", getPlayerName("Reported", reportNumber, true))
				.replaceAll("_Reason_", MessageUtils.getMenuSentence(FilesManager.getReports.getString(getConfigPath(reportNumber)+".Reason"), Message.REPORT_DETAILS, "_Reason_", true));
	}
	
	public static void remove(int reportNumber) {
		FilesManager.getReports.set(getConfigPath(reportNumber), null);
		FilesManager.getReports.set(getConfigPath(reportNumber), "archived");
		for(int reportsNumber = reportNumber+1; reportsNumber <= getTotalReports(); reportsNumber++) {
			String path = getConfigPath(reportsNumber);
			if(!exist(reportsNumber)) break;
			for(String data : FilesManager.getReports.getConfigurationSection(getConfigPath(reportsNumber)).getKeys(false)) FilesManager.getReports.set(getConfigPath(reportsNumber-1)+"."+data, FilesManager.getReports.get(path+"."+data));
			FilesManager.getReports.set(path, null);
			if(!exist(reportsNumber+1)) break;
			FilesManager.getReports.set(path, "moved");
		}
		if(FilesManager.getReports.getString(getConfigPath(reportNumber)).equals("archived")) FilesManager.getReports.set(getConfigPath(reportNumber), null);
		FilesManager.saveReports();
	}
	
	public static void archive(int reportNumber) {
		String path = getConfigPath(reportNumber);
		FilesManager.getReports.set("Archives."+FilesManager.getReports.getString(path+".Date"), "Status:"+getStatus(reportNumber).getConfigWord()+",Appreciation:"+FilesManager.getReports.getString(path+".Appreciation")+
				",REPORTED:_LastName:"+UserUtils.getName(FilesManager.getReports.getString(path+".Reported.UUID"))+",UUID:"+FilesManager.getReports.getString(path+".Reported.UUID")+",IP:"+FilesManager.getReports.getString(path+".Reported.IP")+
				",Gamemode:"+FilesManager.getReports.getString(path+".Reported.Gamemode")+",Location:"+FilesManager.getReports.getString(path+".Reported.Location")+",OnGround:"+FilesManager.getReports.getString(path+".Reported.OnGround")+
				",Health:"+FilesManager.getReports.getString(path+".Reported.Health")+",Food:"+FilesManager.getReports.getString(path+".Reported.Food")+",Effects:"+FilesManager.getReports.getString(path+".Reported.Effects")+
				",Sneak:"+FilesManager.getReports.getString(path+".Reported.Sneak")+",Sprint:"+FilesManager.getReports.getString(path+".Reported.Sprint")+",SIGNALMAN:_LastName:"+UserUtils.getName(FilesManager.getReports.getString(path+".Signalman.UUID"))+
				",UUID:"+FilesManager.getReports.getString(path+".Signalman.UUID")+",IP:"+FilesManager.getReports.getString(path+".Signalman.IP")+",Gamemode:"+FilesManager.getReports.getString(path+".Signalman.Gamemode")+
				",Location:"+FilesManager.getReports.getString(path+".Signalman.Location"));
		remove(reportNumber);
	}
	
}
