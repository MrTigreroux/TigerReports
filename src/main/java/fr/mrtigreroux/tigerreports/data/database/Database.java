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
import java.util.Objects;

import fr.mrtigreroux.tigerreports.logs.Logger;
import fr.mrtigreroux.tigerreports.tasks.ResultCallback;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;
import fr.mrtigreroux.tigerreports.utils.CollectionUtils;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;

/**
 * @author MrTigreroux
 */

public abstract class Database {

	public static final String REPORTS_COLUMNS = "report_id, status, appreciation, date, reported_uuid, reporter_uuid, reason, reported_ip, reported_location, reported_messages, reported_gamemode, reported_on_ground, reported_sneak, reported_sprint, reported_health, reported_food, reported_effects, reporter_ip, reporter_location, reporter_messages, archived";
	private static final int NO_CLOSING_TASK_VALUE = -1;
	private static final long CLOSING_DELAY = 10L * 1000L; // in ms

	protected final TaskScheduler taskScheduler;
	protected Connection connection;
	private int closingTaskId = NO_CLOSING_TASK_VALUE;
	private boolean autoCommit = true;
	private boolean requestedAutoCommit = true;

	public Database(TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
	}

	protected abstract void openConnection() throws Exception;

	public abstract void initialize();

	public abstract boolean isConnectionValid() throws SQLException;

	private boolean checkConnection() {
		return checkConnection(true);
	}

	private synchronized boolean checkConnection(boolean checkAutoCommit) {
		cancelClosing();
		try {
			if (isConnectionValid()) {
				return !checkAutoCommit || checkAutoCommit();
			}
		} catch (SQLException ignored) {
			Logger.SQL.warn(() -> "checkConnection(): isConnectionValid() failed");
		}

		openNewConnection();

		if (connection == null) {
			return false;
		} else {
			return !checkAutoCommit || checkAutoCommit();
		}
	}

	private synchronized boolean checkAutoCommit() {
		if (autoCommit != requestedAutoCommit) {
			Logger.SQL
			        .info(() -> "checkAutoCommit(): this.autoCommit != requested autoCommit, attempt to change it...");
			return setAutoCommit(requestedAutoCommit);
		} else {
			return true;
		}
	}

	private synchronized void openNewConnection() {
		Logger.SQL.debug(() -> "openNewConnection(): start");
		closeConnection(); // prevents to have several connections at the same time
		Logger.SQL.debug(() -> "openNewConnection(): closed connection, try to open a new connection");
		try {
			openConnection();
			autoCommit = true;
		} catch (Exception ignored) {} // exceptions are printed in implementation classes

		if (connection != null) {
			Logger.SQL.debug(() -> "openNewConnection(): openConnection() succeeded");
		} else {
			Logger.SQL.warn(() -> "openNewConnection(): openConnection() failed");
		}
	}

	private PreparedStatement prepare(PreparedStatement ps, final List<Object> parameters) throws SQLException {
		if (parameters != null) {
			for (int i = 1; i <= parameters.size(); i++) {
				final Object parameter = parameters.get(i - 1);
				ps.setObject(i, parameter);
			}
		}
		return ps;
	}

	public void updateAsynchronously(final String query, final List<Object> parameters) {
		updateAsynchronously(query, parameters, null);
	}

	public void updateAsynchronously(final String query, final List<Object> parameters, Runnable doneCallback) {
		Logger.SQL.debug(() -> "updateAsynchronously(" + query + ")");
		taskScheduler.runTaskAsynchronously(() -> {
			update(query, parameters);
			if (doneCallback != null) {
				taskScheduler.runTask(doneCallback);
			}
		});
	}

	public synchronized void update(final String query, final List<Object> parameters) {
		boolean success = false;
		if (checkConnection()) {
			try (PreparedStatement ps = connection.prepareStatement(query)) {
				prepare(ps, parameters);
				ps.executeUpdate();
				success = true;
			} catch (SQLException ex) {
				logDatabaseError(ex);
			}
		}
		final boolean fsuccess = success;
		Logger.SQL.info(
		        () -> "update(" + query + ", " + CollectionUtils.toString(parameters) + "): success = " + fsuccess);
	}

	public abstract void updateUserName(String uuid, String name);

	public void queryAsynchronously(final String query, final List<Object> parameters, TaskScheduler taskScheduler,
	        ResultCallback<QueryResult> resultCallback) {
		Objects.requireNonNull(resultCallback);

		taskScheduler.runTaskAsynchronously(() -> {
			QueryResult qr = query(query, parameters);

			taskScheduler.runTask(() -> {
				resultCallback.onResultReceived(qr);
			});
		});
	}

	public synchronized QueryResult query(final String query, final List<Object> parameters) {
		Logger.SQL.debug(() -> "query(" + query + ", " + CollectionUtils.toString(parameters) + ")");

		List<Map<String, Object>> resultList = new ArrayList<>();
		if (checkConnection()) {
			try (PreparedStatement ps = connection.prepareStatement(query)) {
				prepare(ps, parameters);
				try (ResultSet rs = ps.executeQuery()) {
					Map<String, Object> row = null;
					ResultSetMetaData metaData = rs.getMetaData();
					int columnCount = metaData.getColumnCount();
					while (rs.next()) {
						row = new HashMap<>();
						for (int i = 1; i <= columnCount; i++) {
							row.put(metaData.getColumnName(i), rs.getObject(i));
						}
						resultList.add(row);
					}
				}
			} catch (SQLException ex) {
				logDatabaseError(ex);
			}
		}

		QueryResult qr = new QueryResult(resultList);
		Logger.SQL.info(() -> "query(" + query + ", " + CollectionUtils.toString(parameters) + "): result: "
		        + CollectionUtils.toString(resultList));
		return qr;
	}

