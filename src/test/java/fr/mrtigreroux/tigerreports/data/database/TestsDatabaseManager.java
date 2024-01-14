package fr.mrtigreroux.tigerreports.data.database;

import java.io.File;

import fr.mrtigreroux.tigerreports.TigerReportsMock;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;

/**
 * @author MrTigreroux
 */
public class TestsDatabaseManager {
    
    private static final String MAIN_DB_NAME = "tigerreports.db";
    
    private static Database mainDb = null;
    
    public static Database getCleanMainDatabase(TaskScheduler taskScheduler) {
        Database db = getMainDatabase(taskScheduler);
        clearDatabaseTables(db);
        ((TestsSQLite) db).resetPendingAsyncUpdatesAmount();
        ((TestsSQLite) db).resetNoAsyncUpdateCallbacks();
        return db;
    }
    
    public static Database getMainDatabase(TaskScheduler taskScheduler) {
        if (mainDb == null) {
            synchronized (TestsDatabaseManager.class) {
                if (mainDb == null) {
                    mainDb = getDatabase(
                            taskScheduler,
                            TigerReportsMock.TESTS_PLUGIN_DATA_FOLDER,
                            MAIN_DB_NAME
                    );
                }
            }
        }
        
        return mainDb;
    }
    
    public static Database getDatabase(TaskScheduler taskScheduler, File dbFolder, String dbName) {
        Database db = new TestsSQLite(taskScheduler, dbFolder, dbName);
        db.initialize();
        
        return db;
    }
    
    public static void clearDatabaseTables(Database db) {
        db.update("DELETE FROM tigerreports_users", null);
        db.update("DELETE FROM tigerreports_reports", null);
        db.update("DELETE FROM tigerreports_comments", null);
    }
    
}
