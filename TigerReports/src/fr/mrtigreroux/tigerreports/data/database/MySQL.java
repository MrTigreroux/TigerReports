package fr.mrtigreroux.tigerreports.data.database;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;

import org.bukkit.Bukkit;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;

/**
 * @author MrTigreroux
 */

public class MySQL extends Database {

	private String host, database, username, password;
	private int port;
	private boolean useSsl, verifyServerCertificate;

	public MySQL(String host, int port, String database, String username, String password, boolean useSsl,
	        boolean verifyServerCertificate) {
		this.host = host;
		this.port = port;
		this.database = database;
		this.username = username;
		this.password = password;
		this.useSsl = useSsl;
		this.verifyServerCertificate = verifyServerCertificate;
	}

	public void check() throws SQLException {
		if (host == null || host.isEmpty() || database == null || database.isEmpty() || username == null
		        || username.isEmpty())
			throw new SQLException();
		openConnection();
		connection.createStatement();
	}

	@Override
	public void openConnection() {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			connection = DriverManager.getConnection(
			        "jdbc:mysql://" + host + ":" + port + "/" + database
			                + "?useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&verifyServerCertificate="
			                + (verifyServerCertificate ? "true" : "false") + "&useSSL=" + (useSsl ? "true" : "false"),
			        username, password);
		} catch (ClassNotFoundException missing) {
			logError(ConfigUtils.getInfoMessage("MySQL is missing.", "MySQL n'est pas installe."), null);
		} catch (SQLException ex) {
			logError(ConfigUtils.getInfoMessage("An error has occurred during the connection to the MySQL database:",
			        "Une erreur s'est produite lors de la connexion a la base de donnees MySQL:"), ex);
		}
		return;
	}

	@Override
	public void initialize() {
		Bukkit.getScheduler().runTaskAsynchronously(TigerReports.getInstance(), new Runnable() {

			@Override
			public void run() {
				update("CREATE TABLE IF NOT EXISTS tigerreports_users (uuid char(36) NOT NULL, name varchar(20), cooldown varchar(20), immunity varchar(20), notifications text, true_appreciations int DEFAULT 0, uncertain_appreciations int DEFAULT 0, false_appreciations int DEFAULT 0, reports int DEFAULT 0, reported_times int DEFAULT 0, processed_reports int DEFAULT 0, PRIMARY KEY (uuid)) ENGINE=InnoDB CHARACTER SET=utf8mb4",
				        null);
				update("CREATE TABLE IF NOT EXISTS tigerreports_reports (report_id int NOT NULL AUTO_INCREMENT, status varchar(50) NOT NULL DEFAULT 'Waiting', appreciation varchar(255), date varchar(20) NOT NULL, reported_uuid char(36) NOT NULL, reporter_uuid varchar(255) NOT NULL, reason varchar(255), reported_ip varchar(46), reported_location varchar(510), reported_messages text, reported_gamemode char(10), reported_on_ground char(1), reported_sneak char(1), reported_sprint char(1), reported_health varchar(10), reported_food varchar(10), reported_effects text, reporter_ip varchar(46) NOT NULL, reporter_location varchar(510) NOT NULL, reporter_messages text, archived tinyint(1) NOT NULL DEFAULT 0, PRIMARY KEY (report_id)) ENGINE=InnoDB CHARACTER SET=utf8mb4",
				        null);
				update("CREATE TABLE IF NOT EXISTS tigerreports_comments (report_id int NOT NULL, comment_id INT NOT NULL AUTO_INCREMENT, status varchar(25), date varchar(20), author varchar(255), message text, PRIMARY KEY (comment_id)) ENGINE=InnoDB CHARACTER SET=utf8mb4",
				        null);
			}

		});
	}

	@Override
	public boolean isValid() throws SQLException {
		return connection.isValid(5);
	}

	public void updateUserName(String uuid, String name) {
		updateAsynchronously(
		        "INSERT INTO tigerreports_users (uuid,name) VALUES (?,?) ON DUPLICATE KEY UPDATE name=VALUES(name)",
		        Arrays.asList(uuid, name));
	}

}
