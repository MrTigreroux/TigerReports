package fr.mrtigreroux.tigerreports.tasks;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import fr.mrtigreroux.tigerreports.TestClass;
import fr.mrtigreroux.tigerreports.utils.TestsCheckUtils;

/**
 * @author MrTigreroux
 */
class TestsTaskSchedulerTest extends TestClass {

	private final long MAX_TASK_EXECUTION_DELAY = 10 * 1000L; // in ms
	private final Object SUCCESS_LOCK = new Object();
	private final Runnable SUCCESS_TASK = () -> {
		synchronized (SUCCESS_LOCK) {
			success = true;
			SUCCESS_LOCK.notifyAll();
		}
	};

	private TestsTaskScheduler taskScheduler;
	private boolean success = false;

	@BeforeEach
	void initTest() {
		taskScheduler = new TestsTaskScheduler();
		success = false;
	}

	/**
	 * Test method for {@link fr.mrtigreroux.tigerreports.tasks.TestsTaskScheduler#runTask(java.lang.Runnable)}.
	 */
	@Test
	void testRunTask() {
		taskScheduler.runTask(SUCCESS_TASK);
		checkSuccess();
	}

	/**
	 * Test method for {@link fr.mrtigreroux.tigerreports.tasks.TestsTaskScheduler#runTaskAsynchronously(java.lang.Runnable)}.
	 */
	@Test
	void testRunTaskAsynchronously() {
		taskScheduler.runTaskAsynchronously(SUCCESS_TASK);
		checkSuccess();
	}

	/**
	 * Test method for {@link fr.mrtigreroux.tigerreports.tasks.TestsTaskScheduler#runTaskAndWait(java.util.function.Consumer, long)}.
	 */
	@Test
	void testRunTaskAndWait() {
		assertTrue(taskScheduler.runTaskAndWait((tc) -> {
			tc.setDone();
		}, MAX_TASK_EXECUTION_DELAY));

		final long delay = 100L;
		assertTrue(taskScheduler.runTaskAndWait((tc) -> {
			taskScheduler.runTaskDelayedly(delay, () -> {
				tc.setDone();
			});
		}, MAX_TASK_EXECUTION_DELAY));
		assertFalse(taskScheduler.runTaskAndWait((tc) -> {}, delay));

		assertFalse(taskScheduler.runTaskAndWait((tc) -> {
			try {
				Thread.sleep(5 * delay); // Should be interrupted
				tc.setDone();
			} catch (InterruptedException e) {}
		}, delay));
	}

	/**
	 * Test method for {@link fr.mrtigreroux.tigerreports.tasks.TestsTaskScheduler#runTaskDelayedly(long, java.lang.Runnable)}.
	 */
	@Test
	void testRunTaskDelayedly() {
		final long start = System.currentTimeMillis();
		long delay = 250;
		taskScheduler.runTaskDelayedly(delay, SUCCESS_TASK);
		checkSuccess();
		assertTrue(TestsCheckUtils.longRightValue(System.currentTimeMillis() - start, delay, 5));
	}

	/**
	 * Test method for {@link fr.mrtigreroux.tigerreports.tasks.TestsTaskScheduler#runTaskDelayedlyAsynchronously(long, java.lang.Runnable)}.
	 */
	@Test
	void testRunTaskDelayedlyAsynchronously() {
		final long start = System.currentTimeMillis();
		long delay = 250;
		taskScheduler.runTaskDelayedlyAsynchronously(delay, SUCCESS_TASK);
		checkSuccess();
		assertTrue(TestsCheckUtils.longRightValue(System.currentTimeMillis() - start, delay, 5));
	}

