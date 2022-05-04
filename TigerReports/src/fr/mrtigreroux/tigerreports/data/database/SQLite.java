package fr.mrtigreroux.tigerreports.data.database;

import java.io.File;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;

import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;

/**
 * @author MrTigreroux
 */

public class SQLite extends Database {

	private final File databaseFile;

	public SQLite(TaskScheduler taskScheduler, File databaseFolder, String databaseFileName) {
		super(taskScheduler);
		this.databaseFile = new File(databaseFolder, databaseFileName);
	}

	@Override
	public void openConnection() throws Exception {
		if (!databaseFile.exists()) {
			try {
				databaseFile.createNewFile();
			} catch (IOException ex) {
				logError(ConfigUtils.getInfoMessage("Failed creation of " + databaseFile.getName() + " file.",
				        "La creation du fichier " + databaseFile.getName() + " a echoue."), ex);
				throw ex;
			}
		}

		try {
			Class.forName("org.sqlite.JDBC");
			connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile);
		} catch (ClassNotFoundException missing) {
			logError(ConfigUtils.getInfoMessage("SQLite is missing.", "SQLite n'est pas installe."), null);
			throw missing;
		} catch (SQLException ex) {
			logError(ConfigUtils.getInfoMessage("An error has occurred during the connection to the SQLite database:",
			        "Une erreur s'est produite lors de la connexion a la base de donnees SQLite:"), ex);
			throw ex;
		}
		return;
	}

	@Override
	public void initialize() {
		taskScheduler.runTaskAsynchronously(new Runnable() {

			@Override
			public void run() {
				update("CREATE TABLE IF NOT EXISTS tigerreports_users ('uuid' text NOT NULL PRIMARY KEY, 'name' text, 'cooldown' text, 'immunity' text, 'notifications' text, 'true_appreciations' int DEFAULT '0', 'uncertain_appreciations' int DEFAULT '0', 'false_appreciations' int DEFAULT '0', 'reports' int DEFAULT '0', 'reported_times' int DEFAULT '0', 'processed_reports' int DEFAULT '0')",
				        null);
				update("CREATE TABLE IF NOT EXISTS tigerreports_reports ('report_id' INTEGER PRIMARY KEY, 'status' varchar(50) NOT NULL DEFAULT 'Waiting', 'appreciation' text, 'date' text, 'reported_uuid' text, 'reporter_uuid' text, 'reason' text, 'reported_ip' text, 'reported_location' text, 'reported_messages' text, 'reported_gamemode' text, 'reported_on_ground' text, 'reported_sneak' text, 'reported_sprint' text, 'reported_health' text, 'reported_food' text, 'reported_effects' text, 'reporter_ip' text NOT NULL, 'reporter_location' text NOT NULL, 'reporter_messages' text, 'archived' tinyint(1) NOT NULL DEFAULT 0)",
				        null);
				update("CREATE TABLE IF NOT EXISTS tigerreports_comments ('report_id' INTEGER NOT NULL, 'comment_id' INTEGER PRIMARY KEY, 'status' text, 'date' text, 'author' text, 'message' text)",
				        null);
			}

		});
	}

	@Override
	public boolean isConnectionValid() throws SQLException {
		return connection != null && !connection.isClosed();
	}

	public void updateUserName(String uuid, String name) {
		taskScheduler.runTaskAsynchronously(new Runnable() {

			@Override
			public void run() {
				update("INSERT OR IGNORE INTO tigerreports_users (uuid,name) VALUES (?,?)", Arrays.asList(uuid, name));
				update("UPDATE tigerreports_users SET name = ? WHERE uuid = ?", Arrays.asList(name, uuid));
			}

		});
	}

}
