package fr.mrtigreroux.tigerreports.utils;

import fr.mrtigreroux.tigerreports.data.ConfigFile;

/**
 * @author MrTigreroux
 */

public class ReportUtils {
	
	public static String getConfigPath(int reportNumber) {
		return "Reports.Report"+reportNumber;
	}
	
	public static int getNewReportNumber() {
		for(int reportNumber = 1; reportNumber <= getMaxReports(); reportNumber++) if(!exist(reportNumber)) return reportNumber;
		return -1;
	}

	public static int getNewCommentNumber(int reportNumber) {
		for(int commentNumber = 1; commentNumber <= 54; commentNumber++) if(ConfigFile.REPORTS.get().get(getConfigPath(reportNumber)+".Comments.Comment"+commentNumber) == null) return commentNumber;
		return -1;
	}

	public static boolean exist(int reportNumber) {
		return ConfigFile.REPORTS.get().get(getConfigPath(reportNumber)) != null;
	}
	
	public static boolean permissionRequired() {
		return ConfigUtils.isEnabled(ConfigFile.CONFIG.get(), "Config.PermissionRequired");
	}
	
	public static boolean onlinePlayerRequired() {
		return ConfigUtils.isEnabled(ConfigFile.CONFIG.get(), "Config.ReportOnline");
	}
	
	public static int getMinCharacters() {
		return ConfigFile.CONFIG.get().get("Config.MinCharacters") != null ? ConfigFile.CONFIG.get().getInt("Config.MinCharacters") : 4;
	}
	
	public static double getCooldown() {
		return ConfigFile.CONFIG.get().get("Config.ReportCooldown") != null ? ConfigFile.CONFIG.get().getDouble("Config.ReportCooldown") : 300;
	}
	
	public static long getPunishSeconds() {
		return ConfigFile.CONFIG.get().get("Config.PunishSeconds") != null ? ConfigFile.CONFIG.get().getLong("Config.PunishSeconds") : 3600;
	}

	public static int getMaxReports() {
		return ConfigFile.CONFIG.get().get("Config.MaxReports") != null ? ConfigFile.CONFIG.get().getInt("Config.MaxReports") : 100;
	}
	
	public static int getTotalReports() {
		int totalReports = 0;
		for(int reportNumber = 1; reportNumber <= getMaxReports(); reportNumber++) {
			if(!exist(reportNumber)) break;
			else totalReports++;
		}
		return totalReports;
	}
	
}
