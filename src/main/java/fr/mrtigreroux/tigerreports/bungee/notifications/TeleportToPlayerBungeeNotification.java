package fr.mrtigreroux.tigerreports.bungee.notifications;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import fr.mrtigreroux.tigerreports.bungee.BungeeManager;
import fr.mrtigreroux.tigerreports.data.config.ConfigSound;
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
public class TeleportToPlayerBungeeNotification extends BungeeNotification {

    private static final Logger LOGGER = Logger.BUNGEE.newChild(TeleportToPlayerBungeeNotification.class);

    public final String playerName;
    public final String targetName;

    public TeleportToPlayerBungeeNotification(long creationTime, String playerName, String targetName) {
        super(creationTime);
        this.playerName = CheckUtils.notEmpty(playerName);
        this.targetName = CheckUtils.notEmpty(targetName);
    }

    @Override
    public boolean isEphemeral() {
        return true;
    }

    @Override
    public void onReceive(Database db, TaskScheduler ts, UsersManager um, ReportsManager rm, VaultManager vm,
            BungeeManager bm) {
        if (!isNotifiable(bm)) {
            LOGGER.info(() -> "onReceive(): " + playerName + " player, too old notification, ignored");
            return;
        }

        String localServerName = bm.getServerName();
        if (BungeeManager.DEFAULT_SERVER_NAME.equals(localServerName)) {
            LOGGER.info(() -> "onReceive(): localServerName = unknown (localhost), ignored");
            return;
        }
        if (Bukkit.getPlayer(targetName) == null) {
            LOGGER.info(() -> "onReceive(): target player is not online, ignored");
            return;
        }

        bm.whenPlayerIsOnline(playerName, (p) -> {
            if (!isNotifiable(bm)) {
                LOGGER.info(() -> "onReceive(): " + playerName + " online player, too old notification, ignored");
                return;
            }

            Player t = Bukkit.getPlayer(targetName);
            if (t == null) {
                LOGGER.info(() -> "onReceive(): " + targetName + " target player is no longer online, ignored");
                return;
            }

            p.teleport(t.getLocation());
            ConfigSound.TELEPORT.play(p);
        });

        bm.sendBungeeMessage("ConnectOther", playerName, localServerName);
    }

}
