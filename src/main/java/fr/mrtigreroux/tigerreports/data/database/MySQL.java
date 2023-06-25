package fr.mrtigreroux.tigerreports.data.database;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;

import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;

/**
 * @author MrTigreroux
 */

public class MySQL extends Database {

	private final String driverClassName;
	private final String connectionUrl;
	private final String username;
	private final String password;

	public MySQL(String driverClassName, String connectionUrl, String host, int port, String database, String username,
	        String password, boolean useSsl, boolean verifyServerCertificate, TaskScheduler taskScheduler)
	        throws IllegalArgumentException {
		super(taskScheduler);
		this.driverClassName = driverClassName != null && !driverClassName.isEmpty() ? driverClassName
		        : "com.mysql.jdbc.Driver";
		if (connectionUrl != null && !connectionUrl.isEmpty()) {
			this.connectionUrl = connectionUrl;
		} else {
			if (host == null || host.isEmpty() || database == null || database.isEmpty()) {
				throw new IllegalArgumentException("Database host and name cannot be null or empty.");
			}
			this.connectionUrl = "jdbc:mysql://" + host + ":" + port + "/" + database
			        + "?useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&verifyServerCertificate="
			        + (verifyServerCertificate ? "true" : "false") + "&useSSL=" + (useSsl ? "true" : "false");
		}
		if (username == null || username.isEmpty()) {
			throw new IllegalArgumentException("Database username cannot be null or empty.");
		}
		this.username = username;
		this.password = password;
	}

	public void check() throws Exception {
		openConnection();
		connection.createStatement().close();
	}

	@Override
	public void openConnection() throws Exception {
		try {
			Class.forName(driverClassName);
			connection = DriverManager.getConnection(connectionUrl, username, password);
		} catch (ClassNotFoundException missing) {
			logError(ConfigUtils.getInfoMessage("The JDBC driver '" + driverClassName + "' is missing.",
			        "Le pilote JDBC '" + driverClassName + "' n'est pas installe."), null);
			throw missing;
		} catch (SQLException ex) {
			logError(ConfigUtils.getInfoMessage("An error has occurred during the connection to the MySQL database:",
			        "Une erreur s'est produite lors de la connexion a la base de donnees MySQL:"), ex);
			throw ex;
		}
		return;
	}

	@Override
	public void initialize() {
		update("CREATE TABLE IF NOT EXISTS tigerreports_users (uuid CHAR(36) NOT NULL, name VARCHAR(20), cooldown VARCHAR(20), immunity VARCHAR(20), notifications TEXT, true_appreciations INT DEFAULT 0, uncertain_appreciations INT DEFAULT 0, false_appreciations INT DEFAULT 0, reports INT DEFAULT 0, reported_times INT DEFAULT 0, processed_reports INT DEFAULT 0, PRIMARY KEY (uuid)) ENGINE=InnoDB CHARACTER SET=utf8mb4",
		        null);
		update("CREATE TABLE IF NOT EXISTS tigerreports_reports (report_id INT NOT NULL AUTO_INCREMENT, status VARCHAR(50) NOT NULL DEFAULT 'Waiting', appreciation VARCHAR(255), date VARCHAR(20) NOT NULL, reported_uuid CHAR(36) NOT NULL, reporter_uuid VARCHAR(255) NOT NULL, reason VARCHAR(255), reported_ip VARCHAR(46), reported_location VARCHAR(510), reported_messages TEXT, reported_gamemode CHAR(10), reported_on_ground INT(2), reported_sneak INT(2), reported_sprint INT(2), reported_health VARCHAR(10), reported_food VARCHAR(10), reported_effects TEXT, reporter_ip VARCHAR(46), reporter_location VARCHAR(510), reporter_messages TEXT, archived INT(2) NOT NULL DEFAULT 0, PRIMARY KEY (report_id)) ENGINE=InnoDB CHARACTER SET=utf8mb4",
		        null);
		update("CREATE TABLE IF NOT EXISTS tigerreports_comments (report_id INT NOT NULL, comment_id INT NOT NULL AUTO_INCREMENT, status VARCHAR(25), date VARCHAR(20), author VARCHAR(255), message TEXT, PRIMARY KEY (comment_id)) ENGINE=InnoDB CHARACTER SET=utf8mb4",
		        null);
	}

	@Override
	public boolean isConnectionValid() throws SQLException {
		return connection != null && connection.isValid(5);
	}

	@Override
	public void updateUserName(String uuid, String name) {
		updateAsynchronously(
		        "INSERT INTO tigerreports_users (uuid,name) VALUES (?,?) ON DUPLICATE KEY UPDATE name=VALUES(name)",
		        Arrays.asList(uuid, name));
	}

}
