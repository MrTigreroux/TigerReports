package fr.mrtigreroux.tigerreports.objects.reports;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import fr.mrtigreroux.tigerreports.bungee.BungeeManager;
import fr.mrtigreroux.tigerreports.bungee.notifications.ArchiveBungeeNotification;
import fr.mrtigreroux.tigerreports.bungee.notifications.DeleteBungeeNotification;
import fr.mrtigreroux.tigerreports.bungee.notifications.ProcessAbusiveBungeeNotification;
import fr.mrtigreroux.tigerreports.bungee.notifications.ProcessBungeeNotification;
import fr.mrtigreroux.tigerreports.bungee.notifications.ProcessPunishBungeeNotification;
import fr.mrtigreroux.tigerreports.bungee.notifications.StatusBungeeNotification;
import fr.mrtigreroux.tigerreports.bungee.notifications.UnarchiveBungeeNotification;
import fr.mrtigreroux.tigerreports.bungee.notifications.UsersDataChangedBungeeNotification;
import fr.mrtigreroux.tigerreports.data.config.ConfigFile;
import fr.mrtigreroux.tigerreports.data.config.ConfigSound;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.Appreciation;
import fr.mrtigreroux.tigerreports.data.constants.Statistic;
import fr.mrtigreroux.tigerreports.data.constants.Status;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.data.database.QueryResult;
import fr.mrtigreroux.tigerreports.events.ProcessReportEvent;
import fr.mrtigreroux.tigerreports.events.ReportStatusChangeEvent;
import fr.mrtigreroux.tigerreports.logs.Logger;
import fr.mrtigreroux.tigerreports.managers.ReportsManager;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.managers.VaultManager;
import fr.mrtigreroux.tigerreports.objects.Comment;
import fr.mrtigreroux.tigerreports.objects.DeeplyCloneable;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.objects.users.User.SavedMessage;
import fr.mrtigreroux.tigerreports.tasks.ResultCallback;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;
import fr.mrtigreroux.tigerreports.utils.CheckUtils;
import fr.mrtigreroux.tigerreports.utils.CollectionUtils;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.DatetimeUtils;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;
import fr.mrtigreroux.tigerreports.utils.UserUtils;

/**
 * @author MrTigreroux
 */
public class Report implements DeeplyCloneable<Report> {

    private static final Logger LOGGER = Logger.fromClass(Report.class);

    public static final String REPORTERS_SEPARATOR = ",";
    public static final String DATA_SEPARATOR = "##";

    public static final String REPORT_ID = "report_id";
    public static final String STATUS = "status";
    public static final String APPRECIATION = "appreciation";
    public static final String DATE = "date";
    public static final String REASON = "reason";
    public static final String REPORTED_UUID = "reported_uuid";
    public static final String REPORTER_UUID = "reporter_uuid";
    public static final String ARCHIVED = "archived";

    private final int reportId;
    private StatusDetails statusDetails;
    private AppreciationDetails appreciationDetails;
    private String date;
    private final String reason;
    private final User reported;
    private List<User> reporters;
    private AdvancedData advancedData = null;
    private boolean archived;

    /**
     * 
     * @param reportData       containing archived field
     * @param saveAdvancedData
     * @return
     */
    public static void asynchronouslyFrom(Map<String, Object> reportData, boolean saveAdvancedData, Database db,
            TaskScheduler taskScheduler, UsersManager um, ResultCallback<Report> resultCallback) {
        if (reportData == null) {
            LOGGER.info(() -> "asynchronouslyFrom(): reportData = null");
            resultCallback.onResultReceived(null);
            return;
        }

        boolean archived = QueryResult.isTrue(Objects.requireNonNull(reportData.get(ARCHIVED)));

        asynchronouslyFrom(reportData, archived, saveAdvancedData, db, taskScheduler, um, resultCallback);
    }

    public static void asynchronouslyFrom(Map<String, Object> reportData, boolean archived, boolean saveAdvancedData,
            Database db, TaskScheduler taskScheduler, UsersManager um, ResultCallback<Report> resultCallback) {
        if (reportData == null) {
            LOGGER.info(() -> "asynchronouslyFrom(): reportData = null");
            resultCallback.onResultReceived(null);
            return;
        }

        String reportedUUID = (String) reportData.get(REPORTED_UUID);

        LOGGER.debug(() -> "asynchronouslyFrom(): um.getUserAsynchronously(reportedUUID)");
        um.getUserByUniqueIdAsynchronously(reportedUUID, db, taskScheduler, new ResultCallback<User>() {

            @Override
            public void onResultReceived(User reported) {
                LOGGER.debug(() -> "asynchronouslyFrom(): reported = " + reported);
                if (reported == null) {
                    LOGGER.debug(() -> "asynchronouslyFrom(): reported = null, uuid = " + reportedUUID);
                    resultCallback.onResultReceived(null);
                    return;
                }

                String configReporter = (String) reportData.get(REPORTER_UUID);
                String[] reportersUUID = configReporter.split(REPORTERS_SEPARATOR);
                LOGGER.debug(() -> "asynchronouslyFrom(): reportersUUID = " + configReporter);
                um.getUsersByUniqueIdAsynchronously(reportersUUID, db, taskScheduler, new ResultCallback<List<User>>() {

                    @Override
                    public void onResultReceived(List<User> reporters) {
                        LOGGER.debug(() -> "asynchronouslyFrom(): reporters = " + CollectionUtils.toString(reporters));
                        if (reporters == null || reporters.isEmpty()) {
                            LOGGER.debug(() -> "asynchronouslyFrom(): reporters = null | empty");
                            resultCallback.onResultReceived(null);
                            return;
                        }

                        String statusDetails = (String) reportData.get(STATUS);
                        StatusDetails.asynchronouslyFrom(statusDetails, db, taskScheduler, um,
                                new ResultCallback<Report.StatusDetails>() {

                                    @Override
                                    public void onResultReceived(StatusDetails sd) {
                                        LOGGER.debug(() -> "asynchronouslyFrom(): sd = " + sd);
                                        Report r = new Report((int) reportData.get(REPORT_ID), sd,
                                                AppreciationDetails.from((String) reportData.get(APPRECIATION)),
                                                (String) reportData.get(DATE), reported, reporters,
                                                (String) reportData.get(REASON), archived);

                                        if (saveAdvancedData) {
                                            r.extractAndSaveAdvancedData(reportData);
                                        }

                                        LOGGER.debug(() -> "asynchronouslyFrom(): result = " + r);
                                        resultCallback.onResultReceived(r);
                                    }
                                });

                    }

                });

            }

        });
    }

