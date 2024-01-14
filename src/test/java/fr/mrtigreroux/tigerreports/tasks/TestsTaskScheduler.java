package fr.mrtigreroux.tigerreports.tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import fr.mrtigreroux.tigerreports.TigerReportsMock;
import fr.mrtigreroux.tigerreports.logs.Logger;

/**
 * @author MrTigreroux
 */
public class TestsTaskScheduler implements TaskScheduler {
    
    private static final Logger CLASS_LOGGER =
            TigerReportsMock.getLoggerFromClass(TestsTaskScheduler.class);
    
    private static TestsTaskScheduler mainTaskScheduler;
    
    private final Logger instanceLogger;
    private final String name;
    private final ScheduledExecutorService mainExecutor;
    private final ScheduledExecutorService asyncExecutor;
    private final List<Future<?>> basicTasks = new ArrayList<>();
    private final Map<Integer, ScheduledFuture<?>> scheduledTasks = new HashMap<>();
    private int lastId = 0;
    private AtomicInteger submittedTasksAmount = new AtomicInteger(0);
    
    public static final long DEFAULT_TIMEOUT = 5 * 1000L;
    
    private class MainThreadFactory implements ThreadFactory {
        
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, getThreadsNamePrefix() + "main");
        }
        
    }
    
    private class AsyncThreadFactory implements ThreadFactory {
        
        private AtomicInteger currentThreadIndex = new AtomicInteger(1);
        
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(
                    r,
                    getThreadsNamePrefix() + "async" + currentThreadIndex.getAndIncrement()
            );
        }
        
    }
    
    public static TestsTaskScheduler getCleanMainTaskScheduler() {
        TestsTaskScheduler mainTaskScheduler = getMainTaskScheduler();
        mainTaskScheduler.cleanBeforeUse();
        return mainTaskScheduler;
    }
    
    public static TestsTaskScheduler getMainTaskScheduler() {
        if (mainTaskScheduler == null) {
            synchronized (TestsTaskScheduler.class) {
                if (mainTaskScheduler == null) {
                    mainTaskScheduler = new TestsTaskScheduler("main", 2);
                    CLASS_LOGGER
                            .debug(() -> "getMainTaskScheduler(): new main task scheduler created");
                }
            }
        }
        
        return mainTaskScheduler;
    }
    
    public TestsTaskScheduler() {
        this("default", 2);
    }
    
    public TestsTaskScheduler(String name, int asyncThreadsAmount) {
        this.name = name;
        instanceLogger = CLASS_LOGGER.newChild(name);
        mainExecutor = Executors.newSingleThreadScheduledExecutor(new MainThreadFactory());
        asyncExecutor =
                Executors.newScheduledThreadPool(asyncThreadsAmount, new AsyncThreadFactory());
    }
    
    public String getThreadsNamePrefix() {
        return name + "-";
    }
    
    /**
     * Run {@code task} in main thread and make the current thread wait for its completion for
     * maximum {@code timeout} ms.
     * 
     * @param task    run in main thread, needs to call {@link #setDone()} on {@code TaskCompletion}
     *                when done.
     * @param timeout in ms
     * 
     * @return true if task done, otherwise false (if timeout reached or current thread interrupted)
     */
    public boolean runTaskAndWait(Consumer<TaskCompletion> task, long timeout) {
        TaskCompletion taskCompletion = new TaskCompletion();
        Future<?> taskFuture = runTaskAndGetFuture(() -> {
            task.accept(taskCompletion);
        });
        boolean taskDone = taskCompletion.waitForCompletion(timeout);
        boolean taskWithoutError = checkTaskExecution(taskFuture, 0);
        return taskWithoutError && taskDone;
    }
    
    @Override
    public void runTaskAsynchronously(Runnable task) {
        saveBasicTask(asyncExecutor.submit(task));
    }
    
    @Override
    public void runTask(Runnable task) {
        saveBasicTask(mainExecutor.submit(task));
    }
    
    public Future<?> runTaskAndGetFuture(Runnable task) {
        Future<?> taskFuture = mainExecutor.submit(task);
        saveBasicTask(taskFuture);
        return taskFuture;
    }
    
    private void saveBasicTask(Future<?> basicTask) {
        submittedTasksAmount.incrementAndGet();
        synchronized (this) {
            basicTasks.add(basicTask);
            instanceLogger.debug(() -> "saveBasicTask(): " + basicTask);
            this.notifyAll();
        }
    }
    
    @Override
    public int runTaskDelayedly(long delay, Runnable task) {
        return saveScheduledTask(mainExecutor.schedule(task, delay, TimeUnit.MILLISECONDS));
    }
    
    @Override
    public int runTaskDelayedlyAsynchronously(long delay, Runnable task) {
        return saveScheduledTask(asyncExecutor.schedule(task, delay, TimeUnit.MILLISECONDS));
    }
    
    @Override
    public int runTaskRepeatedly(long delay, long period, Runnable task) {
        return saveScheduledTask(
                mainExecutor.scheduleAtFixedRate(task, delay, period, TimeUnit.MILLISECONDS)
        );
    }
    
    private int saveScheduledTask(ScheduledFuture<?> scheduledTask) {
        submittedTasksAmount.incrementAndGet();
        synchronized (this) {
            int taskId = getNewTaskId();
            scheduledTasks.put(taskId, scheduledTask);
            instanceLogger.debug(() -> "saveScheduledTask(): " + scheduledTask);
            this.notifyAll();
            return taskId;
        }
    }
    
    private int getNewTaskId() {
        //		if (lastId >= TRIGGERING_CLEAN_ID && lastId % TRIGGERING_CLEAN_ID == 0) {
        //			cleanScheduledTasks();
        //		}
        return ++lastId;
    }
    
    //	private synchronized void cleanScheduledTasks() {
    //		int removedAmount = 0;
    //		for (Iterator<ScheduledFuture<?>> it = scheduledTasks.values().iterator(); it.hasNext();) {
    //			if (it.next().isDone()) {
    //				it.remove();
    //				removedAmount++;
    //			}
    //		}
    //		final int fremovedAmount = removedAmount;
    //		LOGGER.info(() -> "cleanScheduledTasks(): removed " + fremovedAmount + " done tasks");
    //	}
    
    @Override
    public void cancelTask(int taskId) {
        submittedTasksAmount.decrementAndGet();
        synchronized (this) {
            ScheduledFuture<?> scheduledTask = scheduledTasks.get(taskId);
            if (scheduledTask != null) {
                scheduledTask.cancel(true);
                scheduledTasks.remove(taskId);
            } else {
                instanceLogger.info(() -> "cancelTask(): task not found (id = " + taskId + ")");
            }
            this.notifyAll();
        }
    }
    
    /**
     * Waits for tasks termination for timeout ms, or beyond stop tasks.
     * 
     * @param timeout in ms
     * 
     * @return true if all tasks have not failed (no error, no cancellation...).
     */
    public synchronized boolean waitForTerminationOrStop(long timeout) {
        return waitForTerminationOrStop(timeout, false);
    }
    
    /**
     * Waits for tasks termination for timeout ms, or beyond stop tasks.
     * 
     * @param timeout in ms
     * @param forced  = true to don't wait for all submitted tasks to be saved
     * 
     * @return true if all tasks have not failed (no error, no cancellation...).
     */
    public synchronized boolean waitForTerminationOrStop(long timeout, boolean forced) {
        long start = System.currentTimeMillis();
        
        while (!forced && getTasksAmount() != submittedTasksAmount.get()) {
            try {
                this.wait(timeout);
                forced = true;
                instanceLogger.warn(
                        () -> "waitForTerminationOrStop(): submitted tasks timeout reached => forced mode enabled"
                );
            } catch (InterruptedException e) {
                instanceLogger.error("waitForTerminationOrStop(): interrupted", e);
            }
        }
        
        int tasksAmount = getTasksAmount();
        int failedTasks = 0;
        
        if (tasksAmount > 0) {
            for (Future<?> basicTask : basicTasks) {
                if (!checkTaskExecution(basicTask, getRemainingTimeout(timeout, start))) {
                    failedTasks++;
                }
            }
            
            for (Future<?> scheduledTask : scheduledTasks.values()) {
                if (!checkTaskExecution(scheduledTask, getRemainingTimeout(timeout, start))) {
                    failedTasks++;
                }
            }
            
            final int ffailedTasks = failedTasks;
            instanceLogger.info(
                    () -> "waitForTerminationOrStop(" + timeout + "ms): failed tasks: "
                            + ffailedTasks + "/" + tasksAmount
            );
        } else {
            if (!forced) {
                instanceLogger.warn(
                        () -> "waitForTerminationOrStop(" + timeout + "ms): no task executed (yet)"
                );
            }
        }
        
        boolean success = failedTasks == 0;
        if (success) {
            clear();
            instanceLogger
                    .debug(() -> "waitForTerminationOrStop(" + timeout + "ms): success => cleared");
        }
        return success;
    }
    
    private static long getRemainingTimeout(long timeout, long start) {
        long result = timeout - (System.currentTimeMillis() - start);
        return result >= 0 ? result : 0L;
    }
    
    private synchronized int getTasksAmount() {
        return basicTasks.size() + scheduledTasks.size();
    }
    
    /**
     * 
     * @param forced = true to don't wait for all submitted tasks to be saved
     * 
     * @return true if all tasks have not failed (no error, no cancellation...).
     */
    public synchronized boolean stopNow(boolean forced) {
        return waitForTerminationOrStop(0, forced);
    }
    
    private boolean checkTaskExecution(Future<?> taskFuture, long timeout) {
        try {
            try {
                taskFuture.get(timeout, TimeUnit.MILLISECONDS);
                return true;
            } catch (TimeoutException e) {
                instanceLogger.info(
                        () -> "checkTaskExecution(): timeout (" + timeout
                                + "ms) reached, cancel task " + taskFuture
                );
                taskFuture.cancel(true);
            }
        } catch (CancellationException e) {
            instanceLogger
                    .warn(() -> "checkTaskExecution(): task " + taskFuture + " was cancelled");
            return true;
        } catch (InterruptedException e) {
            instanceLogger
                    .error("checkTaskExecution(): current thread was interrupted while waiting", e);
        } catch (ExecutionException e) {
            instanceLogger
                    .error("checkTaskExecution(): task " + taskFuture + " threw an exception: ", e);
        }
        return false;
    }
    
    public synchronized void cleanBeforeUse() {
        if (getTasksAmount() > 0) {
            instanceLogger.warn(
                    () -> "cleanBeforeUse(): some tasks (" + basicTasks.size() + " basic, "
                            + scheduledTasks.size()
                            + " scheduled) of a previous use need to be stopped by force"
            );
        }
        stopNow(true);
        clear();
    }
    
    /**
     * Clean this instance after having used it (typically after a test execution).
     * 
     * @return true if all tasks have not failed (no error, no cancellation...).
     */
    public synchronized boolean cleanAfterUse() {
        instanceLogger.debug(() -> "cleanAfterUse()");
        int submitted = submittedTasksAmount.get();
        if (submitted > 0 || getTasksAmount() > 0) {
            instanceLogger.debug(
                    () -> "cleanAfterUse(): some tasks (" + submitted + " submitted, "
                            + basicTasks.size() + " basic, " + scheduledTasks.size()
                            + " scheduled) previously started will be stopped now"
            );
            boolean noError = stopNow(false);
            clear();
            return noError;
        } else {
            return true;
        }
    }
    
    private synchronized void clear() {
        instanceLogger.debug(() -> "clear()");
        basicTasks.clear();
        scheduledTasks.clear();
        lastId = 0;
        submittedTasksAmount.set(0);
    }
    
    /**
     * This instance can no longer be used after this method. No more tasks will be started, already
     * started tasks are not interrupted by this method. Current thread is blocked until the tasks
     * are terminated or the timeout elapsed.
     * 
     * @param timeout in ms
     * 
     * @return {@code true} if the tasks terminated and {@code false} if the timeout elapsed before
     *         termination
     */
    public synchronized boolean destroyAndWaitForTermination(long timeout) {
        mainExecutor.shutdown();
        asyncExecutor.shutdown();
        try {
            return mainExecutor.awaitTermination(timeout, TimeUnit.MILLISECONDS)
                    && asyncExecutor.awaitTermination(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * This instance can no longer be used after this method.
     */
    public synchronized void destroyNow() {
        mainExecutor.shutdownNow();
        asyncExecutor.shutdownNow();
    }
    
    public static boolean cleanMainTaskSchedulerAfterUse() {
        if (mainTaskScheduler != null) {
            return mainTaskScheduler.cleanAfterUse();
        } else {
            return true;
        }
    }
    
    @Override
    public String toString() {
        return name + " TestsTaskScheduler";
    }
    
}
