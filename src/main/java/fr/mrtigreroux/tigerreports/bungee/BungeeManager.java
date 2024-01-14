package fr.mrtigreroux.tigerreports.bungee;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.PluginMessageListener;

import com.google.common.base.Objects;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.bungee.notifications.BungeeNotification;
import fr.mrtigreroux.tigerreports.bungee.notifications.BungeeNotificationType;
import fr.mrtigreroux.tigerreports.bungee.notifications.PlayerOnlineBungeeNotification;
import fr.mrtigreroux.tigerreports.bungee.notifications.TeleportToLocationBungeeNotification;
import fr.mrtigreroux.tigerreports.bungee.notifications.TeleportToPlayerBungeeNotification;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.logs.Logger;
import fr.mrtigreroux.tigerreports.managers.ReportsManager;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.managers.VaultManager;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.tasks.ResultCallback;
import fr.mrtigreroux.tigerreports.utils.CheckUtils;
import fr.mrtigreroux.tigerreports.utils.CollectionUtils;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.UserUtils;

/**
 * @author MrTigreroux
 */

public class BungeeManager implements PluginMessageListener {
    
    private static final Logger LOGGER = Logger.BUNGEE.newChild(BungeeManager.class);
    
    public static final String DEFAULT_SERVER_NAME = "localhost";
    public static final long MAX_COMMUNICATION_SESSION_SETUP_TIME = 2 * 60 * 1000;
    public static final long MAX_ONLINE_SERVER_LAST_NOTIFICATION_TIME = 10 * 60 * 1000;
    public static final long MAX_PLAYERS_LIST_COLLECTION_PER_SERVER_RECENT_REQUEST_TIME = 60 * 1000;
    public static final long MAX_PLAYERS_LIST_COLLECTION_PER_SERVER_RECENT_RECEPTION_TIME =
            MAX_ONLINE_SERVER_LAST_NOTIFICATION_TIME;
    public static final byte MAX_UNSENT_NOTIFICATIONS_KEPT_WHEN_SERVERS_UNKNOWN = 20;
    public static final byte MAX_OFFLINE_SERVER_KEPT_UNSENT_NON_EPHEMERAL_NOTIFICATIONS = 50;
    public static final byte MAX_OFFLINE_SERVER_KEPT_UNSENT_EPHEMERAL_NOTIFICATIONS = 20;
    public static final byte MAX_ONLINE_CALLBACKS_BY_PLAYER = 3;
    
    private final TigerReports tr;
    private final ReportsManager rm;
    private final Database db;
    private final VaultManager vm;
    private final UsersManager um;
    private boolean initialized = false;
    private boolean isCommunicationSessionSetUp = false;
    private Long communicationSessionLastSetUpStartTime;
    private List<ResultCallback<String>> localServerNameResultCallbacks = new ArrayList<>();
    private String localServerName = null;
    
    private static class KnownServer {
        
        private final String name;
        private Long lastNotificationTime = null;
        private Long lastPlayersListRequestTime = null;
        private Long lastPlayersListReceptionTime = null;
        private List<BungeeNotification> unsentNonEphemeralNotifications =
                new CollectionUtils.LimitedOrderedList<>(
                        MAX_OFFLINE_SERVER_KEPT_UNSENT_NON_EPHEMERAL_NOTIFICATIONS
                );
        private List<BungeeNotification> unsentEphemeralNotifications =
                new CollectionUtils.LimitedOrderedList<>(
                        MAX_OFFLINE_SERVER_KEPT_UNSENT_EPHEMERAL_NOTIFICATIONS
                );
        
