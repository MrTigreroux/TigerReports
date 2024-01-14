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
public class CooldownBungeeNotification extends BungeeNotification {
    
    public final String userUniqueId;
    public final String cooldown;
    
    public CooldownBungeeNotification(long creationTime, UUID userUUID, String cooldown) {
        this(creationTime, userUUID.toString(), cooldown);
    }
    
    public CooldownBungeeNotification(long creationTime, String userUniqueId, String cooldown) {
        super(creationTime);
        this.userUniqueId = CheckUtils.notEmpty(userUniqueId);
        this.cooldown = cooldown;
    }
    
    @Override
    public boolean isEphemeral() {
        return true;
    }
    
    @Override
    public void onReceive(Database db, TaskScheduler ts, UsersManager um, ReportsManager rm,
            VaultManager vm, BungeeManager bm) {
        um.getUserByUniqueIdAsynchronously(userUniqueId, db, ts, (u) -> {
            if (u != null) {
                if (isRecent(bm)) {
                    u.setCooldown(cooldown, true, db, bm);
                } else {
                    um.updateDataOfUserWhenPossible(u.getUniqueId(), db, ts);
                }
            }
        });
    }
    
}
