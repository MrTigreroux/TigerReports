package fr.mrtigreroux.tigerreports.bungee;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.Messenger;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.bungee.TestsBungeeNetwork.FakeServer;
import fr.mrtigreroux.tigerreports.bungee.notifications.BungeeNotification;
import fr.mrtigreroux.tigerreports.bungee.notifications.BungeeNotificationType;
import fr.mrtigreroux.tigerreports.bungee.notifications.NewReportBungeeNotification;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.data.database.SQLite;
import fr.mrtigreroux.tigerreports.logs.Logger;
import fr.mrtigreroux.tigerreports.managers.ReportsManager;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.managers.VaultManager;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;
import fr.mrtigreroux.tigerreports.utils.UserUtils;

/**
 * @author MrTigreroux
 */
public class TestsBungeeManager {

    private static final Logger CLASS_LOGGER = Logger.fromClass(TestsBungeeManager.class);

    private final Logger instanceLogger;
    public final BungeeManager bm;
    public final UsersManager um;
    public final ReportsManager rm;
    public final Database db;
    public final VaultManager vm;
    public final FakeServer localServer;
    public final TestsBungeeNetwork testsBungeeNetwork;

    private final Player fakePlayerUsedForBungee;
    private final Map<Byte, BungeeNotificationType> notifTypesMockById = new HashMap<>();

    public static TestsBungeeManager newBasicTestsBungeeManager(FakeServer localServer,
            TestsBungeeNetwork testsBungeeNetwork) {
        TigerReports tr = mock(TigerReports.class);
        Server serverMock = mock(Server.class);
        when(tr.getServer()).thenReturn(serverMock);
        Messenger messengerMock = mock(Messenger.class);
        when(serverMock.getMessenger()).thenReturn(messengerMock);

        ReportsManager rm = mock(ReportsManager.class);
        Database db = mock(SQLite.class);
        VaultManager vm = mock(VaultManager.class);
        UsersManager um = mock(UsersManager.class);
        return new TestsBungeeManager(tr, rm, db, vm, um, localServer, testsBungeeNetwork);
    }

    public TestsBungeeManager(TigerReports tr, ReportsManager rm, Database db, VaultManager vm, UsersManager um,
            FakeServer localServer, TestsBungeeNetwork testsBungeeNetwork) {
        bm = mock(BungeeManager.class,
                Mockito.withSettings().useConstructor(tr, rm, db, vm, um).defaultAnswer(Mockito.CALLS_REAL_METHODS));
        this.um = um;
        this.rm = rm;
        this.db = db;
        this.vm = vm;
        this.localServer = Objects.requireNonNull(localServer);
        this.testsBungeeNetwork = Objects.requireNonNull(testsBungeeNetwork);
        if (!testsBungeeNetwork.isPresent(localServer)) {
            throw new IllegalArgumentException(
                    "The local server " + localServer + " is not present in the tests bungee network");
        }
        instanceLogger = CLASS_LOGGER.newChild(localServer.name);
        fakePlayerUsedForBungee = mock(Player.class);

        setupSendPluginMessageMock();
    }

    public static byte[] newFakeGetServerAnswer(String serverName) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("GetServer");
        out.writeUTF(serverName);