        public KnownServer(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
        
        public boolean isOnline() {
            return lastNotificationTime != null
                    && System.currentTimeMillis()
                            - lastNotificationTime <= MAX_ONLINE_SERVER_LAST_NOTIFICATION_TIME;
        }
        
        public void setLastNotificationTime(long timeInMillis) {
            if (lastNotificationTime == null || lastNotificationTime < timeInMillis) {
                lastNotificationTime = timeInMillis;
            }
        }
        
        public void setOffline() {
            lastNotificationTime = null;
            LOGGER.info(
                    () -> name + " server: setOffline(): unsent ephemeral notifications cleared: "
                            + CollectionUtils.toString(unsentEphemeralNotifications)
            );
            unsentEphemeralNotifications.clear();
        }
        
        public void addUnsentNotification(boolean ephemeralNotification,
                BungeeNotification notification) {
            if (ephemeralNotification) {
                unsentEphemeralNotifications.add(notification);
                LOGGER.debug(
                        () -> name
                                + " server: addUnsentNotification(): now unsentEphemeralNotifications = "
                                + CollectionUtils.toString(unsentEphemeralNotifications)
                );
            } else {
                unsentNonEphemeralNotifications.add(notification);
                LOGGER.debug(
                        () -> name
                                + " server: addUnsentNotification(): now unsentNonEphemeralNotifications = "
                                + CollectionUtils.toString(unsentNonEphemeralNotifications)
                );
            }
        }
        
        public List<BungeeNotification> getAndClearUnsentNotifications(
                boolean ephemeralNotifications) {
            return getAndClearUnsentNotifications(
                    ephemeralNotifications
                            ? unsentEphemeralNotifications
                            : unsentNonEphemeralNotifications
            );
        }
        
        private List<BungeeNotification> getAndClearUnsentNotifications(
                List<BungeeNotification> unsentNotifications) {
            if (!unsentNotifications.isEmpty()) {
                List<BungeeNotification> notifs = new ArrayList<>(unsentNotifications);
                unsentNotifications.clear();
                LOGGER.debug(
                        () -> name
                                + " server: getAndClearUnsentNotifications(): unsent notifications cleared (ephemeral size = "
                                + unsentEphemeralNotifications.size() + ", non ephemeral size = "
                                + unsentNonEphemeralNotifications.size() + ")"
                );
                return notifs;
            } else {
                return null;
            }
        }
        
        public void setLastPlayersListRequestTimeAsNow() {
            lastPlayersListRequestTime = System.currentTimeMillis();
        }
        
        public boolean isLastPlayersListRequestRecent() {
            return lastPlayersListRequestTime != null
                    && System.currentTimeMillis()
                            - lastPlayersListRequestTime <= MAX_PLAYERS_LIST_COLLECTION_PER_SERVER_RECENT_REQUEST_TIME;
        }
        
        public void setLastPlayersListReceptionTimeAsNow() {
            lastPlayersListReceptionTime = System.currentTimeMillis();
        }
        
        public boolean isLastPlayersListReceptionRecent() {
            return lastPlayersListReceptionTime != null
                    && System.currentTimeMillis()
                            - lastPlayersListReceptionTime <= MAX_PLAYERS_LIST_COLLECTION_PER_SERVER_RECENT_RECEPTION_TIME;
        }
        
        @Override
        public String toString() {
            return getName();
        }
        
    }
    
    private Map<String, KnownServer> knownServers = new HashMap<>();
    /**
     * player name -> server name (null if unknown server name but the player is online)
     */
    private Map<String, String> onlinePlayersServer = new HashMap<>();
    
    private List<BungeeNotification> unsentNotificationsWhenServersUnknown =
            new CollectionUtils.LimitedOrderedList<>(
                    MAX_UNSENT_NOTIFICATIONS_KEPT_WHEN_SERVERS_UNKNOWN
            );
    private String playerOfflineNotBroadcastedName = null;
    private UUID playerOfflineNotBroadcastedUUID = null;
    
    private Map<String, CollectionUtils.LimitedOrderedList<ResultCallback<Player>>> onlineCallbacksByPlayerName =
            new HashMap<>();
    
    public BungeeManager(TigerReports tr, ReportsManager rm, Database db, VaultManager vm,
            UsersManager um) {
        this.tr = tr;
        this.rm = rm;
        this.db = db;
        this.vm = vm;
        this.um = um;
        initialize();
    }
    
