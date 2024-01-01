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
public class ProcessPunishBungeeNotification extends BungeeNotification {

    private static final Logger LOGGER = Logger.BUNGEE.newChild(ProcessPunishBungeeNotification.class);

    public final int reportId;
    public final String staffUniqueId;
    public final boolean archive;
    public final String punishment;

    public ProcessPunishBungeeNotification(long creationTime, int reportId, UUID staffUUID, boolean archive,
            String punishment) {
        this(creationTime, reportId, staffUUID.toString(), archive, punishment);
    }

    public ProcessPunishBungeeNotification(long creationTime, int reportId, String staffUniqueId, boolean archive,
            String punishment) {
        super(creationTime);
        this.reportId = reportId;
        this.staffUniqueId = CheckUtils.notEmpty(staffUniqueId);
        this.archive = archive;
        this.punishment = CheckUtils.notEmpty(punishment);
    }

    @Override
    public boolean isEphemeral() {
        return false;
    }

    @Override
    public void onReceive(Database db, TaskScheduler ts, UsersManager um, ReportsManager rm, VaultManager vm,
            BungeeManager bm) {
        getReportAsync(reportId, db, ts, um, rm, bm, LOGGER, (r) -> {
            um.getUserByUniqueIdAsynchronously(staffUniqueId, db, ts, (u) -> {
                r.processWithPunishment(u, true, archive, punishment, isNotifiable(bm), db, rm, vm, bm, ts);
            });
        });
    }

}