	/**
	 * Test method for {@link fr.mrtigreroux.tigerreports.tasks.TestsTaskScheduler#runTaskRepeatedly(long, long, java.lang.Runnable)}.
	 */
	@Test
	void testRunTaskRepeatedly() {
		final long start = System.currentTimeMillis();
		long delay = 50;
		long period = 75;
		int repetitions = 5;
		Boolean[] results = new Boolean[repetitions];
		Boolean[] expected = new Boolean[repetitions];
		for (int i = 0; i < repetitions; i++) {
			results[i] = null;
			expected[i] = true;
		}
		AtomicInteger curResultIndex = new AtomicInteger(0);

		taskScheduler.runTaskRepeatedly(delay, period, () -> {
			synchronized (results) {
				int resultIndex = curResultIndex.getAndIncrement();
				results[resultIndex] = TestsCheckUtils.longRightValue(System.currentTimeMillis() - start,
				        delay + period * resultIndex, 5);
				if (resultIndex == repetitions - 1) {
					synchronized (SUCCESS_LOCK) {
						success = true;
						SUCCESS_LOCK.notifyAll();
					}
				}
			}
		});
		checkSuccess();
		assertArrayEquals(expected, results);
	}

	/**
	 * Test method for {@link fr.mrtigreroux.tigerreports.tasks.TestsTaskScheduler#cancelTask(int)}.
	 */
	@Test
	void testCancelTask() {
		final long start = System.currentTimeMillis();

		long delay = 50;
		long period = 75;
		int repetitions = 5;
		AtomicInteger curResultIndex = new AtomicInteger(0);
		final int taskId = taskScheduler.runTaskRepeatedly(delay, period, () -> {
			curResultIndex.getAndIncrement();
		});
		taskScheduler.runTaskDelayedly((long) (delay + (repetitions - 0.5) * period), () -> {
			taskScheduler.cancelTask(taskId);
		});

		long delay2 = (long) (delay + (repetitions + 1.5) * period);
		taskScheduler.runTaskDelayedly(delay2, SUCCESS_TASK);

		checkSuccess();

		assertTrue(TestsCheckUtils.longRightValue(System.currentTimeMillis() - start, delay2, 5));
		assertEquals(repetitions, curResultIndex.get());
	}

	/**
	 * Test methods for {@link fr.mrtigreroux.tigerreports.tasks.TestsTaskScheduler#destroyAndWaitForTermination(long)}.
	 */
	@Nested
	class DestroyAndWaitForTermination {

		@Test
		void testDestroyAndWaitForTermination() throws InterruptedException {
			TestsTaskScheduler taskScheduler = new TestsTaskScheduler();
			long delay = 50;
			long period = 75;
			int repetitions = 3;
			AtomicInteger curResultIndex = new AtomicInteger(0);
			taskScheduler.runTaskRepeatedly(delay, period, () -> {
				curResultIndex.getAndIncrement();
			});

			Thread.sleep((long) (delay + (repetitions - 0.5) * period));

			assertTrue(taskScheduler.destroyAndWaitForTermination((long) (period * 0.4)));
			int i = curResultIndex.get();
			Thread.sleep(2 * period);
			assertEquals(repetitions, i);
			assertEquals(repetitions, curResultIndex.get());
		}

		@Test
		void testDestroyAndWaitForTermination2() throws InterruptedException {
			long delay = 100L;
			AtomicBoolean taskExecuted = new AtomicBoolean(false);
			taskScheduler.runTaskDelayedly(delay, () -> {
				taskExecuted.set(true);
			});
			assertFalse(taskScheduler.destroyAndWaitForTermination((long) (delay * 0.5)));
			assertFalse(taskExecuted.get());
		}

		@Test
		void testDestroyAndWaitForTermination3() throws InterruptedException {
			long delay = 100L;
			AtomicBoolean taskExecuted = new AtomicBoolean(false);
			taskScheduler.runTaskDelayedly(delay, () -> {
				taskExecuted.set(true);
			});
			assertTrue(taskScheduler.destroyAndWaitForTermination((long) (delay * 1.5)));
			assertTrue(taskExecuted.get());
		}

		@Test
		void testDestroyAndWaitForTermination4() throws InterruptedException {
			AtomicBoolean taskExecuted = new AtomicBoolean(false);
			taskScheduler.runTaskAsynchronously(() -> {
				taskExecuted.set(true);
			});
			assertTrue(taskScheduler.destroyAndWaitForTermination(100L));
			assertTrue(taskExecuted.get());
		}

