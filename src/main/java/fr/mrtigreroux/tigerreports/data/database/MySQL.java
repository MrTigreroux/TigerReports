package fr.mrtigreroux.tigerreports.data.database;

import java.sql.DriverManager;
import java.sql.SQLException;

import org.bukkit.Bukkit;

import fr.mrtigreroux.tigerreports.TigerReports;

/**
 * @author MrTigreroux
 */

public class MySQL extends Database {
	
	private String host, database, username, password;
	private int port;
	
	public MySQL(String host, int port, String database, String username, String password) {
		this.host = host;
		this.port = port;
		this.database = database;
		this.username = username;
		this.password = password;
	}
	
	public void check() throws SQLException {
		if(host == null || host.isEmpty() || database == null || database.isEmpty() || username == null || username.isEmpty() || password == null || password.isEmpty()) throw new SQLException();
		openConnection();
		connection.createStatement();
	}
	
	@Override
	public void openConnection() {
        try {
        	Class.forName("com.mysql.jdbc.Driver");
        	connection = DriverManager.getConnection("jdbc:mysql://"+host+":"+port+"/"+database+"?useUnicode=true&characterEncoding=UTF-8", username, password);
        } catch (SQLException ex) {
        	logError("Error on connection to database:", ex);
        } catch (ClassNotFoundException ex) {
        	logError("MySQL is missing.", null);
        }
        return;
    }
	
	@Override
	public void initialize() {
		Bukkit.getScheduler().runTaskAsynchronously(TigerReports.getInstance(), new Runnable() {
			@Override
			public void run() {
				update("CREATE TABLE IF NOT EXISTS users (uuid char(36) NOT NULL,name varchar(20),cooldown varchar(20),immunity varchar(20),notifications varchar(255),true_appreciations int(5) DEFAULT 0,uncertain_appreciations int(5) DEFAULT 0,false_appreciations int(5) DEFAULT 0,reports int(5) DEFAULT 0,reported_times int(5) DEFAULT 0,processed_reports int(5) DEFAULT 0, PRIMARY KEY (uuid));", null);
				update("CREATE TABLE IF NOT EXISTS reports (report_id int(11) NOT NULL AUTO_INCREMENT, status varchar(50) NOT NULL DEFAULT 'Waiting',appreciation varchar(10),date varchar(20) NOT NULL,reported_uuid char(36) NOT NULL,signalman_uuid char(36) NOT NULL,reason varchar(150),reported_ip varchar(22),reported_location varchar(60),reported_messages varchar(255),reported_gamemode char(10),reported_on_ground char(5),reported_sneak varchar(5),reported_sprint varchar(5),reported_health varchar(10),reported_food varchar(10),reported_effects varchar(100),signalman_ip varchar(22) NOT NULL,signalman_location varchar(60) NOT NULL,signalman_messages varchar(255), PRIMARY KEY (report_id));", null);
				update("CREATE TABLE IF NOT EXISTS archived_reports (report_id int(11) NOT NULL, status varchar(50) NOT NULL,appreciation varchar(10),date varchar(20) NOT NULL,reported_uuid char(36) NOT NULL,signalman_uuid char(36) NOT NULL,reason varchar(150),reported_ip varchar(22),reported_location varchar(60),reported_messages varchar(255),reported_gamemode char(10),reported_on_ground char(5),reported_sneak varchar(5),reported_sprint varchar(5),reported_health varchar(10),reported_food varchar(10),reported_effects varchar(100),signalman_ip varchar(22) NOT NULL,signalman_location varchar(60) NOT NULL,signalman_messages varchar(255), PRIMARY KEY (report_id));", null);
			}
		});
	}
	
	@Override
	public boolean isValid() throws SQLException {
		return connection.isValid(5);
	}
	
	@Override
	public boolean existsTable(String tableName) {
		return query("SHOW TABLES LIKE '"+tableName+"';", null).getResultList().size() >= 1;
	}
	
	@Override
	public void createCommentsTable(int reportId) {
		update("CREATE TABLE IF NOT EXISTS report"+reportId+"_comments (comment_id INT NOT NULL AUTO_INCREMENT,status varchar(7),date varchar(20),author varchar(32), message varchar(255), PRIMARY KEY (comment_id));", null);
	}
    
}

