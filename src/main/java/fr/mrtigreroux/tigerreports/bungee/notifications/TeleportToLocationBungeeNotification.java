package fr.mrtigreroux.tigerreports.bungee.notifications;

import org.bukkit.Location;

import fr.mrtigreroux.tigerreports.bungee.BungeeManager;
import fr.mrtigreroux.tigerreports.data.config.ConfigSound;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.logs.Logger;
import fr.mrtigreroux.tigerreports.managers.ReportsManager;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.managers.VaultManager;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;
import fr.mrtigreroux.tigerreports.utils.CheckUtils;
import fr.mrtigreroux.tigerreports.utils.SerializationUtils;

/**
 * @author MrTigreroux
 */
public class TeleportToLocationBungeeNotification extends BungeeNotification {

    private static final Logger LOGGER = Logger.BUNGEE.newChild(TeleportToLocationBungeeNotification.class);

    public final String playerName;
    public final String serializedLocation;

    public TeleportToLocationBungeeNotification(long creationTime, String playerName, String serializedLocation) {
        super(creationTime);
        this.playerName = CheckUtils.notEmpty(playerName);
        this.serializedLocation = CheckUtils.notEmpty(serializedLocation);
    }

    @Override
    public boolean isEphemeral() {
        return true;
    }

    public Location getLocation() {
        return SerializationUtils.unserializeLocation(serializedLocation);
    }

    @Override
    public void onReceive(Database db, TaskScheduler ts, UsersManager um, ReportsManager rm, VaultManager vm,
            BungeeManager bm) {
        if (!isNotifiable(bm)) {
            LOGGER.info(() -> "onReceive(): " + playerName + " player, too old notification, ignored");
            return;
        }

        bm.whenPlayerIsOnline(playerName, (p) -> {
            if (!isNotifiable(bm)) {
                LOGGER.info(() -> "onReceive(): " + playerName + " online player, too old notification, ignored");
                return;
            }
            try {
                p.teleport(getLocation());
                ConfigSound.TELEPORT.play(p);
            } catch (NullPointerException ex) {
                throw new IllegalArgumentException("Invalid location " + serializedLocation);
            }
        });
    }

}
