package fr.mrtigreroux.tigerreports.utils;

import java.util.*;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

import com.google.common.primitives.Ints;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.data.config.*;
import fr.mrtigreroux.tigerreports.data.constants.MenuItem;
import fr.mrtigreroux.tigerreports.objects.Report;

/**
 * @author MrTigreroux
 */

public class ReportUtils {
	
	public static void sendReport(Report r, String server) {
		int reportId = r.getId();
		TextComponent alert = new TextComponent(Message.ALERT.get().replace("_Server_", server).replace("_Signalman_", r.getPlayerName("Signalman", false, true)).replace("_Reported_", r.getPlayerName("Reported", !ReportUtils.onlinePlayerRequired(), true)).replace("_Reason_", r.getReason(false)));
		alert.setColor(ChatColor.valueOf(MessageUtils.getLastColor(Message.ALERT.get(), "_Reason_").name()));
		if(reportId != -1) {
			alert.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/reports #"+reportId));
			alert.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(Message.ALERT_DETAILS.get().replace("_Report_", r.getName())).create()));
		} else MessageUtils.sendStaffMessage(Message.STAFF_MAX_REPORTS_REACHED.get().replace("_Amount_", ""+getMaxReports()), ConfigSound.STAFF.get());
		MessageUtils.sendStaffMessage(alert, ConfigSound.REPORT.get());
	}
	
	public static Report getReportById(int reportId) {
		return reportId <= 0 ? null : TigerReports.Reports.containsKey(reportId) ? TigerReports.Reports.get(reportId) : formatReport(TigerReports.getDb().query("SELECT * FROM reports WHERE report_id = ?", Collections.singletonList(reportId)).getResult(0), true);
	}
	
	public static Report getReport(int reportIndex) {
		return formatReport(TigerReports.getDb().query("SELECT * FROM reports LIMIT 1 OFFSET ?", Collections.singletonList(reportIndex-1)).getResult(0), true);
	}
	
	public static Report formatReport(Map<String, Object> result, boolean containsAdvancedData) {
		if(result == null) return null;
		Report r = new Report((int) result.get("report_id"), (String) result.get("status"), (String) result.get("appreciation"), (String) result.get("date"), (String) result.get("reported_uuid"), (String) result.get("signalman_uuid"), (String) result.get("reason"));
		if(!containsAdvancedData) return r;
		
		Map<String, String> advancedData = new HashMap<>();
		Set<String> advancedKeys = new HashSet<>(result.keySet());
		advancedKeys.removeAll(Arrays.asList("report_id", "status", "appreciation", "date", "reported_uuid", "signalman_uuid", "reason"));
		for(String key : advancedKeys) advancedData.put(key, (String) result.get(key));
		r.setAdvancedData(advancedData);
		r.save();
		return r;
	}
	
	public static void addReports(String table, Inventory inv, int page, String actions) {
		int size = inv.getSize();
		int firstReport = 1;
		if(page >= 2) {
			inv.setItem(size-7, MenuItem.PAGE_SWITCH_PREVIOUS.get());
			firstReport += (page-1)*27;
		}
		
		List<Map<String, Object>> results = TigerReports.getDb().query("SELECT report_id,status,appreciation,date,reported_uuid,signalman_uuid,reason FROM "+table+" LIMIT 28 OFFSET ?", Collections.singletonList(firstReport-1)).getResultList();
		int index = 0;
		ItemStack empty = new ItemStack(Material.AIR);
		for(int position = 18; position < 45; position++) {
			if(index == -1) inv.setItem(position, empty);
			else {
				Report r = ReportUtils.formatReport(index < results.size() ? results.get(index) : null, false);
				if(r == null) {
					inv.setItem(position, empty);
					index = -1;
				} else {
					inv.setItem(position, r.getItem(actions));
					index++;
				}
			}
		}
		
		if(results.size() == 28) inv.setItem(size-3, MenuItem.PAGE_SWITCH_NEXT.get());
	}
	
	public static int getTotalReports() {
		Object o = TigerReports.getDb().query("SELECT COUNT(report_id) AS Total FROM reports", null).getResult(0, "Total");
		return o instanceof Integer ? (int) o : Ints.checkedCast((long) o);
	}
	
	public static int getMaxReports() {
		return ConfigFile.CONFIG.get().getInt("Config.MaxReports", 100);
	}
	
	public static boolean permissionRequired() {
		return ConfigUtils.isEnabled(ConfigFile.CONFIG.get(), "Config.PermissionRequired");
	}
	
	public static boolean onlinePlayerRequired() {
		return ConfigUtils.isEnabled(ConfigFile.CONFIG.get(), "Config.ReportOnline");
	}
	
	public static int getMinCharacters() {
		return ConfigFile.CONFIG.get().getInt("Config.MinCharacters", 4);
	}
	
	public static double getCooldown() {
		return ConfigFile.CONFIG.get().getDouble("Config.ReportCooldown", 300);
	}
	
	public static long getPunishSeconds() {
		return ConfigFile.CONFIG.get().getLong("Config.PunishSeconds", 3600);
	}

	public static boolean onlyDoneArchives() {
		return ConfigUtils.isEnabled(ConfigFile.CONFIG.get(), "Config.OnlyDoneArchives");
	}
	
}
