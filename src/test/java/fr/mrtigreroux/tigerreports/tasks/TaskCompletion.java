package fr.mrtigreroux.tigerreports.tasks;

/**
 * @author MrTigreroux
 */
public class TaskCompletion {
    
    private boolean done = false;
    
    public synchronized void setDone() {
        done = true;
        this.notifyAll();
    }
    
    /**
     * Make the current thread wait for task completion for maximum timeout ms if it is not
     * interrupted.
     * 
     * @param timeout in ms
     * 
     * @return if task done
     */
    public synchronized boolean waitForCompletion(long timeout) {
        long start = System.currentTimeMillis();
        while (!done && (System.currentTimeMillis() - start <= timeout)) {
            try {
                this.wait(timeout);
            } catch (InterruptedException e) {
                e.printStackTrace();
                return done;
            }
        }
        return done;
    }
    
}
