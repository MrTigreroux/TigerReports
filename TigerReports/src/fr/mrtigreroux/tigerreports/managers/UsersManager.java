package fr.mrtigreroux.tigerreports.managers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.data.database.QueryResult;
import fr.mrtigreroux.tigerreports.logs.Logger;
import fr.mrtigreroux.tigerreports.objects.users.OfflineUserData;
import fr.mrtigreroux.tigerreports.objects.users.OnlineUserData;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.objects.users.UserData;
import fr.mrtigreroux.tigerreports.tasks.ResultCallback;
import fr.mrtigreroux.tigerreports.tasks.SeveralTasksHandler;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;
import fr.mrtigreroux.tigerreports.utils.CollectionUtils;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.DatetimeUtils;

/**
 * @author MrTigreroux
 */

public class UsersManager {

	public static final Logger LOGGER = Logger.fromClass(UsersManager.class);

	private static final byte USERS_CACHE_EXPIRE_DAYS = 2;
	private static final int DATA_UPDATE_COOLDOWN = 5 * 1000; // in ms
	private static final int DATA_UPDATE_MAX_TIME = 5 * 60 * 1000; // in ms

	private final Map<UUID, User> users = new ConcurrentHashMap<>();
	private byte lastUsersCacheCheckDay = 0;
	private final Set<UUID> usersForUpdateData = new HashSet<>();
	private long lastDataUpdateTime = 0;
	private boolean pendingDataUpdate = false;
	private boolean pendingDataUpdateWhenPossible = false;
	private boolean dataUpdateRequested = false;

	private final Map<UUID, String> lastNameFound = new ConcurrentHashMap<>();
	private final Map<String, UUID> lastUniqueIdFound = new ConcurrentHashMap<>();
	private final List<String> exemptedPlayers = new ArrayList<>();

	public UsersManager() {}

	public void addExemptedPlayer(String name) {
		if (name != null && !exemptedPlayers.contains(name))
			exemptedPlayers.add(name);
	}

	public void removeExemptedPlayer(String name) {
		exemptedPlayers.remove(name);
	}

	public List<String> getExemptedPlayers() {
		return exemptedPlayers;
	}

	public void processUserDisconnection(UUID uuid, VaultManager vm) {
		User u = getCachedUser(uuid);
		if (u == null) {
			return;
		}

		u.setUserData(new OfflineUserData(u.getName(), u.getDisplayName(vm)));
	}

	public void freeUsersCacheIfPossible() {
		byte curDay = DatetimeUtils.getCurrentDayOfMonth();
		if (Math.abs(curDay - lastUsersCacheCheckDay) < USERS_CACHE_EXPIRE_DAYS) {
			return;
		}
		lastUsersCacheCheckDay = curDay;

		boolean loggable = LOGGER.isInfoLoggable();
		StringBuilder removedUsers = loggable ? new StringBuilder() : null;

		Iterator<User> usersIt = users.values().iterator();
		while (usersIt.hasNext()) {
			User u = usersIt.next();
			if (!u.hasListener() && Math.abs(curDay - u.getLastDayUsed()) >= USERS_CACHE_EXPIRE_DAYS) {
				// TODO: save it as weakref, try to remove it, gc x2 and then see if it still exists, if so put it back in users because it is used in the app, else it can be removed, nobody uses it.
				u.destroy();
				usersIt.remove();
				if (loggable) {
					removedUsers.append(u.getName()).append(",");
				}
			}
		}
		LOGGER.info(() -> {
			int length = removedUsers.length();
			if (length > 0) {
				removedUsers.setLength(length - 1);
			}
			return "freeUsersCacheIfPossible(): removed users: " + removedUsers;
		});
	}

	public void getUsersAsynchronously(String[] uuids, Database db, TaskScheduler taskScheduler,
	        ResultCallback<List<User>> resultCallback) {
		SeveralTasksHandler<User> usersTaskHandler = new SeveralTasksHandler<>();

		for (String uuid : uuids) {
			getUserAsynchronously(uuid, db, taskScheduler, usersTaskHandler.newTaskResultSlot());
		}

		usersTaskHandler.whenAllTasksDone(true, resultCallback);
	}

