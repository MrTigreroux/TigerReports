package fr.mrtigreroux.tigerreports.data.database;

import java.io.File;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.bukkit.Bukkit;

import fr.mrtigreroux.tigerreports.TigerReports;

/**
 * @author MrTigreroux
 */

public class SQLite extends Database {
	
	public SQLite() {}
	
	@Override
	public void openConnection() {
		File dataFolder = new File(TigerReports.getInstance().getDataFolder(), "tigerreports.db");
		if(!dataFolder.exists()) {
			try {
				dataFolder.createNewFile();
			} catch (IOException ex) {
				logError("Failed creation of tigerreports.db file.", ex);
			}
		}
		
		try {
			Class.forName("org.sqlite.JDBC");
			connection = DriverManager.getConnection("jdbc:sqlite:"+dataFolder);
		} catch (ClassNotFoundException missing) {
			logError("SQLite is missing.", null);
		} catch (SQLException ex) {
			logError("Error on connection to database:", ex);
		}
		return;
    }

	@Override
	public void initialize() {
		Bukkit.getScheduler().runTaskAsynchronously(TigerReports.getInstance(), new Runnable() {
			@Override
			public void run() {
				String reportColumns = "('report_id' INTEGER PRIMARY KEY, 'status' varchar(50) NOT NULL DEFAULT 'Waiting', 'appreciation' varchar(10), 'date' varchar(20), 'reported_uuid' char(36), 'signalman_uuid' char(36), 'reason' varchar(150), 'reported_ip' varchar(22), 'reported_location' varchar(50), 'reported_messages' varchar(255), 'reported_gamemode' char(10), 'reported_on_ground' char(5), 'reported_sneak' varchar(5), 'reported_sprint' varchar(5), 'reported_health' varchar(10), 'reported_food' varchar(10), 'reported_effects' varchar(100), 'signalman_ip' varchar(22) NOT NULL, 'signalman_location' varchar(50) NOT NULL, 'signalman_messages' varchar(255));";
				update("CREATE TABLE IF NOT EXISTS users ('uuid' char(36) NOT NULL, 'name' varchar(20), 'cooldown' varchar(20), 'immunity' varchar(20), 'notifications' varchar(255), 'true_appreciations' int(5) DEFAULT '0', 'uncertain_appreciations' int(5) DEFAULT '0', 'false_appreciations' int(5) DEFAULT '0', 'reports' int(5) DEFAULT '0', 'reported_times' int(5) DEFAULT '0', 'processed_reports' int(5) DEFAULT '0');", null);
				update("CREATE TABLE IF NOT EXISTS reports "+reportColumns, null);
				update("CREATE TABLE IF NOT EXISTS comments ('report_id' INTEGER NOT NULL, 'comment_id' INTEGER PRIMARY KEY,'status' varchar(7), 'date' varchar(20), 'author' varchar(32), 'message' varchar(255));", null);			
				update("CREATE TABLE IF NOT EXISTS archived_reports "+reportColumns, null);
			}
		});
	}
	
	@Override
	public boolean isValid() throws SQLException {
		return !connection.isClosed();
	}
	
}