		@Test
		void testDestroyAndWaitForTermination5() throws InterruptedException {
			AtomicBoolean taskStarted = new AtomicBoolean(false);
			AtomicBoolean taskExecuted = new AtomicBoolean(false);
			AtomicBoolean taskInterrupted = new AtomicBoolean(false);
			long delay = 100L;
			taskScheduler.runTaskAsynchronously(() -> {
				taskStarted.set(true);
				try {
					Thread.sleep(delay);
					taskExecuted.set(true);
				} catch (InterruptedException e) {
					taskInterrupted.set(true);
				}
			});
			Thread.sleep((long) (delay * 0.25)); // time to start the task but not enough to execute it
			assertTrue(taskStarted.get());

			assertFalse(taskScheduler.destroyAndWaitForTermination((long) (delay * 0.5)));
			assertFalse(taskExecuted.get());
			assertFalse(taskInterrupted.get());

			Thread.sleep(delay);
			assertTrue(taskExecuted.get());
			assertFalse(taskInterrupted.get());
		}

		@Test
		void testDestroyAndWaitForTermination6() throws InterruptedException {
			AtomicBoolean taskExecuted = new AtomicBoolean(false);
			taskScheduler.runTask(() -> {
				taskExecuted.set(true);
			});
			assertTrue(taskScheduler.destroyAndWaitForTermination(100L));
			assertTrue(taskExecuted.get());
		}

		@Test
		void testDestroyAndWaitForTermination7() throws InterruptedException {
			AtomicBoolean taskStarted = new AtomicBoolean(false);
			AtomicBoolean taskExecuted = new AtomicBoolean(false);
			AtomicBoolean taskInterrupted = new AtomicBoolean(false);
			long delay = 100L;
			taskScheduler.runTask(() -> {
				taskStarted.set(true);
				try {
					Thread.sleep(delay);
					taskExecuted.set(true);
				} catch (InterruptedException e) {
					taskInterrupted.set(true);
				}
			});
			Thread.sleep((long) (delay * 0.25)); // time to start the task but not enough to execute it
			assertTrue(taskStarted.get());

			assertFalse(taskScheduler.destroyAndWaitForTermination((long) (delay * 0.5)));
			assertFalse(taskExecuted.get());
			assertFalse(taskInterrupted.get());

			Thread.sleep(delay);
			assertTrue(taskExecuted.get());
			assertFalse(taskInterrupted.get());
		}

		@Test
		void testDestroyAndWaitForTermination8() throws InterruptedException {
			AtomicBoolean taskStarted = new AtomicBoolean(false);
			AtomicBoolean taskExecuted = new AtomicBoolean(false);
			AtomicBoolean taskInterrupted = new AtomicBoolean(false);
			AtomicBoolean task2Started = new AtomicBoolean(false);
			AtomicBoolean task2Executed = new AtomicBoolean(false);
			AtomicBoolean task2Interrupted = new AtomicBoolean(false);
			AtomicBoolean task3Executed = new AtomicBoolean(false);

			long delay = 100L;
			taskScheduler.runTask(() -> {
				taskStarted.set(true);
				try {
					Thread.sleep(delay);
					taskExecuted.set(true);
				} catch (InterruptedException e) {
					taskInterrupted.set(true);
				}
			});
			taskScheduler.runTaskAsynchronously(() -> {
				task2Started.set(true);
				try {
					Thread.sleep(delay);
					task2Executed.set(true);
				} catch (InterruptedException e) {
					task2Interrupted.set(true);
				}
			});
			taskScheduler.runTaskDelayedly(delay, () -> {
				task3Executed.set(true);
			});

			Thread.sleep((long) (delay * 0.25)); // time to start the tasks but not enough to execute it
			assertTrue(taskStarted.get());
			assertTrue(task2Started.get());

			assertFalse(taskScheduler.destroyAndWaitForTermination((long) (delay * 0.5)));
			assertFalse(taskExecuted.get());
			assertFalse(taskInterrupted.get());
			assertFalse(task2Executed.get());
			assertFalse(task2Interrupted.get());
			assertFalse(task3Executed.get());

			Thread.sleep(delay);
			assertTrue(taskExecuted.get());
			assertFalse(taskInterrupted.get());
			assertTrue(task2Executed.get());
			assertFalse(task2Interrupted.get());
			assertTrue(task3Executed.get());
		}

	}

