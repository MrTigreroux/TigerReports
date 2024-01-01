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
import fr.mrtigreroux.tigerreports.bungee.BungeeManager;
import fr.mrtigreroux.tigerreports.bungee.notifications.UsersDataChangedBungeeNotification;
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
import fr.mrtigreroux.tigerreports.utils.LogUtils;
import fr.mrtigreroux.tigerreports.utils.UserUtils;

/**
 * @author MrTigreroux
 */

public class UsersManager {

    private static final Logger LOGGER = Logger.fromClass(UsersManager.class);

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

    private final List<String> exemptedPlayers = new ArrayList<>();

    public UsersManager() {}

    public void addExemptedPlayer(String name) {
        if (name != null && !exemptedPlayers.contains(name)) {
            exemptedPlayers.add(name);
        }
    }

    public void removeExemptedPlayer(String name) {
        exemptedPlayers.remove(name);
    }

    public List<String> getExemptedPlayers() {
        return exemptedPlayers;
    }

    public void processUserConnection(Player p) {
        LOGGER.info(() -> "processUserConnection(" + p.getName() + ")");
        if (!p.isOnline()) {
            LOGGER.info(() -> "processUserConnection(" + p.getName() + "): player is not online, cancelled");
            return;
        }
        updateAndGetOnlineUser(p.getUniqueId(), new OnlineUserData(p));
    }

