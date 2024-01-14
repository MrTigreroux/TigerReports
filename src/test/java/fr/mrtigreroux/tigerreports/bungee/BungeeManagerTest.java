package fr.mrtigreroux.tigerreports.bungee;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

import java.io.IOException;

import org.bukkit.Bukkit;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import fr.mrtigreroux.tigerreports.TestClass;
import fr.mrtigreroux.tigerreports.bungee.TestsBungeeNetwork.Basic2ServersNetwork;
import fr.mrtigreroux.tigerreports.bungee.TestsBungeeNetwork.FakeServer;
import fr.mrtigreroux.tigerreports.bungee.notifications.BungeeNotification;
import fr.mrtigreroux.tigerreports.bungee.notifications.BungeeNotificationType;
import fr.mrtigreroux.tigerreports.bungee.notifications.NewReportBungeeNotification;
import fr.mrtigreroux.tigerreports.bungee.notifications.PlayerOnlineBungeeNotification;
import fr.mrtigreroux.tigerreports.bungee.notifications.StatusBungeeNotification;
import fr.mrtigreroux.tigerreports.data.Holder;
import fr.mrtigreroux.tigerreports.utils.AssertionUtils;

/**
 * @author MrTigreroux
 */
public class BungeeManagerTest extends TestClass {
    
    @Nested
    class SendAndReceiveBungeeNotification {
        
        @Test
        void test1() {
            testSendAndReceiveBungeeNotification(
                    new NewReportBungeeNotification(100L, "reportServer", false, "reportBasicData")
            );
        }
        
        @Test
        void test2() {
            testSendAndReceiveBungeeNotification(
                    new StatusBungeeNotification(101L, 5, "statusDetails")
            );
        }
        
        void testSendAndReceiveBungeeNotification(BungeeNotification notif) {
            Basic2ServersNetwork network = TestsBungeeNetwork.getBasic2ServersNetwork();
            
            BungeeNotificationType notifType =
                    BungeeNotificationType.getByDataClass(notif.getClass());
            
            Holder<BungeeNotification> receivedNotifH = new Holder<>();
            
            assertTrue(network.server1.getTestsTaskScheduler().runTaskAndWait((tc) -> {
                network.server2.getTestsTaskScheduler().runTask(() -> {
                    try {
                        network.server2.getTestsBm()
                                .expectPluginNotification(
                                        BungeeNotificationType.getByDataClass(
                                                PlayerOnlineBungeeNotification.class
                                        ),
                                        (receivedNotif, normalExec) -> {
                                            // Do normal behavior
                                            try (
                                                    MockedStatic<Bukkit> bukkitMock = mockStatic(
                                                            Bukkit.class,
                                                            Mockito.CALLS_REAL_METHODS
                                                    )
                                            ) {
                                                bukkitMock.when(() -> Bukkit.getPlayer(anyString()))
                                                        .thenReturn(null);
                                                normalExec.run();
                                            }
                                        }
                                );
                        network.server2.getTestsBm()
                                .expectPluginNotification(notifType, (receivedNotif, normalExec) -> {
                                    receivedNotifH.set(receivedNotif);
                                    tc.setDone();
                                });
                    } catch (IllegalArgumentException | IllegalAccessException | IOException e) {
                        throw new RuntimeException(e);
                    }
                    
                    network.server1.getTestsTaskScheduler().runTask(() -> {
                        try {
                            network.server1.getTestsBm()
                                    .expectPluginNotification(
                                            BungeeNotificationType.getByDataClass(
                                                    PlayerOnlineBungeeNotification.class
                                            ),
                                            (receivedNotif, normalExec) -> {
                                                // Do normal behavior
                                                try (
                                                        MockedStatic<Bukkit> bukkitMock =
                                                                mockStatic(
                                                                        Bukkit.class,
                                                                        Mockito.CALLS_REAL_METHODS
                                                                )
                                                ) {
                                                    bukkitMock.when(
                                                            () -> Bukkit.getPlayer(anyString())
                                                    ).thenReturn(null);
                                                    normalExec.run();
                                                }
                                            }
                                    );
                        } catch (
                                IllegalArgumentException | IllegalAccessException | IOException e
                        ) {
                            throw new RuntimeException(e);
                        }
                        
                        network.server1.getTestsBm().executeTaskSendingPluginMessage(() -> {
                            network.server1
                                    .playerConnection(FakeServer.newFakeOnlinePlayer("player1"));
                            network.server2.getTestsTaskScheduler().runTask(() -> {
                                network.server2.getTestsBm().executeTaskSendingPluginMessage(() -> {
                                    network.server2.playerConnection(
                                            FakeServer.newFakeOnlinePlayer("player2")
                                    );
                                    
                                    network.server1.getTestsTaskScheduler().runTask(() -> {
                                        network.server1.getTestsBm()
                                                .executeTaskSendingPluginMessage(() -> {
                                                    network.server1.getTestsBm().bm
                                                            .sendPluginNotificationToAll(notif);
                                                });
                                    });
                                });
                            });
                        });
                    });
                });
            }, network.getFakeCommunicationDelay() * 1000));
            
            AssertionUtils.assertDeepEquals(notif, receivedNotifH.get());
            
            assertTrue(network.cleanTaskSchedulersAfterUse());
        }
        
    }
    
    // TODO test 1 network 2 servers
    // player1 joins server1
    // check server1 bm has player1, server2 bm has no player
    // check server1 bm sees server2 as offline
    // check server2 bm sees server1 as offline
    // player1 joins server2
    // check server1 bm has no player, server2 bm has player1
    // check server1 bm sees server2 as offline
    // check server2 bm sees server1 as offline
    // player2 joins server1
    // check server1 bm has player2 and sees server2 has player1, server2 has player1 and sees server1 has player2
    // check server1 bm sees server2 as online
    // check server2 bm sees server1 as online
    // player3 joins server1
    // check same as before + player3
    // check server1 bm sees server2 as online
    // check server2 bm sees server1 as online
    
    // 2nd test:
    // situation with servers each has 1 player, we wait time to exceed last notif time
    // check that the servers are still considering each other as online
    
}