	/**
	 * Test methods for {@link fr.mrtigreroux.tigerreports.tasks.TestsTaskScheduler#destroyAndWaitForTermination(long)}.
	 */
	@Nested
	class WaitForTerminationOrStop {

		@Test
		void testWaitForTerminationOrStop() throws InterruptedException {
			TestsTaskScheduler taskScheduler = new TestsTaskScheduler();
			long delay = 50;
			long period = 75;
			int repetitions = 3;
			AtomicInteger curResultIndex = new AtomicInteger(0);
			taskScheduler.runTaskRepeatedly(delay, period, () -> {
				curResultIndex.getAndIncrement();
			});

			Thread.sleep((long) (delay + (repetitions - 0.5) * period));

			assertFalse(taskScheduler.waitForTerminationOrStop((long) (period * 0.2)));
			int i = curResultIndex.get();
			Thread.sleep(2 * period);
			assertEquals(repetitions, i);
			assertEquals(repetitions, curResultIndex.get());
		}

		@Test
		void testWaitForTermination2() throws InterruptedException {
			long delay = 100L;
			AtomicBoolean taskExecuted = new AtomicBoolean(false);
			taskScheduler.runTaskDelayedly(delay, () -> {
				taskExecuted.set(true);
			});
			assertFalse(taskScheduler.waitForTerminationOrStop((long) (delay * 0.5)));
			assertFalse(taskExecuted.get());
		}

		@Test
		void testWaitForTermination3() throws InterruptedException {
			long delay = 100L;
			AtomicBoolean taskExecuted = new AtomicBoolean(false);
			taskScheduler.runTaskDelayedly(delay, () -> {
				taskExecuted.set(true);
			});
			assertTrue(taskScheduler.waitForTerminationOrStop((long) (delay * 1.5)));
			assertTrue(taskExecuted.get());
		}

		@Test
		void testWaitForTermination4() throws InterruptedException {
			AtomicBoolean taskExecuted = new AtomicBoolean(false);
			taskScheduler.runTaskAsynchronously(() -> {
				taskExecuted.set(true);
			});
			assertTrue(taskScheduler.waitForTerminationOrStop(100L));
			assertTrue(taskExecuted.get());
		}

		@Test
		void testWaitForTermination5() throws InterruptedException {
			AtomicBoolean taskStarted = new AtomicBoolean(false);
			AtomicBoolean taskExecuted = new AtomicBoolean(false);
			AtomicBoolean taskInterrupted = new AtomicBoolean(false);
			long delay = 1000L;
			taskScheduler.runTaskAsynchronously(() -> {
				taskStarted.set(true);
				try {
					Thread.sleep(delay);
					taskExecuted.set(true);
				} catch (InterruptedException e) {
					taskInterrupted.set(true);
				}
			});
			Thread.sleep((long) (delay * 0.25)); // time to start the task but not enough to execute it
			assertTrue(taskStarted.get());

			assertFalse(taskScheduler.waitForTerminationOrStop((long) (delay * 0.05)));
			assertFalse(taskExecuted.get());
			assertTrue(taskInterrupted.get());

			Thread.sleep(delay);
			assertFalse(taskExecuted.get());
			assertTrue(taskInterrupted.get());
		}

		@Test
		void testWaitForTermination6() throws InterruptedException {
			AtomicBoolean taskExecuted = new AtomicBoolean(false);
			taskScheduler.runTask(() -> {
				taskExecuted.set(true);
			});
			assertTrue(taskScheduler.waitForTerminationOrStop(100L));
			assertTrue(taskExecuted.get());
		}