    private void initialize() {
        if (ConfigUtils.isEnabled("BungeeCord.Enabled")) {
            Messenger messenger = tr.getServer().getMessenger();
            messenger.registerOutgoingPluginChannel(tr, "BungeeCord");
            messenger.registerIncomingPluginChannel(tr, "BungeeCord", this);
            initialized = true;
            Logger.CONFIG.info(
                    () -> ConfigUtils.getInfoMessage(
                            "The plugin is using BungeeCord.",
                            "Le plugin utilise BungeeCord."
                    )
            );
        } else {
            Logger.CONFIG.info(
                    () -> ConfigUtils.getInfoMessage(
                            "The plugin is not using BungeeCord.",
                            "Le plugin n'utilise pas BungeeCord."
                    )
            );
        }
    }
    
    public void startSetupCommunicationSession(boolean forced) {
        if (forced || (!isCommunicationSessionSetUp && !isCommunicationSessionPendingSetup())) {
            LOGGER.info(() -> "startSetupCommunicationSession(): start setup...");
            isCommunicationSessionSetUp = false;
            if (collectServerName() && sendBungeeMessage("GetServers")) {
                communicationSessionLastSetUpStartTime = System.currentTimeMillis();
            }
        } else {
            LOGGER.info(() -> "startSetupCommunicationSession(): ignored");
        }
    }
    
    private boolean isCommunicationSessionPendingSetup() {
        return communicationSessionLastSetUpStartTime != null
                && System.currentTimeMillis()
                        - communicationSessionLastSetUpStartTime < MAX_COMMUNICATION_SESSION_SETUP_TIME;
    }
    
    // TODO check all servers use the same TR version: compare with the version on the db
    private void finishSetupCommunicationSession() {
        if (isCommunicationSessionPendingSetup()) {
            communicationSessionLastSetUpStartTime = null;
            for (KnownServer server : getKnownServers()) {
                collectServerPlayers(server);
            }
            
            LOGGER.info(
                    () -> "finishSetupCommunicationSession(): attempt to send all unsent notifications (to all) when servers were unknown..."
            );
            for (BungeeNotification notif : unsentNotificationsWhenServersUnknown) {
                sendPluginNotificationToAll(notif);
            }
            unsentNotificationsWhenServersUnknown.clear();
            
            isCommunicationSessionSetUp = true;
            LOGGER.info(() -> "finishSetupCommunicationSession(): session is now ready");
        } else {
            LOGGER.info(
                    () -> "finishSetupCommunicationSession(): session is not pending setup, ignored"
            );
        }
    }
    
    private void collectServerPlayers(KnownServer server) {
        if (
            !server.isLastPlayersListReceptionRecent() && !server.isLastPlayersListRequestRecent()
        ) {
            LOGGER.debug(
                    () -> "collectServerPlayers(): collect players of " + server
                            + " server from Bungee server"
            );
            server.setLastPlayersListRequestTimeAsNow();
            sendBungeeMessage("PlayerList", server.getName());
        } else {
            LOGGER.info(
                    () -> "collectServerPlayers(): " + server
                            + " server players list already requested or collected recently, cancel collection"
            );
        }
    }
    
    private boolean collectServerName() {
        if (localServerName == null) {
            return sendBungeeMessage("GetServer");
        } else {
            return true;
        }
    }
    
    public void whenPlayerIsOnline(String playerName, ResultCallback<Player> onlineCallback) {
        Player p = Bukkit.getPlayer(playerName);
        if (p != null) {
            onlineCallback.onResultReceived(p);
        } else {
            addPlayerOnlineCallback(playerName, onlineCallback);
        }
    }
    
    private void addPlayerOnlineCallback(String playerName, ResultCallback<Player> onlineCallback) {
        CollectionUtils.LimitedOrderedList<ResultCallback<Player>> playerOnlineCallbacks =
                onlineCallbacksByPlayerName.get(playerName);
        if (playerOnlineCallbacks == null) {
            playerOnlineCallbacks =
                    new CollectionUtils.LimitedOrderedList<>(MAX_ONLINE_CALLBACKS_BY_PLAYER);
            onlineCallbacksByPlayerName.put(playerName, playerOnlineCallbacks);
        }
        playerOnlineCallbacks.add(onlineCallback);
    }
    
