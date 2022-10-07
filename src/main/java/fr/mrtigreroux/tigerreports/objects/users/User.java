package fr.mrtigreroux.tigerreports.objects.users;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.data.config.ConfigFile;
import fr.mrtigreroux.tigerreports.data.config.ConfigSound;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.Appreciation;
import fr.mrtigreroux.tigerreports.data.constants.Permission;
import fr.mrtigreroux.tigerreports.data.constants.Statistic;
import fr.mrtigreroux.tigerreports.data.constants.Status;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.data.database.QueryResult;
import fr.mrtigreroux.tigerreports.logs.Logger;
import fr.mrtigreroux.tigerreports.managers.BungeeManager;
import fr.mrtigreroux.tigerreports.managers.ReportsManager;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.managers.VaultManager;
import fr.mrtigreroux.tigerreports.objects.Comment;
import fr.mrtigreroux.tigerreports.objects.menus.ArchivedReportsMenu;
import fr.mrtigreroux.tigerreports.objects.menus.CommentsMenu;
import fr.mrtigreroux.tigerreports.objects.menus.ConfirmationMenu;
import fr.mrtigreroux.tigerreports.objects.menus.Menu;
import fr.mrtigreroux.tigerreports.objects.menus.ProcessMenu;
import fr.mrtigreroux.tigerreports.objects.menus.PunishmentMenu;
import fr.mrtigreroux.tigerreports.objects.menus.ReasonMenu;
import fr.mrtigreroux.tigerreports.objects.menus.ReportMenu;
import fr.mrtigreroux.tigerreports.objects.menus.ReportsMenu;
import fr.mrtigreroux.tigerreports.objects.menus.UserAgainstArchivedReportsMenu;
import fr.mrtigreroux.tigerreports.objects.menus.UserAgainstReportsMenu;
import fr.mrtigreroux.tigerreports.objects.menus.UserArchivedReportsMenu;
import fr.mrtigreroux.tigerreports.objects.menus.UserMenu;
import fr.mrtigreroux.tigerreports.objects.menus.UserReportsMenu;
import fr.mrtigreroux.tigerreports.objects.reports.Report;
import fr.mrtigreroux.tigerreports.objects.users.OnlineUserData.PendingProcessPunishingData;
import fr.mrtigreroux.tigerreports.tasks.ResultCallback;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.DatetimeUtils;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;
import fr.mrtigreroux.tigerreports.utils.ReportUtils;
import fr.mrtigreroux.tigerreports.utils.UserUtils;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * @author MrTigreroux
 */
public class User {

	private static final Logger LOGGER = Logger.fromClass(User.class);

	public static final String IMMUNITY_ALWAYS = "always";
	private static final String COOLDOWN_QUERY = "SELECT cooldown FROM tigerreports_users WHERE uuid = ?";
	private static final String NOTIFICATIONS_QUERY = "SELECT notifications FROM tigerreports_users WHERE uuid = ?";
	public static final String NOTIFICATIONS_SEPARATOR = "#next#";
	public static final String COMMENT_NOTIFICATION_DATA_SEPARATOR = ":";

	protected final UUID uuid;
	private UserData data;
	private String immunity = null;
	protected String cooldown = null;
	private Map<String, Integer> statistics = null;
	public List<SavedMessage> lastMessages = new ArrayList<>();
	private final Set<UserListener> listeners = new HashSet<>();

	public User(UUID uuid, UserData data) {
		this.uuid = Objects.requireNonNull(uuid);
		this.data = Objects.requireNonNull(data);
	}

	public interface UserListener {

		void onCooldownChange(User u);

		void onStatisticsChange(User u);

	}

	public boolean addListener(UserListener listener, Database db, TaskScheduler taskScheduler, UsersManager um) {
		LOGGER.info(() -> getName() + ": addListener(" + listener + ")");

		boolean wasEmpty = listeners.isEmpty();
		boolean success = listeners.add(listener);

		if (wasEmpty) { // Data is potentially expired or has never been collected
			LOGGER.info(() -> getName() + ": addListener(" + listener + "): updateDataOfUserWhenPossible(" + getName()
			        + ")");
			um.updateDataOfUserWhenPossible(getUniqueId(), db, taskScheduler);
		}
		return success;
	}

	public void broadcastCooldownChanged() {
		for (UserListener l : listeners) {
			l.onCooldownChange(this);
		}
	}

	public void broadcastStatisticsChanged() {
		for (UserListener l : listeners) {
			l.onStatisticsChange(this);
		}
	}

	public boolean hasListener() {
		return !listeners.isEmpty();
	}

	public boolean removeListener(UserListener listener) {
		LOGGER.info(() -> getName() + ": removeListener(" + listener + ")");
		boolean success = listeners.remove(listener);
		if (listeners.isEmpty()) {
			destroy();
		}

		return success;
	}

	public UUID getUniqueId() {
		return uuid;
	}

	private OnlineUserData getOnlineUserData() {
		LOGGER.info(() -> getName() + ": getOnlineUserData(): data = " + data);
		return data instanceof OnlineUserData ? (OnlineUserData) data : null;
	}

	private OfflineUserData getOfflineUserData() {
		return data instanceof OfflineUserData ? (OfflineUserData) data : null;
	}

	public boolean hasOnlineUserData() {
		return data instanceof OnlineUserData;
	}

	public boolean hasOfflineUserData() {
		return data instanceof OfflineUserData;
	}

	public boolean hasSameUserDataType(UserData otherUserData) {
		LOGGER.info(() -> getName() + ": hasSameUserDataType(" + otherUserData + "): "
		        + data.getClass().equals(otherUserData.getClass()));
		return data.getClass().equals(otherUserData.getClass());
	}

	public void setUserData(UserData data) {
		LOGGER.info(() -> getName() + ": setUserData(" + data + ")");
		this.data = Objects.requireNonNull(data);
	}

