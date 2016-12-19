package com.hpe.caf.worker.jobtracking;


public enum JobTrackingWorkerStatus {

    /**
     * Worker processed task and was successful.
     */
    COMPLETED,

    /**
     * Worker failed to report job task progress
     */
    PROGRESS_UPDATE_FAILED
}
