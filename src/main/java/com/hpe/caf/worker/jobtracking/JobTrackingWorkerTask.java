package com.hpe.caf.worker.jobtracking;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.Objects;

/**
 * This task is used to inform the Job Tracking worker of the progress of another task.
 * EITHER jobTaskId OR proxiedTaskInfo should have a value.
 */
public class JobTrackingWorkerTask {
    /**
     * The jobTaskId of the tracked task whose progress is being reported.
     * If this has a value then proxiedTaskInfo should be null.
     */
    private String jobTaskId;

    /**
     * Details of a proxied task.
     * If this has a value then jobTaskId should be null.
     */
    private ProxiedTaskInfo proxiedTaskInfo;


    @Min(0)
    @Max(100)
    private int estimatedPercentageCompleted;


    public String getJobTaskId() {
        return jobTaskId;
    }


    public void setJobTaskId(String jobTaskId) {
        this.jobTaskId = Objects.requireNonNull(jobTaskId);
        this.proxiedTaskInfo = null;
    }


    public ProxiedTaskInfo getProxiedTaskInfo() {
        return proxiedTaskInfo;
    }


    public void setProxiedTaskInfo(ProxiedTaskInfo proxiedTaskInfo) {
        this.proxiedTaskInfo = proxiedTaskInfo;
        this.jobTaskId = null;
    }

    public int getEstimatedPercentageCompleted() {
        return estimatedPercentageCompleted;
    }


    public void setEstimatedPercentageCompleted(int estimatedPercentageCompleted) {
        this.estimatedPercentageCompleted = estimatedPercentageCompleted;
    }
}
