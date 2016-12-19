package com.hpe.caf.worker.jobtracking;

import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 * Currently the worker framework requires every worker to issue a result message.
 * In the case of the Job Tracking worker the result message has no value so is effectively a dummy message.
 * It will never be consumed and will eventually time out.
 * This will remain the case until CAF-601 "Enhance Worker Framework to support Datagram MEP" is addressed.
 */
public class JobTrackingWorkerResult {
    /**
     * Worker-specific return code.
     */
    @NotNull
    private JobTrackingWorkerStatus status;


    public JobTrackingWorkerStatus getStatus() {
        return status;
    }


    public void setStatus(JobTrackingWorkerStatus status) {
        this.status = status;
    }


    public JobTrackingWorkerResult() {
    }


    public JobTrackingWorkerResult(JobTrackingWorkerStatus status) {
        this.status = Objects.requireNonNull(status);
    }
}
