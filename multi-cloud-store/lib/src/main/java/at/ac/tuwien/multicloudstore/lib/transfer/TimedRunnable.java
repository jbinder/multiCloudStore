package at.ac.tuwien.multicloudstore.lib.transfer;

public interface TimedRunnable extends Runnable {
    /**
     * Use this method to check if the task has been completed.
     * @return true if the task has been completed, false else
     */
    boolean isComplete();
}
