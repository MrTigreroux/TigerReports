package fr.mrtigreroux.tigerreports.bungee.notifications;

import java.util.Random;

import fr.mrtigreroux.tigerreports.bungee.notifications.BungeeNotificationType.NotificationDataField;
import fr.mrtigreroux.tigerreports.utils.RandomUtils;

/**
 * @author MrTigreroux
 */
public class TestsBungeeNotificationType {
    
    public static BungeeNotification newRandomNotification(Random random,
            BungeeNotificationType notifType) {
        BungeeNotification notif = notifType.newEmptyNotification();
        BungeeNotificationType.setNotificationCreationTime(
                RandomUtils.getRandomLong(random, 0L, Long.MAX_VALUE),
                notif
        );
        for (NotificationDataField dataField : notifType.getDataFields()) {
            dataField.setValue(notif, RandomUtils.getRandomFieldValue(random, dataField.field));
        }
        return notif;
    }
    
}