	public void insertAsynchronously(final String query, final List<Object> parameters, TaskScheduler taskScheduler,
	        ResultCallback<Integer> resultCallback) {
		Logger.SQL.debug(() -> "insertAsynchronously(" + query + ")");
		taskScheduler.runTaskAsynchronously(() -> {
			int generatedKey = insert(query, parameters);
			if (resultCallback != null) {
				Logger.SQL.debug(() -> "insertAsynchronously(" + query + "): resultCallback != null");
				taskScheduler.runTask(() -> {
					resultCallback.onResultReceived(generatedKey);
				});
			}
		});
	}

	/**
	 * This method must be synchronized because the queries generated keys are bound to the (unique) connection and not directly to the PreparedStatement.
	 * 
	 * @param query
	 * @param parameters
	 * @return
	 */
	public synchronized int insert(final String query, final List<Object> parameters) {
		Logger.SQL.debug(() -> "insert(" + query + ", " + CollectionUtils.toString(parameters) + ")");

		int inserted = -1;
		if (checkConnection()) {
			try (PreparedStatement ps = connection.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS)) {
				prepare(ps, parameters);
				ps.executeUpdate();

				try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
					if (generatedKeys.next()) {
						inserted = generatedKeys.getInt(1);
					}
				}
			} catch (SQLException ex) {
				logDatabaseError(ex);
			}
		}

		final int finserted = inserted;
		Logger.SQL.info(() -> "insert(" + query + ", " + CollectionUtils.toString(parameters) + "): inserted id = "
		        + finserted);
		return inserted;
	}

	public synchronized void executeTransaction(Runnable operations) {
		boolean disabledAutoCommit = setAutoCommit(false);
		if (disabledAutoCommit) {
			Logger.SQL.info(() -> "executeTransaction(): transaction start, setAutoCommit(false) succeeded");
			operations.run();

			Logger.SQL.debug(() -> "executeTransaction(): transaction operations run, commit...");
			commit();
			Logger.SQL.debug(() -> "executeTransaction(): committed");
			Logger.SQL.info(() -> "executeTransaction(): transaction end, setAutoCommit(true)");
			setAutoCommit(true);
		} else {
			Logger.SQL.warn(() -> "executeTransaction(): failed to change autoCommit, cancel transaction");
		}
	}

	public synchronized void startClosing() {
		if (closingTaskId != NO_CLOSING_TASK_VALUE || connection == null) {
			return;
		}

		closingTaskId = taskScheduler.runTaskDelayedlyAsynchronously(CLOSING_DELAY, () -> {
			if (closingTaskId != NO_CLOSING_TASK_VALUE) {
				closingTaskId = NO_CLOSING_TASK_VALUE;
				closeConnection();
			}
		});
	}

	public synchronized void cancelClosing() {
		if (closingTaskId == NO_CLOSING_TASK_VALUE) {
			return;
		}
		taskScheduler.cancelTask(closingTaskId);
		closingTaskId = NO_CLOSING_TASK_VALUE;
	}

	public synchronized void closeConnection() {
		try {
			if (connection != null) {
				connection.close();
			}
			connection = null;
			Logger.SQL.info(() -> "closeConnection(): succeeded");

			cancelClosing();
		} catch (SQLException ex) {
			Logger.SQL.warn(() -> "closeConnection(): failed", ex);
		}
	}

	private synchronized boolean setAutoCommit(boolean state) {
		requestedAutoCommit = state;
		if (autoCommit == state) {
			return true;
		}

		if (checkConnection(false)) { // checkAutoCommit = false to avoid infinite loop
			try {
				connection.setAutoCommit(state);
				autoCommit = state;
				Logger.SQL.debug(() -> "setAutoCommit(" + state + "): success");
				return true;
			} catch (SQLException e) {
				logDatabaseError(e);
			}
		}
		return false;
	}

	private synchronized void rollback() {
		if (autoCommit) {
			Logger.SQL.warn(() -> "rollback(): autoCommit is enabled, rollback cancelled");
			return;
		}

		Logger.SQL.debug(() -> "rollback(): checkConnection()");
		if (checkConnection()) {
			try {
				connection.rollback();
				Logger.SQL.info(() -> "rollback(): success");
			} catch (SQLException e) {
				logDatabaseError(e);
			}
		}
	}

	private synchronized void commit() {
		if (autoCommit) {
			Logger.SQL.warn(() -> "commit(): autoCommit is enabled, commit cancelled");
			return;
		}

		Logger.SQL.debug(() -> "commit(): checkConnection()");
		if (checkConnection()) {
			try {
				connection.commit();
				Logger.SQL.debug(() -> "commit(): success");
			} catch (SQLException e) {
				Logger.SQL.debug(() -> "commit(): failed, therefore rollback()");
				rollback();
				logDatabaseError(e);
			}
		}
	}

	private void logDatabaseError(Exception ex) {
		logError(ConfigUtils.getInfoMessage("An error has occurred with the database:",
		        "Une erreur est survenue avec la base de donnees:"), ex);
	}

	void logError(String message, Exception ex) {
		Logger.SQL.error(message, ex);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void finalize() throws Throwable {
		try {
			closeConnection();
		} catch (Throwable ex) {
			throw ex;
		} finally {
			super.finalize();
		}
	}

}
