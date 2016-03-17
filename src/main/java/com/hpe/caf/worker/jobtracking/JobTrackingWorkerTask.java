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
     * The taskId of the other task whose progress is being reported.
     */
    @NotNull
    private String trackedTaskId;


    /**
     * The type of event being reported about the task identified by trackedTaskId.
     */
    private JobTrackingEventType eventType;


    @Min(0)
    @Max(100)
    private int estimatedPercentageCompleted;


    public String getTrackedTaskId() {
        return trackedTaskId;
    }


    public void setTrackedTaskId(String trackedTaskId) {
        this.trackedTaskId = Objects.requireNonNull(trackedTaskId);
    }


    public JobTrackingEventType getEventType() {
        return eventType;
    }


    public void setEventType(JobTrackingEventType eventType) {
        this.eventType = eventType;
    }


    public int getEstimatedPercentageCompleted() {
        return estimatedPercentageCompleted;
    }


    public void setEstimatedPercentageCompleted(int estimatedPercentageCompleted) {
        this.estimatedPercentageCompleted = estimatedPercentageCompleted;
    }
}