		@Test
		void testWaitForTermination7() throws InterruptedException {
			AtomicBoolean taskStarted = new AtomicBoolean(false);
			AtomicBoolean taskExecuted = new AtomicBoolean(false);
			AtomicBoolean taskInterrupted = new AtomicBoolean(false);
			long delay = 1000L;
			taskScheduler.runTask(() -> {
				taskStarted.set(true);
				try {
					Thread.sleep(delay);
					taskExecuted.set(true);
				} catch (InterruptedException e) {
					taskInterrupted.set(true);
				}
			});
			Thread.sleep((long) (delay * 0.25)); // time to start the task but not enough to execute it
			assertTrue(taskStarted.get());

			assertFalse(taskScheduler.waitForTerminationOrStop((long) (delay * 0.05)));
			assertFalse(taskExecuted.get());
			assertTrue(taskInterrupted.get());

			Thread.sleep(delay);
			assertFalse(taskExecuted.get());
			assertTrue(taskInterrupted.get());
		}

		@Test
		void testWaitForTermination8() throws InterruptedException {
			testWaitForTerminationOrStopSeveralTasks();
		}

		void testWaitForTerminationOrStopSeveralTasks() throws InterruptedException {
			AtomicBoolean taskStarted = new AtomicBoolean(false);
			AtomicBoolean taskExecuted = new AtomicBoolean(false);
			AtomicBoolean taskInterrupted = new AtomicBoolean(false);
			AtomicBoolean task2Started = new AtomicBoolean(false);
			AtomicBoolean task2Executed = new AtomicBoolean(false);
			AtomicBoolean task2Interrupted = new AtomicBoolean(false);
			AtomicBoolean task3Executed = new AtomicBoolean(false);

			long delay = 100L;
			taskScheduler.runTask(() -> {
				taskStarted.set(true);
				try {
					Thread.sleep(delay);
					taskExecuted.set(true);
				} catch (InterruptedException e) {
					taskInterrupted.set(true);
				}
			});
			taskScheduler.runTaskAsynchronously(() -> {
				task2Started.set(true);
				try {
					Thread.sleep(delay);
					task2Executed.set(true);
				} catch (InterruptedException e) {
					task2Interrupted.set(true);
				}
			});
			taskScheduler.runTaskDelayedly(delay, () -> {
				task3Executed.set(true);
			});

			Thread.sleep((long) (delay * 0.25)); // time to start the tasks but not enough to execute it
			assertTrue(taskStarted.get());
			assertTrue(task2Started.get());

			assertFalse(taskScheduler.waitForTerminationOrStop((long) (delay * 0.1)));
			assertFalse(taskExecuted.get());
			assertTrue(taskInterrupted.get());
			assertFalse(task2Executed.get());
			assertTrue(task2Interrupted.get());
			assertFalse(task3Executed.get());

			Thread.sleep(delay);
			assertFalse(taskExecuted.get());
			assertTrue(taskInterrupted.get());
			assertFalse(task2Executed.get());
			assertTrue(task2Interrupted.get());
			assertFalse(task3Executed.get());
		}

		@Test
		void testWaitForTerminationOrStopThenReuseTaskScheduler() throws InterruptedException {
			testWaitForTerminationOrStopSeveralTasks();
			testWaitForTerminationOrStopSeveralTasks();
		}

	}

	@Test
	void testDestroyNow() throws InterruptedException {
		AtomicBoolean taskStarted = new AtomicBoolean(false);
		AtomicBoolean taskExecuted = new AtomicBoolean(false);
		AtomicBoolean taskInterrupted = new AtomicBoolean(false);
		long delay = 50L;
		taskScheduler.runTaskAsynchronously(() -> {
			taskStarted.set(true);
			try {
				Thread.sleep(delay);
				taskExecuted.set(true);
			} catch (InterruptedException e) {
				taskInterrupted.set(true);
			}
		});
		Thread.sleep((long) (delay * 0.25)); // time to start the task but not enough to execute it
		assertTrue(taskStarted.get());

		taskScheduler.destroyNow();
		Thread.sleep((long) (delay * 1.5));
		assertFalse(taskExecuted.get());
		assertTrue(taskInterrupted.get());
	}

