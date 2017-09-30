/*
 * Copyright 2015-2017 EntIT Software LLC, a Micro Focus company.
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


    /**
     * The task's estimated percentage complete.
     */
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
