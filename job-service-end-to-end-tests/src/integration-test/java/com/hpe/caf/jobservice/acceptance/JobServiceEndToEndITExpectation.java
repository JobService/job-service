package com.hpe.caf.jobservice.acceptance;

import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.worker.example.ExampleWorkerStatus;

import java.util.List;

/**
 * Holds values to be used to verify test results.
 */
public class JobServiceEndToEndITExpectation {
    private String trackTo;
    private String jobId;
    private String correlationId;
    private String taskClassifier;
    private int taskApiVersion;
    private TaskStatus taskStatus;
    private ExampleWorkerStatus workerResultStatus;
    private List<String> workerItemAssetIds;

    public JobServiceEndToEndITExpectation(String trackTo, String jobId, String correlationId, String taskClassifier, int taskApiVersion, TaskStatus taskStatus, ExampleWorkerStatus workerResultStatus, List<String> workerItemAssetIds) {
        this.trackTo = trackTo;
        this.jobId = jobId;
        this.correlationId = correlationId;
        this.taskClassifier = taskClassifier;
        this.taskApiVersion = taskApiVersion;
        this.taskStatus = taskStatus;
        this.workerResultStatus = workerResultStatus;
        this.workerItemAssetIds = workerItemAssetIds;
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
