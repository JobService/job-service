/*
 * Copyright 2016-2020 Micro Focus or one of its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
    private final String partitionId;
    private final String jobId;
    private final String correlationId;
    private final String taskClassifier;
    private final int taskApiVersion;
    private final TaskStatus taskStatus;
    private final ExampleWorkerStatus workerResultStatus;
    private final List<String> workerItemAssetIds;

    public JobServiceEndToEndITExpectation(boolean expectJobCancellation, String trackTo, final String partitionId, String jobId, String correlationId, String taskClassifier, int taskApiVersion, TaskStatus taskStatus, ExampleWorkerStatus workerResultStatus, List<String> workerItemAssetIds) {
        this.expectJobCancellation = expectJobCancellation;
        this.trackTo = trackTo;
        this.partitionId = partitionId;
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

    public String getPartitionId() {
        return partitionId;
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