	public void getUserAsynchronously(String uuid, Database db, TaskScheduler taskScheduler,
	        ResultCallback<User> resultCallback) {
		getUserAsynchronously(UUID.fromString(uuid), db, taskScheduler, resultCallback);
	}

	public void getUserAsynchronously(UUID uuid, Database db, TaskScheduler taskScheduler,
	        ResultCallback<User> resultCallback) {
		User u = getOnlineUser(uuid);
		if (u != null) {
			resultCallback.onResultReceived(u);
		} else {
			u = getCachedUser(uuid);
			if (u != null && u.hasOfflineUserData()) {
				resultCallback.onResultReceived(u);
			} else {
				VaultManager vm = TigerReports.getInstance().getVaultManager();
				getNameAsynchronously(uuid, db, taskScheduler, new NameResultCallback() {

					@Override
					public void onNameReceived(String name) {
						if (name == null) {
							resultCallback.onResultReceived(null);
							return;
						}

						vm.getVaultDisplayNameAsynchronously(Bukkit.getOfflinePlayer(uuid), taskScheduler,
						        new VaultManager.DisplayNameResultCallback() {

							        @Override
							        public void onDisplayNameReceived(String displayName) {
								        User u = getUser(uuid, new OfflineUserData(name, displayName));

								        resultCallback.onResultReceived(u);
							        }

						        });
					}
				});
			}
		}
	}

	public User getOnlineUser(String uuid) {
		return getOnlineUser(UUID.fromString(uuid));
	}

	public User getOnlineUser(UUID uuid) {
		return getOnlineUser(Bukkit.getPlayer(uuid));
	}

	public User getOnlineUser(Player p) {
		if (p == null) {
			return null;
		}

		return getUser(p.getUniqueId(), new OnlineUserData(p));
	}

	public User getUser(UUID uuid, UserData userData) {
		User u = getCachedUser(uuid);
		if (u == null) {
			u = new User(uuid, userData);
			users.put(uuid, u);
		} else if (!u.hasSameUserDataType(userData)) {
			u.setUserData(userData);
		}

		return u;
	}

	private User getCachedUser(UUID uuid) {
		User u = users.get(uuid);
		if (u != null) {
			u.updateLastDayUsed();
		}
		return u;
	}

	public void removeCachedUser(UUID uuid) {
		LOGGER.info(() -> "removeCachedUser(" + uuid + ")");
		users.remove(uuid);
	}

	public Collection<User> getUsers() {
		return users.values();
	}

	public List<User> getOnlineUsers() {
		List<User> onlineUsers = new ArrayList<>();
		for (Player p : Bukkit.getOnlinePlayers()) {
			User u = getOnlineUser(p);
			if (u != null) {
				onlineUsers.add(u);
			}
		}
		return onlineUsers;
	}

	public UUID getUniqueId(String name) {
		UUID uuid = lastUniqueIdFound.get(name);
		if (uuid == null) {
			@SuppressWarnings("deprecation")
			OfflinePlayer p = Bukkit.getOfflinePlayer(name);
			if (p != null) {
				uuid = p.getUniqueId();
			}
		}
		if (uuid != null) {
			lastUniqueIdFound.put(name, uuid);
			return uuid;
		}
		LOGGER.warn(() -> ConfigUtils.getInfoMessage("The UUID of the name <" + name + "> was not found.",
		        "L'UUID du pseudo <" + name + "> n'a pas ete trouve."));
		return null;
	}

	public interface NameResultCallback {
		void onNameReceived(String name);
	}

	public void getNameAsynchronously(UUID uuid, Database db, TaskScheduler taskScheduler,
	        NameResultCallback resultCallback) {
		getNameAsynchronously(uuid, null, db, taskScheduler, resultCallback);
	}