    public void processPlayerConnection(Player p) {
        if (!initialized) {
            LOGGER.info(() -> "processPlayerConnection(" + p + "): bm unitialized");
            return;
        }
        if (p == null || !p.isOnline()) {
            LOGGER.info(() -> "processPlayerConnection(" + p + "): player is null or offline");
            return;
        }
        
        if (Bukkit.getOnlinePlayers().size() == 1) { // the current server hasn't received BungeeCord notifications for a certain time because there was no online player
            isCommunicationSessionSetUp = false;
            LOGGER.info(
                    () -> "processPlayerConnection(): isCommunicationSessionSetUp set to false because no player before"
            );
        }
        startSetupCommunicationSession(false);
        
        if (playerOfflineNotBroadcastedUUID != null && playerOfflineNotBroadcastedName != null) {
            if (playerOfflineNotBroadcastedUUID != p.getUniqueId()) {
                String curPlayerOfflineNotBroadcastedName = playerOfflineNotBroadcastedName;
                UUID curPlayerOfflineNotBroadcastedUUID = playerOfflineNotBroadcastedUUID;
                getServerName((localServerName) -> {
                    sendPluginNotificationToAll(
                            new PlayerOnlineBungeeNotification(
                                    getNetworkCurrentTime(),
                                    curPlayerOfflineNotBroadcastedName,
                                    curPlayerOfflineNotBroadcastedUUID,
                                    false,
                                    localServerName,
                                    null
                            )
                    );
                });
            } // else the player is now online, playerOfflineNotBroadcasted must be ignored
            playerOfflineNotBroadcastedUUID = null;
            playerOfflineNotBroadcastedName = null;
        }
        
        User onlineUser = um.getOnlineUser(p);
        if (onlineUser != null) {
            updatePlayerOnlineInNetwork(
                    p.getName(),
                    true,
                    p.getUniqueId(),
                    onlineUser.getLastMessagesMinDatetimeOfInsertableMessages()
            );
            processPlayerOnlineCallbacks(p);
        } // else the player just disconnected
    }
    
    private void processPlayerOnlineCallbacks(Player p) {
        String playerName = p.getName();
        CollectionUtils.LimitedOrderedList<ResultCallback<Player>> playerOnlineCallbacks =
                onlineCallbacksByPlayerName.get(playerName);
        if (playerOnlineCallbacks == null) {
            LOGGER.debug(() -> "processPlayerOnlineCallbacks(" + playerName + "): no callback");
            return;
        }
        LOGGER.debug(
                () -> "processPlayerOnlineCallbacks(" + playerName + "): callbacks = "
                        + CollectionUtils.toString(playerOnlineCallbacks)
        );
        
        onlineCallbacksByPlayerName.remove(playerName);
        
        for (ResultCallback<Player> callback : playerOnlineCallbacks) {
            callback.onResultReceived(p);
        }
        
        playerOnlineCallbacks.clear();
    }
    
    public void processPlayerDisconnection(String name, UUID uuid) {
        if (!initialized) {
            return;
        }
        
        updatePlayerOnlineInNetwork(name, false, uuid, null);
    }
    
    /**
     * Gets the server name if known, or {@link #DEFAULT_SERVER_NAME}.
     * 
     * @return
     */
    public String getServerName() {
        if (localServerName == null) {
            collectServerName();
        }
        return localServerName != null ? localServerName : DEFAULT_SERVER_NAME;
    }
    
    /**
     * Gets the server name from the BungeeCord network.
     * 
     * @param resultCallback
     */
    public void getServerName(ResultCallback<String> resultCallback) {
        if (localServerName != null) {
            resultCallback.onResultReceived(localServerName);
        } else {
            localServerNameResultCallbacks.add(resultCallback);
            collectServerName();
        }
    }
    
    public void sendPluginNotificationToAll(BungeeNotification notif) {
        Collection<KnownServer> knownServers = getKnownServers();
        if (knownServers.isEmpty()) {
            unsentNotificationsWhenServersUnknown.add(notif);
            LOGGER.info(
                    () -> "sendPluginNotificationToAll(): servers are unknown, saved notification "
                            + notif + " to try to send it later when servers will be collected"
            );
        } else {
            LOGGER.info(
                    () -> "sendPluginNotificationToAll(): attempt to send " + notif
                            + " to all known servers..."
            );
            for (KnownServer server : knownServers) {
                sendPluginNotificationToServer(server, notif);
            }
        }
    }
    
