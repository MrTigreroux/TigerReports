package fr.mrtigreroux.tigerreports.data.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Bukkit;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;

/**
 * @author MrTigreroux
 */

public abstract class Database {
    
	protected Connection connection;
	private int closingTaskId = -1;
	
	public Database() {}
	
	public abstract void openConnection();
	public abstract void initialize();
	public abstract boolean isValid() throws SQLException;

	private void checkConnection() {
		cancelClosing();
		try {
			if(connection != null && isValid())
				return;
		} catch (Exception ignored) {}
		openConnection();
	}
	
	private PreparedStatement prepare(PreparedStatement ps, final List<Object> parameters) throws SQLException {
		if(parameters != null) {
			for(int i = 1; i <= parameters.size(); i++) {
				final Object parameter = parameters.get(i-1);
				ps.setObject(i, parameter);
			}
		}
		return ps;
	}

	public void update(final String query, final List<Object> parameters) {
		checkConnection();
		try (PreparedStatement ps = connection.prepareStatement(query)) {
			prepare(ps, parameters);
			ps.executeUpdate();
		} catch (SQLException ex) {
			logDatabaseError(ex);
		}
	}
	
	public void updateAsynchronously(final String query, final List<Object> parameters) {
		Bukkit.getScheduler().runTaskAsynchronously(TigerReports.getInstance(), new Runnable() {
			
			@Override
			public void run() {
				update(query, parameters);
			}
			
		});
	}
	
	public QueryResult query(final String query, final List<Object> parameters) {
		checkConnection();
		try (PreparedStatement ps = connection.prepareStatement(query)) {
			prepare(ps, parameters);
			ResultSet rs = ps.executeQuery();
			
			List<Map<String, Object>> resultList = new ArrayList<>();
			Map<String, Object> row = null;
			ResultSetMetaData metaData = rs.getMetaData();
			int columnCount = metaData.getColumnCount();
			while(rs.next()) {
				row = new HashMap<>();
				for(int i = 1; i <= columnCount; i++) row.put(metaData.getColumnName(i), rs.getObject(i));
				resultList.add(row);
			}
			
			close(rs);
			return new QueryResult(resultList);
		} catch (SQLException ex) {
			logDatabaseError(ex);
			return new QueryResult(new ArrayList<>());
		}
	}
    
	public int insert(final String query, final List<Object> parameters) {
		checkConnection();
		try (PreparedStatement ps = connection.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS)) {
			prepare(ps, parameters);
			ps.executeUpdate();
			
			try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
				if(generatedKeys.next())
					return generatedKeys.getInt(1);
				else
					return -1;
			}
		} catch (SQLException ex) {
			logDatabaseError(ex);
			return -1;
		}
	}
	
	private void close(ResultSet rs) {
		if(rs != null)
			try {
				rs.close();
			} catch (SQLException ignored) {}
	}
	
	public void startClosing() {
		if(closingTaskId != -1 || connection == null)
			return;
		closingTaskId = Bukkit.getScheduler().scheduleSyncDelayedTask(TigerReports.getInstance(), new Runnable() {
			
			@Override
			public void run() {
				if(closingTaskId != -1) {
					closeConnection();
					closingTaskId = -1;
				}
			}
			
		}, 1200);
	}
	
	public void cancelClosing() {
		if(closingTaskId == -1)
			return;
		Bukkit.getScheduler().cancelTask(closingTaskId);
		closingTaskId = -1;
	}
	
	public void closeConnection() {
		try {
			if(connection == null || connection.isClosed())
				return;
			connection.close();
			connection = null;
		} catch (SQLException ignored) {}
	}
	
	private void logDatabaseError(Exception ex) {
		logError(ConfigUtils.getInfoMessage("An error has occurred with the database:", "Une erreur est survenue avec la base de donnees:"), ex);
	}
	
	void logError(String message, Exception ex) {
		Bukkit.getLogger().log(Level.SEVERE, message, ex);
	}
    
}