	public void checkExistsAsynchronously(Database db, TaskScheduler taskScheduler, UsersManager um,
	        ResultCallback<Boolean> resultCallback) {
		Player p = getPlayer();
		if (p != null) {
			resultCallback.onResultReceived(true);
		} else {
			db.queryAsynchronously("SELECT uuid FROM tigerreports_users WHERE uuid = ?",
			        Collections.singletonList(uuid.toString()), taskScheduler, new ResultCallback<QueryResult>() {

				        @Override
				        public void onResultReceived(QueryResult qr) {
					        Object o = qr.getResult(0, "uuid");
					        boolean exists = o != null;
					        if (!exists) {
						        um.removeCachedUser(uuid);
					        }
					        resultCallback.onResultReceived(exists);
				        }

			        });
		}
	}

	public String getName() {
		return data.getName();
	}

	public String getDisplayName(VaultManager vm) {
		return data.getDisplayName(vm);
	}

	public String getDisplayName(VaultManager vm, boolean staff) {
		return data.getDisplayName(vm, staff);
	}

	public boolean isOnline() {
		OnlineUserData onData = getOnlineUserData();
		return onData != null && onData.p.isOnline();
	}

	public boolean isOnlineInNetwork(BungeeManager bm) {
		return UserUtils.isOnline(getName(), bm);
	}

	public Player getPlayer() {
		OnlineUserData onData = getOnlineUserData();
		LOGGER.info(() -> getName() + ": getPlayer(): onData = " + onData);
		return onData != null ? onData.p : null;
	}

	public boolean hasPermission(Permission perm) {
		Player p = getPlayer();
		LOGGER.info(() -> getName() + ": hasPermission(" + perm.get() + "): p = " + p);
		return p != null && p.hasPermission(perm.get());
	}

	public boolean canArchive(Report r) {
		return hasPermission(Permission.STAFF_ARCHIVE)
		        && (r.getStatus() == Status.DONE || !ReportUtils.onlyDoneArchives());
	}

	public void sendMessage(Object message) {
		Player p = getPlayer();
		if (p == null) {
			return;
		}

		if (message instanceof TextComponent) {
			p.spigot().sendMessage((TextComponent) message);
		} else {
			p.sendMessage((String) message);
		}
	}

	public void sendErrorMessage(String message) {
		LOGGER.info(() -> getName() + ": sendErrorMessage(" + message + ")");
		Player p = getPlayer();
		if (p == null) {
			LOGGER.info(() -> getName() + ": sendErrorMessage(" + message + "): p = null, cannot send error message");
			return;
		}

		p.sendMessage(message);
		ConfigSound.ERROR.play(p);
	}

	public void sendReportNotification(Report r, boolean direct, Database db, VaultManager vm, BungeeManager bm) {
		if (!isOnline()) {
			return;
		}

		if (!direct && r.getStatus() != Status.DONE) {
			return;
		}

		sendMessage(MessageUtils.getAdvancedMessage(
		        Message.REPORT_NOTIFICATION.get()
		                .replace("_Player_", UserUtils.getStaffDisplayName(r.getProcessorStaff(), vm))
		                .replace("_Appreciation_", r.getAppreciation().getDisplayName())
		                .replace("_Time_", DatetimeUtils.getTimeAgo(r.getDate())),
		        "_Report_", r.getName(), r.getText(vm, bm), null));
	}

	public void setImmunity(String immunity, boolean bungee, Database db, BungeeManager bm, UsersManager um) {
		updateImmunity(immunity, um);

		if (!bungee) {
			if (bm != null) {
				bm.sendPlayerNewImmunityNotification(this.immunity, uuid);
			}
			db.updateAsynchronously("UPDATE tigerreports_users SET immunity = ? WHERE uuid = ?",
			        Arrays.asList(this.immunity, uuid.toString()));
		}
	}

	public boolean updateImmunity(String newImmunity, UsersManager um) {
		if (hasPermission(Permission.REPORT_EXEMPT)) {
			newImmunity = IMMUNITY_ALWAYS;
		}

		if (!Objects.equals(immunity, newImmunity)) {
			immunity = newImmunity;

			if (IMMUNITY_ALWAYS.equals(immunity)) {
				um.addExemptedPlayer(getName());
			} else {
				um.removeExemptedPlayer(getName());
			}

			return true;
		} else {
			return false;
		}
	}

	public void startImmunity(boolean bungee, Database db, BungeeManager bm, UsersManager um) {
		setImmunity(DatetimeUtils.getRelativeDatetime(ConfigFile.CONFIG.get().getLong("Config.ReportedImmunity", 120)),
		        bungee, db, bm, um);
	}

	public void getImmunityAsynchronously(Database db, TaskScheduler taskScheduler, UsersManager um, BungeeManager bm,
	        ResultCallback<String> resultCallback) {
		if (hasPermission(Permission.REPORT_EXEMPT)) {
			if (!IMMUNITY_ALWAYS.equals(immunity)) {
				setImmunity(IMMUNITY_ALWAYS, false, db, bm, um);
			}
		} else if (isOnline() && IMMUNITY_ALWAYS.equals(immunity)) {
			setImmunity(null, false, db, bm, um);
		} else {
			if (immunity == null) {
				db.queryAsynchronously("SELECT immunity FROM tigerreports_users WHERE uuid = ?",
				        Collections.singletonList(uuid.toString()), taskScheduler, new ResultCallback<QueryResult>() {

					        @Override
					        public void onResultReceived(QueryResult qr) {
						        updateImmunity((String) qr.getResult(0, "immunity"), um);
						        if (immunity == null) {
							        resultCallback.onResultReceived(null);
						        } else {
							        resultCallback.onResultReceived(getImmunity());
						        }
					        }

				        });
				return;
			}
		}
		resultCallback.onResultReceived(getImmunity());
	}

