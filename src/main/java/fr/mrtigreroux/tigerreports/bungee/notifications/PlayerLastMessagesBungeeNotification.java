package fr.mrtigreroux.tigerreports.bungee.notifications;

import java.util.List;
import java.util.UUID;

import fr.mrtigreroux.tigerreports.bungee.BungeeManager;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.managers.ReportsManager;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.managers.VaultManager;
import fr.mrtigreroux.tigerreports.objects.reports.Report;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;
import fr.mrtigreroux.tigerreports.utils.CheckUtils;

/**
 * @author MrTigreroux
 */
public class PlayerLastMessagesBungeeNotification extends BungeeNotification {

    public final String playerUniqueId;
    public final String serializedLastMessages;

    public PlayerLastMessagesBungeeNotification(long creationTime, UUID playerUUID,
            List<User.SavedMessage> lastMessages) {
        this(creationTime, playerUUID.toString(), Report.AdvancedData.serializeMessages(lastMessages));
    }

    public PlayerLastMessagesBungeeNotification(long creationTime, String playerUniqueId,
            String serializedLastMessages) {
        super(creationTime);
        this.playerUniqueId = CheckUtils.notEmpty(playerUniqueId);
        this.serializedLastMessages = CheckUtils.notEmpty(serializedLastMessages);
    }

    @Override
    public boolean isEphemeral() {
        return true;
    }

    @Override
    public void onReceive(Database db, TaskScheduler ts, UsersManager um, ReportsManager rm, VaultManager vm,
            BungeeManager bm) {
        User onlineUser = um.getOnlineUser(playerUniqueId);
        if (onlineUser != null) {
            onlineUser.updateLastMessages(Report.AdvancedData.unserializeMessages(serializedLastMessages));
        }
    }

}
