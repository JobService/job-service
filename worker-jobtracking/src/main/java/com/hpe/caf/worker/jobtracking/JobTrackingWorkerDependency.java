/*
 * Copyright 2016-2024 Open Text.
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
package com.hpe.caf.worker.jobtracking;

import jakarta.validation.constraints.NotNull;
import java.util.Objects;

/**
 * This class is used by the Job Tracking worker to run dependent jobs that are now available.
 */
public class JobTrackingWorkerDependency
{
    @NotNull
    private String partitionId;
    @NotNull
    private String jobId;
    @NotNull
    private String taskClassifier;
    @NotNull
    private int taskApiVersion;
    @NotNull
    private byte[] taskData;
    @NotNull
    private String taskPipe;
    @NotNull
    private String targetPipe;

    public String getPartitionId() {
        return partitionId;
    }

    public void setPartitionId(final String partitionId) {
        this.partitionId = Objects.requireNonNull(partitionId);
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = Objects.requireNonNull(jobId);
    }

    public String getTaskClassifier() {
        return taskClassifier;
    }

    public void setTaskClassifier(String taskClassifier) {
        this.taskClassifier = Objects.requireNonNull(taskClassifier);
    }

    public int getTaskApiVersion() {
        return taskApiVersion;
    }

    public void setTaskApiVersion(int taskApiVersion) {
        this.taskApiVersion = Objects.requireNonNull(taskApiVersion);
    }

    public byte[] getTaskData() {
        return taskData;
    }

    public void setTaskData(byte[] taskData) {
        this.taskData = Objects.requireNonNull(taskData);
    }

    public String getTaskPipe() {
        return taskPipe;
    }

    public void setTaskPipe(String taskPipe) {
        this.taskPipe = Objects.requireNonNull(taskPipe);
    }

    public String getTargetPipe() {
        return targetPipe;
    }

    public void setTargetPipe(String targetPipe) {
        //Raise exception if targetPipe is empty. Null targetPipe is valid
        if (targetPipe != null && targetPipe.isEmpty())
            throw new IllegalArgumentException("Target Pipe is empty");
        this.targetPipe = targetPipe;
    }
}
