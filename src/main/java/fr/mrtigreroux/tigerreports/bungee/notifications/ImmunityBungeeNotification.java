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
public class ImmunityBungeeNotification extends BungeeNotification {

    public final String userUniqueId;
    public final String immunity;

    public ImmunityBungeeNotification(long creationTime, UUID userUUID, String immunity) {
        this(creationTime, userUUID.toString(), immunity);
    }

    public ImmunityBungeeNotification(long creationTime, String userUniqueId, String immunity) {
        super(creationTime);
        this.userUniqueId = CheckUtils.notEmpty(userUniqueId);
        this.immunity = immunity;
    }

    @Override
    public boolean isEphemeral() {
        return true;
    }

    @Override
    public void onReceive(Database db, TaskScheduler ts, UsersManager um, ReportsManager rm, VaultManager vm,
            BungeeManager bm) {
        um.getUserByUniqueIdAsynchronously(userUniqueId, db, ts, (u) -> {
            if (u != null) {
                if (isRecent(bm)) {
                    u.setImmunity(immunity, true, db, bm, um);
                } else {
                    um.updateDataOfUserWhenPossible(u.getUniqueId(), db, ts);
                }
            }
        });
    }

}
