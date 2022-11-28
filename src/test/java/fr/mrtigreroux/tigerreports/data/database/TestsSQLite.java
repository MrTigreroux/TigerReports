package fr.mrtigreroux.tigerreports.data.database;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import fr.mrtigreroux.tigerreports.logs.Logger;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;

/**
 * @author MrTigreroux
 */
public class TestsSQLite extends SQLite {

	private static final Logger LOGGER = Logger.fromClass(TestsSQLite.class);
	private final AtomicInteger pendingAsyncUpdatesAmount = new AtomicInteger(0);
	private final List<Runnable> noAsyncUpdateCallbacks = new ArrayList<>();

	/**
	 * @param taskScheduler
	 * @param databaseFolder
	 * @param databaseFileName
	 */
	public TestsSQLite(TaskScheduler taskScheduler, File databaseFolder, String databaseFileName) {
		super(taskScheduler, databaseFolder, databaseFileName);
	}

	@Override
	public void updateAsynchronously(String query, List<Object> parameters) {
		int pendUpdates = pendingAsyncUpdatesAmount.incrementAndGet();
		LOGGER.debug(() -> "updateAsynchronously(): pendingAsyncUpdatesAmount incremented, new value = " + pendUpdates);
		super.updateAsynchronously(query, parameters, () -> {
			int pendUpdates2 = pendingAsyncUpdatesAmount.decrementAndGet();
			LOGGER.debug(
			        () -> "updateAsynchronously(): pendingAsyncUpdatesAmount decremented, new value = " + pendUpdates2);
			checkAndBroadcastNoAsyncUpdate();
		});
	}

	public void resetPendingAsyncUpdatesAmount() {
		LOGGER.debug(() -> "resetPendingAsyncUpdatesAmount()");
		pendingAsyncUpdatesAmount.set(0);
	}

	public void resetNoAsyncUpdateCallbacks() {
		synchronized (noAsyncUpdateCallbacks) {
			noAsyncUpdateCallbacks.clear();
		}
	}

	/**
	 * Should be executed synchronously.
	 */
	public void whenNoAsyncUpdate(Runnable callback) {
		if (pendingAsyncUpdatesAmount.get() == 0) {
			callback.run();
		} else {
			synchronized (noAsyncUpdateCallbacks) {
				LOGGER.debug(() -> "whenNoAsyncUpdate(): add callback");
				noAsyncUpdateCallbacks.add(callback);
				checkAndBroadcastNoAsyncUpdate(); // mandatory because pendingAsyncUpdatesAmount could be set to 0 when the current thread reached the else block of the current method and checkAndBroadcastNoAsyncUpdate was executed before that
				                                  // synchronized block
			}
		}
	}

	/**
	 * Should be executed synchronously.
	 */
	private void checkAndBroadcastNoAsyncUpdate() {
		if (pendingAsyncUpdatesAmount.get() == 0) {
			List<Runnable> callbacksToExecute;
			synchronized (noAsyncUpdateCallbacks) {
				callbacksToExecute = new ArrayList<>(noAsyncUpdateCallbacks);
				noAsyncUpdateCallbacks.clear();
			}
			LOGGER.debug(() -> "checkAndBroadcastNoAsyncUpdate(): broadcast no async update...");
			callbacksToExecute.forEach(c -> c.run());
		}
	}

	public boolean noPendingAsyncUpdateAndNoCallback() {
		return pendingAsyncUpdatesAmount.get() == 0 && noAsyncUpdateCallbacks.isEmpty();
	}

	public boolean noAsyncUpdateCallback() {
		return noAsyncUpdateCallbacks.isEmpty();
	}

	public boolean noPendingAsyncUpdate() {
		int value = pendingAsyncUpdatesAmount.get();
		LOGGER.debug(() -> "noPendingAsyncUpdate(): pendingAsyncUpdatesAmount = " + value);
		return value == 0;
	}

}
