package com.hpe.caf.worker.jobtracking;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 * This task is used to inform the Job Tracking worker of the progress of another task.
 */
public class JobTrackingWorkerTask {
    /**
     * The jobTaskId of the tracked task whose progress is being reported.
     */
    @NotNull
    private String jobTaskId;


    @Min(0)
    @Max(100)
    private int estimatedPercentageCompleted;


    public String getJobTaskId() {
        return jobTaskId;
    }


    public void setJobTaskId(String jobTaskId) {
        this.jobTaskId = Objects.requireNonNull(jobTaskId);
    }


    public int getEstimatedPercentageCompleted() {
        return estimatedPercentageCompleted;
    }


    public void setEstimatedPercentageCompleted(int estimatedPercentageCompleted) {
        this.estimatedPercentageCompleted = estimatedPercentageCompleted;
    }
}
