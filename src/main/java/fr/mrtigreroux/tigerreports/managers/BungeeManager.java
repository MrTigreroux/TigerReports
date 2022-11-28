package fr.mrtigreroux.tigerreports.managers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.PluginMessageListener;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.data.config.ConfigSound;
import fr.mrtigreroux.tigerreports.data.constants.Appreciation;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.logs.Logger;
import fr.mrtigreroux.tigerreports.objects.reports.Report;
import fr.mrtigreroux.tigerreports.objects.reports.Report.AppreciationDetails;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.tasks.ResultCallback;
import fr.mrtigreroux.tigerreports.utils.CollectionUtils;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;
import fr.mrtigreroux.tigerreports.utils.ReportUtils;

/**
 * @author MrTigreroux
 */

public class BungeeManager implements PluginMessageListener {

	public static final class NotificationType {

		public static final String PROCESS = "process";
		public static final String PROCESS_PUNISH = "process_punish";
		public static final String PROCESS_ABUSIVE = "process_abusive";
		public static final String TP_PLAYER = "tp_player";
		public static final String TP_LOC = "tp_loc";
		public static final String PLAYER_ONLINE = "player_online";
		public static final String PLAYER_LAST_MESSAGES = "player_last_messages";
		public static final String CHANGE_STATISTIC = "change_statistic";
		public static final String COMMENT = "comment";
		public static final String STOP_COOLDOWN = "stop_cooldown";
		public static final String NEW_COOLDOWN = "new_cooldown";
		public static final String NEW_IMMUNITY = "new_immunity";
		public static final String DATA_CHANGED = "data_changed";
		public static final String UNARCHIVE = "unarchive";
		public static final String ARCHIVE = "archive";
		public static final String PUNISH = "punish";
		public static final String DELETE = "delete";
		public static final String NEW_REPORT = "new_report";
		public static final String NEW_STATUS = "new_status";

		private NotificationType() {}

	}

	private static final Logger LOGGER = Logger.fromClass(BungeeManager.class);

	public static final String MESSAGE_DATA_SEPARATOR = " ";
	public static final int RECENT_MESSAGE_MAX_DELAY = 10 * 1000;
	public static final int NOTIFY_MESSAGE_MAX_DELAY = 60 * 1000;
	public static final String DEFAULT_SERVER_NAME = "localhost";
	public static final long MAX_COMMUNICATION_SESSION_SETUP_TIME = 2 * 60 * 1000;
	public static final long MAX_ONLINE_SERVER_LAST_NOTIFICATION_TIME = 10 * 60 * 1000;
	public static final long MAX_PLAYERS_LIST_COLLECTION_PER_SERVER_RECENT_REQUEST_TIME = 60 * 1000;
	public static final long MAX_PLAYERS_LIST_COLLECTION_PER_SERVER_RECENT_RECEPTION_TIME = MAX_ONLINE_SERVER_LAST_NOTIFICATION_TIME;
	public static final byte MAX_UNSENT_NOTIFICATIONS_KEPT_WHEN_SERVERS_UNKNOWN = 20;
	public static final byte MAX_OFFLINE_SERVER_KEPT_UNSENT_NON_EPHEMERAL_NOTIFICATIONS = 50;
	public static final byte MAX_OFFLINE_SERVER_KEPT_UNSENT_EPHEMERAL_NOTIFICATIONS = 20;

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
		private List<Notification> unsentNonEphemeralNotifications = new CollectionUtils.LimitedOrderedList<>(
		        MAX_OFFLINE_SERVER_KEPT_UNSENT_NON_EPHEMERAL_NOTIFICATIONS);
		private List<Notification> unsentEphemeralNotifications = new CollectionUtils.LimitedOrderedList<>(
		        MAX_OFFLINE_SERVER_KEPT_UNSENT_EPHEMERAL_NOTIFICATIONS);