    public void processUserDisconnection(UUID uuid, VaultManager vm, TaskScheduler taskScheduler) {
        User u = getCachedUser(uuid);
        LOGGER.info(() -> "processUserDisconnection(" + uuid + "): u = " + u);
        if (u == null) {
            return;
        }

        u.setOffline();
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

            if (!u.isOnline() && !u.hasListener() && Math.abs(curDay - u.getLastDayUsed()) >= USERS_CACHE_EXPIRE_DAYS) {
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

    public void getUsersByUniqueIdAsynchronously(String[] uuids, Database db, TaskScheduler taskScheduler,
            ResultCallback<List<User>> resultCallback) {
        if (uuids == null || uuids.length == 0) {
            LOGGER.debug(() -> "getUsersAsynchronously(): uuids = null | empty");
            resultCallback.onResultReceived(null);
            return;
        }

        if (uuids.length == 1) {
            LOGGER.debug(() -> "getUsersAsynchronously(): uuids length = 1");
            getUserByUniqueIdAsynchronously(uuids[0], db, taskScheduler, u -> {
                List<User> result = new ArrayList<>();
                result.add(u);
                resultCallback.onResultReceived(result);
            });
            return;
        }

        LOGGER.debug(() -> "getUsersAsynchronously(): several uuids");
        SeveralTasksHandler<User> usersTaskHandler = new SeveralTasksHandler<>();

        for (String uuid : uuids) {
            getUserByUniqueIdAsynchronously(uuid, db, taskScheduler, usersTaskHandler.newTaskResultSlot());
        }

        usersTaskHandler.whenAllTasksDone(true, resultCallback);
    }

    public void getUserByNameAsynchronously(String name, Database db, TaskScheduler taskScheduler,
            ResultCallback<User> resultCallback) {
        Objects.requireNonNull(name);

        UUID uuid = UserUtils.getOnlinePlayerUniqueId(name);

        if (uuid != null) {
            LOGGER.debug(() -> "getUserByNameAsynchronously(" + name + "): online player uuid = " + uuid);
            getUserByUniqueIdAsynchronously(uuid, db, taskScheduler, resultCallback);
        } else {
            db.queryAsynchronously("SELECT uuid,name FROM tigerreports_users WHERE name LIKE ?",
                    Collections.singletonList(name), taskScheduler, (qr) -> {
                        Map<String, Object> userDataResult = qr.getResult(0);
                        if (userDataResult == null) {
                            LOGGER.debug(() -> "getUserByNameAsynchronously(" + name + "): db result = null");
                            resultCallback.onResultReceived(null);
                            return;
                        }

                        String userUniqueIdStr = (String) userDataResult.get("uuid");
                        if (userUniqueIdStr == null || userUniqueIdStr.isEmpty()) {
                            LOGGER.debug(() -> "getUserByNameAsynchronously(" + name + "): db uuid result = null");
                            resultCallback.onResultReceived(null);
                            return;
                        }
                        UUID userUniqueId = UUID.fromString(userUniqueIdStr);
                        String userName = (String) userDataResult.get("name");
                        LOGGER.debug(() -> "getUserByNameAsynchronously(" + name + "): db result: uuid = "
                                + userUniqueId + ", name = " + userName);

                        VaultManager vm = TigerReports.getInstance().getVaultManager();
                        getOfflineUserAsynchronously(userUniqueId, userName, vm, taskScheduler, resultCallback);
                    });
        }
    }

    public void getUserByUniqueIdAsynchronously(String uuid, Database db, TaskScheduler taskScheduler,
            ResultCallback<User> resultCallback) {
        getUserByUniqueIdAsynchronously(UUID.fromString(uuid), db, taskScheduler, resultCallback);
    }

    public void getUserByUniqueIdAsynchronously(UUID uuid, Database db, TaskScheduler taskScheduler,
            ResultCallback<User> resultCallback) {
        Objects.requireNonNull(uuid);
        User u = getOnlineUser(uuid);
        if (u != null) {
            resultCallback.onResultReceived(u);
        } else { // if user is cached, getOnlineUser() has set it to offline if necessary
            u = getCachedUser(uuid);
            if (u != null) {
                resultCallback.onResultReceived(u);
            } else {
                VaultManager vm = TigerReports.getInstance().getVaultManager();
                getNameAsynchronously(uuid, db, taskScheduler, (name) -> {
                    getOfflineUserAsynchronously(uuid, name, vm, taskScheduler, resultCallback);
                });
            }
        }
    }

    /**
     * Can return an online user if cached.
     * 
     * @param uuid
     * @param name
     * @param vm
     * @param taskScheduler
     * @param resultCallback
     */
    private void getOfflineUserAsynchronously(UUID uuid, String name, VaultManager vm, TaskScheduler taskScheduler,
            ResultCallback<User> resultCallback) {
        if (uuid == null || name == null) {
            LOGGER.debug(() -> "getOfflineUserAsynchronously(" + uuid + ", " + name + "): uuid or name is null");
            resultCallback.onResultReceived(null);
            return;
        }

        vm.getVaultDisplayNameAsynchronously(Bukkit.getOfflinePlayer(uuid), name, taskScheduler, (displayName) -> {
            LOGGER.debug(() -> "getOfflineUserAsynchronously(" + uuid + ", " + name + "): got display name ("
                    + displayName + "), save offline user to cache");
            User u = getOnlineUser(uuid);
            if (u == null) {
                u = updateAndGetUser(uuid, displayName, new OfflineUserData(name));
            }
            resultCallback.onResultReceived(u);
        });
    }

    public User getOnlineUser(String uuid) {
        return getOnlineUser(UUID.fromString(uuid));
    }

    public User getOnlineUser(UUID uuid) {
        return getCachedOnlineUser(uuid);
    }

    public User getOnlineUser(Player p) {
        if (p == null) {
            return null;
        }
        return getCachedOnlineUser(p.getUniqueId());
    }

    public User getCachedOnlineUser(UUID uuid) {
        User u = getCachedUser(uuid);
        if (u != null && u.isOnline()) {
            return u;
        } else {
            return null;
        }
    }

    private User updateAndGetOnlineUser(UUID uuid, OnlineUserData userData) {
        return updateAndGetUser(uuid, null, userData);
    }

    /**
     * The display name of an online player will be retrieved if {@code displayName} is null. But if the player is offline, the display name must be passed to {@code displayName} (null is allowed).
     * 
     * @param uuid
     * @param displayName null for online player, or if unknown by Vault and Bukkit
     * @param userData
     * @return
     */
    private User updateAndGetUser(UUID uuid, String displayName, UserData userData) {
        User u = getCachedUser(uuid);
        if (u == null) {
            LOGGER.info(() -> "updateAndGetUser(" + uuid + "): cached u = null, create new");
            if (displayName == null) {
                VaultManager vm = TigerReports.getInstance().getVaultManager();
                displayName = userData.getDisplayName(vm);
            }
            u = new User(uuid, displayName, userData);
            users.put(uuid, u);
        } else if (!u.hasSameUserDataType(userData)) {
            LOGGER.info(() -> "updateAndGetUser(" + uuid + "): not same user data type, change it");
            u.setUserData(userData);
        } else if (u.hasOnlineUserData()) {
            OnlineUserData oud = (OnlineUserData) userData;
            if (u.getPlayer() != oud.p) {
                LOGGER.info(() -> "updateAndGetUser(" + uuid
                        + "): same online user data type, but different player, change it");
                u.setUserData(userData);
            }
        }

        final User fu = u;
        LOGGER.info(() -> "updateAndGetUser(" + uuid + "): u = " + fu);

        return u;
    }

    public User getCachedUser(UUID uuid) {
        User u = users.get(uuid);
        if (u != null) {
            u.updateLastDayUsed();
        }
        LOGGER.info(() -> "getCachedUser(" + uuid + "): u = " + u);
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
            } else {
                LogUtils.logUnexpectedOfflineUser(LOGGER, "getOnlineUsers()", p);
            }
        }
        return onlineUsers;
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

        User u = getCachedUser(uuid);
        if (u != null) {
            resultCallback.onNameReceived(u.getName());
            return;
        }

        if (p == null) {
            p = Bukkit.getOfflinePlayer(uuid);
        }
        String name = p.getName();

        if (name != null && !name.isEmpty()) {
            LOGGER.debug(() -> "getNameAsynchronously(" + uuid + "): found name (" + name + ") with OfflinePlayer");
            resultCallback.onNameReceived(name);
        } else {
            LOGGER.debug(() -> "getNameAsynchronously(" + uuid
                    + "): did not found name with cache and OfflinePlayer, query db");
            db.queryAsynchronously("SELECT name FROM tigerreports_users WHERE uuid = ?",
                    Collections.singletonList(uuid.toString()), taskScheduler, (qr) -> {
                        String dbName = (String) qr.getResult(0, "name");
                        LOGGER.debug(() -> "getNameAsynchronously(" + uuid + "): found name (" + dbName + ") from db");
                        resultCallback.onNameReceived(dbName);
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
            LOGGER.info(
                    () -> "updateData(): cancelled because there is no user to collect data from, calls freeUsersCacheIfPossible()");
            pendingDataUpdate = false;
            freeUsersCacheIfPossible();
            return false;
        }

        LOGGER.info(() -> "updateData(): start collecting users: " + CollectionUtils.toString(usersUUID));
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
            String cooldown = DatetimeUtils.getRelativeDatetime(seconds);

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
            bm.sendPluginNotificationToAll(new UsersDataChangedBungeeNotification(bm.getNetworkCurrentTime(),
                    (String[]) queryParams.toArray()));
        }
    }

}