    public Report(int reportId, StatusDetails statusDetails, AppreciationDetails appreciationDetails, String date,
            User reported, List<User> reporters, String reason) {
        this(reportId, statusDetails, appreciationDetails, date, reported, reporters, reason, false);
    }

    public Report(int reportId, StatusDetails statusDetails, AppreciationDetails appreciationDetails, String date,
            User reported, List<User> reporters, String reason, boolean archived) {
        this.reportId = reportId;
        this.statusDetails = Objects.requireNonNull(statusDetails);
        this.appreciationDetails = Objects.requireNonNull(appreciationDetails);
        this.date = date;
        this.reported = Objects.requireNonNull(reported);
        this.reporters = CheckUtils.notEmpty(reporters);
        this.reason = reason;
        this.archived = archived;
    }

    public static class StatusDetails implements DeeplyCloneable<StatusDetails> {

        public static final String STATUS_IN_PROGRESS_SEPARATOR = "-";
        public static final String STATUS_DONE_SEPARATOR = " by ";

        private final Status status;
        /**
         * Can be null, even if status is IN_PROGRESS or DONE.
         */
        private final User staff;

        public static StatusDetails from(Status status, User staff) {
            if (status != Status.IN_PROGRESS && status != Status.DONE) {
                staff = null;
            }
            return new StatusDetails(status, staff);
        }

        public static void asynchronouslyFrom(String statusDetails, Database db, TaskScheduler taskScheduler,
                UsersManager um, ResultCallback<StatusDetails> resultCallback) {
            Status status = Status.from(statusDetails);
            String statusPrefix = null;
            UUID staffUUID = null;
            if (status == Status.IN_PROGRESS) {
                statusPrefix = Status.IN_PROGRESS.getConfigName() + StatusDetails.STATUS_IN_PROGRESS_SEPARATOR;
            } else if (status == Status.DONE) {
                statusPrefix = Status.DONE.getConfigName() + StatusDetails.STATUS_DONE_SEPARATOR;
            }

            if (statusPrefix != null) {
                try {
                    staffUUID = UUID.fromString(statusDetails.replaceFirst(statusPrefix, ""));
                } catch (IllegalArgumentException e) {
                    String fstatusPrefix = statusPrefix;
                    LOGGER.info(() -> "StatusDetails: asynchronouslyFrom(): invalid staff uuid: "
                            + statusDetails.replaceFirst(fstatusPrefix, ""));
                }
            }

            if (staffUUID != null) {
                String fstaffUUID = staffUUID.toString();
                LOGGER.debug(() -> "StatusDetails: asynchronouslyFrom(): staffUUID != null, getUserAsynchronously("
                        + fstaffUUID + ")");
                um.getUserByUniqueIdAsynchronously(staffUUID, db, taskScheduler, new ResultCallback<User>() {

                    @Override
                    public void onResultReceived(User u) {
                        LOGGER.debug(() -> "StatusDetails: asynchronouslyFrom(): staff user received: " + u);
                        resultCallback.onResultReceived(new StatusDetails(status, u));
                    }

                });
            } else {
                LOGGER.debug(() -> "StatusDetails: asynchronouslyFrom(): staffUUID = null");
                resultCallback.onResultReceived(new StatusDetails(status, null));
            }
        }

        private StatusDetails(Status status, User staff) {
            this.status = status;
            this.staff = staff;
        }

        @Override
        public String toString() {
            String configStatus = status.getConfigName();
            if (status == Status.IN_PROGRESS) {
                configStatus += StatusDetails.STATUS_IN_PROGRESS_SEPARATOR;
            } else if (status == Status.DONE) {
                configStatus += StatusDetails.STATUS_DONE_SEPARATOR;
            } else {
                return configStatus;
            }
            return configStatus + (staff != null ? staff.getUniqueId() : "null");
        }