	@Test
	void testRunEmbeddedTasksDelayedly() {
		final long start = System.currentTimeMillis();
		long delay = 50;
		long delay2 = 100;

		taskScheduler.runTaskDelayedly(delay, () -> {
			taskScheduler.runTaskDelayedly(delay2, SUCCESS_TASK);
		});
		checkSuccess();
		assertTrue(TestsCheckUtils.longRightValue(System.currentTimeMillis() - start, delay + delay2, 5));
	}

	@Nested
	class GetCleanMainTaskScheduler {

		@Test
		void testGetCleanMainTaskSchedulerAfterWaitForTerminationOrStop() throws InterruptedException {
			testSeveralTasksWithWaitForTerminationOrStop(TestsTaskScheduler.getCleanMainTaskScheduler());
			LOGGER.info(() -> "testGetCleanMainTaskSchedulerAfterNonStoppedTasks(): second call");
			testSeveralTasksWithWaitForTerminationOrStop(TestsTaskScheduler.getCleanMainTaskScheduler());
		}

		@Test
		void testGetCleanMainTaskSchedulerAfterNonStoppedTasks() throws InterruptedException {
			testSeveralTasksWithoutAnyStop(TestsTaskScheduler.getCleanMainTaskScheduler());
			LOGGER.info(() -> "testGetCleanMainTaskSchedulerAfterNonStoppedTasks(): second call");
			testSeveralTasksWithoutAnyStop(TestsTaskScheduler.getCleanMainTaskScheduler());
		}

		@Test
		void testGetCleanMainTaskSchedulerAfterNonStoppedRepeatedTask() throws InterruptedException {
			TestsTaskScheduler taskScheduler = TestsTaskScheduler.getCleanMainTaskScheduler();
			long delay = 50;
			long period = 75;
			int repetitions = 3;
			AtomicInteger curResultIndex = new AtomicInteger(0);
			taskScheduler.runTaskRepeatedly(delay, period, () -> {
				curResultIndex.getAndIncrement();
			});

			Thread.sleep((long) (delay + (repetitions - 0.5) * period));

			int i = curResultIndex.get();
			assertEquals(repetitions, i);

			taskScheduler = TestsTaskScheduler.getCleanMainTaskScheduler();
			AtomicBoolean taskStarted = new AtomicBoolean(false);
			AtomicBoolean taskExecuted = new AtomicBoolean(false);
			AtomicBoolean taskInterrupted = new AtomicBoolean(false);
			taskScheduler.runTask(() -> {
				taskStarted.set(true);
				try {
					Thread.sleep(delay);
					taskExecuted.set(true);
				} catch (InterruptedException e) {
					taskInterrupted.set(true);
				}
			});

			Thread.sleep(Math.max(delay, 2 * period));
			assertTrue(taskStarted.get());
			assertTrue(taskExecuted.get());
			assertFalse(taskInterrupted.get());

			assertEquals(repetitions, i,
			        () -> "Repeated task should be stopped by TestsTaskScheduler.getCleanMainTaskScheduler");
		}

		void testSeveralTasksWithWaitForTerminationOrStop(TestsTaskScheduler taskScheduler)
		        throws InterruptedException {
			AtomicBoolean taskStarted = new AtomicBoolean(false);
			AtomicBoolean taskExecuted = new AtomicBoolean(false);
			AtomicBoolean taskInterrupted = new AtomicBoolean(false);
			AtomicBoolean task2Started = new AtomicBoolean(false);
			AtomicBoolean task2Executed = new AtomicBoolean(false);
			AtomicBoolean task2Interrupted = new AtomicBoolean(false);
			AtomicBoolean task3Executed = new AtomicBoolean(false);

			long delay = 100L;
			taskScheduler.runTask(() -> {
				taskStarted.set(true);
				try {
					Thread.sleep(delay);
					taskExecuted.set(true);
				} catch (InterruptedException e) {
					taskInterrupted.set(true);
				}
			});
			taskScheduler.runTaskAsynchronously(() -> {
				task2Started.set(true);
				try {
					Thread.sleep(delay);
					task2Executed.set(true);
				} catch (InterruptedException e) {
					task2Interrupted.set(true);
				}
			});
			taskScheduler.runTaskDelayedly(delay, () -> {
				task3Executed.set(true);
			});

			Thread.sleep((long) (delay * 0.25)); // time to start the tasks but not enough to execute it
			assertTrue(taskStarted.get());
			assertTrue(task2Started.get());

			assertFalse(taskScheduler.waitForTerminationOrStop((long) (delay * 0.5)));
			assertFalse(taskExecuted.get());
			assertTrue(taskInterrupted.get());
			assertFalse(task2Executed.get());
			assertTrue(task2Interrupted.get());
			assertFalse(task3Executed.get());

			Thread.sleep(delay);
			assertFalse(taskExecuted.get());
			assertTrue(taskInterrupted.get());
			assertFalse(task2Executed.get());
			assertTrue(task2Interrupted.get());
			assertFalse(task3Executed.get());
		}

