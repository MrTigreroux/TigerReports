package fr.mrtigreroux.tigerreports.bungee.notifications;

import fr.mrtigreroux.tigerreports.bungee.BungeeManager;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.logs.Level;
import fr.mrtigreroux.tigerreports.logs.Logger;
import fr.mrtigreroux.tigerreports.managers.ReportsManager;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.managers.VaultManager;
import fr.mrtigreroux.tigerreports.objects.reports.Report;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.tasks.ResultCallback;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;

/**
 * @author MrTigreroux
 */
public abstract class BungeeNotification {

    public static final int RECENT_MAX_DELAY = 10 * 1000;
    public static final int NOTIFY_MAX_DELAY = 60 * 1000;

    /**
     * NB: The notification creation time can be really different of its sending time to a certain server.
     */
    public final long creationTime;

    protected BungeeNotification(long creationTime) {
        this.creationTime = creationTime;
    }

    public abstract boolean isEphemeral();

    public abstract void onReceive(Database db, TaskScheduler ts, UsersManager um, ReportsManager rm, VaultManager vm,
            BungeeManager bm);

    public long getElapsedTime(BungeeManager bm) {
        return bm.getNetworkCurrentTime() - creationTime;
    }

    public boolean isRecent(BungeeManager bm) {
        return getElapsedTime(bm) < RECENT_MAX_DELAY;
    }

    public boolean isNotifiable(BungeeManager bm) {
        return getElapsedTime(bm) < NOTIFY_MAX_DELAY;
    }

    protected void getReportAsync(int reportId, Database db, TaskScheduler ts, UsersManager um, ReportsManager rm,
            BungeeManager bm, Logger logger, ResultCallback<Report> resultCallback) {
        rm.getReportByIdAsynchronously(reportId, false, isNotifiable(bm), db, ts, um, (r) -> {
            if (r == null) {
                logger.log(isNotifiable(bm) ? Level.WARN : Level.INFO, () -> "Report #" + reportId
                        + " is null (has probably been deleted since), ignoring notification");
                return;
            }
            resultCallback.onResultReceived(r);
        });
    }

    protected void getUserAsyncIfNotifiable(String userUniqueId, Database db, TaskScheduler ts, UsersManager um,
            BungeeManager bm, ResultCallback<User> resultCallback) {
        if (isNotifiable(bm)) {
            um.getUserByUniqueIdAsynchronously(userUniqueId, db, ts, resultCallback);
        } else {
            resultCallback.onResultReceived(null);
        }
    }

}
