package fr.mrtigreroux.tigerreports.runnables;

import java.util.HashMap;

import org.bukkit.Bukkit;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.data.ConfigSound;
import fr.mrtigreroux.tigerreports.data.Message;
import fr.mrtigreroux.tigerreports.data.Status;
import fr.mrtigreroux.tigerreports.objects.Report;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;
import fr.mrtigreroux.tigerreports.utils.ReportUtils;

/**
 * @author MrTigreroux
 */

public class ReportsNotifier implements Runnable {

	private static int taskId = -1;
	
	@Override
	public void run() {
		String reportsNotifications = getReportsNotification();
		if(reportsNotifications != null) MessageUtils.sendStaffMessage(reportsNotifications, ConfigSound.STAFF.get());
	}
	
	public static String getReportsNotification() {
		String reportsNotification = Message.REPORTS_NOTIFICATION.get();
		HashMap<Status, Integer> statusTypes = new HashMap<>();
		int totalAmount = 0;
		for(int reportNumber = 1; reportNumber <= ReportUtils.getTotalReports(); reportNumber++) {
			Status status = new Report(reportNumber).getStatus();
			statusTypes.put(status, (statusTypes.get(status) != null ? statusTypes.get(status) : 0)+1);
		}
		for(Status status : Status.values()) {
			int amount = (statusTypes.get(status) != null ? statusTypes.get(status) : 0);
			String statusPlaceHolder = "_"+status.getConfigWord()+"_";
			if(!reportsNotification.contains(statusPlaceHolder)) break;
			reportsNotification = reportsNotification.replace(statusPlaceHolder, (amount <= 1 ? Message.REPORT_TYPE : Message.REPORTS_TYPE).get().replace("_Number_", amount+"").replace("_Type_", status.getWord(null).toLowerCase()));
			totalAmount += amount;
		}
		if(totalAmount == 0) return null;
		return reportsNotification;
	}

	public static void start() {
		stop();
		int interval = ConfigUtils.getReportsNotificationsInterval();
		if(interval == 0) return;
		taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(TigerReports.getInstance(), new ReportsNotifier(), interval, interval);
	}
	
	public static void stop() {
		if(taskId == -1) return;
		Bukkit.getScheduler().cancelTask(taskId);
		taskId = -1;
	}

}