		void testSeveralTasksWithoutAnyStop(TestsTaskScheduler taskScheduler) throws InterruptedException {
			AtomicBoolean taskStarted = new AtomicBoolean(false);
			AtomicBoolean taskExecuted = new AtomicBoolean(false);
			AtomicBoolean taskInterrupted = new AtomicBoolean(false);
			AtomicBoolean task2Started = new AtomicBoolean(false);
			AtomicBoolean task2Executed = new AtomicBoolean(false);
			AtomicBoolean task2Interrupted = new AtomicBoolean(false);
			AtomicBoolean task3Executed = new AtomicBoolean(false);

			long delay = 100L;
			taskScheduler.runTask(() -> {
				taskStarted.set(true);
				try {
					Thread.sleep(delay);
					taskExecuted.set(true);
				} catch (InterruptedException e) {
					taskInterrupted.set(true);
				}
			});
			taskScheduler.runTaskAsynchronously(() -> {
				task2Started.set(true);
				try {
					Thread.sleep(delay);
					task2Executed.set(true);
				} catch (InterruptedException e) {
					task2Interrupted.set(true);
				}
			});
			taskScheduler.runTaskDelayedly(delay, () -> {
				task3Executed.set(true);
			});

			Thread.sleep((long) (delay * 0.25)); // time to start the tasks but not enough to execute it
			assertTrue(taskStarted.get());
			assertTrue(task2Started.get());

			assertFalse(taskExecuted.get());
			assertFalse(taskInterrupted.get());
			assertFalse(task2Executed.get());
			assertFalse(task2Interrupted.get());
			assertFalse(task3Executed.get());

			Thread.sleep(delay);
			assertTrue(taskExecuted.get());
			assertFalse(taskInterrupted.get());
			assertTrue(task2Executed.get());
			assertFalse(task2Interrupted.get());
			assertTrue(task3Executed.get());
		}

	}

	@Test
	void testStopNow() throws InterruptedException {
		long delay = 50;
		long period = 75;
		int repetitions = 3;
		AtomicInteger curResultIndex = new AtomicInteger(0);
		taskScheduler.runTaskRepeatedly(delay, period, () -> {
			curResultIndex.getAndIncrement();
		});

		Thread.sleep((long) (delay + (repetitions - 0.5) * period));

		int i = curResultIndex.get();
		assertEquals(repetitions, i);

		taskScheduler.stopNow(false);

		Thread.sleep(2 * period);

		assertEquals(repetitions, i, () -> "Repeated task should be stopped");
	}

	private void checkSuccess() {
		long start = System.currentTimeMillis();
		synchronized (SUCCESS_LOCK) {
			while (!success && (System.currentTimeMillis() - start < MAX_TASK_EXECUTION_DELAY)) {
				try {
					SUCCESS_LOCK.wait(MAX_TASK_EXECUTION_DELAY);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		assertTrue(success);
	}

	@AfterEach
	public void stopTest() {
		if (taskScheduler != null) {
			taskScheduler.destroyNow();
			taskScheduler = null;
		}
	}

}