    public void sendPluginNotificationToServer(String serverName, BungeeNotification notif) {
        KnownServer server = getKnownServer(serverName);
        if (server == null) {
            LOGGER.warn(
                    () -> "sendPluginNotificationToServer(): " + serverName + " server is unknown"
            );
            return;
        }
        
        sendPluginNotificationToServer(server, notif);
    }
    
    public void sendPluginNotificationToServer(KnownServer server, BungeeNotification notif) {
        if (server == null) {
            throw new IllegalArgumentException("Server is null");
        }
        if (notif == null) {
            throw new IllegalArgumentException("Notification is null");
        }
        
        if (!server.isOnline()) { // no notification for too long time
            if (!notif.isEphemeral()) { // non ephemeral notification, saved in all cases
                LOGGER.debug(
                        () -> "sendPluginNotificationToServer(): " + server
                                + " server is seen as offline, maybe online, save non ephemeral notification "
                                + notif + " to send it later"
                );
                server.addUnsentNotification(false, notif);
            } else {
                if (!server.isLastPlayersListReceptionRecent()) { // the server is maybe online
                    LOGGER.debug(
                            () -> "sendPluginNotificationToServer(): " + server
                                    + " server is seen as offline, maybe online, save ephemeral notification "
                                    + notif + " to send it shortly or never"
                    );
                    server.addUnsentNotification(true, notif);
                } else { // the server is offline
                    LOGGER.debug(
                            () -> "sendPluginNotificationToServer(): " + server
                                    + " server is offline, " + notif
                                    + " ephemeral notification will never be sent"
                    );
                }
            }
            collectServerPlayers(server);
        } else {
            if (!sendPluginNotificationTo(server.getName(), notif)) {
                if (!notif.isEphemeral()) {
                    LOGGER.debug(
                            () -> "sendPluginNotificationToServer(" + server
                                    + "): sendPluginMessageTo() failed, save non ephemeral notification "
                                    + notif + " to send it later"
                    );
                    server.addUnsentNotification(false, notif);
                } else {
                    LOGGER.debug(
                            () -> "sendPluginNotificationToServer(" + server
                                    + "): sendPluginMessageTo() failed, " + notif
                                    + " ephemeral notification will never be sent"
                    );
                }
            }
        }
    }
    
    private boolean sendPluginNotificationTo(String networkTarget, BungeeNotification notif) {
        if (!initialized) {
            LOGGER.info(() -> "sendPluginMessageTo(): bm unitialized, cancelled");
            return false;
        }
        
        Player p = UserUtils.getRandomPlayer();
        if (p == null) {
            LOGGER.info(() -> "sendPluginMessageTo(): no online player, cancelled");
            return false;
        }
        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Forward");
            out.writeUTF(networkTarget);
            out.writeUTF("TigerReports");
            
            ByteArrayOutputStream msgBytesOut = new ByteArrayOutputStream();
            DataOutputStream msgDataOut = new DataOutputStream(msgBytesOut);
            BungeeNotificationType notifType =
                    BungeeNotificationType.getByDataClass(notif.getClass());
            msgDataOut.writeByte(notifType.getId());
            notifType.writeNotification(msgDataOut, notif);
            
            byte[] msgBytes = msgBytesOut.toByteArray();
            out.writeShort(msgBytes.length);
            out.write(msgBytes);
            
            p.sendPluginMessage(tr, "BungeeCord", out.toByteArray());
            
            Logger.BUNGEE.info(
                    () -> "<-- SENT (to: " + networkTarget + "): " + notifType.toString(notif)
            );
            return true;
        } catch (Exception ex) {
            LOGGER.warn(() -> "sendPluginMessageTo(" + networkTarget + ", " + notif + "): ", ex);
            return false;
        }
    }
    