	public String getImmunity() {
		if (IMMUNITY_ALWAYS.equals(immunity)) {
			return immunity;
		} else {
			double seconds = DatetimeUtils.getSecondsBetweenNowAndDatetime(immunity);
			return seconds > 0 ? DatetimeUtils.convertToSentence(seconds) : null;
		}
	}

	public void setCooldown(String cooldown, boolean bungee, Database db, BungeeManager bm) {
		updateCooldown(cooldown);

		if (!bungee) {
			if (bm != null) {
				bm.sendPlayerNewCooldownNotification(cooldown, uuid);
			}
			db.updateAsynchronously("UPDATE tigerreports_users SET cooldown = ? WHERE uuid = ?",
			        Arrays.asList(cooldown, uuid.toString()));
		}
	}

	public boolean updateCooldown(String newCooldown) {
		if (!Objects.equals(cooldown, newCooldown)) {
			cooldown = newCooldown;

			broadcastCooldownChanged();
			return true;
		} else {
			return false;
		}
	}

	public void startCooldown(long seconds, Database db, BungeeManager bm) {
		setCooldown(DatetimeUtils.getRelativeDatetime(seconds), false, db, bm);
	}

	public void punish(long seconds, User staff, boolean bungee, Database db, BungeeManager bm, VaultManager vm) {
		String time = DatetimeUtils.convertToSentence(seconds);
		if (!bungee) {
			startCooldown(seconds, db, bm);
			if (staff != null) {
				bm.sendPlayerPunishNotification(staff.getUniqueId(), uuid, seconds);
			}
		}
		if (staff != null) {
			MessageUtils.sendStaffMessage(Message.STAFF_PUNISH.get()
			        .replace("_Player_", staff.getDisplayName(vm, true))
			        .replace("_Target_", getDisplayName(vm, false))
			        .replace("_Time_", time), ConfigSound.STAFF.get());
			sendMessage(Message.PUNISHED.get().replace("_Time_", time));
		}
	}

	public void getCooldownAsynchronously(Database db, TaskScheduler taskScheduler,
	        ResultCallback<String> resultCallback) {
		db.queryAsynchronously(COOLDOWN_QUERY, Collections.singletonList(uuid.toString()), taskScheduler,
		        new ResultCallback<QueryResult>() {

			        @Override
			        public void onResultReceived(QueryResult qr) {
				        updateCooldown((String) qr.getResult(0, "cooldown"));
				        resultCallback.onResultReceived(getCooldown());
			        }

		        });
	}

	public String getCooldown() {
		if (cooldown == null) {
			return null;
		}

		double seconds = DatetimeUtils.getSecondsBetweenNowAndDatetime(cooldown);
		return seconds > 0 ? DatetimeUtils.convertToSentence(seconds) : null;
	}

	public void stopCooldown(User staff, boolean bungee, Database db, BungeeManager bm) {
		setCooldown(null, bungee, db, null);

		if (staff != null) {
			VaultManager vm = TigerReports.getInstance().getVaultManager();
			MessageUtils.sendStaffMessage(Message.STAFF_STOPCOOLDOWN.get()
			        .replace("_Player_", staff.getDisplayName(vm, true))
			        .replace("_Target_", getDisplayName(vm, false)), ConfigSound.STAFF.get());
			sendMessage(Message.COOLDOWN_STOPPED.get());

			if (!bungee) {
				bm.sendPlayerStopCooldownNotification(staff.getUniqueId(), uuid);
			}
		}
	}

	public void changeStatistic(Statistic statistic, int relativeValue, Database db, BungeeManager bm) {
		changeStatistic(statistic.getConfigName(), relativeValue, false, db, bm);
	}

	public void changeStatistic(String statisticName, int relativeValue, boolean bungee, Database db,
	        BungeeManager bm) {
		Integer statisticValue = getStatistics(true).get(statisticName);
		if (statisticValue == null) {
			statisticValue = 0;
		}
		updateStatistic(statisticName, statisticValue + relativeValue);

		if (!bungee) {
			if (bm != null) {
				bm.sendChangeStatisticNotification(relativeValue, statisticName, uuid);
			}
			db.updateAsynchronously("UPDATE tigerreports_users SET `" + statisticName + "` = `" + statisticName
			        + "` + ? WHERE uuid = ?", Arrays.asList(relativeValue, uuid.toString()));
		}

	}

	public Map<String, Integer> getStatistics() {
		return getStatistics(false);
	}

	private Map<String, Integer> getStatistics(boolean initialize) {
		if (initialize && statistics == null) {
			statistics = new HashMap<>();
		}
		return statistics;
	}

	public void getStatisticsAsynchronously(boolean useCache, Database db, TaskScheduler taskScheduler,
	        ResultCallback<Map<String, Integer>> resultCallback) {
		if (useCache && statistics != null) {
			resultCallback.onResultReceived(getStatistics(false));
			return;
		}
		db.queryAsynchronously(
		        "SELECT true_appreciations,uncertain_appreciations,false_appreciations,reports,reported_times,processed_reports FROM tigerreports_users WHERE uuid = ?",
		        Collections.singletonList(uuid.toString()), taskScheduler, new ResultCallback<QueryResult>() {

			        @Override
			        public void onResultReceived(QueryResult qr) {
				        updateStatistics(qr.getResult(0));
				        resultCallback.onResultReceived(getStatistics(false));
			        }

		        });
	}

	public boolean updateStatistics(Map<String, Object> newStatistics) {
		boolean changed = false;

		final boolean areStatistics = newStatistics != null;
		for (Statistic statistic : Statistic.values()) {
			String statName = statistic.getConfigName();
			Integer value = null;
			if (areStatistics) {
				value = (Integer) newStatistics.get(statName);
			}
			if (value == null) {
				value = 0;
			}
			Integer previousValue = getStatistics(true).put(statName, value);
			changed |= !Objects.equals(value, previousValue);
		}

		if (changed) {
			broadcastStatisticsChanged();
		}
		return changed;
	}