	public void getNameAsynchronously(UUID uuid, OfflinePlayer p, Database db, TaskScheduler taskScheduler,
	        NameResultCallback resultCallback) {
		Objects.requireNonNull(uuid);

		String name = lastNameFound.get(uuid);
		if (name == null) {
			if (p == null) {
				p = Bukkit.getOfflinePlayer(uuid);
			}
			name = p.getName();
			if (name != null && !name.isEmpty()) {
				lastNameFound.put(uuid, name);
			}
		}

		if (name != null) {
			resultCallback.onNameReceived(name);
		} else {
			db.queryAsynchronously("SELECT name FROM tigerreports_users WHERE uuid = ?",
			        Collections.singletonList(uuid.toString()), taskScheduler, new ResultCallback<QueryResult>() {

				        @Override
				        public void onResultReceived(QueryResult qr) {
					        String name = (String) qr.getResult(0, "name");
					        if (name != null && !name.isEmpty()) {
						        lastNameFound.put(uuid, name);
					        }
					        resultCallback.onNameReceived(name);
				        }

			        });
		}
	}

	public void updateDataOfUserWhenPossible(UUID userUUID, Database db, TaskScheduler taskScheduler) {
		usersForUpdateData.add(userUUID);
		updateDataWhenPossible(db, taskScheduler);
	}

	public void updateDataOfUsersWhenPossible(List<UUID> usersUUID, Database db, TaskScheduler taskScheduler) {
		usersForUpdateData.addAll(usersUUID);
		updateDataWhenPossible(db, taskScheduler);
	}

	public void updateDataWhenPossible(Database db, TaskScheduler taskScheduler) {
		long timeBeforeNextUpdateData = getTimeBeforeNextDataUpdate();
		LOGGER.info(() -> ("updateDataWhenPossible(): timeBeforeNextUpdate = " + timeBeforeNextUpdateData));
		if (timeBeforeNextUpdateData == 0) {
			try {
				updateData(db, taskScheduler);
			} catch (IllegalStateException underCooldown) {
				updateDataWhenPossible(db, taskScheduler);
				LOGGER.info(() -> "updateDataWhenPossible(): calls updateDataWhenPossible()");
			}
		} else if (timeBeforeNextUpdateData == -1) {
			dataUpdateRequested = true;
		} else {
			if (!pendingDataUpdateWhenPossible) {
				pendingDataUpdateWhenPossible = true;
				long now = System.currentTimeMillis();
				taskScheduler.runTaskDelayedly(timeBeforeNextUpdateData, new Runnable() {

					@Override
					public void run() {
						pendingDataUpdateWhenPossible = false;
						LOGGER.info(() -> "updateDataWhenPossible(): calls updateData() (after having waited "
						        + (System.currentTimeMillis() - now) + "ms)");
						try {
							updateData(db, taskScheduler);
						} catch (IllegalStateException underCooldown) {
							updateDataWhenPossible(db, taskScheduler);
						}
					}

				});
			}
		}
	}

	/**
	 * Update data of users.
	 * 
	 * @param db
	 * @param taskScheduler
	 * @return true if update started because it was needed
	 * @throws IllegalStateException if {@link #getTimeBeforeNextDataUpdate} != 0
	 */
	public boolean updateData(Database db, TaskScheduler taskScheduler) throws IllegalStateException {
		LOGGER.info(() -> "updateData()");

		long timeBeforeNextUpdateData = getTimeBeforeNextDataUpdate();
		if (timeBeforeNextUpdateData != 0) {
			LOGGER.info(() -> "updateData(): cancelled because timeBeforeNextUpdateData = " + timeBeforeNextUpdateData);
			throw new IllegalStateException("Data update is under cooldown.");
		}
		lastDataUpdateTime = System.currentTimeMillis();
		pendingDataUpdate = true;

		Set<UUID> usersUUID = usersForUpdateData;
		usersUUID.addAll(getUsersWithListener());
		if (usersUUID == null || usersUUID.isEmpty()) {
			LOGGER.info(() -> "updateData(): cancelled because there is no user, calls freeUsersCacheIfPossible()");
			pendingDataUpdate = false;
			freeUsersCacheIfPossible();
			return false;
		}

		LOGGER.info(() -> "updateData(): start collecting users");
		collectUsersDataAsynchronously(usersUUID, db, taskScheduler, new ResultCallback<QueryResult>() {

			@Override
			public void onResultReceived(QueryResult qr) {
				if (qr != null) {
					List<Map<String, Object>> usersData = qr.getResultList();
					for (Map<String, Object> userData : usersData) {
						String uuid = (String) userData.get("uuid");
						if (uuid == null || uuid.isEmpty()) {
							continue;
						}
						User u = getCachedUser(UUID.fromString(uuid));
						if (u != null) {
							u.update(userData, UsersManager.this);
						}
					}
				}

				LOGGER.info(() -> "updateData(): end (overall time spent: "
				        + (System.currentTimeMillis() - lastDataUpdateTime) + "ms), cached users: "
				        + CollectionUtils.toString(users.keySet()));

				pendingDataUpdate = false;

				if (dataUpdateRequested) {
					dataUpdateRequested = false;
					updateDataWhenPossible(db, taskScheduler);
				} else {
					usersForUpdateData.removeAll(usersUUID);
				}
			}

		});
		return true;
	}

