package fr.mrtigreroux.tigerreports.utils;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;

public class VersionUtils {

	private static double version = 0;
	
	public static String ver() {
		String pkg = Bukkit.getServer().getClass().getPackage().getName();
		return pkg.substring(pkg.lastIndexOf(".")+1);
    }
	
	private static double getVersion() {
		if(version == 0) {
			String ver = Bukkit.getVersion();
			ver = ver.substring(ver.indexOf('(')+5, ver.length()-1).replaceFirst("\\.", "");
			try {
				version = Double.parseDouble(ver+(StringUtils.countMatches(ver, ".") == 0 ? ".0" : ""));
			} catch (Exception ignored) {}
		}
		return version;
	}
	
	public static boolean isOldVersion() {
		return getVersion() < 18;
	}
	
}