	public boolean updateStatistic(String statName, int statValue) {
		if (statistics == null || !Objects.equals(statistics.get(statName), statValue)) {
			getStatistics(true).put(statName, statValue);
			broadcastStatisticsChanged();
			return true;
		} else {
			return false;
		}
	}

	public static class SavedMessage {

		private final String datetime;
		private final String message;

		public static SavedMessage from(String savedMessage) {
			int datetimeLength = DatetimeUtils.DATETIME_FORMAT.length();
			if (savedMessage != null && savedMessage.length() >= datetimeLength) {
				return new SavedMessage(savedMessage.substring(0, datetimeLength),
				        savedMessage.substring(datetimeLength + 1));
			} else {
				return null;
			}
		}

		public SavedMessage(String message) {
			this(DatetimeUtils.getNowDatetime(), message);
		}

		private SavedMessage(String datetime, String message) {
			this.datetime = datetime;
			this.message = message;
		}

		public String getDatetime() {
			return datetime;
		}

		public String getMessage() {
			return message;
		}

		@Override
		public String toString() {
			return datetime + ":" + message;
		}

		@Override
		public int hashCode() {
			return Objects.hash(datetime, message);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof SavedMessage)) {
				return false;
			}
			SavedMessage other = (SavedMessage) obj;
			return Objects.equals(datetime, other.datetime) && Objects.equals(message, other.message);
		}

	}

	public void updateLastMessages(String newMessage) {
		updateLastMessages(() -> {
			lastMessages.add(new SavedMessage(DatetimeUtils.getNowDatetime(), newMessage));
		});
	}

	public void updateLastMessages(SavedMessage[] messages) {
		updateLastMessages(() -> {
			for (SavedMessage savedMsg : messages) {
				insertMessageToLastMessages(savedMsg);
			}
		});
	}

	private void updateLastMessages(Runnable actions) {
		int lastMessagesMaxAmount = ReportUtils.getMessagesHistory();
		if (lastMessagesMaxAmount <= 0) {
			return;
		}

		actions.run();

		int lastMsgsToRemoveAmount = lastMessages.size() - lastMessagesMaxAmount;
		if (lastMsgsToRemoveAmount > 0) {
			for (int i = 0; i < lastMsgsToRemoveAmount; i++) {
				lastMessages.remove(0);
			}
		}
	}

	private void insertMessageToLastMessages(SavedMessage savedMessage) {
		ZonedDateTime msgDatetime = DatetimeUtils.getZonedDateTime(savedMessage.getDatetime());
		int i = 0;
		for (SavedMessage lastMsg : lastMessages) {
			if (msgDatetime.isBefore(DatetimeUtils.getZonedDateTime(lastMsg.getDatetime()))) {
				lastMessages.add(i, savedMessage);
				break;
			}
			i++;
		}
		lastMessages.add(savedMessage);
	}

	public List<SavedMessage> getLastMessages() {
		return lastMessages;
	}

	public List<SavedMessage> getLastMessagesAfterDatetime(String datetime) {
		ZonedDateTime startDatetime = DatetimeUtils.getZonedDateTime(datetime);
		if (startDatetime == null) {
			return lastMessages;
		} else {
			return lastMessages.stream()
			        .filter((msg) -> DatetimeUtils.getZonedDateTime(msg.getDatetime()).isAfter(startDatetime))
			        .collect(Collectors.toList());
		}
	}

	/**
	 * 
	 * @return null if any message could be added to current last messages (enough space)
	 */
	public String getLastMessagesMinDatetimeOfInsertableMessages() {
		if (lastMessages.isEmpty()) {
			return null;
		} else {
			return lastMessages.get(0).getDatetime();
		}
	}

	public void openReasonMenu(int page, User tu, Database db, VaultManager vm) {
		if (!isOnline()) {
			return;
		}
		new ReasonMenu(this, page, tu, vm).open(true);
	}

	public void afterProcessingAReport(boolean sound, ReportsManager rm, Database db, TaskScheduler taskScheduler,
	        VaultManager vm, BungeeManager bm, UsersManager um) {
		if (ConfigUtils.isEnabled("Config.CloseMenuAfterReportProcessing")) {
			Player p = getPlayer();
			if (p != null) {
				p.closeInventory();
			}
		} else {
			openReportsMenu(1, sound, rm, db, taskScheduler, vm, bm, um);
		}
	}

	public void openReportsMenu(int page, boolean sound, ReportsManager rm, Database db, TaskScheduler taskScheduler,
	        VaultManager vm, BungeeManager bm, UsersManager um) {
		if (!isOnline()) {
			return;
		}
		new ReportsMenu(this, page, rm, db, taskScheduler, vm, bm, um).open(sound);
	}

	public void openReportMenu(Report r, ReportsManager rm, Database db, TaskScheduler taskScheduler, VaultManager vm,
	        BungeeManager bm, UsersManager um) {
		if (!isOnline()) {
			return;
		}
		new ReportMenu(this, r.getId(), rm, db, taskScheduler, vm, bm, um).setReport(r).open(true);
	}

	public void openReportMenu(int reportId, ReportsManager rm, Database db, TaskScheduler taskScheduler,
	        VaultManager vm, BungeeManager bm, UsersManager um) {
		if (!isOnline()) {
			return;
		}
		new ReportMenu(this, reportId, rm, db, taskScheduler, vm, bm, um).open(true);
	}

	public void openProcessMenu(Report r, ReportsManager rm, Database db, TaskScheduler taskScheduler, VaultManager vm,
	        BungeeManager bm, UsersManager um) {
		if (!isOnline()) {
			return;
		}
		new ProcessMenu(this, r.getId(), rm, db, taskScheduler, vm, bm, um).setReport(r).open(true);
	}

	public void openPunishmentMenu(int page, Report r, ReportsManager rm, Database db, TaskScheduler taskScheduler,
	        VaultManager vm, BungeeManager bm, UsersManager um) {
		Player p = getPlayer();
		if (p == null) {
			return;
		}
		String command = ConfigFile.CONFIG.get().getString("Config.Punishments.PunishmentsCommand");
		if (command != null && !command.equalsIgnoreCase("none")) {
			r.process(this, Appreciation.TRUE, false, this.hasPermission(Permission.STAFF_ARCHIVE_AUTO), true, db, rm,
			        vm, bm, taskScheduler);
			try {
				Bukkit.dispatchCommand(p, command
				        .replace("_Reported_", r.getPlayerName(Report.ParticipantType.REPORTED, false, false, vm, bm))
				        .replace("_Staff_", getName())
				        .replace("_Id_", Integer.toString(r.getId()))
				        .replace("_Reason_", r.getReason(false))
				        .replace("_Reporter_", r.getPlayerName(Report.ParticipantType.REPORTER, false, false, vm, bm)));
				return;
			} catch (Exception ignored) {}
		} else {
			new PunishmentMenu(this, page, r.getId(), rm, db, taskScheduler, vm, bm, um).setReport(r).open(true);
		}
	}

	public void openConfirmationMenu(Report r, ConfirmationMenu.Action action, ReportsManager rm, Database db,
	        TaskScheduler taskScheduler, VaultManager vm, BungeeManager bm, UsersManager um) {
		if (!isOnline()) {
			return;
		}
		new ConfirmationMenu(this, r.getId(), action, rm, db, taskScheduler, vm, bm, um).setReport(r).open(true);
	}

	public void openCommentsMenu(int page, Report r, ReportsManager rm, Database db, TaskScheduler taskScheduler,
	        UsersManager um, BungeeManager bm, VaultManager vm) {
		if (!isOnline()) {
			return;
		}
		new CommentsMenu(this, page, r.getId(), rm, db, taskScheduler, um, bm, vm).setReport(r).open(true);
	}

	public void openArchivedReportsMenu(int page, boolean sound, ReportsManager rm, Database db,
	        TaskScheduler taskScheduler, VaultManager vm, BungeeManager bm, UsersManager um) {
		if (!isOnline()) {
			return;
		}
		new ArchivedReportsMenu(this, page, rm, db, taskScheduler, vm, bm, um).open(sound);
	}

	public void openUserMenu(User tu, ReportsManager rm, Database db, TaskScheduler taskScheduler, VaultManager vm,
	        UsersManager um) {
		if (!isOnline()) {
			return;
		}
		new UserMenu(this, tu, rm, db, taskScheduler, vm, um).open(true);
	}

	public void openUserReportsMenu(User tu, int page, ReportsManager rm, Database db, TaskScheduler taskScheduler,
	        VaultManager vm, BungeeManager bm, UsersManager um) {
		if (!isOnline()) {
			return;
		}
		new UserReportsMenu(this, page, tu, rm, db, taskScheduler, vm, bm, um).open(true);
	}

	public void openUserArchivedReportsMenu(User tu, int page, ReportsManager rm, Database db,
	        TaskScheduler taskScheduler, VaultManager vm, BungeeManager bm, UsersManager um) {
		if (!isOnline()) {
			return;
		}
		new UserArchivedReportsMenu(this, page, tu, rm, db, taskScheduler, vm, bm, um).open(true);
	}

	public void openUserAgainstReportsMenu(User tu, int page, ReportsManager rm, Database db,
	        TaskScheduler taskScheduler, VaultManager vm, BungeeManager bm, UsersManager um) {
		if (!isOnline()) {
			return;
		}
		new UserAgainstReportsMenu(this, page, tu, rm, db, taskScheduler, vm, bm, um).open(true);
	}

	public void openUserAgainstArchivedReportsMenu(User tu, int page, ReportsManager rm, Database db,
	        TaskScheduler taskScheduler, VaultManager vm, BungeeManager bm, UsersManager um) {
		if (!isOnline()) {
			return;
		}
		new UserAgainstArchivedReportsMenu(this, page, tu, rm, db, taskScheduler, vm, bm, um).open(true);
	}

	public void setOpenedMenu(Menu menu) {
		OnlineUserData onData = getOnlineUserData();
		if (onData == null) {
			return;
		}

		if (onData.openedMenu != null) {
			LOGGER.info(() -> getName() + ": setOpenedMenu(): close previous menu " + onData.openedMenu);
			onData.openedMenu.onClose();
		}
		onData.openedMenu = menu;
	}

	public Menu getOpenedMenu() {
		OnlineUserData onData = getOnlineUserData();
		return onData != null ? onData.openedMenu : null;
	}

	public void sendLinesWithReportButton(String[] lines, Report r) {
		Player p = getPlayer();
		if (p == null) {
			return;
		}
		String reportName = r.getName();
		String reportButtonMsg = Message.REPORT_BUTTON.get().replace("_Report_", reportName);
		String alertDetailsMsg = Message.ALERT_DETAILS.get().replace("_Report_", reportName);
		for (String line : lines) {
			sendMessageWithReportButton(line, r, reportButtonMsg, alertDetailsMsg);
		}
		ConfigSound.MENU.play(p);
		p.closeInventory();
	}

	public void sendMessageWithReportButton(String message, Report r) {
		if (!isOnline()) {
			return;
		}
		String reportName = r.getName();
		sendMessageWithReportButton(message, r, Message.REPORT_BUTTON.get().replace("_Report_", reportName),
		        Message.ALERT_DETAILS.get().replace("_Report_", reportName));
	}

	private void sendMessageWithReportButton(String message, Report r, String reportButtonMsg, String alertDetailsMsg) {
		if (!isOnline()) {
			return;
		}
		sendMessage(MessageUtils.getAdvancedMessage(message, "_ReportButton_", reportButtonMsg, alertDetailsMsg,
		        "/tigerreports:reports #" + r.getId()));
	}

	private void sendMessageWithCancelButton(String msgPath, String reportName) {
		if (!isOnline()) {
			return;
		}
		sendMessage(MessageUtils.getAdvancedMessage(Message.get(msgPath + ".Text").replace("_Report_", reportName),
		        "_CancelButton_", Message.get(msgPath + ".Cancel-button.Text"),
		        Message.get(msgPath + ".Cancel-button.Hover").replace("_Report_", reportName),
		        "/tigerreports:reports canceledit"));
	}

	public void setStaffNotifications(boolean state) {
		OnlineUserData onData = getOnlineUserData();
		if (onData == null) {
			return;
		}
		onData.notifications = state;
	}

	public boolean acceptsNotifications() {
		OnlineUserData onData = getOnlineUserData();
		return onData != null && onData.notifications;
	}

	public List<String> getNotifications(Database db) {
		QueryResult qr = db.query(NOTIFICATIONS_QUERY, Collections.singletonList(uuid.toString()));

		return getNotificationsFromQueryResult(qr);
	}

	public void getNotificationsAsynchronously(Database db, TaskScheduler taskScheduler,
	        ResultCallback<List<String>> resultCallback) {
		db.queryAsynchronously(NOTIFICATIONS_QUERY, Collections.singletonList(uuid.toString()), taskScheduler,
		        new ResultCallback<QueryResult>() {

			        @Override
			        public void onResultReceived(QueryResult qr) {
				        resultCallback.onResultReceived(getNotificationsFromQueryResult(qr));
			        }

		        });
	}

	private List<String> getNotificationsFromQueryResult(QueryResult qr) {
		String notifs = (String) qr.getResult(0, "notifications");
		List<String> notifications;
		if (notifs != null) {
			notifications = new ArrayList<>(Arrays.asList(notifs.split(NOTIFICATIONS_SEPARATOR)));
		} else {
			notifications = new ArrayList<>();
		}
		return notifications;
	}

	public void setNotificationsAsynchronously(List<String> notifications, Database db) {
		db.updateAsynchronously("UPDATE tigerreports_users SET notifications = ? WHERE uuid = ?", Arrays.asList(
		        notifications != null ? String.join(NOTIFICATIONS_SEPARATOR, notifications) : null, uuid.toString()));
	}

	public void addReportNotification(int reportId, Database db, TaskScheduler taskScheduler) {
		getNotificationsAsynchronously(db, taskScheduler, new ResultCallback<List<String>>() {

			@Override
			public void onResultReceived(List<String> notifications) {
				notifications.add(Integer.toString(reportId));
				setNotificationsAsynchronously(notifications, db);
			}

		});
	}

	/**
	 * 
	 * @param reportId
	 * @param commentId
	 * @param add       = true to add the notification, false to remove it.
	 */
	public void setCommentNotification(int reportId, int commentId, boolean add, Database db,
	        TaskScheduler taskScheduler) {
		String notification = reportId + COMMENT_NOTIFICATION_DATA_SEPARATOR + commentId;
		getNotificationsAsynchronously(db, taskScheduler, new ResultCallback<List<String>>() {

			@Override
			public void onResultReceived(List<String> notifications) {
				boolean changed = false;
				if (add) {
					changed = notifications.add(notification);
				} else {
					changed = notifications.remove(notification);
				}
				if (changed) {
					setNotificationsAsynchronously(notifications, db);
				}
			}

		});
	}

	public void toggleCommentSentState(Comment c, Database db, TaskScheduler taskScheduler, VaultManager vm,
	        BungeeManager bm) {
		Report r = c.getReport();
		int reportId = r.getId();
		int commentId = c.getId();
		boolean isPrivate = c.getStatus(true).equals("Private");
		if (isPrivate && isOnline()) {
			sendCommentNotification(r, c, true, db, vm, bm);
		} else if (isPrivate && isOnlineInNetwork(bm)) {
			bm.sendReportCommentNotification(reportId, commentId, getName());
		} else {
			ReportsManager rm = TigerReports.getInstance().getReportsManager();
			if (isPrivate) {
				c.setStatus("Sent", db, rm);
				setCommentNotification(reportId, commentId, true, db, taskScheduler);
			} else {
				c.setStatus("Private", db, rm);
				setCommentNotification(reportId, commentId, false, db, taskScheduler);
			}
		}
	}

	// TODO: Less SQL queries
	public void sendNotifications(ReportsManager rm, Database db, TaskScheduler taskScheduler, VaultManager vm,
	        BungeeManager bm, UsersManager um) {
		if (!isOnline()) {
			return;
		}
		getNotificationsAsynchronously(db, taskScheduler, new ResultCallback<List<String>>() {

			@Override
			public void onResultReceived(List<String> notifications) {
				for (String notification : notifications) {
					if (notification != null) {
						if (notification.contains(COMMENT_NOTIFICATION_DATA_SEPARATOR)) {
							String[] parts = notification.split(COMMENT_NOTIFICATION_DATA_SEPARATOR);
							try {
								rm.getReportByIdAsynchronously(Integer.parseInt(parts[0]), false, true, db,
								        taskScheduler, um, new ResultCallback<Report>() {

									        @Override
									        public void onResultReceived(Report r) {
										        r.getCommentByIdAsynchronously(Integer.parseInt(parts[1]), db,
										                taskScheduler, um, new ResultCallback<Comment>() {

											                @Override
											                public void onResultReceived(Comment c) {
												                sendCommentNotification(r, c, false, db, vm, bm);
											                }

										                });
									        }

								        });
							} catch (NumberFormatException invalidNotification) {}
						} else if (ConfigUtils.playersNotifications()) {
							try {
								rm.getReportByIdAsynchronously(Integer.parseInt(notification), false, true, db,
								        taskScheduler, um, new ResultCallback<Report>() {

									        @Override
									        public void onResultReceived(Report r) {
										        sendReportNotification(r, false, db, vm, bm);
									        }

								        });
							} catch (NumberFormatException invalidNotification) {}
						}
					}
				}
			}

		});
		setNotificationsAsynchronously(null, db);
	}

	public void sendCommentNotification(Report r, Comment c, boolean direct, Database db, VaultManager vm,
	        BungeeManager bm) {
		if (r == null || c == null) {
			return;
		}

		Player p = getPlayer();
		if (p == null) {
			return;
		}

		if (!direct && !c.getStatus(true).equals("Sent")) {
			return;
		}
		p.sendMessage(Message.COMMENT_NOTIFICATION.get()
		        .replace("_Player_", c.getAuthorDisplayName(vm))
		        .replace("_Reported_", r.getPlayerName(Report.ParticipantType.REPORTED, false, true, vm, bm))
		        .replace("_Time_", DatetimeUtils.getTimeAgo(r.getDate()))
		        .replace("_Message_", c.getMessage()));
		c.setStatus("Read " + DatetimeUtils.getNowDatetime(), db, TigerReports.getInstance().getReportsManager());
	}

	public void startCreatingComment(Report r) {
		startEditingComment(new Comment(r, null, null, null, null, null));
	}

	public void startEditingComment(Comment c) {
		OnlineUserData onData = getOnlineUserData();
		if (onData == null) {
			return;
		}
		Player p = onData.p;

		if (c.getAuthorUniqueId() != null && !c.getAuthorUniqueId().equals(uuid)) {
			ConfigSound.ERROR.play(p);
			return;
		}

		cancelProcessPunishingWithStaffReason();
		onData.editingComment = c;
		p.closeInventory();
		String reportName = c.getReport().getName();
		sendMessageWithCancelButton(Message.EDIT_COMMENT.getPath(), reportName);
	}

	public void cancelEditingComment() {
		OnlineUserData onData = getOnlineUserData();
		if (onData == null) {
			return;
		}
		if (onData.editingComment == null) {
			return;
		}

		Report r = onData.editingComment.getReport();
		sendMessageWithReportButton(Message.CANCEL_COMMENT.get().replace("_Report_", r.getName()), r);
		onData.editingComment = null;
	}

	public boolean isEditingComment() {
		OnlineUserData onData = getOnlineUserData();
		return onData != null && onData.editingComment != null;
	}

	public void terminateEditingComment(String commentMessage, ReportsManager rm, Database db,
	        TaskScheduler taskScheduler, UsersManager um, BungeeManager bm, VaultManager vm) {
		OnlineUserData onData = getOnlineUserData();
		if (onData == null) {
			return;
		}
		if (onData.editingComment == null) {
			return;
		}

		Report r = onData.editingComment.getReport();
		if (onData.editingComment.getId() == null) {
			r.addComment(this, commentMessage, db, taskScheduler, new ResultCallback<Integer>() {

				@Override
				public void onResultReceived(Integer commentId) {
					LOGGER.info(() -> getName() + ": terminateEditingComment(): rm.updateDataWhenPossible()");
					rm.updateDataWhenPossible(db, taskScheduler, um);
				}

			});

		} else {
			onData.editingComment.addMessage(commentMessage, db, rm);
		}
		onData.editingComment = null;
		openCommentsMenu(1, r, rm, db, taskScheduler, um, bm, vm);
	}

	public void teleportToReportParticipant(Report r, Report.ParticipantType targetType, boolean currentLocation,
	        VaultManager vm, BungeeManager bm) {
		Player p = getPlayer();
		if (p == null) {
			return;
		}

		String target = r.getPlayerName(targetType, false, false, vm, bm);
		Player t = Bukkit.getPlayer(target);
		String locType;
		String serverName = null;
		Location loc = null;
		String configLoc = null;
		boolean tpToOnlineTargetInDifferentServer = false;

		if (currentLocation) {
			if (t == null) {
				if (bm.isPlayerOnline(target)) {
					tpToOnlineTargetInDifferentServer = true;
				} else {
					MessageUtils.sendErrorMessage(p, Message.PLAYER_OFFLINE.get().replace("_Player_", target));
					return;
				}
			} else {
				serverName = "localhost";
				loc = t.getLocation();
			}
			locType = "CURRENT";
		} else {
			configLoc = r.getOldLocation(targetType);
			loc = MessageUtils.unformatLocation(configLoc);
			if (loc == null) {
				MessageUtils.sendErrorMessage(p, Message.LOCATION_UNKNOWN.get().replace("_Player_", target));
				return;
			}
			serverName = MessageUtils.getServer(configLoc);
			locType = "OLD";
		}

		ConfigUtils.processCommands(ConfigFile.CONFIG.get(), "Config.AutoCommandsBeforeTeleportation", r, p, vm, bm);

		sendMessageWithReportButton(Message.valueOf("TELEPORT_" + locType + "_LOCATION")
		        .get()
		        .replace("_Player_", targetType.getName().replace("_Player_", target))
		        .replace("_Report_", r.getName()), r);

		if (tpToOnlineTargetInDifferentServer) {
			bm.tpPlayerToPlayerInOtherServer(p.getName(), target);
		} else if (serverName.equals("localhost") || bm.getServerName().equals(serverName)) {
			p.teleport(loc);
			ConfigSound.TELEPORT.play(p);
		} else if (configLoc != null) {
			bm.tpPlayerToOtherServerLocation(p.getName(), serverName, configLoc);
		}
	}

	public void processPunishing(Report r, String punishmentConfigPath, FileConfiguration configFile, ReportsManager rm,
	        Database db, TaskScheduler taskScheduler, VaultManager vm, BungeeManager bm) {
		processPunishing(r, null, punishmentConfigPath, configFile, rm, db, taskScheduler, vm, bm);
	}

	public void processPunishing(Report r, String staffReason, String punishmentConfigPath,
	        FileConfiguration configFile, ReportsManager rm, Database db, TaskScheduler taskScheduler, VaultManager vm,
	        BungeeManager bm) {
		Player p = getPlayer();
		if (p == null) {
			return;
		}

		if (!checkAccessToReport(r)) {
			return;
		}

		ConfigUtils.processCommands(configFile, punishmentConfigPath + ".PunishCommands", r, p, staffReason, vm, bm);

		String punishmentName = configFile.getString(punishmentConfigPath + ".Name");
		if (staffReason != null) {
			punishmentName = punishmentName.replace("_StaffReason_", staffReason);
		}
		afterProcessingAReport(false, rm, db, taskScheduler, vm, bm, TigerReports.getInstance().getUsersManager());
		r.processWithPunishment(this, false, this.hasPermission(Permission.STAFF_ARCHIVE_AUTO), punishmentName, true,
		        db, rm, vm, bm, taskScheduler);
	}

	public void startProcessPunishingWithStaffReason(Report r, String punishmentConfigPath) {
		OnlineUserData onData = getOnlineUserData();
		if (onData == null) {
			return;
		}
		cancelEditingComment();
		onData.pendingProcessPunishingData = new PendingProcessPunishingData(r, punishmentConfigPath);
		onData.p.closeInventory();
		String reportName = r.getName();
		sendMessageWithCancelButton(Message.PROCESS_REPORT_PUNISHING_WITH_STAFF_REASON.getPath(), reportName);
	}

	public void cancelProcessPunishingWithStaffReason() {
		OnlineUserData onData = getOnlineUserData();
		if (onData == null) {
			return;
		}
		if (onData.pendingProcessPunishingData == null) {
			return;
		}

		Report r = onData.pendingProcessPunishingData.r;
		sendMessageWithReportButton(
		        Message.CANCEL_PROCESS_REPORT_PUNISHING_WITH_STAFF_REASON.get().replace("_Report_", r.getName()), r);
		onData.pendingProcessPunishingData = null;
	}

	public boolean isProcessPunishingWithStaffReason() {
		OnlineUserData onData = getOnlineUserData();
		return onData != null && onData.pendingProcessPunishingData != null;
	}

	public void terminateProcessPunishingWithStaffReason(String staffReason, ReportsManager rm, Database db,
	        TaskScheduler taskScheduler, VaultManager vm, BungeeManager bm) {
		OnlineUserData onData = getOnlineUserData();
		if (onData == null) {
			return;
		}
		if (staffReason == null || onData.pendingProcessPunishingData == null) {
			return;
		}

		processPunishing(onData.pendingProcessPunishingData.r, staffReason,
		        onData.pendingProcessPunishingData.punishmentConfigPath, ConfigFile.CONFIG.get(), rm, db, taskScheduler,
		        vm, bm);
		onData.pendingProcessPunishingData = null;
	}

	public boolean checkAccessToReport(Report r) {
		if (!canAccessToReport(r, false)) {
			sendErrorMessage(Message.PERMISSION_ACCESS_DETAILS.get().replace("_Report_", r.getName()));
			return false;
		} else {
			return true;
		}
	}

	/**
	 * 
	 * @param r                     != null
	 * @param allowAccessIfArchived if false, archived reports are considered not accessible (must be restored to be accessed).
	 * @return
	 */
	public boolean canAccessToReport(Report r, boolean allowAccessIfArchived) {
		if (!isOnline()) {
			return false;
		}

		if (!allowAccessIfArchived && r.isArchived()) {
			return false;
		}

		Status reportStatus = r.getStatus();
		boolean reportIsManagedByAnotherStaff = reportStatus == Status.IN_PROGRESS && r.getProcessingStaff() != null
		        && !uuid.equals(r.getProcessingStaff().getUniqueId());
		if ((reportIsManagedByAnotherStaff || reportStatus == Status.IMPORTANT || reportStatus == Status.DONE)
		        && !hasPermission(Permission.STAFF_ADVANCED)) {
			return false;
		} else {
			return true;
		}
	}

	public String getIPAddress() {
		Player p = getPlayer();
		if (p == null) {
			return null;
		}
		InetSocketAddress address = p.getAddress();
		if (address != null) {
			InetAddress inetAddress = address.getAddress();
			if (inetAddress != null) {
				return inetAddress.getHostAddress();
			}
		}
		return null;
	}

	public void updateBasicData(Database db, BungeeManager bm, UsersManager um) {
		db.updateUserName(getUniqueId().toString(), getName());
		setImmunity(hasPermission(Permission.REPORT_EXEMPT) ? User.IMMUNITY_ALWAYS : null, false, db, bm, um);
	}

	public void update(Map<String, Object> userData, UsersManager um) {
		updateImmunity((String) userData.get("immunity"), um);
		updateCooldown((String) userData.get("cooldown"));
		updateStatistics(userData);
	}

	public byte getLastDayUsed() {
		OfflineUserData offlineUserData = getOfflineUserData();
		return offlineUserData != null ? offlineUserData.lastDayUsed : -1;
	}

	public void updateLastDayUsed() {
		OfflineUserData offlineUserData = getOfflineUserData();
		if (offlineUserData != null) {
			offlineUserData.lastDayUsed = DatetimeUtils.getCurrentDayOfMonth();
		}
	}

	public void destroy() {
		LOGGER.info(() -> getName() + ": destroy(): clear listeners");
		listeners.clear();
	}

}
