package fr.mrtigreroux.tigerreports.tasks;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import fr.mrtigreroux.tigerreports.logs.Logger;

public class SeveralTasksHandler<R> {

    private static final Logger LOGGER = Logger.fromClass(SeveralTasksHandler.class);

    private final List<R> tasksResult = new ArrayList<R>();
    private final List<Boolean> tasksDone = new ArrayList<Boolean>();
    private ResultCallback<List<R>> resultCallback;
    private boolean allTasksAdded = false;
    private boolean removeNullResults = false;

    public SeveralTasksHandler() {}

    /**
     * Adds a task result slot and gives a {@code ResultCallback<R>} to use for a task.
     * 
     * @return
     * @throws IllegalStateException if {@link #whenAllTasksDone(ResultCallback)} has been called before
     */
    public ResultCallback<R> newTaskResultSlot() throws IllegalStateException {
        if (this.allTasksAdded) {
            throw new IllegalStateException("Cannot add a task result slot when all tasks are marked as added");
        }

        int taskIndex;

        synchronized (tasksResult) {
            taskIndex = tasksResult.size();
            tasksResult.add(null);
            tasksDone.add(false);
        }

        LOGGER.info(() -> this + ": added result slot of index " + taskIndex);

        return (result) -> {
            LOGGER.info(() -> SeveralTasksHandler.this + ": received result of task " + taskIndex);
            saveTaskResult(result, taskIndex);
            checkAllTasksDone();
        };
    }

    public void whenAllTasksDone(boolean removeNullResults, ResultCallback<List<R>> resultCallback) {
        this.resultCallback = Objects.requireNonNull(resultCallback);
        this.allTasksAdded = true;
        this.removeNullResults = removeNullResults;
        checkAllTasksDone();
    }

    private void saveTaskResult(R result, int taskIndex) {
        synchronized (tasksResult) {
            tasksResult.set(taskIndex, result);
            tasksDone.set(taskIndex, true);
        }
        LOGGER.info(() -> this + ": saved result of task " + taskIndex);
    }

    private void checkAllTasksDone() {
        if (!allTasksAdded) {
            return;
        }

        synchronized (tasksResult) {
            for (Boolean taskDone : tasksDone) {
                if (Boolean.FALSE.equals(taskDone)) {
                    return;
                }
            }

            if (removeNullResults) {
                int removedAmount = 0;
                Iterator<R> it = tasksResult.iterator();
                while (it.hasNext()) {
                    if (it.next() == null) {
                        it.remove();
                        removedAmount++;
                    }
                }
                final int fremovedAmount = removedAmount;
                LOGGER.info(() -> this + ": all tasks done, removed " + fremovedAmount + " null results");
            }
        }

        LOGGER.info(() -> this + ": all tasks done, send list of results");
        resultCallback.onResultReceived(tasksResult);
        return;
    }

}
