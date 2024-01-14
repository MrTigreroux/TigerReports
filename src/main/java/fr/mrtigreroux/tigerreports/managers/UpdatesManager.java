package fr.mrtigreroux.tigerreports.managers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;

import org.bukkit.plugin.Plugin;

import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.data.database.MySQL;
import fr.mrtigreroux.tigerreports.data.database.SQLite;
import fr.mrtigreroux.tigerreports.logs.Logger;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.FileUtils;
import fr.mrtigreroux.tigerreports.utils.VersionUtils;

/**
 * @author MrTigreroux
 */
public class UpdatesManager {
    
    private static final Logger LOGGER = Logger.fromClass(UpdatesManager.class);
    
    public static final String DEFAULT_LAST_USED_VERSION = "0";
    
    private static boolean recreatedReportsTable = false;
    
    private UpdatesManager() {}
    
    public static void updateLastVersionUsed(Plugin plugin) {
        File versionFile = getVersionFile(plugin);
        try {
            FileUtils.setFileLines(
                    versionFile,
                    Arrays.asList(
                            "This file is used for updates, it should not be edited.",
                            plugin.getDescription().getVersion()
                    )
            );
            
            Logger.MAIN.info(() -> "updated version file " + versionFile.getName());
        } catch (IOException | SecurityException e) {
            LOGGER.error(
                    ConfigUtils.getInfoMessage(
                            "An error occurred while trying to update the version file "
                                    + versionFile.getName() + ":",
                            "Une erreur est survenue en essayant de mettre a jour le fichier de version "
                                    + versionFile.getName() + ":"
                    ),
                    e
            );
        }
    }
    
    /**
     * 
     * @param plugin
     * 
     * @return the last version used if version file present and readable, otherwise
     *         {@value #DEFAULT_LAST_USED_VERSION}
     */
    public static String getLastVersionUsed(Plugin plugin) {
        File versionFile = getVersionFile(plugin);
        try {
            List<String> versionFileLines = FileUtils.getFileLines(versionFile);
            if (versionFileLines.size() >= 2) {
                return versionFileLines.get(1);
            }
        } catch (FileNotFoundException e) {
            LOGGER.info(
                    () -> "version file " + versionFile.getName()
                            + " does not exist, last used version is therefore unknown"
            );
        } catch (IOException | SecurityException e) {
            LOGGER.error(
                    ConfigUtils.getInfoMessage(
                            "An error occurred while trying to get the last version used:",
                            "Une erreur est survenue en essayant de recuperer la derniere version utilisee:"
                    ),
                    e
            );
        }
        
        return DEFAULT_LAST_USED_VERSION;
    }
    
    private static File getVersionFile(Plugin plugin) {
        return FileUtils.getPluginDataFile(
                plugin,
                plugin.getDescription().getName().toLowerCase() + ".version"
        );
    }
    
    public static boolean needUpdatesInstructions(Plugin plugin) {
        String version = plugin.getDescription().getVersion();
        String lastVersionUsed = getLastVersionUsed(plugin);
        Logger.MAIN.info(() -> "last version used = " + lastVersionUsed);
        
        if (!version.equals(lastVersionUsed)) {
            try {
                return needUpdatesInstructions(
                        VersionUtils.toInt(lastVersionUsed),
                        VersionUtils.toInt(version)
                );
            } catch (NumberFormatException e) {
                LOGGER.error(
                        "needUpdatesInstructions(): invalid version format: version = " + version
                                + ", lastVersionUsed = " + lastVersionUsed,
                        e
                );
            }
        }
        return false;
    }
    
    private static boolean needUpdatesInstructions(int oldVersion, int newVersion) {
        if (oldVersion >= newVersion) {
            return false;
        }
        
        Map<Integer, BiConsumer<TaskScheduler, Database>> updatesInstructions =
                getUpdatesInstructions();
        
        for (Integer instructionVersion : updatesInstructions.keySet()) {
            if (oldVersion < instructionVersion && instructionVersion <= newVersion) {
                return true;
            }
        }
        
        return false;
    }
    
    public static void runUpdatesInstructions(String oldVersion, Plugin plugin, TaskScheduler ts,
            Database db) {
        try {
            runUpdatesInstructions(
                    VersionUtils.toInt(oldVersion),
                    VersionUtils.toInt(plugin.getDescription().getVersion()),
                    ts,
                    db
            );
        } catch (NumberFormatException e) {
            LOGGER.error(
                    "runUpdatesInstructions(): invalid version format: oldVersion = " + oldVersion
                            + ", current version = " + plugin.getDescription().getVersion(),
                    e
            );
        }
    }
    
    private static void runUpdatesInstructions(int oldVersion, int newVersion, TaskScheduler ts,
            Database db) {
        for (
            Entry<Integer, BiConsumer<TaskScheduler, Database>> entry : getUpdatesInstructions()
                    .entrySet()
        ) {
            Integer instructionVersion = entry.getKey();
            if (oldVersion < instructionVersion && instructionVersion <= newVersion) {
                entry.getValue().accept(ts, db);
            }
        }
    }
    
    private static Map<Integer, BiConsumer<TaskScheduler, Database>> getUpdatesInstructions() {
        Map<Integer, BiConsumer<TaskScheduler, Database>> updatesInstructions =
                new LinkedHashMap<>();
        
        recreatedReportsTable = false;
        updatesInstructions.put(VersionUtils.toInt("5.0.16"), (ts, db) -> {
            if (db instanceof MySQL) {
                ts.runTaskAsynchronously(() -> {
                    db.update(
                            "ALTER TABLE tigerreports_reports MODIFY archived INT(2) NOT NULL DEFAULT 0",
                            null
                    );
                    db.update(
                            "ALTER TABLE tigerreports_reports MODIFY reported_on_ground INT(2)",
                            null
                    );
                    db.update(
                            "ALTER TABLE tigerreports_reports MODIFY reported_sneak INT(2)",
                            null
                    );
                    db.update(
                            "ALTER TABLE tigerreports_reports MODIFY reported_sprint INT(2)",
                            null
                    );
                    db.update(
                            "ALTER TABLE tigerreports_reports MODIFY reporter_ip VARCHAR(46)",
                            null
                    );
                    db.update(
                            "ALTER TABLE tigerreports_reports MODIFY reported_ip VARCHAR(46)",
                            null
                    );
                    db.update(
                            "ALTER TABLE tigerreports_reports MODIFY reporter_location VARCHAR(510)",
                            null
                    );
                    db.update(
                            "ALTER TABLE tigerreports_reports MODIFY reported_location VARCHAR(510)",
                            null
                    );
                });
            } else {
                ts.runTaskAsynchronously(() -> {
                    recreateReportsTable((SQLite) db);
                });
            }
        });
        
        return updatesInstructions;
    }
    
    private static void recreateReportsTable(SQLite db) {
        if (recreatedReportsTable) {
            LOGGER.info(() -> "recreateReportsTable(): already recreated, ignored");
            return;
        }
        
        recreatedReportsTable = true;
        db.executeTransaction(() -> {
            db.update("CREATE TABLE tmp_tigerreports_reports " + SQLite.REPORTS_TABLE, null);
            db.update(
                    "INSERT INTO tmp_tigerreports_reports(" + Database.REPORTS_COLUMNS + ") SELECT "
                            + Database.REPORTS_COLUMNS + " FROM tigerreports_reports",
                    null
            );
            db.update("DROP TABLE tigerreports_reports", null);
            db.update("ALTER TABLE tmp_tigerreports_reports RENAME TO tigerreports_reports", null);
        });
    }
    
}