        @Override
        public int hashCode() {
            return Objects.hash(staff, status);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof StatusDetails)) {
                return false;
            }
            StatusDetails other = (StatusDetails) obj;
            return Objects.equals(staff, other.staff) && status == other.status;
        }

        @Override
        public StatusDetails deepClone() {
            return new StatusDetails(status, staff);
        }

    }

    public static class AppreciationDetails implements DeeplyCloneable<AppreciationDetails> {

        public static final String APPRECIATION_PUNISHMENT_SEPARATOR = "/";

        public final Appreciation appreciation;
        public final String punishment;

        public static AppreciationDetails from(Appreciation appreciation, String punishment) {
            if (appreciation != Appreciation.TRUE) {
                punishment = null;
            }
            return new AppreciationDetails(appreciation, punishment);
        }

        public static AppreciationDetails from(String appreciationDetails) {
            if (appreciationDetails == null) {
                return null;
            }

            String[] tokens = appreciationDetails.split(AppreciationDetails.APPRECIATION_PUNISHMENT_SEPARATOR, 2);
            if (tokens.length == 0) {
                return null;
            }
            return new AppreciationDetails(Appreciation.from(tokens[0]), tokens.length >= 2 ? tokens[1] : null);
        }

        private AppreciationDetails(Appreciation appreciation) {
            this(appreciation, null);
        }

        private AppreciationDetails(Appreciation appreciation, String punishment) {
            this.appreciation = Objects.requireNonNull(appreciation);
            this.punishment = punishment;
        }

        @Override
        public String toString() {
            if (punishment == null) {
                return appreciation.toString();
            } else {
                return appreciation + AppreciationDetails.APPRECIATION_PUNISHMENT_SEPARATOR + punishment;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(appreciation, punishment);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof AppreciationDetails)) {
                return false;
            }
            AppreciationDetails other = (AppreciationDetails) obj;
            return appreciation == other.appreciation && Objects.equals(punishment, other.punishment);
        }

        @Override
        public AppreciationDetails deepClone() {
            return new AppreciationDetails(appreciation, punishment);
        }

    }

    public interface ReportListener {
        void onReportDataChange(Report r);

        void onReportDelete(int reportId);
    }

    public int getId() {
        return reportId;
    }

    public String getName() {
        return Message.REPORT_NAME.get().replace("_Id_", Integer.toString(reportId));
    }

    public User getReported() {
        return reported;
    }

    public User getReporter() {
        return reporters.get(0);
    }

    public List<User> getReporters() {
        return reporters;
    }

    public User getLastReporter() {
        return reporters.get(reporters.size() - 1);
    }

    public int getReportersAmount() {
        return reporters.size();
    }

    public UUID getReportedUniqueId() {
        return reported.getUniqueId();
    }

    public UUID getReporterUniqueId() {
        return getReporter().getUniqueId();
    }

    public boolean isStackedReport() {
        return reporters.size() > 1;
    }

    public enum ParticipantType {

        REPORTED("reported", Message.REPORTED_NAME),
        REPORTER("reporter", Message.REPORTER_NAME);

        final String configName;
        final Message nameMsg;

        ParticipantType(String configName, Message name) {
            this.configName = configName;
            nameMsg = name;
        }

        public String getName() {
            return nameMsg.get();
        }

    }

    public String getPlayerName(ParticipantType type, boolean onlineSuffix, boolean color, VaultManager vm,
            BungeeManager bm) {
        return getPlayerName(type == ParticipantType.REPORTED ? reported : getReporter(), type, onlineSuffix, color, vm,
                bm);
    }

    public String getPlayerName(User u, ParticipantType type, boolean onlineSuffix, boolean color, VaultManager vm,
            BungeeManager bm) {
        String name;

        if (color) {
            name = type.getName().replace("_Player_", u.getDisplayName(vm, false));
        } else {
            name = u.getName();
        }

        if (name == null) {
            return Message.NOT_FOUND_MALE.get();
        }

        if (onlineSuffix) {
            name += (u.isOnlineInNetwork(bm) ? Message.ONLINE_SUFFIX : Message.OFFLINE_SUFFIX).get();
        }

        return name;
    }

    public String getReportersNames(int first, boolean onlineSuffix, VaultManager vm, BungeeManager bm) {
        String name = Message.REPORTERS_NAMES.get();
        StringBuilder names = new StringBuilder();
        for (int i = first; i < reporters.size(); i++) {
            User reporter = reporters.get(i);
            names.append(name.replace("_Player_",
                    getPlayerName(reporter, ParticipantType.REPORTER, onlineSuffix, true, vm, bm)));
        }
        return names.toString();
    }

    public String getReportersUUIDStr() {
        StringBuilder uuids = new StringBuilder();
        for (User reporter : reporters) {
            if (uuids.length() > 0) {
                uuids.append(REPORTERS_SEPARATOR);
            }
            uuids.append(reporter.getUniqueId().toString());
        }
        return uuids.toString();
    }

    public List<UUID> getReportersUUID() {
        List<UUID> uuids = new ArrayList<>();
        for (User reporter : reporters) {
            uuids.add(reporter.getUniqueId());
        }
        return uuids;
    }

    public String getDate() {
        return date != null ? date : Message.NOT_FOUND_FEMALE.get();
    }

    public void setStatus(StatusDetails statusDetails, boolean bungee, Database db, ReportsManager rm,
            BungeeManager bm) {
        boolean changed = false;
        changed |= updateStatusDetails(statusDetails);
        boolean isUndone = getStatus() != Status.DONE;
        if (isUndone) {
            changed |= updateAppreciationDetails(AppreciationDetails.from(Appreciation.NONE, null));
        }

        if (changed) {
            rm.broadcastReportDataChanged(this);
        }

        if (!bungee) {
            String configStatus = getStatusDetails();
            bm.sendPluginNotificationToAll(
                    new StatusBungeeNotification(bm.getNetworkCurrentTime(), reportId, configStatus));
            if (isUndone) { // removes any previous appreciation
                db.updateAsynchronously(
                        "UPDATE tigerreports_reports SET status = ?,appreciation = ? WHERE report_id = ?",
                        Arrays.asList(configStatus, getAppreciationDetails(), reportId));
            } else {
                db.updateAsynchronously("UPDATE tigerreports_reports SET status = ? WHERE report_id = ?",
                        Arrays.asList(configStatus, reportId));
            }
        }

        try {
            Bukkit.getServer().getPluginManager().callEvent(new ReportStatusChangeEvent(this, bungee));
        } catch (Exception ignored) {}
    }

    public Status getStatus() {
        return statusDetails.status;
    }

    public String getStatusDetails() {
        return statusDetails.toString();
    }

    public String getStatusWithDetails(VaultManager vm) {
        Status status = getStatus();
        if (status == Status.DONE) {
            Appreciation appreciation = appreciationDetails.appreciation;
            String suffix = appreciation == Appreciation.TRUE
                    ? Message.get("Words.Done-suffix.True-appreciation").replace("_Punishment_", getPunishment())
                    : Message.get("Words.Done-suffix.Other-appreciation")
                            .replace("_Appreciation_", appreciation.getDisplayName());
            String processorName = UserUtils.getStaffDisplayName(getProcessorStaff(), vm);
            return status.getDisplayName(processorName) + suffix;
        } else if (status == Status.IN_PROGRESS) {
            String processingName = UserUtils.getStaffDisplayName(getProcessingStaff(), vm);
            return status.getDisplayName(processingName, true);
        } else {
            return status.getDisplayName(null);
        }
    }

    public String getReason(boolean menu) {
        return reason != null
                ? menu ? MessageUtils.getMenuSentence(reason, Message.REPORT_DETAILS, "_Reason_", true) : reason
                : Message.NOT_FOUND_FEMALE.get();
    }

    public Appreciation getAppreciation() {
        return appreciationDetails.appreciation;
    }

    public String getAppreciationDetails() {
        return appreciationDetails.toString();
    }

    public User getProcessorStaff() {
        return getStatus() == Status.DONE ? statusDetails.staff : null;
    }

    public User getProcessingStaff() {
        return getStatus() == Status.IN_PROGRESS ? statusDetails.staff : null;
    }

    public String getPunishment() {
        return appreciationDetails.punishment != null ? appreciationDetails.punishment : Message.NONE_FEMALE.get();
    }

    public String implementDetails(String message, boolean menu, VaultManager vm, BungeeManager bm) {
        String reportersNames = isStackedReport() ? getReportersNames(0, true, vm, bm)
                : getPlayerName(ParticipantType.REPORTER, true, true, vm, bm);

        return message.replace("_Status_", getStatusWithDetails(vm))
                .replace("_Date_", getDate())
                .replace("_Reporters_", reportersNames)
                .replace("_Reported_", getPlayerName(ParticipantType.REPORTED, true, true, vm, bm))
                .replace("_Reason_", getReason(menu));
    }

    public AdvancedData getAdvancedData() {
        return advancedData;
    }

    public boolean hasAdvancedData() {
        return advancedData != null;
    }

    private void setAdvancedData(AdvancedData advancedData) {
        if (advancedData != null) {
            this.advancedData = advancedData;
        }
    }

    private void clearAdvancedData() {
        advancedData = null;
    }

    void extractAndSaveAdvancedData(Map<String, Object> reportData) {
        if (isArchived()) {
            return;
        }

        setAdvancedData(AdvancedData.fromMap(reportData));
    }

    public static class AdvancedData {

        public static final String MESSAGES_SEPARATOR = "#next#";
        public static final String EFFECTS_SEPARATOR = ",";

        public static final String REPORTER_IP = "reporter_ip";
        public static final String REPORTER_LOCATION = "reporter_location";
        public static final String REPORTER_MESSAGES = "reporter_messages";

        public static final String REPORTED_IP = "reported_ip";
        public static final String REPORTED_LOCATION = "reported_location";
        public static final String REPORTED_MESSAGES = "reported_messages";
        public static final String REPORTED_GAMEMODE = "reported_gamemode";
        public static final String REPORTED_ON_GROUND = "reported_on_ground";
        public static final String REPORTED_SNEAK = "reported_sneak";
        public static final String REPORTED_SPRINT = "reported_sprint";
        public static final String REPORTED_HEALTH = "reported_health";
        public static final String REPORTED_FOOD = "reported_food";
        public static final String REPORTED_EFFECTS = "reported_effects";

        final String reporterIP;
        final String reporterLocation;
        final String reporterMessages;
        final String reportedIP;
        final String reportedLocation;
        final String reportedMessages;
        final String reportedGamemode;
        final boolean reportedOnGround;
        final boolean reportedSneak;
        final boolean reportedSprint;
        final String reportedHealth;
        final String reportedFood;
        final String reportedEffects;

        public static AdvancedData fromMap(Map<String, Object> advancedData) {
            return new AdvancedData(getAdvancedDataAsString(advancedData, REPORTER_IP),
                    getAdvancedDataAsString(advancedData, REPORTER_LOCATION),
                    getAdvancedDataAsString(advancedData, REPORTER_MESSAGES),
                    getAdvancedDataAsString(advancedData, REPORTED_IP),
                    getAdvancedDataAsString(advancedData, REPORTED_LOCATION),
                    getAdvancedDataAsString(advancedData, REPORTED_MESSAGES),
                    getAdvancedDataAsString(advancedData, REPORTED_GAMEMODE),
                    getAdvancedDataAsBoolean(advancedData, REPORTED_ON_GROUND),
                    getAdvancedDataAsBoolean(advancedData, REPORTED_SNEAK),
                    getAdvancedDataAsBoolean(advancedData, REPORTED_SPRINT),
                    getAdvancedDataAsString(advancedData, REPORTED_HEALTH),
                    getAdvancedDataAsString(advancedData, REPORTED_FOOD),
                    getAdvancedDataAsString(advancedData, REPORTED_EFFECTS));
        }

        private static String getAdvancedDataAsString(Map<String, Object> advancedData, String key) {
            Object data = advancedData.get(key);
            return data != null ? data.toString() : null;
        }

        private static boolean getAdvancedDataAsBoolean(Map<String, Object> advancedData, String key) {
            return QueryResult.isTrue(advancedData.get(key));
        }

        public AdvancedData(String reporterIP, String reporterLocation, String reporterMessages, String reportedIP,
                String reportedLocation, String reportedMessages, String reportedGamemode, boolean reportedOnGround,
                boolean reportedSneak, boolean reportedSprint, String reportedHealth, String reportedFood,
                String reportedEffects) {
            this.reporterIP = reporterIP;
            this.reporterLocation = reporterLocation;
            this.reporterMessages = reporterMessages;
            this.reportedIP = reportedIP;
            this.reportedLocation = reportedLocation;
            this.reportedMessages = reportedMessages;
            this.reportedGamemode = reportedGamemode;
            this.reportedOnGround = reportedOnGround;
            this.reportedSneak = reportedSneak;
            this.reportedSprint = reportedSprint;
            this.reportedHealth = reportedHealth;
            this.reportedFood = reportedFood;
            this.reportedEffects = reportedEffects;
        }

        public boolean hasOnlineReportedData() {
            return reportedGamemode != null;
        }

        @Override
        public String toString() {
            return "AdvancedData [reporterIP=" + reporterIP + ", reporterLocation=" + reporterLocation
                    + ", reporterMessages=" + reporterMessages + ", reportedIP=" + reportedIP + ", reportedLocation="
                    + reportedLocation + ", reportedMessages=" + reportedMessages + ", reportedGamemode="
                    + reportedGamemode + ", reportedOnGround=" + reportedOnGround + ", reportedSneak=" + reportedSneak
                    + ", reportedSprint=" + reportedSprint + ", reportedHealth=" + reportedHealth + ", reportedFood="
                    + reportedFood + ", reportedEffects=" + reportedEffects + "]";
        }

        @Override
        public int hashCode() {
            return Objects.hash(reportedEffects, reportedFood, reportedGamemode, reportedHealth, reportedIP,
                    reportedLocation, reportedMessages, reportedOnGround, reportedSneak, reportedSprint, reporterIP,
                    reporterLocation, reporterMessages);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof AdvancedData)) {
                return false;
            }
            AdvancedData other = (AdvancedData) obj;
            return Objects.equals(reportedEffects, other.reportedEffects)
                    && Objects.equals(reportedFood, other.reportedFood)
                    && Objects.equals(reportedGamemode, other.reportedGamemode)
                    && Objects.equals(reportedHealth, other.reportedHealth)
                    && Objects.equals(reportedIP, other.reportedIP)
                    && Objects.equals(reportedLocation, other.reportedLocation)
                    && Objects.equals(reportedMessages, other.reportedMessages)
                    && reportedOnGround == other.reportedOnGround && reportedSneak == other.reportedSneak
                    && reportedSprint == other.reportedSprint && Objects.equals(reporterIP, other.reporterIP)
                    && Objects.equals(reporterLocation, other.reporterLocation)
                    && Objects.equals(reporterMessages, other.reporterMessages);
        }

        public static String serializeConfigEffects(Collection<PotionEffect> effects) {
            StringBuilder configEffects = new StringBuilder();
            for (PotionEffect effect : effects) {
                configEffects.append(effect.getType().getName())
                        .append(":")
                        .append(effect.getAmplifier() + 1)
                        .append("/")
                        .append(effect.getDuration())
                        .append(",");
            }
            int length = configEffects.length();
            return length > 1 ? configEffects.deleteCharAt(length - 1).toString() : null;
        }

        public static String serializeMessages(List<SavedMessage> messages) {
            return messages != null && !messages.isEmpty()
                    ? MessageUtils.joinElements(MESSAGES_SEPARATOR, messages, false)
                    : null;
        }

        public static SavedMessage[] unserializeMessages(String messages) {
            if (messages == null || messages.isEmpty()) {
                return new SavedMessage[0];
            } else {
                String[] tokens = messages.split(MESSAGES_SEPARATOR);
                SavedMessage[] result = new SavedMessage[tokens.length];
                for (int i = 0; i < tokens.length; i++) {
                    result[i] = SavedMessage.from(tokens[i]);
                }
                return result;
            }
        }

        public static String serializeGamemode(GameMode gamemode) {
            return gamemode.toString().toLowerCase();
        }

        public static String unserializeGamemode(String gamemode) {
            if (gamemode == null) {
                return null;
            }

            try {
                return Message.valueOf(gamemode.toUpperCase()).get();
            } catch (Exception invalidGamemode) {
                return gamemode.substring(0, 1).toUpperCase() + gamemode.substring(1).toLowerCase();
            }
        }

    }

    public String implementData(String message, boolean advanced, VaultManager vm, BungeeManager bm) {
        if (advancedData == null) {
            return null;
        }

        String defaultData;
        String reportedAdvancedData = "";
        if (advancedData.hasOnlineReportedData()) {
            String effects;
            String effectsList = advancedData.reportedEffects;
            if (effectsList != null && effectsList.contains(":") && effectsList.contains("/")) {
                StringBuilder effectsLines = new StringBuilder();
                for (String effect : effectsList.split(AdvancedData.EFFECTS_SEPARATOR)) {
                    String type = effect.split(":")[0].replace("_", " ");
                    String duration = effect.split("/")[1];
                    effectsLines.append(Message.EFFECT.get()
                            .replace("_Type_", type.charAt(0) + type.substring(1).toLowerCase())
                            .replace("_Amplifier_", effect.split(":")[1].replace("/" + duration, ""))
                            .replace("_Duration_", Long.toString(Long.parseLong(duration) / 20)));
                }
                effects = effectsLines.toString();
            } else {
                effects = Message.NONE_MALE.get();
            }
            defaultData = Message.DEFAULT_DATA.get()
                    .replace("_Gamemode_", AdvancedData.unserializeGamemode(advancedData.reportedGamemode))
                    .replace("_OnGround_", (advancedData.reportedOnGround ? Message.YES : Message.NO).get())
                    .replace("_Sneak_", (advancedData.reportedSneak ? Message.YES : Message.NO).get())
                    .replace("_Sprint_", (advancedData.reportedSprint ? Message.YES : Message.NO).get())
                    .replace("_Health_", advancedData.reportedHealth)
                    .replace("_Food_", advancedData.reportedFood)
                    .replace("_Effects_", effects);
            reportedAdvancedData = !advanced ? ""
                    : Message.ADVANCED_DATA_REPORTED.get()
                            .replace("_UUID_",
                                    MessageUtils.getMenuSentence(getReportedUniqueId().toString(),
                                            Message.ADVANCED_DATA_REPORTED, "_UUID_", false))
                            .replace("_IP_", advancedData.reportedIP);
        } else {
            defaultData = Message.PLAYER_WAS_OFFLINE.get();
        }

        return message.replace("_Reported_", getPlayerName(ParticipantType.REPORTED, true, true, vm, bm))
                .replace("_DefaultData_", defaultData)
                .replace("_AdvancedData_", !advanced ? ""
                        : reportedAdvancedData + Message.ADVANCED_DATA_REPORTER.get()
                                .replace("_Player_", getPlayerName(ParticipantType.REPORTER, true, true, vm, bm))
                                .replace("_UUID_",
                                        MessageUtils.getMenuSentence(getReporterUniqueId().toString(),
                                                Message.ADVANCED_DATA_REPORTER, "_UUID_", false))
                                .replace("_IP_", advancedData.reporterIP != null ? advancedData.reporterIP
                                        : Message.NOT_FOUND_FEMALE.get()));
    }

    public String getOldLocation(ParticipantType type) {
        if (advancedData == null) {
            return null;
        }
        return type == ParticipantType.REPORTER ? advancedData.reporterLocation : advancedData.reportedLocation;
    }

    public User.SavedMessage[] getMessagesHistory(ParticipantType type) {
        if (advancedData == null) {
            return new User.SavedMessage[0];
        }
        String messages = type == ParticipantType.REPORTER ? advancedData.reporterMessages
                : advancedData.reportedMessages;
        return AdvancedData.unserializeMessages(messages);
    }

    public ItemStack getItem(String actions, VaultManager vm, BungeeManager bm) {
        Status status = getStatus();
        return status.getIcon()
                .amount(getReportersAmount())
                .hideFlags(true)
                .glow(status == Status.WAITING)
                .name(Message.REPORT.get().replace("_Report_", getName()))
                .lore(implementDetails(Message.REPORT_DETAILS.get(), true, vm, bm)
                        .replace("_Actions_", actions != null ? actions : "")
                        .split(ConfigUtils.getLineBreakSymbol()))
                .create();
    }

    public String getText(VaultManager vm, BungeeManager bm) {
        return implementDetails(Message.get("Messages.Report-details")
                .replace("_Report_", Message.REPORT.get().replace("_Report_", getName())), false, vm, bm);
    }

    public boolean isArchived() {
        return archived;
    }

    public void process(User staff, Appreciation appreciation, boolean bungee, boolean autoArchive, boolean notifyStaff,
            Database db, ReportsManager rm, VaultManager vm, BungeeManager bm, TaskScheduler taskScheduler) {
        processing(staff, AppreciationDetails.from(appreciation, null), bungee, autoArchive,
                (autoArchive ? Message.STAFF_PROCESS_AUTO : Message.STAFF_PROCESS).get()
                        .replace("_Appreciation_", appreciation.getDisplayName()),
                notifyStaff, null, db, rm, bm, vm, taskScheduler);
        if (!bungee) {
            bm.sendPluginNotificationToAll(new ProcessBungeeNotification(bm.getNetworkCurrentTime(), reportId,
                    staff.getUniqueId(), autoArchive, appreciation));
        }
    }

    public void processWithPunishment(User staff, boolean bungee, boolean autoArchive, String punishment,
            boolean notifyStaff, Database db, ReportsManager rm, VaultManager vm, BungeeManager bm,
            TaskScheduler taskScheduler) {
        if (punishment == null || punishment.isEmpty()) {
            process(staff, Appreciation.TRUE, bungee, autoArchive, notifyStaff, db, rm, vm, bm, taskScheduler);
            return;
        }

        processing(staff, AppreciationDetails.from(Appreciation.TRUE, punishment), bungee, autoArchive,
                (autoArchive ? Message.STAFF_PROCESS_PUNISH_AUTO : Message.STAFF_PROCESS_PUNISH).get()
                        .replace("_Punishment_", punishment)
                        .replace("_Reported_", getPlayerName(ParticipantType.REPORTED, false, true, vm, bm)),
                notifyStaff, null, db, rm, bm, vm, taskScheduler);
        if (!bungee) {
            bm.sendPluginNotificationToAll(new ProcessPunishBungeeNotification(bm.getNetworkCurrentTime(), reportId,
                    staff.getUniqueId(), autoArchive, punishment));
        }
    }

    public void processAbusive(User staff, boolean bungee, boolean archive, long punishSeconds, boolean notifyStaff,
            Database db, ReportsManager rm, UsersManager um, BungeeManager bm, VaultManager vm,
            TaskScheduler taskScheduler) {
        String time = DatetimeUtils.convertToSentence(punishSeconds);
        processing(staff, new AppreciationDetails(Appreciation.FALSE), bungee, archive,
                Message.STAFF_PROCESS_ABUSIVE.get().replace("_Time_", time), notifyStaff, punishSeconds, db, rm, bm, vm,
                taskScheduler);

        if (!bungee) {
            um.startCooldownForUsers(reporters, punishSeconds, db, bm);
            Player p = staff.getPlayer();
            if (p != null) {
                ConfigUtils.processCommands(ConfigFile.CONFIG.get(), "Config.AbusiveReport.Commands", this, p, vm, bm);
            }

            bm.sendPluginNotificationToAll(new ProcessAbusiveBungeeNotification(bm.getNetworkCurrentTime(), reportId,
                    staff.getUniqueId(), archive, punishSeconds));
        }

        String punishedMsg = Message.PUNISHED.get().replace("_Time_", time);
        for (User reporter : reporters) {
            if (reporter != null) {
                reporter.sendMessage(punishedMsg);
            }
        }
    }

    private void processing(User staff, AppreciationDetails appreciationDetails, boolean bungee, boolean archive,
            String staffMessage, boolean notifyStaff, Long punishSeconds, Database db, ReportsManager rm,
            BungeeManager bm, VaultManager vm, TaskScheduler taskScheduler) {
        Objects.requireNonNull(staff); // Eventually, create a special User CONSOLE in the future, but not null.

        boolean changed = false;
        changed |= updateStatusDetails(StatusDetails.from(Status.DONE, staff));
        changed |= updateAppreciationDetails(appreciationDetails);
        changed |= updateArchived(archive);

        if (changed) {
            rm.broadcastReportDataChanged(this);
        }

        if (notifyStaff) {
            MessageUtils.sendStaffMessage(
                    MessageUtils.getAdvancedMessage(staffMessage.replace("_Player_", staff.getDisplayName(vm, true)),
                            "_Report_", getName(), getText(vm, bm), null),
                    ConfigSound.STAFF.get());
        }

        if (!bungee) {
            db.updateAsynchronously(
                    "UPDATE tigerreports_reports SET status = ?,appreciation = ?,archived = ? WHERE report_id = ?",
                    Arrays.asList(getStatusDetails(), getAppreciationDetails(), archive ? 1 : 0, reportId));

            staff.changeStatistic(Statistic.PROCESSED_REPORTS, 1, db, null);

            String appreciationStatisticConfigName = getAppreciation().getStatisticsName();
            String[] uuidOfChangedStatsUsers = new String[reporters.size() + 1];
            uuidOfChangedStatsUsers[0] = staff.getUniqueId().toString();
            int i = 1;
            for (User reporter : reporters) {
                // TODO: group stat change
                reporter.changeStatistic(appreciationStatisticConfigName, 1, false, db, null);
                uuidOfChangedStatsUsers[i] = reporter.getUniqueId().toString();
                if (ConfigUtils.playersNotifications()) {
                    if (reporter.isOnline()) {
                        reporter.sendReportNotification(this, true, db, vm, bm);
                    } else if (!bm.isPlayerOnline(reporter.getName())) {
                        reporter.addReportNotification(getId(), db, taskScheduler);
                    }
                }
                i++;
            }

            bm.sendPluginNotificationToAll(
                    new UsersDataChangedBungeeNotification(bm.getNetworkCurrentTime(), uuidOfChangedStatsUsers));
        } else {
            if (ConfigUtils.playersNotifications()) {
                for (User reporter : reporters) {
                    if (reporter != null) {
                        reporter.sendReportNotification(this, true, db, vm, bm);
                    }
                }
            }
        }

        Bukkit.getServer().getPluginManager().callEvent(new ProcessReportEvent(Report.this, staff.getName(), bungee));
    }

    public void addComment(User u, String message, Database db, TaskScheduler taskScheduler,
            ResultCallback<Integer> resultCallback) {
        addComment(u.getUniqueId().toString(), message, db, taskScheduler, resultCallback);
    }

    public void addComment(String author, String message, Database db, TaskScheduler taskScheduler,
            ResultCallback<Integer> resultCallback) {
        db.insertAsynchronously(
                "INSERT INTO tigerreports_comments (report_id,status,date,author,message) VALUES (?,?,?,?,?)",
                Arrays.asList(reportId, "Private", DatetimeUtils.getNowDatetime(), author, message), taskScheduler,
                resultCallback);
    }

    public void getCommentByIdAsynchronously(int commentId, Database db, TaskScheduler taskScheduler, UsersManager um,
            ResultCallback<Comment> resultCallback) {
        db.queryAsynchronously("SELECT * FROM tigerreports_comments WHERE report_id = ? AND comment_id = ?",
                Arrays.asList(reportId, commentId), taskScheduler, new ResultCallback<QueryResult>() {

                    @Override
                    public void onResultReceived(QueryResult qr) {
                        getCommentAsynchronouslyFrom(qr.getResult(0), db, taskScheduler, um, resultCallback);
                    }

                });
    }

    public void getCommentAsynchronously(int commentIndex, Database db, TaskScheduler taskScheduler, UsersManager um,
            ResultCallback<Comment> resultCallback) {
        db.queryAsynchronously("SELECT * FROM tigerreports_comments WHERE report_id = ? LIMIT 1 OFFSET ?",
                Arrays.asList(reportId, commentIndex - 1), taskScheduler, new ResultCallback<QueryResult>() {

                    @Override
                    public void onResultReceived(QueryResult qr) {
                        getCommentAsynchronouslyFrom(qr.getResult(0), db, taskScheduler, um,
                                new ResultCallback<Comment>() {

                                    @Override
                                    public void onResultReceived(Comment c) {
                                        resultCallback.onResultReceived(c);
                                    }

                                });
                    }

                });
    }

    public void getCommentAsynchronouslyFrom(Map<String, Object> result, Database db, TaskScheduler taskScheduler,
            UsersManager um, ResultCallback<Comment> resultCallback) {
        if (result == null) {
            resultCallback.onResultReceived(null);
        } else {
            String authorUUID = (String) result.get("author");
            um.getUserByUniqueIdAsynchronously(authorUUID, db, taskScheduler, new ResultCallback<User>() {

                @Override
                public void onResultReceived(User author) {
                    resultCallback.onResultReceived(
                            new Comment(Report.this, (int) result.get("comment_id"), (String) result.get("status"),
                                    (String) result.get("date"), author, (String) result.get("message")));
                }

            });
        }
    }

    public void delete(User staff, boolean bungee, Database db, TaskScheduler taskScheduler, ReportsManager rm,
            VaultManager vm, BungeeManager bm) {
        if (staff != null) {
            MessageUtils.sendStaffMessage(MessageUtils.getAdvancedMessage(
                    (isArchived() ? Message.STAFF_DELETE_ARCHIVE : Message.STAFF_DELETE).get()
                            .replace("_Player_", staff.getDisplayName(vm, true)),
                    "_Report_", getName(), getText(vm, bm), null), ConfigSound.STAFF.get());

            if (!bungee) {
                bm.sendPluginNotificationToAll(new DeleteBungeeNotification(bm.getNetworkCurrentTime(),
                        getBasicDataAsString(), staff.getUniqueId()));
                taskScheduler.runTaskAsynchronously(new Runnable() {

                    @Override
                    public void run() {
                        List<Object> param = Collections.singletonList(reportId);
                        db.update("DELETE FROM tigerreports_reports WHERE report_id = ?", param);
                        db.update("DELETE FROM tigerreports_comments WHERE report_id = ?", param);
                    }

                });
            }
        }

        rm.reportIsDeleted(reportId);
    }

    public void archive(User staff, boolean bungee, Database db, ReportsManager rm, VaultManager vm, BungeeManager bm) {
        updateArchivedWithBroadcast(true, rm);

        if (staff != null) {
            MessageUtils.sendStaffMessage(MessageUtils.getAdvancedMessage(
                    Message.STAFF_ARCHIVE.get().replace("_Player_", staff.getDisplayName(vm, true)), "_Report_",
                    getName(), getText(vm, bm), null), ConfigSound.STAFF.get());
            if (!bungee) {
                bm.sendPluginNotificationToAll(
                        new ArchiveBungeeNotification(bm.getNetworkCurrentTime(), reportId, staff.getUniqueId()));
                db.updateAsynchronously("UPDATE tigerreports_reports SET archived = ? WHERE report_id = ?",
                        Arrays.asList(1, reportId));
            }
        }
    }

    public void unarchive(User staff, boolean bungee, Database db, ReportsManager rm, VaultManager vm,
            BungeeManager bm) {
        updateArchivedWithBroadcast(false, rm);

        if (staff != null) {
            MessageUtils.sendStaffMessage(MessageUtils.getAdvancedMessage(
                    Message.STAFF_RESTORE.get().replace("_Player_", staff.getDisplayName(vm, true)), "_Report_",
                    getName(), getText(vm, bm), null), ConfigSound.STAFF.get());

            if (!bungee) {
                bm.sendPluginNotificationToAll(
                        new UnarchiveBungeeNotification(bm.getNetworkCurrentTime(), reportId, staff.getUniqueId()));
                db.updateAsynchronously("UPDATE tigerreports_reports SET archived = ? WHERE report_id = ?",
                        Arrays.asList(0, reportId));
            }
        }
    }

    public void deleteFromArchives(User staff, boolean bungee, Database db, TaskScheduler taskScheduler,
            ReportsManager rm, VaultManager vm, BungeeManager bm) {
        if (!isArchived()) {
            return;
        }

        delete(staff, bungee, db, taskScheduler, rm, vm, bm);
    }

    private boolean updateStatusDetails(StatusDetails newStatusDetails) {
        if (!Objects.equals(statusDetails, Objects.requireNonNull(newStatusDetails))) {
            statusDetails = newStatusDetails;
            return true;
        } else {
            return false;
        }
    }

    private void updateStatusDetails(String newStatusDetails, Database db, TaskScheduler taskScheduler, UsersManager um,
            ResultCallback<Boolean> resultCallback) {
        if (!Objects.equals(statusDetails.toString(), Objects.requireNonNull(newStatusDetails))) {
            StatusDetails.asynchronouslyFrom(newStatusDetails, db, taskScheduler, um,
                    new ResultCallback<Report.StatusDetails>() {

                        @Override
                        public void onResultReceived(StatusDetails sd) {
                            if (sd != null) {
                                statusDetails = sd;
                                resultCallback.onResultReceived(true);
                            } else {
                                resultCallback.onResultReceived(false);
                            }
                        }

                    });
        } else {
            resultCallback.onResultReceived(false);
        }
    }

    private boolean updateAppreciationDetails(AppreciationDetails newAppreciationDetails) {
        if (!newAppreciationDetails.equals(appreciationDetails)) {
            appreciationDetails = newAppreciationDetails;
            return true;
        } else {
            return false;
        }
    }

    private void updateReportersAndDate(String newReporters, String newDate, Database db, TaskScheduler taskScheduler,
            UsersManager um, ResultCallback<Boolean> resultCallback) {
        String[] newReportersUUID = newReporters.split(REPORTERS_SEPARATOR);
        int newReportersUUIDLength = newReportersUUID.length;

        boolean changed = !Objects.equals(date, newDate);
        changed |= newReportersUUIDLength != reporters.size();
        if (!changed) {
            for (int i = 0; i < reporters.size(); i++) {
                User reporter = reporters.get(i);
                if (i >= newReportersUUIDLength || !reporter.getUniqueId().toString().equals(newReportersUUID[i])) {
                    changed = true;
                    break;
                }
            }
        }

        if (changed) {
            um.getUsersByUniqueIdAsynchronously(newReportersUUID, db, taskScheduler, new ResultCallback<List<User>>() {

                @Override
                public void onResultReceived(List<User> reporters) {
                    if (reporters != null && !reporters.isEmpty()) {
                        Report.this.reporters = reporters;
                        date = newDate;
                        resultCallback.onResultReceived(true);
                    } else {
                        resultCallback.onResultReceived(false);
                    }
                }

            });
        } else {
            resultCallback.onResultReceived(false);
        }
    }

    private boolean updateArchived(boolean newArchived) {
        if (!Objects.equals(archived, newArchived)) {
            archived = newArchived;
            // Advanced data is fixed. It can be present or missing but cannot change. If the report is archived, advanced data is useless and we free
            // memory.
            if (isArchived()) {
                clearAdvancedData();
            }
            return true;
        } else {
            return false;
        }
    }

    private boolean updateArchivedWithBroadcast(boolean newArchived, ReportsManager rm) {
        boolean changed = updateArchived(newArchived);
        if (changed) {
            rm.broadcastReportDataChanged(this);
        }
        return changed;
    }

    @Override
    public int hashCode() {
        return Objects.hash(reportId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Report)) {
            return false;
        }
        Report other = (Report) obj;
        return reportId == other.reportId;
    }

    public void update(Map<String, Object> reportData, boolean saveAdvancedData, Database db,
            TaskScheduler taskScheduler, UsersManager um, ResultCallback<Boolean> resultCallback) {
        boolean archived = false;
        if (reportData != null) {
            archived = QueryResult.isTrue(reportData.get(ARCHIVED));
        }
        update(reportData, archived, saveAdvancedData, db, taskScheduler, um, resultCallback);
    }

    public void update(Map<String, Object> reportData, boolean archived, boolean saveAdvancedData, Database db,
            TaskScheduler taskScheduler, UsersManager um, ResultCallback<Boolean> resultCallback) {
        int reportId = (int) reportData.get(REPORT_ID);
        if (this.reportId != reportId) {
            throw new IllegalArgumentException("Report data concerns another report");
        }

        String newStatusDetails = (String) reportData.get(STATUS);
        updateStatusDetails(newStatusDetails, db, taskScheduler, um, new ResultCallback<Boolean>() {

            @Override
            public void onResultReceived(Boolean changed) {
                String newReporters = (String) reportData.get(REPORTER_UUID);
                updateReportersAndDate(newReporters, (String) reportData.get(DATE), db, taskScheduler, um,
                        new ResultCallback<Boolean>() {

                            @Override
                            public void onResultReceived(Boolean changed2) {
                                changed2 |= changed;

                                changed2 |= updateAppreciationDetails(
                                        AppreciationDetails.from((String) reportData.get(APPRECIATION)));
                                changed2 |= updateArchived(archived);

                                if (saveAdvancedData) {
                                    extractAndSaveAdvancedData(reportData);
                                    // changed2 doesn't take into account advanced data save
                                }

                                if (changed2) {
                                    LOGGER.info(() -> "update(): report changed, new value:" + Report.this);
                                }
                                resultCallback.onResultReceived(changed);
                            }

                        });
            }

        });
    }

    @Override
    public Report deepClone() {
        Report clone = new Report(reportId, statusDetails.deepClone(), appreciationDetails.deepClone(), date, reported,
                new ArrayList<>(reporters), reason, archived);
        clone.setAdvancedData(advancedData);
        return clone;
    }

    @Override
    public String toString() {
        return "Report [reportId=" + reportId + ", statusDetails=" + statusDetails + ", appreciationDetails="
                + appreciationDetails + ", date=" + date + ", reason=" + reason + ", reported=" + reported
                + ", reporters=" + reporters + ", advancedData=" + advancedData + ", archived=" + archived + "]";
    }

    public String getBasicDataAsString() {
        String[] basicData = new String[] { Integer.toString(reportId), getStatusDetails(), getAppreciationDetails(),
                date, reported.getUniqueId().toString(), getReportersUUIDStr(), reason,
                Integer.toString(archived ? 1 : 0) };
        for (int i = 0; i < basicData.length; i++) {
            basicData[i].replace(DATA_SEPARATOR, "");
        }
        return String.join(DATA_SEPARATOR, basicData);
    }

    public static Map<String, Object> parseBasicDataFromString(String dataAsString) {
        if (dataAsString == null || dataAsString.isEmpty()) {
            return null;
        }

        String[] data = dataAsString.split(DATA_SEPARATOR);
        if (data.length < 8) {
            LOGGER.info(() -> "parseBasicDataFromString(" + dataAsString + "): data length < 8");
            return null;
        }

        Map<String, Object> result = new HashMap<>();
        try {
            result.put(REPORT_ID, Integer.parseInt(data[0]));
        } catch (NumberFormatException ex) {
            LOGGER.info(() -> "parseBasicDataFromString(" + dataAsString + "): invalid report id: " + data[0]);
            return null;
        }

        result.put(STATUS, data[1]);
        result.put(APPRECIATION, data[2]);
        result.put(DATE, data[3]);
        result.put(REPORTED_UUID, data[4]);
        result.put(REPORTER_UUID, data[5]);
        result.put(REASON, data[6]);
        try {
            result.put(ARCHIVED, Integer.parseInt(data[7]));
        } catch (NumberFormatException ex) {
            LOGGER.info(() -> "parseBasicDataFromString(" + dataAsString + "): invalid archived value: " + data[7]);
            return null;
        }
        return result;
    }

}
