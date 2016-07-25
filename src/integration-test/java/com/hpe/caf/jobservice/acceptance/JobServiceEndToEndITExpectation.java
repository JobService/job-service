package com.hpe.caf.jobservice.acceptance;

import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.worker.example.ExampleWorkerStatus;

import java.util.List;

/**
 * Holds values to be used to verify test results.
 */
public class JobServiceEndToEndITExpectation {
    private final boolean expectJobCancellation;
    private final String trackTo;
    private final String jobId;
    private final String correlationId;
    private final String taskClassifier;
    private final int taskApiVersion;
    private final TaskStatus taskStatus;
    private final ExampleWorkerStatus workerResultStatus;
    private final List<String> workerItemAssetIds;

    public JobServiceEndToEndITExpectation(boolean expectJobCancellation, String trackTo, String jobId, String correlationId, String taskClassifier, int taskApiVersion, TaskStatus taskStatus, ExampleWorkerStatus workerResultStatus, List<String> workerItemAssetIds) {
        this.expectJobCancellation = expectJobCancellation;
        this.trackTo = trackTo;
        this.jobId = jobId;
        this.correlationId = correlationId;
        this.taskClassifier = taskClassifier;
        this.taskApiVersion = taskApiVersion;
        this.taskStatus = taskStatus;
        this.workerResultStatus = workerResultStatus;
        this.workerItemAssetIds = workerItemAssetIds;
    }

    public boolean isExpectJobCancellation() {
        return expectJobCancellation;
    }

    public String getTrackTo() {
        return trackTo;
    }

    public String getJobId() {
        return jobId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getTaskClassifier() {
        return taskClassifier;
    }

    public int getTaskApiVersion() {
        return taskApiVersion;
    }

    public TaskStatus getTaskStatus() {
        return taskStatus;
    }

    public ExampleWorkerStatus getWorkerResultStatus() {
        return workerResultStatus;
    }

    public List<String> getWorkerItemAssetIds() {
        return workerItemAssetIds;
    }
}
