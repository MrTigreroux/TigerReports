package fr.mrtigreroux.tigerreports.runnables;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.data.config.ConfigFile;
import fr.mrtigreroux.tigerreports.data.config.ConfigSound;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.Status;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;

/**
 * @author MrTigreroux
 */

public class ReportsNotifier implements Runnable {

	private static int taskId = -1;
	
	@Override
	public void run() {
		String reportsNotifications = getReportsNotification();
		if(reportsNotifications != null)
			MessageUtils.sendStaffMessage(reportsNotifications, ConfigSound.STAFF.get());
	}
	
	public static String getReportsNotification() {
		String reportsNotification = Message.REPORTS_NOTIFICATION.get();
		HashMap<Status, Integer> statusTypes = new HashMap<>();
		int totalAmount = 0;
		for(Map<String, Object> result : TigerReports.getInstance().getDb().query("SELECT status FROM tigerreports_reports WHERE archived = ?", Collections.singletonList(0)).getResultList()) {
			Status status = Status.getFrom((String) result.get("status"));
			statusTypes.put(status, (statusTypes.get(status) != null ? statusTypes.get(status) : 0)+1);
		}
		
		for(Status status : Status.values()) {
			String statusPlaceHolder = "_"+status.getConfigWord()+"_";
			if(!reportsNotification.contains(statusPlaceHolder))
				break;
			int amount = (statusTypes.get(status) != null ? statusTypes.get(status) : 0);
			reportsNotification = reportsNotification
					.replace(statusPlaceHolder, (amount <= 1 ? Message.REPORT_TYPE : Message.REPORTS_TYPE).get()
							.replace("_Amount_", Integer.toString(amount))
							.replace("_Type_", status.getWord(null).toLowerCase()));
			totalAmount += amount;
		}
		
		return totalAmount == 0 ? null : reportsNotification;
	}
	
	public static void start() {
		stop();
		int interval = ConfigFile.CONFIG.get().getInt("Config.ReportsNotifications.MinutesInterval", 0)*1200;
		if(interval > 0)
			taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(TigerReports.getInstance(), new ReportsNotifier(), interval, interval);
	}
	
	public static void stop() {
		if(taskId != -1) {
			Bukkit.getScheduler().cancelTask(taskId);
			taskId = -1;
		}
	}

}