        return out.toByteArray();
    }

    public static byte[] newFakeGetServersAnswer(Set<String> serversList) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("GetServers");
        out.writeUTF(String.join(", ", serversList));

        return out.toByteArray();
    }

    public static byte[] newFakePlayerListAnswer(String serverName, List<String> playersName) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("PlayerList");
        out.writeUTF(serverName);
        out.writeUTF(String.join(", ", playersName));

        return out.toByteArray();
    }

    public void expectPluginNotification(BungeeNotificationType notifType,
            BiConsumer<BungeeNotification, Runnable> notifOnReceiveCallback)
            throws IllegalArgumentException, IllegalAccessException, EOFException, IOException {
        BungeeNotificationType notifTypeMock = mock(BungeeNotificationType.class);

        doAnswer((inv) -> {
            DataInputStream inputStream = inv.getArgument(0);
            BungeeNotification notif = notifType.readNotification(inputStream);
            instanceLogger.debug(() -> "Notif received and read: " + notif);

            BungeeNotification notifMock = mock(NewReportBungeeNotification.class);
            when(notifMock.toString()).thenReturn("Mock for " + notif);
            doAnswer((inv2) -> {
                notifOnReceiveCallback.accept(notif, () -> {
                    notif.onReceive(inv2.getArgument(0), inv2.getArgument(1), inv2.getArgument(2), inv2.getArgument(3),
                            inv2.getArgument(4), inv2.getArgument(5));
                });
                // notifOnReceiveCallback.onResultReceived(notif);
                return null;
            }).when(notifMock)
                    .onReceive(any(Database.class), any(TaskScheduler.class), any(UsersManager.class),
                            any(ReportsManager.class), any(VaultManager.class), any(BungeeManager.class));
            doAnswer((inv2) -> {
                return notifType.toString(notif);
            }).when(notifTypeMock).toString(notifMock);
            return notifMock;
        }).when(notifTypeMock).readNotification(any(DataInputStream.class));

        notifTypesMockById.put(notifType.getId(), notifTypeMock);
    }

    private void setupSendPluginMessageMock() {
        doAnswer((invocation) -> {
            String channel = invocation.getArgument(1);
            byte[] request = invocation.getArgument(2);
            ByteArrayDataInput in = ByteStreams.newDataInput(request);

            final byte[] answer;
            String subchannel = in.readUTF();
            switch (subchannel) {
            case "Forward":
                String networkTarget = in.readUTF();

                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF(subchannel);
                out.writeUTF(networkTarget);
                int pluginMessageLength = request.length - out.toByteArray().length;

                byte[] pluginMessage = new byte[pluginMessageLength];
                in.readFully(pluginMessage);

                runAfterFakeCommunicationDelay(
                        () -> fakeSendPluginNotificationToNetworkTarget(networkTarget, pluginMessage));
                answer = null;
                break;
            case "GetServer":
                answer = newFakeGetServerAnswer(localServer.name);
                break;
            case "GetServers":
                answer = newFakeGetServersAnswer(testsBungeeNetwork.getFakeServersName());
                break;
            case "PlayerList":
                String serverName = in.readUTF();
                answer = newFakePlayerListAnswer(serverName,
                        testsBungeeNetwork.getFakeServer(serverName).getPlayersName());
                break;
            default:
                throw new IllegalStateException(
                        "Unexpected fakePlayerUsedForBungee.sendPluginMessage() with subchannel = " + subchannel);
            }

            if (answer != null) {
                runAfterFakeCommunicationDelay(() -> executeTaskSendingPluginMessage(() -> {
                    bm.onPluginMessageReceived(channel, fakePlayerUsedForBungee, answer);
                }));
            }
            return null;
        }).when(fakePlayerUsedForBungee).sendPluginMessage(any(Plugin.class), any(String.class), any(byte[].class));
    }

    private void runAfterFakeCommunicationDelay(Runnable taskAfterDelay) {
        instanceLogger.debug(() -> "fakeCommunicationDelay()");
        localServer.getTestsTaskScheduler()
                .runTaskDelayedly(testsBungeeNetwork.getFakeCommunicationDelay(), taskAfterDelay);
    }

    public void fakeSendPluginNotificationToNetworkTarget(String networkTarget, byte[] message) {
        if ("ALL".equals(networkTarget)) {
            instanceLogger.debug(() -> "fakeSendPluginNotificationToNetworkTarget(ALL)");
            for (TestsBungeeNetwork.FakeServer server : testsBungeeNetwork.getFakeServers()) {
                if (!localServer.name.equals(server.name)) {
                    fakeSendPluginNotificationToServer(server, message);
                }
            }
        } else {
            FakeServer targetServer = testsBungeeNetwork.getFakeServer(networkTarget);
            if (targetServer == null) {
                throw new IllegalArgumentException("The network target " + networkTarget + " is unknown");
            }
            fakeSendPluginNotificationToServer(targetServer, message);
        }
    }

    public void fakeSendPluginNotificationToServer(TestsBungeeNetwork.FakeServer server, byte[] message) {
        server.getTestsTaskScheduler().runTask(() -> {
            instanceLogger.debug(() -> "fakeSendPluginNotificationToServer(): target = " + server.name);
            server.getTestsBm().receivePluginMessage("BungeeCord", message);
        });
    }

    public void executeTaskSendingPluginMessage(Runnable task) {
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class, Mockito.CALLS_REAL_METHODS)) {
            userUtilsMock.when(() -> UserUtils.getRandomPlayer()).then((invocation) -> fakePlayerUsedForBungee);

            task.run();
        }
    }

    public void receivePluginMessage(String channel, byte[] message) {
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class, Mockito.CALLS_REAL_METHODS);
                MockedStatic<BungeeNotificationType> bungeeNotificationTypeMock = mockStatic(
                        BungeeNotificationType.class, Mockito.CALLS_REAL_METHODS)) {
            userUtilsMock.when(() -> UserUtils.getRandomPlayer()).then((inv) -> fakePlayerUsedForBungee);
            bungeeNotificationTypeMock.when(() -> BungeeNotificationType.getById(any(byte.class))).then((inv) -> {
                byte id = inv.getArgument(0);
                BungeeNotificationType notifTypeMock = notifTypesMockById.get(id);
                if (notifTypeMock != null) {
                    return notifTypeMock;
                } else {
                    throw new IllegalStateException("BungeeNotificationType.getById(" + id + ") is not expected");
                }
            });

            bm.onPluginMessageReceived(channel, fakePlayerUsedForBungee, message);
        }
    }

    public static <N extends BungeeNotification> N assertCalledOnceSendPluginNotificationToAll(BungeeManager bm,
            Class<N> notifClass) {
        ArgumentCaptor<BungeeNotification> bungeeNotifCaptor = ArgumentCaptor.forClass(BungeeNotification.class);
        verify(bm, times(1)).sendPluginNotificationToAll(any(notifClass));
        verify(bm, atLeastOnce()).sendPluginNotificationToAll(bungeeNotifCaptor.capture());
        for (BungeeNotification notif : bungeeNotifCaptor.getAllValues()) {
            if (notif != null && notifClass.isInstance(notif)) {
                return notifClass.cast(notif);
            }
        }
        throw new IllegalStateException("sendPluginNotificationToAll was not called with " + notifClass.getName());
    }

    public static <N extends BungeeNotification> void assertNeverCalledSendPluginNotificationToAll(BungeeManager bm,
            Class<N> notifClass) {
        verify(bm, times(0)).sendPluginNotificationToAll(any(notifClass));
    }

}
