package fr.mrtigreroux.tigerreports.bungee.notifications;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Sets;

import fr.mrtigreroux.tigerreports.TestClass;
import fr.mrtigreroux.tigerreports.utils.AssertionUtils;
import fr.mrtigreroux.tigerreports.utils.TestsFileUtils;

/**
 * @author MrTigreroux
 */
public class BungeeNotificationTypeTest extends TestClass {

    private static final String JAVA_CLASS_FILE_SUFFIX = ".java";
    private static final Set<String> NOT_BUNGEE_NOTIFICATION_FILES_NAME = Sets.newSet(
            BungeeNotification.class.getSimpleName() + JAVA_CLASS_FILE_SUFFIX,
            BungeeNotificationType.class.getSimpleName() + JAVA_CLASS_FILE_SUFFIX);

    @Test
    void testIds() {
        byte id = 0;
        boolean idExists = true;

        BungeeNotificationType notifType;
        do {
            try {
                notifType = BungeeNotificationType.getById(id);
                assertEquals(id, notifType.getId());
                id++;
            } catch (IllegalArgumentException ex) {
                idExists = false;
            }
        } while (idExists);

        assertEquals(id, BungeeNotificationType.getNotificationTypeByIdArray().length);
    }

    @Test
    void testDataClasses() {
        Set<Class<? extends BungeeNotification>> dataClasses = new HashSet<>();
        for (BungeeNotificationType type : BungeeNotificationType.getNotificationTypeByIdArray()) {
            BungeeNotificationType byDataClass = BungeeNotificationType.getByDataClass(type.getDataClass());
            assertTrue(type == byDataClass);
            assertTrue(dataClasses.add(type.getDataClass()),
                    () -> type.getDataClass() + " is defined in several BungeeNotificationTypes");
        }
        assertEquals(BungeeNotificationType.getNotificationTypeByIdArray().length, dataClasses.size());
    }

    @Test
    void testAllNotificationTypesDefined() throws ClassNotFoundException, IOException {
        Path notifsPackagePath = TestsFileUtils.getProjectDirectoryPath(Paths.get("src", "main", "java",
                BungeeNotification.class.getPackage().getName().replace(".", File.separator)));
        Set<String> packageNotifsClassName = Files.list(notifsPackagePath).filter((path) -> {
            File file = path.toFile();
            String fileName = file.getName();
            return file.isFile() && fileName.endsWith(JAVA_CLASS_FILE_SUFFIX)
                    && !NOT_BUNGEE_NOTIFICATION_FILES_NAME.contains(fileName);
        }).map((path) -> {
            String fileName = path.toFile().getName();
            return fileName.substring(0, fileName.length() - JAVA_CLASS_FILE_SUFFIX.length());
        }).collect(Collectors.toSet());

        Set<String> definedNotifsClassName = Arrays.stream(BungeeNotificationType.getNotificationTypeByIdArray())
                .map((notifType) -> notifType.getDataClass().getSimpleName())
                .collect(Collectors.toSet());

        AssertionUtils.assertSetEquals(packageNotifsClassName, definedNotifsClassName, "defined notifs data class");
    }

    @Test
    void testRightDataClass() {
        StatusBungeeNotification statusBungeeNotification = new StatusBungeeNotification(101L, 1, "statusDetails");
        assertTrue(BungeeNotificationType.getByDataClass(statusBungeeNotification.getClass()) == BungeeNotificationType
                .getByDataClass(StatusBungeeNotification.class));

        NewReportBungeeNotification newReportBungeeNotification = new NewReportBungeeNotification(101L, "reportServer",
                true, "reportBasicData");
        assertTrue(
                BungeeNotificationType.getByDataClass(newReportBungeeNotification.getClass()) == BungeeNotificationType
                        .getByDataClass(NewReportBungeeNotification.class));
    }

    @Nested
    class WriteThenReadNotification {

        @Test
        void testNormalNotification() throws IllegalArgumentException, IllegalAccessException, IOException {
            BungeeNotification notif = new StatusBungeeNotification(101L, 61, "statusDetails");
            BungeeNotificationType notifType = BungeeNotificationType.getByDataClass(notif.getClass());
            testWriteThenReadNotification(notifType, notif);
        }

        @Test
        void testAllFieldTypesStaticValues() throws IllegalArgumentException, IllegalAccessException, IOException {
            BungeeNotificationType notifType = new BungeeNotificationType(Byte.MAX_VALUE,
                    TestsBungeeNotification.class);
            testWriteThenReadNotification(notifType,
                    new TestsBungeeNotification(101L, false, (byte) 0, (short) 0, 0, 0f, 0d, 0L, null, null));
            testWriteThenReadNotification(notifType,
                    new TestsBungeeNotification(101L, false, (byte) 0, (short) 0, 0, 0f, 0d, 0L, "", new String[0]));
            testWriteThenReadNotification(notifType,
                    new TestsBungeeNotification(101L, true, (byte) 1, (short) 1, 1, 1f, 1d, 1L, "1", new String[1]));
            testWriteThenReadNotification(notifType, new TestsBungeeNotification(101L, false, (byte) -1, (short) -1, -1,
                    -1f, -1d, -1L, "-1", new String[] { "abc" }));
            testWriteThenReadNotification(notifType, new TestsBungeeNotification(101L, true, (byte) 3, (short) 3, 3,
                    1.1f, 1.1d, 3L, "123", new String[] { "abc", "def" }));
        }

        @Test
        void testAllFieldTypesRandomValues() throws IllegalArgumentException, IllegalAccessException, IOException {
            BungeeNotificationType notifType = new BungeeNotificationType(Byte.MAX_VALUE,
                    TestsBungeeNotification.class);
            Random random = new Random();
            BungeeNotification notif = TestsBungeeNotificationType.newRandomNotification(random, notifType);
            testWriteThenReadNotification(notifType, notif);
        }

        @Test
        void testAllNotificationTypesRandomValues()
                throws IllegalArgumentException, IllegalAccessException, IOException {
            Random random = new Random();
            for (BungeeNotificationType notifType : BungeeNotificationType.getNotificationTypeByIdArray()) {
                BungeeNotification notif = TestsBungeeNotificationType.newRandomNotification(random, notifType);
                testWriteThenReadNotification(notifType, notif);
            }
        }

        void testWriteThenReadNotification(BungeeNotificationType notifType, BungeeNotification notif)
                throws IllegalArgumentException, IllegalAccessException, IOException {
            LOGGER.debug(() -> "Notif to write: " + notif);

            ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
            DataOutputStream dataOut = new DataOutputStream(bytesOut);
            notifType.writeNotification(dataOut, notif);

            DataInputStream dataIn = new DataInputStream(new ByteArrayInputStream(bytesOut.toByteArray()));
            BungeeNotification readNotificationData = notifType.readNotification(dataIn);
            LOGGER.debug(() -> "Notif read: " + readNotificationData);

            AssertionUtils.assertDeepEquals(notif, readNotificationData);
        }

    }

}