    public boolean sendBungeeMessage(String... message) {
        if (!initialized) {
            LOGGER.info(
                    () -> "sendBungeeMessage(" + Arrays.toString(message) + "): bm unitialized, cancelled"
            );
            return false;
        }
        
        Player p = UserUtils.getRandomPlayer();
        if (p == null) {
            LOGGER.info(
                    () -> "sendBungeeMessage(" + Arrays.toString(message) + "): no online player, cancelled"
            );
            return false;
        }
        
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        for (String part : message) {
            out.writeUTF(part);
        }
        p.sendPluginMessage(tr, "BungeeCord", out.toByteArray());
        Logger.BUNGEE.info(() -> "<-- SENT (to: Bungee server): " + Arrays.toString(message));
        return true;
    }
    
    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] messageReceived) {
        if (!channel.equals("BungeeCord")) {
            return;
        }
        
        try {
            ByteArrayDataInput in = ByteStreams.newDataInput(messageReceived);
            String subchannel = in.readUTF();
            switch (subchannel) {
                case "TigerReports":
                    short msgLen = in.readShort();
                    byte[] msgBytes = new byte[msgLen];
                    in.readFully(msgBytes);
                    DataInputStream msgDataIn =
                            new DataInputStream(new ByteArrayInputStream(msgBytes));
                    
                    BungeeNotificationType notifType =
                            BungeeNotificationType.getById(msgDataIn.readByte());
                    BungeeNotification notif = notifType.readNotification(msgDataIn);
                    Logger.BUNGEE
                            .info(
                                    () -> "--> RECEIVED (sent at: " + notif.creationTime
                                            + ", elapsed: " + notif.getElapsedTime(
                                                    BungeeManager.this
                                            ) + "ms): " + notifType.toString(notif)
                            );
                    notif.onReceive(db, tr, um, rm, vm, this);
                    break;
                
                case "GetServer":
                    localServerName = in.readUTF();
                    Logger.BUNGEE.info(
                            () -> "--> RECEIVED (from: Bungee server): GetServer - "
                                    + localServerName
                    );
                    knownServers.remove(localServerName); // the current server could have been added when localServerName was null
                    localServerNameResultCallbacks
                            .forEach((callback) -> callback.onResultReceived(localServerName));
                    localServerNameResultCallbacks.clear();
                    break;
                
                case "GetServers":
                    String[] servers = in.readUTF().split(", ");
                    Logger.BUNGEE.info(
                            () -> "--> RECEIVED (from: Bungee server): GetServers - "
                                    + Arrays.toString(servers)
                    );
                    
                    if (servers.length == 0) {
                        LOGGER.error(
                                ConfigUtils.getInfoMessage(
                                        "The received servers list of the BungeeCord network is empty.",
                                        "La liste des serveurs du reseau BungeeCord recue est vide."
                                )
                        );
                    } else {
                        knownServers.keySet().retainAll(Arrays.asList(servers));
                        for (String serverName : servers) {
                            if (
                                isValidDifferentServerName(serverName)
                                        && !knownServers.containsKey(serverName)
                            ) {
                                knownServers.put(serverName, new KnownServer(serverName));
                            }
                        }
                        finishSetupCommunicationSession();
                    }
                    break;
                
                case "PlayerList":
                    String serverNameArg = in.readUTF();
                    String serverName = !"ALL".equals(serverNameArg) ? serverNameArg : null;
                    String serverPlayersArg = in.readUTF();
                    String[] serverPlayers = serverPlayersArg != null && !serverPlayersArg.isEmpty()
                            ? serverPlayersArg.split(", ")
                            : null;
                    Logger.BUNGEE.info(
                            () -> "--> RECEIVED (from: Bungee server): PlayerList - serverName = "
                                    + serverNameArg + ", serverPlayers = " + serverPlayersArg
                    );
                    
                    LOGGER.debug(
                            () -> "onPluginMessageReceived(): PlayerList: serverNameArg = "
                                    + serverNameArg + ", serverName = " + serverName
                                    + ", serverPlayersArg = " + serverPlayersArg
                                    + ", serverPlayers = " + Arrays.toString(serverPlayers) + " (" + (serverPlayers != null ? serverPlayers.length : null) + ")"
                    );
                    
                    if (serverName != null) {
                        KnownServer server = getKnownServer(serverName);
                        if (server == null) {
                            LOGGER.error(
                                    "onPluginMessageReceived(): PlayerList: " + serverName
                                            + " server is unknown"
                            );
                        } else {
                            server.setLastPlayersListReceptionTimeAsNow();
                            
                            if (serverPlayers != null && serverPlayers.length >= 1) {
                                LOGGER.info(
                                        () -> "onPluginMessageReceived(): PlayerList: setServerLastNotificationTime("
                                                + serverName
                                                + ", now) because the server has an online player"
                                );
                                setServerLastNotificationTime(server, System.currentTimeMillis());
                            } else {
                                LOGGER.info(
                                        () -> "onPluginMessageReceived(): PlayerList: " + serverName
                                                + " set as offline because the server has no online player"
                                );
                                server.setOffline();
                            }
                            clearAllOnlinePlayersOfServerLocally(serverName);
                        }
                    }
                    if (serverPlayers != null) {
                        for (String playerName : serverPlayers) {
                            if (!playerName.isEmpty()) {
                                setPlayerOnlineLocally(playerName, true, serverName);
                            }
                        }
                    }
                    break;
                
                default:
                    Logger.BUNGEE.debug(
                            () -> "--> RECEIVED (from: Bungee server): message in ignored "
                                    + subchannel + " subchannel"
                    );
                    break;
            }
        } catch (Exception ex) {
            LOGGER.error(
                    ConfigUtils.getInfoMessage(
                            "An error has occurred when processing a BungeeCord notification:",
                            "Une erreur est survenue en traitant une notification BungeeCord:"
                    ),
                    ex
            );
        }
    }
    
    public boolean isValidDifferentServerName(String serverName) {
        return serverName != null && !serverName.isEmpty() && !serverName.equals(localServerName);
    }
    
    private Collection<KnownServer> getKnownServers() {
        return knownServers.values();
    }
    
    private KnownServer getKnownServer(String serverName) {
        return knownServers.get(serverName);
    }
    
    public void setServerLastNotificationTime(String serverName, long lastNotificationSendTime) {
        KnownServer server = getKnownServer(serverName);
        if (server != null) {
            setServerLastNotificationTime(server, lastNotificationSendTime);
        } else {
            LOGGER.warn(
                    () -> "setOnlineServerLastNotificationTime(): " + serverName
                            + " server is unknown"
            );
        }
    }
    
    private void setServerLastNotificationTime(KnownServer server, long lastNotificationSendTime) {
        server.setLastNotificationTime(lastNotificationSendTime);
        sendAllUnsentNotificationsOfServer(server);
    }
    
    private void sendAllUnsentNotificationsOfServer(KnownServer server) {
        if (server.isOnline()) {
            List<BungeeNotification> unsentEphemeralNotifications =
                    server.getAndClearUnsentNotifications(true);
            if (unsentEphemeralNotifications != null) {
                LOGGER.info(
                        () -> "sendAllUnsentNotificationsOfServer(" + server
                                + "): server is online, attempt to send all its unsent ephemeral notifications..."
                );
                for (BungeeNotification notif : unsentEphemeralNotifications) {
                    sendPluginNotificationToServer(server, notif);
                }
            } else {
                LOGGER.debug(
                        () -> "sendAllUnsentNotificationsOfServer(" + server
                                + "): server is online but no unsent ephemeral notification to send to it"
                );
            }
            
            List<BungeeNotification> unsentNonEphemeralNotifications =
                    server.getAndClearUnsentNotifications(false);
            if (unsentNonEphemeralNotifications != null) {
                LOGGER.info(
                        () -> "sendAllUnsentNotificationsOfServer(" + server
                                + "): server is online, attempt to send all its unsent non ephemeral notifications..."
                );
                for (BungeeNotification notif : unsentNonEphemeralNotifications) {
                    sendPluginNotificationToServer(server, notif);
                }
            } else {
                LOGGER.debug(
                        () -> "sendAllUnsentNotificationsOfServer(" + server
                                + "): server is online but no unsent non ephemeral notification to send to it"
                );
            }
        }
    }
    
    public boolean isPlayerOnline(String name) {
        return onlinePlayersServer.containsKey(name);
    }
    
    public String getPlayerServerName(String name) {
        return onlinePlayersServer.get(name);
    }
    
    public List<String> getOnlinePlayers() {
        return new ArrayList<>(onlinePlayersServer.keySet());
    }
    
    private void clearAllOnlinePlayersOfServerLocally(String serverName) {
        if (serverName == null) {
            return;
        }
        
        Iterator<String> it = onlinePlayersServer.values().iterator();
        while (it.hasNext()) {
            if (serverName.equals(it.next())) {
                it.remove();
            }
        }
    }
    
    public void setPlayerOnlineLocally(String playerName, boolean online, String serverName) {
        CheckUtils.notEmpty(playerName);
        if (online) {
            onlinePlayersServer.put(playerName, serverName);
            LOGGER.debug(
                    () -> "setPlayerOnlineLocally(): " + playerName + " player set as online on "
                            + serverName + " server"
            );
        } else {
            String playerLastKnownServer = onlinePlayersServer.get(playerName);
            if (playerLastKnownServer == null || Objects.equal(playerLastKnownServer, serverName)) {
                // this check prevents old server from removing the new server of the player
                onlinePlayersServer.remove(playerName);
                LOGGER.debug(
                        () -> "setPlayerOnlineLocally(): " + playerName + " player set as offline"
                );
            } else {
                LOGGER.debug(
                        () -> "setPlayerOnlineLocally(" + playerName
                                + ", online = false): ignored because last known server "
                                + playerLastKnownServer + " != " + serverName + " server"
                );
            }
        }
    }
    
    private void updatePlayerOnlineInNetwork(String playerName, boolean online, UUID playerUUID,
            String lastMessagesStartDatetime) {
        setPlayerOnlineLocally(playerName, online, localServerName);
        if (Bukkit.getOnlinePlayers().size() >= (online ? 1 : 2)) { // if the notif is for an offline player, that offline player can still be counted in Bukkit.getOnlinePlayers() for short time
            getServerName((localServerName) -> {
                sendPluginNotificationToAll(
                        new PlayerOnlineBungeeNotification(
                                getNetworkCurrentTime(),
                                playerName,
                                playerUUID,
                                online,
                                localServerName,
                                lastMessagesStartDatetime
                        )
                );
            });
        } else {
            LOGGER.info(
                    () -> "updatePlayerOnlineInNetwork(): no online player, PlayerOnline notification will be sent later"
            );
            playerOfflineNotBroadcastedName = playerName;
            playerOfflineNotBroadcastedUUID = playerUUID;
        }
    }
    
    public void tpPlayerToOtherServerLocation(String playerName, String serverName,
            String serializedLoc) {
        KnownServer server = getKnownServer(serverName);
        if (server == null) {
            LOGGER.info(
                    () -> "sendPluginNotificationToServer(): " + serverName + " server is unknown"
            );
            return;
        }
        
        setServerLastNotificationTime(server, System.currentTimeMillis()); // make the target server online because it will receive the current player, in order to send the notification
        sendPluginNotificationToServer(
                server,
                new TeleportToLocationBungeeNotification(
                        getNetworkCurrentTime(),
                        playerName,
                        serializedLoc
                )
        );
        sendBungeeMessage("ConnectOther", playerName, serverName); // sent after in order to keep the player on the current server for sending the previous notification
    }
    
    public void tpPlayerToPlayerInOtherServer(String playerName, String targetName) {
        sendPluginNotificationToAll(
                new TeleportToPlayerBungeeNotification(
                        getNetworkCurrentTime(),
                        playerName,
                        targetName
                )
        );
    }
    
    public long getNetworkCurrentTime() {
        return System.currentTimeMillis();
    }
    
    public void destroy() {
        Messenger messenger = tr.getServer().getMessenger();
        messenger.unregisterOutgoingPluginChannel(tr, "BungeeCord");
        messenger.unregisterIncomingPluginChannel(tr, "BungeeCord", this);
        initialized = false;
    }
    
}
