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

	protected final TaskScheduler taskScheduler;
	protected Connection connection;
	private int closingTaskId = -1;
	private boolean forcedClosing = false;

	public Database(TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
	}

	public abstract void openConnection() throws Exception;

	public abstract void initialize();

	public abstract boolean isConnectionValid() throws SQLException;

	private boolean checkConnection() {
		cancelClosing();
		try {
			if (isConnectionValid())
				return true;
		} catch (SQLException ignored) {}
		try {
			openConnection();
		} catch (Exception ignored) {}
		return connection != null;
	}

	public void update(final String query, final List<Object> parameters) {
		Logger.SQL.info(() -> "update(" + query + ", " + CollectionUtils.toString(parameters) + ")");
		if (checkConnection()) {
			try (PreparedStatement ps = connection.prepareStatement(query)) {
				prepare(ps, parameters);
				ps.executeUpdate();
			} catch (SQLException ex) {
				logDatabaseError(ex);
			}
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
		taskScheduler.runTaskAsynchronously(new Runnable() {

			@Override
			public void run() {
				update(query, parameters);
			}

		});
	}

	public abstract void updateUserName(String uuid, String name);

	public QueryResult query(final String query, final List<Object> parameters) {
		if (checkConnection()) {
			try (PreparedStatement ps = connection.prepareStatement(query)) {
				prepare(ps, parameters);
				ResultSet rs = ps.executeQuery();

				List<Map<String, Object>> resultList = new ArrayList<>();
				Map<String, Object> row = null;
				ResultSetMetaData metaData = rs.getMetaData();
				int columnCount = metaData.getColumnCount();
				while (rs.next()) {
					row = new HashMap<>();
					for (int i = 1; i <= columnCount; i++)
						row.put(metaData.getColumnName(i), rs.getObject(i));
					resultList.add(row);
				}

				close(rs);
				Logger.SQL.info(() -> "query(" + query + ", " + CollectionUtils.toString(parameters) + "): result: "
				        + CollectionUtils.toString(resultList));
				return new QueryResult(resultList);
			} catch (SQLException ex) {
				logDatabaseError(ex);
				return new QueryResult(new ArrayList<>());
			}
		} else {
			return new QueryResult(new ArrayList<>());
		}
	}

	public void queryAsynchronously(final String query, final List<Object> parameters, TaskScheduler taskScheduler,
	        ResultCallback<QueryResult> resultCallback) {
		Objects.requireNonNull(resultCallback);

		taskScheduler.runTaskAsynchronously(new Runnable() {

			@Override
			public void run() {
				QueryResult qr = query(query, parameters);

				taskScheduler.runTask(new Runnable() {

					@Override
					public void run() {
						resultCallback.onResultReceived(qr);
					}

				});
			}

		});
	}

	public int insert(final String query, final List<Object> parameters) {
		Logger.SQL.info(() -> "insert(" + query + ", " + CollectionUtils.toString(parameters) + ")");
		if (checkConnection()) {
			try (PreparedStatement ps = connection.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS)) {
				prepare(ps, parameters);
				ps.executeUpdate();

				try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
					if (generatedKeys.next()) {
						return generatedKeys.getInt(1);
					} else {
						return -1;
					}
				}
			} catch (SQLException ex) {
				logDatabaseError(ex);
				return -1;
			}
		} else {
			return -1;
		}
	}

	public void insertAsynchronously(final String query, final List<Object> parameters, TaskScheduler taskScheduler,
	        ResultCallback<Integer> resultCallback) {
		taskScheduler.runTaskAsynchronously(new Runnable() {

			@Override
			public void run() {
				int generatedKey = insert(query, parameters);
				if (resultCallback != null) {
					taskScheduler.runTask(new Runnable() {

						@Override
						public void run() {
							resultCallback.onResultReceived(generatedKey);
						}

					});
				}
			}

		});
	}

	private void close(ResultSet rs) {
		if (rs != null) {
			try {
				rs.close();
			} catch (SQLException ignored) {}
		}
	}

	public void startClosing() {
		startClosing(false);
	}

	public void startClosing(boolean forceClosing) {
		if (closingTaskId != -1 || connection == null)
			return;
		try {
			if (connection.isClosed())
				return;
		} catch (SQLException ignored) {}

		forcedClosing = true;
		closingTaskId = taskScheduler.runTaskDelayedly(60L * 1000L, new Runnable() {

			@Override
			public void run() {
				if (closingTaskId != -1) {
					closingTaskId = -1;
					closeConnection();
				}
			}

		});
	}

	/**
	 * Cancel closing connection task except if {@link forcedClosing} is true.
	 */
	public void cancelClosing() {
		if (forcedClosing || closingTaskId == -1)
			return;
		taskScheduler.cancelTask(closingTaskId);
		closingTaskId = -1;
	}

	public void closeConnection() {
		try {
			if (connection != null) {
				connection.close();
			}
			connection = null;
			forcedClosing = false;

			cancelClosing();
		} catch (SQLException ignored) {}
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
