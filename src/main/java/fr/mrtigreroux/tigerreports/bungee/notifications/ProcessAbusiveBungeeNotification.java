package fr.mrtigreroux.tigerreports.bungee.notifications;

import java.util.UUID;

import fr.mrtigreroux.tigerreports.bungee.BungeeManager;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.logs.Logger;
import fr.mrtigreroux.tigerreports.managers.ReportsManager;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.managers.VaultManager;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;
import fr.mrtigreroux.tigerreports.utils.CheckUtils;

/**
 * @author MrTigreroux
 */
public class ProcessAbusiveBungeeNotification extends BungeeNotification {
    
    private static final Logger LOGGER =
            Logger.BUNGEE.newChild(ProcessAbusiveBungeeNotification.class);
    
    public final int reportId;
    public final String staffUniqueId;
    public final boolean archive;
    public final long punishSeconds;
    
    public ProcessAbusiveBungeeNotification(long creationTime, int reportId, UUID staffUUID,
            boolean archive, long punishSeconds) {
        this(creationTime, reportId, staffUUID.toString(), archive, punishSeconds);
    }
    
    public ProcessAbusiveBungeeNotification(long creationTime, int reportId, String staffUniqueId,
            boolean archive, long punishSeconds) {
        super(creationTime);
        this.reportId = reportId;
        this.staffUniqueId = CheckUtils.notEmpty(staffUniqueId);
        this.archive = archive;
        this.punishSeconds = CheckUtils.strictlyPositive(punishSeconds);
    }
    
    @Override
    public boolean isEphemeral() {
        return false;
    }
    
    @Override
    public void onReceive(Database db, TaskScheduler ts, UsersManager um, ReportsManager rm,
            VaultManager vm, BungeeManager bm) {
        getReportAsync(reportId, db, ts, um, rm, bm, LOGGER, (r) -> {
            um.getUserByUniqueIdAsynchronously(staffUniqueId, db, ts, (u) -> {
                r.processAbusive(
                        u,
                        true,
                        archive,
                        punishSeconds,
                        isNotifiable(bm),
                        db,
                        rm,
                        um,
                        bm,
                        vm,
                        ts
                );
            });
        });
    }
    
}
