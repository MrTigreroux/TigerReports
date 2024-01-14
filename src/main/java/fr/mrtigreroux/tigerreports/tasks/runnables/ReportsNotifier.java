package fr.mrtigreroux.tigerreports.tasks.runnables;

import java.util.Arrays;

import org.bukkit.entity.Player;

import fr.mrtigreroux.tigerreports.data.config.ConfigFile;
import fr.mrtigreroux.tigerreports.data.config.ConfigSound;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.Status;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;

/**
 * @author MrTigreroux
 */

public class ReportsNotifier implements Runnable {
    
    private static int taskId = -1;
    private final Database db;
    private final TaskScheduler taskScheduler;
    
    public ReportsNotifier(Database db, TaskScheduler taskScheduler) {
        super();
        this.db = db;
        this.taskScheduler = taskScheduler;
    }
    
    @Override
    public void run() {
        sendReportsNotification(null, db, taskScheduler);
    }
    
    public static void sendReportsNotification(Player target, Database db,
            TaskScheduler taskScheduler) {
        taskScheduler.runTaskAsynchronously(new Runnable() {
            
            @Override
            public void run() {
                String reportsNotification = Message.REPORTS_NOTIFICATION.get();
                int totalAmount = 0;
                
                for (Status status : Status.values()) {
                    String statusPlaceHolder = "_" + status.getConfigName() + "_";
                    if (!reportsNotification.contains(statusPlaceHolder)) {
                        break;
                    }
                    Object amountObj = db.query(
                            "SELECT COUNT(DISTINCT report_id) AS amount FROM tigerreports_reports WHERE archived = ? AND status LIKE ?",
                            Arrays.asList(0, status.getConfigName() + "%")
                    ).getResult(0, "amount");
                    Integer amount;
                    if (amountObj instanceof Long) {
                        amount = Math.toIntExact((Long) amountObj);
                    } else {
                        amount = (Integer) amountObj;
                    }
                    // TODO: See to use a batch to run the queries
                    if (amount == null || amount < 0) {
                        amount = 0;
                    }
                    reportsNotification = reportsNotification.replace(
                            statusPlaceHolder,
                            (amount <= 1 ? Message.REPORT_TYPE : Message.REPORTS_TYPE).get()
                                    .replace("_Amount_", Long.toString(amount))
                                    .replace("_Type_", status.getDisplayName(null).toLowerCase())
                    );
                    totalAmount += amount;
                }
                
                if (totalAmount > 0) {
                    final String reportsNotif = reportsNotification;
                    taskScheduler.runTask(new Runnable() {
                        
                        @Override
                        public void run() {
                            if (target != null) {
                                target.sendMessage(reportsNotif);
                            } else {
                                MessageUtils
                                        .sendStaffMessage(reportsNotif, ConfigSound.STAFF.get());
                            }
                        }
                        
                    });
                }
            }
            
        });
    }
    
    public static void startIfNeeded(Database db, TaskScheduler taskScheduler) {
        if (taskId != -1) {
            return; // Already started.
        }
        
        long interval = ConfigFile.CONFIG.get()
                .getInt("Config.Notifications.Staff.MinutesInterval", 0) * 60 * 1000L;
        if (interval > 0) {
            taskId = taskScheduler
                    .runTaskRepeatedly(interval, interval, new ReportsNotifier(db, taskScheduler));
        }
    }
    
    public static void stop(TaskScheduler taskScheduler) {
        if (taskId != -1) {
            taskScheduler.cancelTask(taskId);
            taskId = -1;
        }
    }
    
}