	public Set<UUID> getUsersWithListener() {
		Set<UUID> res = new HashSet<>();
		for (User u : users.values()) {
			if (u.hasListener()) {
				res.add(u.getUniqueId());
			}
		}
		return res;
	}

	/**
	 * Time in milliseconds.
	 * 
	 * @return -1 if undefined, 0 if no cooldown
	 */
	private long getTimeBeforeNextDataUpdate() {
		long now = System.currentTimeMillis();

		long timeSinceLastUpdate = now - lastDataUpdateTime;
		if (pendingDataUpdate) {
			if (timeSinceLastUpdate <= DATA_UPDATE_MAX_TIME) {
				LOGGER.info(() -> " getTimeBeforeNextDataUpdate(): pending update, undefined next data update time");
				return -1;
			} else {
				LOGGER.warn(() -> ConfigUtils.getInfoMessage(
				        "The last data update of users took a lot of time, data updates are now allowed again.",
				        "La derniere mise a jour des donnees des utilisateurs a pris beaucoup de temps, les mises a jour des donnees sont desormais a nouveau possibles."));
			}
		}

		if (timeSinceLastUpdate < DATA_UPDATE_COOLDOWN) {
			LOGGER.info(() -> "getTimeBeforeNextDataUpdate(): cancelled because under cooldown, timeSinceLastUpdate = "
			        + timeSinceLastUpdate);
			return DATA_UPDATE_COOLDOWN - timeSinceLastUpdate;
		}

		return 0;
	}

	private void collectUsersDataAsynchronously(Set<UUID> uuids, Database db, TaskScheduler taskScheduler,
	        ResultCallback<QueryResult> resultCallback) {
		if (uuids == null || uuids.isEmpty()) {
			resultCallback.onResultReceived(null);
			return;
		}

		StringBuilder query = new StringBuilder("SELECT * FROM tigerreports_users WHERE uuid IN (?");
		for (int i = 1; i < uuids.size(); i++) {
			query.append(",?");
		}
		query.append(")");

		List<Object> queryParams = new ArrayList<>();
		for (UUID uuid : uuids) {
			queryParams.add(uuid.toString());
		}
		LOGGER.info(() -> "collectUsersDataAsynchronously(): users: " + CollectionUtils.toString(queryParams));
		db.queryAsynchronously(query.toString(), queryParams, taskScheduler, resultCallback);
	}

	public void startCooldownForUsers(List<User> users, long seconds, Database db, BungeeManager bm) {
		if (users.isEmpty()) {
			return;
		}

		if (users.size() == 1) {
			User u = users.get(0);
			if (u != null) {
				u.startCooldown(seconds, db, bm);
			}
		} else {
			String cooldown = DatetimeUtils.getRelativeDate(seconds);

			StringBuilder query = new StringBuilder("UPDATE tigerreports_users SET cooldown = ? WHERE uuid IN (");
			int size = users.size();
			List<Object> queryParams = new ArrayList<>();
			queryParams.add(cooldown);
			for (int i = 0; i < size; i++) {
				User u = users.get(i);
				if (u != null) {
					if (i == 0) {
						query.append("?");
					} else {
						query.append(",?");
					}
					queryParams.add(u.getUniqueId().toString());
					u.updateCooldown(cooldown);
				}
			}
			query.append(")");

			db.updateAsynchronously(query.toString(), queryParams);

			queryParams.remove(0);
			bm.sendUsersDataChanged((String[]) queryParams.toArray());
		}
	}

}
