package fr.mrtigreroux.tigerreports.tasks;

public interface TaskScheduler {

	void runTaskAsynchronously(Runnable task);

	void runTask(Runnable task);

	/**
	 * Run task in main thread after {@code delay} ms.
	 * 
	 * @param delay time in ms to wait before running the task
	 * @param task
	 * @return taskId
	 */
	int runTaskDelayedly(long delay, Runnable task);

	/**
	 * Run task in main thread after {@code delay} and every {@code period} ms.
	 * 
	 * @param delay  time in ms to wait before running the task
	 * @param period time in ms to wait between runs
	 * @param task
	 * @return taskId
	 */
	int runTaskRepeatedly(long delay, long period, Runnable task);

	void cancelTask(int taskId);

}
