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

/**
 * @author MrTigreroux
 */

public abstract class Database {
    
	Connection connection;
	private int closingTaskId = -1;

	public Database() {}

	public abstract boolean isValid() throws SQLException;
	public abstract void openConnection();
	public abstract void initialize();
	public abstract boolean existsTable(String tableName);

	public void checkConnection() {
		cancelClosing();
		try {
			if(connection != null && isValid()) return;
		} catch (SQLException ignored) {}
		openConnection();
	}
	
	public PreparedStatement prepare(PreparedStatement ps, final List<Object> parameters) throws SQLException {
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
			logError("Error occurred with database:", ex);
		}
	}
	
	public void updateAsynchronously(final String query, final List<Object> parameters) {
		Bukkit.getScheduler().runTaskAsynchronously(TigerReports.getInstance(), () -> update(query, parameters));
	}
	
	public QueryResult query(final String query, final List<Object> parameters) {
		checkConnection();
	    try (PreparedStatement ps = connection.prepareStatement(query)) {
	    	prepare(ps, parameters);
	    	ResultSet rs = ps.executeQuery();
			
			List<Map<String, Object>> resultList = new ArrayList<>();
		    Map<String, Object> row;
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
			logError("Error occurred with database:", ex);
			return null;
		}
	}
    
	public int insert(final String query, final List<Object> parameters) {
		checkConnection();
		try (PreparedStatement ps = connection.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS);) {
			prepare(ps, parameters);
			ps.executeUpdate();
			
			try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
				if(generatedKeys.next()) return generatedKeys.getInt(1);
				else return -1;
			}
		} catch (SQLException ex) {
			logError("Error occurred with database:", ex);
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
		if(closingTaskId != -1 || connection == null) return;
		closingTaskId = Bukkit.getScheduler().scheduleSyncDelayedTask(TigerReports.getInstance(), () -> {
            if(closingTaskId != -1) {
                closeConnection();
                closingTaskId = -1;
            }
        }, 1200);
	}
	
	public void cancelClosing() {
		if(closingTaskId == -1) return;
		Bukkit.getScheduler().cancelTask(closingTaskId);
		closingTaskId = -1;
	}
	
	public void closeConnection() {
		try {
			if(connection == null || connection.isClosed()) return;
			connection.close();
			connection = null;
		} catch (SQLException ignored) {}
	}
	
	public void logError(String message, Exception ex) {
		Bukkit.getLogger().log(Level.SEVERE, "[TigerReports] "+message, ex);
	}
    
}
