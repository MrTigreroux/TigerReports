package fr.mrtigreroux.tigerreports.bungee.notifications;

import java.util.UUID;

import fr.mrtigreroux.tigerreports.bungee.BungeeManager;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.logs.Logger;
import fr.mrtigreroux.tigerreports.managers.ReportsManager;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.managers.VaultManager;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;

/**
 * @author MrTigreroux
 */
public class UnarchiveBungeeNotification extends BungeeNotification {
    
    private static final Logger LOGGER = Logger.BUNGEE.newChild(UnarchiveBungeeNotification.class);
    
    public final int reportId;
    public final String staffUniqueId;
    
    public UnarchiveBungeeNotification(long creationTime, int reportId, UUID staffUUID) {
        this(creationTime, reportId, staffUUID.toString());
    }
    
    public UnarchiveBungeeNotification(long creationTime, int reportId, String staffUniqueId) {
        super(creationTime);
        this.reportId = reportId;
        this.staffUniqueId = staffUniqueId;
    }
    
    @Override
    public boolean isEphemeral() {
        return true;
    }
    
    @Override
    public void onReceive(Database db, TaskScheduler ts, UsersManager um, ReportsManager rm,
            VaultManager vm, BungeeManager bm) {
        getReportAsync(reportId, db, ts, um, rm, bm, LOGGER, (r) -> {
            getUserAsyncIfNotifiable(staffUniqueId, db, ts, um, bm, (u) -> {
                r.unarchive(u, true, db, rm, vm, bm);
            });
        });
    }
    
}