		public KnownServer(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public boolean isOnline() {
			return lastNotificationTime != null
			        && System.currentTimeMillis() - lastNotificationTime <= MAX_ONLINE_SERVER_LAST_NOTIFICATION_TIME;
		}

		public void setLastNotificationTime(long timeInMillis) {
			lastNotificationTime = timeInMillis;
		}

		public void setOffline() {
			lastNotificationTime = null;
			LOGGER.info(() -> name + " server: setOffline(): unsent ephemeral notifications cleared: "
			        + CollectionUtils.toString(unsentEphemeralNotifications));
			unsentEphemeralNotifications.clear();
		}

		public void addUnsentNotification(boolean ephemeralNotification, Notification notification) {
			if (ephemeralNotification) {
				unsentEphemeralNotifications.add(notification);
				LOGGER.debug(() -> name + " server: addUnsentNotification(): now unsentEphemeralNotifications = "
				        + CollectionUtils.toString(unsentEphemeralNotifications));
			} else {
				unsentNonEphemeralNotifications.add(notification);
				LOGGER.debug(() -> name + " server: addUnsentNotification(): now unsentNonEphemeralNotifications = "
				        + CollectionUtils.toString(unsentNonEphemeralNotifications));
			}
		}

		public List<Notification> getAndClearUnsentNotifications(boolean ephemeralNotifications) {
			return getAndClearUnsentNotifications(
			        ephemeralNotifications ? unsentEphemeralNotifications : unsentNonEphemeralNotifications);
		}

		private List<Notification> getAndClearUnsentNotifications(List<Notification> unsentNotifications) {
			if (!unsentNotifications.isEmpty()) {
				List<Notification> notifs = new ArrayList<>(unsentNotifications);
				unsentNotifications.clear();
				LOGGER.debug(() -> name
				        + " server: getAndClearUnsentNotifications(): unsent notifications cleared (ephemeral size = "
				        + unsentEphemeralNotifications.size() + ", non ephemeral size = "
				        + unsentNonEphemeralNotifications.size() + ")");
				return notifs;
			} else {
				return null;
			}
		}

		public void setLastPlayersListRequestTimeAsNow() {
			lastPlayersListRequestTime = System.currentTimeMillis();
		}

		public boolean isLastPlayersListRequestRecent() {
			return lastPlayersListRequestTime != null && System.currentTimeMillis()
			        - lastPlayersListRequestTime <= MAX_PLAYERS_LIST_COLLECTION_PER_SERVER_RECENT_REQUEST_TIME;
		}

		public void setLastPlayersListReceptionTimeAsNow() {
			lastPlayersListReceptionTime = System.currentTimeMillis();
		}

		public boolean isLastPlayersListReceptionRecent() {
			return lastPlayersListReceptionTime != null && System.currentTimeMillis()
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

	private static class Notification {

		final long time;
		final Object[] parts;

		private Notification(Object[] parts) {
			this(System.currentTimeMillis(), parts);
		}

		private Notification(long time, Object[] parts) {
			this.time = time;
			this.parts = parts;
		}

		@Override
		public String toString() {
			return "Notification [time=" + time + ", parts=" + Arrays.toString(parts) + "]";
		}

	}

	/**
	 * NB: The notification time will be the one of sending, and not the one of the creation of the notification.
	 */
	private static class UnsentNotification {

		final boolean ephemeral;
		final Object[] parts;

		private UnsentNotification(boolean ephemeral, Object[] parts) {
			this.ephemeral = ephemeral;
			this.parts = parts;
		}

	}

	/**
	 * NB: The notification time will be the one of sending, and not the one of the creation of the notification.
	 */
	private List<UnsentNotification> unsentNotificationsWhenServersUnknown = new CollectionUtils.LimitedOrderedList<>(
	        MAX_UNSENT_NOTIFICATIONS_KEPT_WHEN_SERVERS_UNKNOWN);
	private String playerOfflineNotBroadcastedName = null;
	private UUID playerOfflineNotBroadcastedUUID = null;

	private static class TeleportLocationNotification {

		final long sendTime;
		final Location loc;

		public TeleportLocationNotification(long sendTime, Location loc) {
			this.sendTime = sendTime;
			this.loc = java.util.Objects.requireNonNull(loc);
		}

	}

	/**
	 * player name -> teleport location notification
	 */
	private Map<String, TeleportLocationNotification> playersTeleportLocNotif = new HashMap<>();

	public BungeeManager(TigerReports tr, ReportsManager rm, Database db, VaultManager vm, UsersManager um) {
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
			Logger.CONFIG.info(() -> ConfigUtils.getInfoMessage("The plugin is using BungeeCord.",
			        "Le plugin utilise BungeeCord."));
		} else {
			Logger.CONFIG.info(() -> ConfigUtils.getInfoMessage("The plugin is not using BungeeCord.",
			        "Le plugin n'utilise pas BungeeCord."));
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
		return communicationSessionLastSetUpStartTime != null && System.currentTimeMillis()
		        - communicationSessionLastSetUpStartTime < MAX_COMMUNICATION_SESSION_SETUP_TIME;
	}

	private void finishSetupCommunicationSession() {
		if (isCommunicationSessionPendingSetup()) {
			communicationSessionLastSetUpStartTime = null;
			for (KnownServer server : getKnownServers()) {
				collectServerPlayers(server);
			}

			LOGGER.info(
			        () -> "finishSetupCommunicationSession(): attempt to send all unsent notifications (to all) when servers where unknown...");
			for (UnsentNotification unNotif : unsentNotificationsWhenServersUnknown) {
				sendPluginNotificationToAll(unNotif.ephemeral, unNotif.parts);
			}
			unsentNotificationsWhenServersUnknown.clear();

			isCommunicationSessionSetUp = true;
			LOGGER.info(() -> "finishSetupCommunicationSession(): session is now ready");
		} else {
			LOGGER.info(() -> "finishSetupCommunicationSession(): session is not pending setup, ignored");
		}
	}

	private void collectServerPlayers(KnownServer server) {
		if (!server.isLastPlayersListReceptionRecent() && !server.isLastPlayersListRequestRecent()) {
			LOGGER.debug(() -> "collectServerPlayers(): collect players of " + server + " server from Bungee server");
			server.setLastPlayersListRequestTimeAsNow();
			sendBungeeMessage("PlayerList", server.getName());
		} else {
			LOGGER.info(() -> "collectServerPlayers(): " + server
			        + " server players list already requested or collected recently, cancel collection");
		}
	}

	private boolean collectServerName() {
		if (localServerName == null) {
			return sendBungeeMessage("GetServer");
		} else {
			return true;
		}
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
			        () -> "processPlayerConnection(): isCommunicationSessionSetUp set to false because no player before");
		}
		startSetupCommunicationSession(false);

		if (playerOfflineNotBroadcastedUUID != null) {
			if (playerOfflineNotBroadcastedUUID != p.getUniqueId()) {
				getServerName((localServerName) -> {
					sendPlayerOnlineNotification(playerOfflineNotBroadcastedName, false, localServerName,
					        playerOfflineNotBroadcastedUUID, null);
				});
			} // else the player is now online, playerOfflineNotBroadcasted must be ignored
			playerOfflineNotBroadcastedUUID = null;
			playerOfflineNotBroadcastedName = null;
		}

		User onlineUser = um.getOnlineUser(p);
		if (onlineUser != null) {
			updatePlayerOnlineInNetwork(p.getName(), true, p.getUniqueId(),
			        onlineUser.getLastMessagesMinDatetimeOfInsertableMessages());
		} // else the player just disconnected

		teleportPlayerIfHasTPLocNotification(p);
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

	public void sendPluginNotificationToAll(boolean nowOrNever, Object... notifParts) {
		Collection<KnownServer> knownServers = getKnownServers();
		if (knownServers.isEmpty()) {
			unsentNotificationsWhenServersUnknown.add(new UnsentNotification(nowOrNever, notifParts));
			LOGGER.info(() -> "sendPluginNotificationToAll(): servers are unknown, saved notification "
			        + Arrays.toString(notifParts) + " (ephemeral = " + nowOrNever
			        + ") to try to send it later when servers will be collected");
		} else {
			Notification notif = new Notification(notifParts);
			LOGGER.info(() -> "sendPluginNotificationToAll(): attempt to send " + notif + " (ephemeral = " + nowOrNever
			        + ") to all known servers...");
			for (KnownServer server : knownServers) {
				sendPluginNotificationToServer(server, nowOrNever, notif);
			}
		}
	}

	public void sendPluginNotificationToServer(String serverName, boolean nowOrNever, Object... notifParts) {
		KnownServer server = getKnownServer(serverName);
		if (server == null) {
			LOGGER.warn(() -> "sendPluginNotificationToServer(): " + serverName + " server is unknown");
			return;
		}

		sendPluginNotificationToServer(server, nowOrNever, notifParts);
	}

	public void sendPluginNotificationToServer(KnownServer server, boolean nowOrNever, Object... notifParts) {
		sendPluginNotificationToServer(server, nowOrNever, new Notification(notifParts));
	}

	public void sendPluginNotificationToServer(KnownServer server, boolean nowOrNever, Notification notif) {
		if (server == null) {
			throw new IllegalArgumentException("Server is null");
		}
		if (notif == null || notif.parts == null || notif.parts.length == 0) {
			throw new IllegalArgumentException("Empty notification");
		}

		if (!server.isOnline()) { // no notification for too long time
			if (!nowOrNever) { // non ephemeral notification, saved in all cases
				LOGGER.debug(() -> "sendPluginNotificationToServer(): " + server
				        + " server is seen as offline, maybe online, save non ephemeral notification " + notif
				        + " to send it later");
				server.addUnsentNotification(false, notif);
			} else {
				if (!server.isLastPlayersListReceptionRecent()) { // the server is maybe online
					LOGGER.debug(() -> "sendPluginNotificationToServer(): " + server
					        + " server is seen as offline, maybe online, save ephemeral notification " + notif
					        + " to send it shortly or never");
					server.addUnsentNotification(true, notif);
				} else { // the server is offline
					LOGGER.debug(() -> "sendPluginNotificationToServer(): " + server + " server is offline, " + notif
					        + " notification will never be sent");
				}
			}
			collectServerPlayers(server);
		} else {
			if (!sendPluginMessageTo(server.getName(), notif.time + MESSAGE_DATA_SEPARATOR
			        + MessageUtils.joinElements(MESSAGE_DATA_SEPARATOR, notif.parts, true))) {
				if (!nowOrNever) {
					LOGGER.debug(() -> "sendPluginNotificationToServer(" + server
					        + "): sendPluginMessageTo() failed, save non ephemeral notification " + notif
					        + " to send it later");
					server.addUnsentNotification(false, notif);
				} else {
					LOGGER.debug(() -> "sendPluginNotificationToServer(" + server + "): sendPluginMessageTo() failed, "
					        + notif + " ephemeral notification will never be sent");
				}
			}
		}
	}

	private boolean sendPluginMessageTo(String serverName, String message) {
		if (!initialized) {
			LOGGER.info(() -> "sendPluginMessageTo(): bm unitialized, cancelled");
			return false;
		}

		Player p = getRandomPlayer();
		if (p == null) {
			LOGGER.info(() -> "sendPluginMessageTo(): no online player, cancelled");
			return false;
		}
		try {
			ByteArrayDataOutput out = ByteStreams.newDataOutput();
			out.writeUTF("Forward");
			out.writeUTF(serverName);
			out.writeUTF("TigerReports");

			ByteArrayOutputStream messageOut = new ByteArrayOutputStream();
			DataOutputStream messageStream = new DataOutputStream(messageOut);
			messageStream.writeUTF(message);

			byte[] messageBytes = messageOut.toByteArray();
			out.writeShort(messageBytes.length);
			out.write(messageBytes);

			p.sendPluginMessage(tr, "BungeeCord", out.toByteArray());

			Logger.BUNGEE.info(() -> "<-- SENT (to: " + serverName + "): '" + message + "'");
			return true;
		} catch (IOException ex) {
			LOGGER.warn(() -> "sendPluginMessageTo(" + serverName + ", " + message + "): ", ex);
			return false;
		}
	}

	public boolean sendBungeeMessage(String... message) {
		if (!initialized) {
			LOGGER.info(() -> "sendBungeeMessage(" + Arrays.toString(message) + "): bm unitialized, cancelled");
			return false;
		}

		Player p = getRandomPlayer();
		if (p == null) {
			LOGGER.info(() -> "sendBungeeMessage(" + Arrays.toString(message) + "): no online player, cancelled");
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
				byte[] messageBytes = new byte[in.readShort()];
				in.readFully(messageBytes);
				DataInputStream messageStream = new DataInputStream(new ByteArrayInputStream(messageBytes));
				String fullMessage = messageStream.readUTF();
				processPluginNotification(fullMessage);
				break;

			case "GetServer":
				localServerName = in.readUTF();
				Logger.BUNGEE.info(() -> "--> RECEIVED (from: Bungee server): GetServer - " + localServerName);
				knownServers.remove(localServerName); // the current server could have been added when localServerName is null
				localServerNameResultCallbacks.forEach((callback) -> callback.onResultReceived(localServerName));
				localServerNameResultCallbacks.clear();
				break;

			case "GetServers":
				String[] servers = in.readUTF().split(", ");
				Logger.BUNGEE
				        .info(() -> "--> RECEIVED (from: Bungee server): GetServers - " + Arrays.toString(servers));

				if (servers.length == 0) {
					LOGGER.error(
					        ConfigUtils.getInfoMessage("The received servers list of the BungeeCord network is empty.",
					                "La liste des serveurs du reseau BungeeCord recue est vide."));
				} else {
					knownServers.keySet().retainAll(Arrays.asList(servers));
					for (String serverName : servers) {
						if (isValidDifferentServerName(serverName) && !knownServers.containsKey(serverName)) {
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
				Logger.BUNGEE.info(() -> "--> RECEIVED (from: Bungee server): PlayerList - serverName = "
				        + serverNameArg + ", serverPlayers = " + serverPlayersArg);

				LOGGER.debug(() -> "onPluginMessageReceived(): PlayerList: serverNameArg = " + serverNameArg
				        + ", serverName = " + serverName + ", serverPlayersArg = " + serverPlayersArg
				        + ", serverPlayers = " + Arrays.toString(serverPlayers) + "("
				        + (serverPlayers != null ? serverPlayers.length : null) + ")");

				if (serverName != null) {
					KnownServer server = getKnownServer(serverName);
					if (server == null) {
						LOGGER.error("onPluginMessageReceived(): PlayerList: " + serverName + " server is unknown");
					} else {
						server.setLastPlayersListReceptionTimeAsNow();

						if (serverPlayers != null && serverPlayers.length >= 1) {
							LOGGER.info(() -> "onPluginMessageReceived(): PlayerList: setServerLastNotificationTime("
							        + serverName + ", now) because the server has an online player");
							setServerLastNotificationTime(server, System.currentTimeMillis());
						} else {
							LOGGER.info(() -> "onPluginMessageReceived(): PlayerList: " + serverName
							        + " set as offline because the server has no online player");
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
				        () -> "--> RECEIVED (from: Bungee server): message in ignored " + subchannel + " subchannel");
				break;
			}
		} catch (Exception ex) {
			LOGGER.error(ConfigUtils.getInfoMessage("An error has occurred when processing a BungeeCord notification:",
			        "Une erreur est survenue en traitant une notification BungeeCord:"), ex);
		}
	}

	private void processPluginNotification(String fullMessage) {
		try {
			int index = fullMessage.indexOf(' ');
			long sendTime = Long.parseLong(fullMessage.substring(0, index));
			long now = System.currentTimeMillis();
			long elapsedTime = now - sendTime;
			boolean isRecentMsg = elapsedTime < RECENT_MESSAGE_MAX_DELAY;
			boolean notify = elapsedTime < NOTIFY_MESSAGE_MAX_DELAY;

			String message = fullMessage.substring(index + 1);
			Logger.BUNGEE.info(() -> "--> RECEIVED (sent at: " + sendTime + ", elapsed: " + elapsedTime + "ms): '"
			        + message + "'");
			String[] parts = message.split(MESSAGE_DATA_SEPARATOR);

			switch (parts[1]) {
			case NotificationType.NEW_REPORT:
				String reportServer = parts[0];
				int reportDataStartIndex = message.indexOf(parts[3]);
				String reportDataAsString = getReportDataAsString(message, reportDataStartIndex);
				setServerLastNotificationTime(reportServer, sendTime);
				processNewReportMessage(isRecentMsg, notify, reportServer, Boolean.parseBoolean(parts[2]),
				        reportDataAsString);
				break;
			case NotificationType.NEW_STATUS:
				getReportAsynchronously(parts[2], true, db, (r) -> {
					if (r != null) {
						Report.StatusDetails.asynchronouslyFrom(parts[0], db, tr, um, (sd) -> {
							r.setStatus(sd, true, db, rm, BungeeManager.this);
						});
					}
				});
				break;
			case NotificationType.PROCESS:
				getReportAndUserIfNotifyAsynchronously(parts, notify, db, (r, u) -> {
					if (r != null && u != null) {
						r.process(u, Appreciation.from(parts[4]), true, parts[3].equals("1"), notify, db, rm, vm,
						        BungeeManager.this, tr);
					} else {
						LOGGER.info(() -> "PROCESS: invalid r (" + r + ") or u (" + u + "), msg: " + message);
					}
				});
				break;
			case NotificationType.PROCESS_PUNISH:
				boolean auto = parts[3].equals("1");
				String punishment = message.substring(parts[4].indexOf("/") + 1);
				getReportAndUserIfNotifyAsynchronously(parts, notify, db, (r, u) -> {
					if (r != null && u != null) {
						r.processWithPunishment(u, true, auto, punishment, notify, db, rm, vm, BungeeManager.this, tr);
					} else {
						LOGGER.info(() -> "PROCESS_PUNISH: invalid r (" + r + ") or u (" + u + "), msg: " + message);
					}
				});
				break;
			case NotificationType.PROCESS_ABUSIVE:
				boolean autoArchive = parts[3].equals("1");
				long punishSeconds = 0;

				try {
					punishSeconds = parts.length >= 6 && parts[5] != null ? Long.parseLong(parts[5])
					        : ReportUtils.getAbusiveReportCooldown();
				} catch (NumberFormatException e) {
					LOGGER.warn(() -> "PROCESS_ABUSIVE: invalid punishSeconds (" + parts[5] + ")");
					punishSeconds = ReportUtils.getAbusiveReportCooldown();
				}
				long fpunishSeconds = punishSeconds;

				getReportAndUserIfNotifyAsynchronously(parts, notify, db, (r, u) -> {
					if (r != null && u != null) {
						r.processAbusive(u, true, autoArchive, fpunishSeconds, notify, db, rm, um, BungeeManager.this,
						        vm, tr);
					} else {
						LOGGER.info(() -> "PROCESS_ABUSIVE: invalid r (" + r + ") or u (" + u + "), msg: " + message);
					}
				});
				break;
			case NotificationType.DELETE:
				// The report is not saved in cache, and is even deleted from cache if cached.
				getReportFromData(message, parts[2], (r) -> {
					if (r == null) {
						return;
					}

					if (notify) {
						getUserAsynchronously(parts[0], (u) -> {
							r.delete(u, true, db, tr, rm, vm, BungeeManager.this);
						});
					} else {
						r.delete(null, true, db, tr, rm, vm, BungeeManager.this);
					}
				});
				break;
			case NotificationType.ARCHIVE:
				getReportAndUserIfNotifyAsynchronously(parts, notify, db, (r, u) -> {
					if (r != null) {
						r.archive(u, true, db, rm, vm, BungeeManager.this);
					}
				});
				break;
			case NotificationType.UNARCHIVE:
				getReportAndUserIfNotifyAsynchronously(parts, notify, db, (r, u) -> {
					if (r != null) {
						r.unarchive(u, true, db, rm, vm, BungeeManager.this);
					}
				});
				break;

			case NotificationType.DATA_CHANGED:
				if (parts.length <= 2) {
					return;
				}

				if (isRecentMsg) {
					// Wait for the database to be updated
					tr.runTaskDelayedly(RECENT_MESSAGE_MAX_DELAY - elapsedTime, () -> {
						updateUsersData(parts, 2);
					});
				} else {
					updateUsersData(parts, 2);
				}

				break;
			case NotificationType.NEW_IMMUNITY:
				getUserAsynchronously(parts[3], (u) -> {
					if (u != null) {
						if (isRecentMsg) {
							u.setImmunity(parts[0].equals("null") ? null : parts[0].replace("_", " "), true, db,
							        BungeeManager.this, um);
						} else {
							um.updateDataOfUserWhenPossible(u.getUniqueId(), db, tr);
						}
					}
				});
				break;
			case NotificationType.NEW_COOLDOWN:
				getUserAsynchronously(parts[3], (u) -> {
					if (u != null) {
						if (isRecentMsg) {
							u.setCooldown(parts[0].equals("null") ? null : parts[0].replace("_", " "), true, db,
							        BungeeManager.this);
						} else {
							um.updateDataOfUserWhenPossible(u.getUniqueId(), db, tr);
						}
					}
				});
				break;
			case NotificationType.PUNISH:
				getUserAsynchronously(parts[3], (u) -> {
					if (u != null) {
						if (notify) {
							getUserAsynchronously(parts[0], (staff) -> {
								u.punish(Long.parseLong(parts[4]), staff, true, db, BungeeManager.this, vm);
							});
						} else {
							um.updateDataOfUserWhenPossible(u.getUniqueId(), db, tr);
						}
					}
				});
				break;
			case NotificationType.STOP_COOLDOWN:
				getUserAsynchronously(parts[3], (u) -> {
					if (u != null) {
						if (notify) {
							getUserAsynchronously(parts[0], (staff) -> {
								u.stopCooldown(staff, true, db, BungeeManager.this);
							});
						} else {
							um.updateDataOfUserWhenPossible(u.getUniqueId(), db, tr);
						}
					}
				});
				break;
			case NotificationType.CHANGE_STATISTIC:
				getUserAsynchronously(parts[3], (u) -> {
					if (u != null) {
						if (isRecentMsg) {
							u.changeStatistic(parts[2], Integer.parseInt(parts[0]), true, db, BungeeManager.this);
						} else {
							um.updateDataOfUserWhenPossible(u.getUniqueId(), db, tr);
						}
					}
				});
				break;
			case NotificationType.TP_LOC:
				if (notify) {
					String locStr = parts[2];
					try {
						teleportPlayerLocallyWhenOnline(parts[0],
						        new TeleportLocationNotification(sendTime, MessageUtils.unformatLocation(locStr)));
					} catch (NullPointerException ex) {
						throw new IllegalArgumentException("Invalid location " + locStr);
					}
				} else {
					LOGGER.info(() -> "TP_LOC: " + parts[0] + " player, too old notification, ignored");
				}
				break;
			case NotificationType.TP_PLAYER:
				if (!notify) {
					break;
				}

				if (localServerName != null) {
					String target = parts[2];
					Player t = Bukkit.getPlayer(target);
					if (t != null) {
						String staff = parts[0];
						teleportPlayerLocallyWhenOnline(staff,
						        new TeleportLocationNotification(System.currentTimeMillis(), t.getLocation()));
						sendBungeeMessage("ConnectOther", staff, localServerName);
					}
				}
				break;
			case NotificationType.COMMENT:
				User ru = um.getOnlineUser(parts[3]);
				if (ru == null) {
					return;
				}
				rm.getReportByIdAsynchronously(Integer.parseInt(parts[0]), false, notify, db, tr, um, (r) -> {
					if (r != null) {
						r.getCommentByIdAsynchronously(Integer.parseInt(parts[2]), db, tr, um, (c) -> {
							ru.sendCommentNotification(r, c, true, db, vm, BungeeManager.this);
						});
					}
				});
				break;
			case NotificationType.PLAYER_ONLINE:
				String playerName = parts[0];
				boolean playerOnlineInNetwork = Boolean.parseBoolean(parts[2]);
				String notifServerName = !"null".equals(notifServerName = parts[3]) ? notifServerName : null;
				UUID playerUUID = UUID.fromString(parts[4]);
				String lastMessagesStartDatetime = playerOnlineInNetwork
				        && !"null".equals(lastMessagesStartDatetime = parts[5])
				                ? lastMessagesStartDatetime.replace("_", " ")
				                : null;

				setServerLastNotificationTime(notifServerName, sendTime);

				if (notify && Bukkit.getPlayer(playerUUID) == null) { // prevents changing local online status of an online player on the current server
					setPlayerOnlineLocally(playerName, playerOnlineInNetwork, notifServerName);

					boolean notifFromValidDifferentServer = notifServerName != null
					        && !notifServerName.equals(localServerName);
					if (playerOnlineInNetwork && notifFromValidDifferentServer) {
						User cachedUser = um.getCachedUser(playerUUID);
						if (cachedUser != null) {
							sendPlayerLastMessages(notifServerName, playerUUID,
							        cachedUser.getLastMessagesAfterDatetime(lastMessagesStartDatetime));
						}
					}
				} // else ignore notif because player is online in current server or because notif is too old (probably was sent during a period of time where there were no online player in the current server, after
				  // that period, a player joined, triggered the collection of all online players connected in the network and allowed the reception of that current notif that can be ignored because pointless)
				break;
			case NotificationType.PLAYER_LAST_MESSAGES:
				String playerUUIDStr2 = parts[0];
				if (parts.length >= 2) {
					int lastMessagesStartIndex = message.indexOf(parts[2]);
					if (lastMessagesStartIndex >= 0) {
						User onlineUser = um.getOnlineUser(playerUUIDStr2);
						if (onlineUser != null) {
							onlineUser.updateLastMessages(
							        Report.AdvancedData.unformatMessages(message.substring(lastMessagesStartIndex)));
						}
					}
				}
				break;
			default:
				break;
			}
		} catch (Exception ex) {
			LOGGER.error(ConfigUtils.getInfoMessage("An error has occurred when processing a BungeeCord notification:",
			        "Une erreur est survenue en traitant une notification BungeeCord:"), ex);
		}
	}

	private Player getRandomPlayer() {
		return Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
	}

	private interface ReportAndUserResultCallback {
		void onReportAndUserReceived(Report r, User u);
	}

	private void getReportAndUserIfNotifyAsynchronously(String[] parts, boolean notify, Database db,
	        ReportAndUserResultCallback resultCallback) {
		getReportAsynchronously(parts[2], notify, db, (r) -> {
			if (notify) {
				getUserAsynchronously(parts[0], (u) -> {
					resultCallback.onReportAndUserReceived(r, u);
				});
			} else {
				resultCallback.onReportAndUserReceived(r, null);
			}
		});
	}

	private void getReportAsynchronously(String reportId, boolean useCache, Database db,
	        ResultCallback<Report> resultCallback) {
		try {
			getReportAsynchronously(Integer.parseInt(reportId), useCache, db, resultCallback);
		} catch (NumberFormatException ex) {
			LOGGER.warn(() -> "getReportAsynchronously(): invalid reportId: " + reportId, ex);
		}
	}

	private void getReportAsynchronously(int reportId, boolean useCache, Database db,
	        ResultCallback<Report> resultCallback) {
		rm.getReportByIdAsynchronously(reportId, false, useCache, db, tr, um, resultCallback);
	}

	private void getUserAsynchronously(String uuid, ResultCallback<User> resultCallback) {
		um.getUserAsynchronously(uuid, db, tr, resultCallback);
	}

	private void getReportFromData(String message, String firstPart, ResultCallback<Report> resultCallback) {
		int reportDataStartIndex = message.indexOf(firstPart);
		if (reportDataStartIndex < 0) {
			resultCallback.onResultReceived(null);
			return;
		}
		String reportDataAsString = message.substring(reportDataStartIndex);
		Map<String, Object> reportData = Report.parseBasicDataFromString(reportDataAsString);
		if (reportData == null) {
			LOGGER.info(() -> "getReportFromData(): reportData = null, reportDataAsString = " + reportDataAsString);
			resultCallback.onResultReceived(null);
			return;
		}

		Report.asynchronouslyFrom(reportData, false, db, tr, um, resultCallback);
	}

	private String getReportDataAsString(String message, int reportDataStartIndex) {
		if (reportDataStartIndex < 0) {
			return null;
		}

		return message.substring(reportDataStartIndex);
	}

	private void processNewReportMessage(boolean isRecentMsg, boolean notify, String reportServer,
	        boolean reportMissingData, String reportDataAsString) {
		Map<String, Object> reportData = Report.parseBasicDataFromString(reportDataAsString);
		if (reportData == null) {
			LOGGER.info(
			        () -> "processNewReportMessage(): reportData = null, reportDataAsString = " + reportDataAsString);
			return;
		}

		int reportId = (int) reportData.get(Report.REPORT_ID);

		LOGGER.info(() -> "processNewReportMessage(): reportData = " + CollectionUtils.toString(reportData)
		        + ", isRecentMsg = " + isRecentMsg + ", notify = " + notify);
		if (isRecentMsg) {
			rm.updateAndGetReport(reportId, reportData, false, false, db, tr, um, createNewReportResultCallback(notify,
			        reportServer, reportMissingData, reportDataAsString, reportData));
		} else if (notify) {
			getReportAsynchronously(reportId, false, db, createNewReportResultCallback(notify, reportServer,
			        reportMissingData, reportDataAsString, reportData));
		} else {
			getReportAsynchronously(reportId, false, db, (r) -> {
				if (r != null) {
					ReportUtils.sendReport(r, reportServer, notify, db, vm, BungeeManager.this);
				}
			});
		}

	}

	private ResultCallback<Report> createNewReportResultCallback(boolean notify, String reportServer,
	        boolean reportMissingData, String reportDataAsString, Map<String, Object> reportData) {
		return new ResultCallback<Report>() {

			@Override
			public void onResultReceived(Report r) {
				if (r != null && r.getBasicDataAsString().equals(reportDataAsString)) {
					sendReportAndImplementMissingData(r, reportServer, notify, reportMissingData);
				} else {
					sendReportWithReportData(reportData, reportServer, notify, reportMissingData);
				}
			}

		};
	}

	private void sendReportWithReportData(Map<String, Object> reportData, String reportServer, boolean notify,
	        boolean reportMissingData) {
		Report.asynchronouslyFrom(reportData, false, db, tr, um, (r) -> {
			if (r != null) {
				sendReportAndImplementMissingData(r, reportServer, notify, reportMissingData);
			}
		});
	}

	private void sendReportAndImplementMissingData(Report r, String reportServer, boolean notify,
	        boolean reportMissingData) {
		ReportUtils.sendReport(r, reportServer, notify, db, vm, BungeeManager.this);
		if (reportMissingData) {
			implementMissingData(r, db);
		}
	}

	private void updateUsersData(String[] parts, int uuidStartIndex) {
		List<UUID> usersUUID = new ArrayList<>();
		for (int i = uuidStartIndex; i < parts.length; i++) {
			try {
				usersUUID.add(UUID.fromString(parts[i]));
			} catch (IllegalArgumentException ignored) {}
		}
		um.updateDataOfUsersWhenPossible(usersUUID, db, tr);
	}

	private void teleportPlayerIfHasTPLocNotification(Player p) {
		TeleportLocationNotification tpLocNotif = playersTeleportLocNotif.get(p.getName());
		if (tpLocNotif != null) {
			teleportPlayerLocallyWhenOnline(p.getName(), tpLocNotif);
		}
	}

	private void teleportPlayerLocallyWhenOnline(String playerName, TeleportLocationNotification tpLocNotif) {
		if (playerName == null || tpLocNotif == null) {
			LOGGER.warn(() -> "teleportWhenPossible(): playerName = " + playerName + ", tpLocNotif = " + tpLocNotif
			        + ", cancelled");
			return;
		}

		if (System.currentTimeMillis() - tpLocNotif.sendTime > NOTIFY_MESSAGE_MAX_DELAY) {
			LOGGER.info(() -> "teleportWhenPossible(" + playerName + "): too old notification, cancelled");
			playersTeleportLocNotif.remove(playerName);
			return;
		}

		Player p = Bukkit.getPlayer(playerName);
		if (p != null) {
			p.teleport(tpLocNotif.loc);
			ConfigSound.TELEPORT.play(p);
			playersTeleportLocNotif.remove(playerName);
		} else {
			playersTeleportLocNotif.put(playerName, tpLocNotif);
		}
	}

	private void implementMissingData(Report r, Database db) {
		Map<String, Object> reportData = new HashMap<>();
		if (ReportUtils.collectAndFillReportedData(r.getReported(), this, reportData)) {
			StringBuilder queryArguments = new StringBuilder();
			List<Object> queryParams = new ArrayList<>();
			for (Entry<String, Object> data : reportData.entrySet()) {
				if (queryArguments.length() > 0) {
					queryArguments.append(",");
				}
				queryArguments.append("`").append(data.getKey()).append("`=?");
				queryParams.add(data.getValue());
			}
			queryParams.add(r.getId());

			String query = "UPDATE tigerreports_reports SET " + queryArguments + " WHERE report_id=?";
			db.updateAsynchronously(query, queryParams);
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

	private void setServerLastNotificationTime(String serverName, long lastNotificationSendTime) {
		KnownServer server = getKnownServer(serverName);
		if (server != null) {
			setServerLastNotificationTime(server, lastNotificationSendTime);
		} else {
			LOGGER.warn(() -> "setOnlineServerLastNotificationTime(): " + serverName + " server is unknown");
		}
	}

	private void setServerLastNotificationTime(KnownServer server, long lastNotificationSendTime) {
		server.setLastNotificationTime(lastNotificationSendTime);
		sendAllUnsentNotificationsOfServer(server);
	}

	private void sendAllUnsentNotificationsOfServer(KnownServer server) {
		if (server.isOnline()) {
			List<Notification> unsentEphemeralNotifications = server.getAndClearUnsentNotifications(true);
			if (unsentEphemeralNotifications != null) {
				LOGGER.info(() -> "sendAllUnsentNotificationsOfServer(" + server
				        + "): server is online, attempt to send all its unsent ephemeral notifications...");
				for (Notification notif : unsentEphemeralNotifications) {
					sendPluginNotificationToServer(server, true, notif);
				}
			} else {
				LOGGER.debug(() -> "sendAllUnsentNotificationsOfServer(" + server
				        + "): server is online but no unsent ephemeral notification to send to it");
			}

			List<Notification> unsentNonEphemeralNotifications = server.getAndClearUnsentNotifications(false);
			if (unsentNonEphemeralNotifications != null) {
				LOGGER.info(() -> "sendAllUnsentNotificationsOfServer(" + server
				        + "): server is online, attempt to send all its unsent non ephemeral notifications...");
				for (Notification notif : unsentNonEphemeralNotifications) {
					sendPluginNotificationToServer(server, false, notif);
				}
			} else {
				LOGGER.debug(() -> "sendAllUnsentNotificationsOfServer(" + server
				        + "): server is online but no unsent non ephemeral notification to send to it");
			}
		}
	}

	public boolean isPlayerOnline(String name) {
		return onlinePlayersServer.containsKey(name);
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

	private void setPlayerOnlineLocally(String playerName, boolean online, String serverName) {
		if (playerName == null || playerName.isEmpty()) {
			throw new IllegalArgumentException("Invalid player name: " + playerName);
		}
		if (online) {
			onlinePlayersServer.put(playerName, serverName);
			LOGGER.debug(() -> "setPlayerOnlineLocally(): " + playerName + " player set as online on " + serverName
			        + " server");
		} else {
			String playerLastKnownServer = onlinePlayersServer.get(playerName);
			if (playerLastKnownServer == null || Objects.equal(playerLastKnownServer, serverName)) {
				// this check prevents old server from removing the new server of the player
				onlinePlayersServer.remove(playerName);
				LOGGER.debug(() -> "setPlayerOnlineLocally(): " + playerName + " player set as offline");
			} else {
				LOGGER.debug(() -> "setPlayerOnlineLocally(" + playerName
				        + ", online = false): ignored because last known server " + playerLastKnownServer + " != "
				        + serverName + " server");
			}
		}
	}

	private void updatePlayerOnlineInNetwork(String playerName, boolean online, UUID playerUUID,
	        String lastMessagesStartDatetime) {
		setPlayerOnlineLocally(playerName, online, localServerName);
		if (Bukkit.getOnlinePlayers().size() >= (online ? 1 : 2)) { // if the notif is for an offline player, that offline player can still be counted in Bukkit.getOnlinePlayers() for short time
			getServerName((localServerName) -> {
				sendPlayerOnlineNotification(playerName, online, localServerName, playerUUID,
				        lastMessagesStartDatetime);
			});
		} else {
			LOGGER.info(
			        () -> "updatePlayerOnlineInNetwork(): no online player, player_online notification will be sent later");
			playerOfflineNotBroadcastedName = playerName;
			playerOfflineNotBroadcastedUUID = playerUUID;
		}
	}

	public void sendNewStatusNotification(String statusDetails, int reportId) {
		sendPluginNotificationToAll(false, statusDetails, NotificationType.NEW_STATUS, reportId);
	}

	public void sendProcessNotification(String staffUUID, String processNotificationType, int reportId, boolean archive,
	        AppreciationDetails appreciationDetails) {
		if (!NotificationType.PROCESS.equals(processNotificationType)
		        && !NotificationType.PROCESS_PUNISH.equals(processNotificationType)) {
			throw new IllegalArgumentException("invalid process notification type: " + processNotificationType);
		}

		sendPluginNotificationToAll(false, staffUUID, processNotificationType, reportId, archive ? "1" : "0",
		        appreciationDetails.toString());
	}

	public void sendProcessAbusiveNotification(String staffUUID, int reportId, boolean archive,
	        AppreciationDetails appreciationDetails, long punishSeconds) {
		sendPluginNotificationToAll(false, staffUUID, NotificationType.PROCESS_ABUSIVE, reportId, archive ? "1" : "0",
		        appreciationDetails.toString(), punishSeconds);
	}

	public void sendArchiveNotification(UUID staffUUID, int reportId) {
		sendPluginNotificationToAll(true, staffUUID, NotificationType.ARCHIVE, reportId);
	}

	public void sendUnarchiveNotification(UUID staffUUID, int reportId) {
		sendPluginNotificationToAll(true, staffUUID, NotificationType.UNARCHIVE, reportId);
	}

	public void sendDeleteNotification(UUID staffUUID, String reportBasicData) {
		sendPluginNotificationToAll(true, staffUUID, NotificationType.DELETE, reportBasicData);
	}

	public void sendPlayerOnlineNotification(String playerName, boolean online, String serverName, UUID playerUUID,
	        String lastMessagesStartDatetime) {
		sendPluginNotificationToAll(true, playerName, NotificationType.PLAYER_ONLINE, online, serverName, playerUUID,
		        (lastMessagesStartDatetime != null ? lastMessagesStartDatetime.replace(" ", "_") : null));
	}

	public void sendPlayerNewImmunityNotification(String immunity, UUID uuid) {
		sendPluginNotificationToAll(true, (immunity != null ? immunity.replace(" ", "_") : "null"),
		        NotificationType.NEW_IMMUNITY, "user", uuid);
	}

	public void sendPlayerNewCooldownNotification(String cooldown, UUID uuid) {
		sendPluginNotificationToAll(true, (cooldown != null ? cooldown.replace(" ", "_") : "null"),
		        NotificationType.NEW_COOLDOWN, "user", uuid);
	}

	public void sendPlayerStopCooldownNotification(UUID staffUUID, UUID targetUUID) {
		sendPluginNotificationToAll(true, staffUUID, NotificationType.STOP_COOLDOWN, "user", targetUUID);
	}

	public void sendPlayerPunishNotification(UUID staffUUID, UUID targetUUID, long seconds) {
		sendPluginNotificationToAll(true, staffUUID, NotificationType.PUNISH, "user", targetUUID, seconds);
	}

	public void sendReportCommentNotification(int reportId, int commentId, String playerName) {
		sendPluginNotificationToAll(true, reportId, NotificationType.COMMENT, commentId, playerName);
	}

	public void sendChangeStatisticNotification(int relativeValue, String statisticName, UUID uuid) {
		sendPluginNotificationToAll(true, relativeValue, NotificationType.CHANGE_STATISTIC, statisticName, uuid);
	}

	public void tpPlayerToOtherServerLocation(String playerName, String serverName, String configLoc) {
		KnownServer server = getKnownServer(serverName);
		if (server == null) {
			LOGGER.info(() -> "sendPluginNotificationToServer(): " + serverName + " server is unknown");
			return;
		}

		setServerLastNotificationTime(server, System.currentTimeMillis()); // make the target server online because it will receive the current player, in order to send the notification
		sendPluginNotificationToServer(server, true, playerName, NotificationType.TP_LOC, configLoc);
		sendBungeeMessage("ConnectOther", playerName, serverName); // sent after in order to keep the player on the current server for sending the previous notification
	}

	public void tpPlayerToPlayerInOtherServer(String playerName, String targetName) {
		sendPluginNotificationToAll(true, playerName, NotificationType.TP_PLAYER, targetName);
	}

	public void sendPlayerLastMessages(String targetServerName, UUID playerUUID, List<User.SavedMessage> lastMessages) {
		if (lastMessages == null || lastMessages.isEmpty()) {
			return;
		}
		sendPluginNotificationToServer(targetServerName, true, playerUUID, NotificationType.PLAYER_LAST_MESSAGES,
		        Report.AdvancedData.formatMessages(lastMessages));
	}

	public void sendUsersDataChangedNotification(String... usersUUID) {
		if (usersUUID == null || usersUUID.length == 0) {
			return;
		}
		sendPluginNotificationToAll(true, "users", NotificationType.DATA_CHANGED, String.join(" ", usersUUID));
	}

	public void destroy() {
		Messenger messenger = tr.getServer().getMessenger();
		messenger.unregisterOutgoingPluginChannel(tr, "BungeeCord");
		messenger.unregisterIncomingPluginChannel(tr, "BungeeCord", this);
		initialized = false;
	}

}
