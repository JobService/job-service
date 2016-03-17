package com.hpe.caf.worker.jobtracking;

public enum JobTrackingEventType {
    /**
     * The tracked task is in progress.
     */
    IN_PROGRESS,

    /**
     * The tracked task message was retried , i.e. submitted to the retry queue.
     */
    RETRIED,

    /**
     * The tracked task message was rejected, i.e. submitted to the rejected queue.
     */
    REJECTED
}
