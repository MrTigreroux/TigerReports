package fr.mrtigreroux.tigerreports.bungee.notifications;

import java.util.UUID;

import fr.mrtigreroux.tigerreports.bungee.BungeeManager;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.managers.ReportsManager;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.managers.VaultManager;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;
import fr.mrtigreroux.tigerreports.utils.CheckUtils;

/**
 * @author MrTigreroux
 */
public class PunishBungeeNotification extends BungeeNotification {
    
    public final String staffUniqueId;
    public final String targetUniqueId;
    public final long seconds;
    
    public PunishBungeeNotification(long creationTime, UUID staffUUID, UUID targetUUID,
            long seconds) {
        this(creationTime, staffUUID.toString(), targetUUID.toString(), seconds);
    }
    
    public PunishBungeeNotification(long creationTime, String staffUniqueId, String targetUniqueId,
            long seconds) {
        super(creationTime);
        this.staffUniqueId = CheckUtils.notEmpty(staffUniqueId);
        this.targetUniqueId = CheckUtils.notEmpty(targetUniqueId);
        this.seconds = seconds;
    }
    
    @Override
    public boolean isEphemeral() {
        return true;
    }
    
    @Override
    public void onReceive(Database db, TaskScheduler ts, UsersManager um, ReportsManager rm,
            VaultManager vm, BungeeManager bm) {
        um.getUserByUniqueIdAsynchronously(targetUniqueId, db, ts, (u) -> {
            if (u != null) {
                if (isNotifiable(bm)) {
                    um.getUserByUniqueIdAsynchronously(staffUniqueId, db, ts, (staff) -> {
                        u.punish(seconds, staff, true, db, bm, vm);
                    });
                } else {
                    um.updateDataOfUserWhenPossible(u.getUniqueId(), db, ts);
                }
            }
        });
    }
    
}
