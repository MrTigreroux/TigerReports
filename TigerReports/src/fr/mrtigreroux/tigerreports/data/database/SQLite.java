package fr.mrtigreroux.tigerreports.data.database;

import java.io.File;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;

import org.bukkit.Bukkit;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;

/**
 * @author MrTigreroux
 */

public class SQLite extends Database {

	public SQLite() {}

	@Override
	public void openConnection() {
		File dataFolder = new File(TigerReports.getInstance().getDataFolder(), "tigerreports.db");
		if (!dataFolder.exists()) {
			try {
				dataFolder.createNewFile();
			} catch (IOException ex) {
				logError(ConfigUtils.getInfoMessage("Failed creation of tigerreports.db file.",
				        "La creation du fichier tigerreports.db a echoue."), ex);
			}
		}

		try {
			Class.forName("org.sqlite.JDBC");
			connection = DriverManager.getConnection("jdbc:sqlite:" + dataFolder);
		} catch (ClassNotFoundException missing) {
			logError(ConfigUtils.getInfoMessage("SQLite is missing.", "SQLite n'est pas installe."), null);
		} catch (SQLException ex) {
			logError(ConfigUtils.getInfoMessage("An error has occurred during the connection to the SQLite database:",
			        "Une erreur s'est produite lors de la connexion a la base de donnees SQLite:"), ex);
		}
		return;
	}

	@Override
	public void initialize() {
		Bukkit.getScheduler().runTaskAsynchronously(TigerReports.getInstance(), new Runnable() {

			@Override
			public void run() {
				update("CREATE TABLE IF NOT EXISTS tigerreports_users ('uuid' text NOT NULL PRIMARY KEY, 'name' text, 'cooldown' text, 'immunity' text, 'notifications' text, 'true_appreciations' int DEFAULT '0', 'uncertain_appreciations' int DEFAULT '0', 'false_appreciations' int DEFAULT '0', 'reports' int DEFAULT '0', 'reported_times' int DEFAULT '0', 'processed_reports' int DEFAULT '0');",
				        null);
				update("CREATE TABLE IF NOT EXISTS tigerreports_reports ('report_id' INTEGER PRIMARY KEY, 'status' varchar(50) NOT NULL DEFAULT 'Waiting', 'appreciation' text, 'date' text, 'reported_uuid' text, 'reporter_uuid' text, 'reason' text, 'reported_ip' text, 'reported_location' text, 'reported_messages' text, 'reported_gamemode' text, 'reported_on_ground' text, 'reported_sneak' text, 'reported_sprint' text, 'reported_health' text, 'reported_food' text, 'reported_effects' text, 'reporter_ip' text NOT NULL, 'reporter_location' text NOT NULL, 'reporter_messages' text, 'archived' tinyint(1) NOT NULL DEFAULT 0);",
				        null);
				update("CREATE TABLE IF NOT EXISTS tigerreports_comments ('report_id' INTEGER NOT NULL, 'comment_id' INTEGER PRIMARY KEY, 'status' text, 'date' text, 'author' text, 'message' text);",
				        null);
			}

		});
	}

	@Override
	public boolean isValid() throws SQLException {
		return !connection.isClosed();
	}

	public void updateUserName(String uuid, String name) {
		Bukkit.getScheduler().runTaskAsynchronously(TigerReports.getInstance(), new Runnable() {

			@Override
			public void run() {
				update("INSERT OR IGNORE INTO tigerreports_users (uuid,name) VALUES (?,?)", Arrays.asList(uuid, name));
				update("UPDATE tigerreports_users SET name = ? WHERE uuid = ?", Arrays.asList(name, uuid));
			}

		});
	}

}
