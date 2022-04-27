/*
 * Copyright 2016-2022 Micro Focus or one of its affiliates.
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

import java.time.Instant;

/**
 * Represents a database record for the job table.
 */
public class DBJob {

    private String jobId;
    private JobStatus status;
    private float percentageComplete;
    private Instant createDate;
    private Instant lastUpdateDate;
    private String failureDetails;

    public void setJobId(final String jobId) {
        this.jobId = jobId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setStatus(final JobStatus jobStatus) {
        this.status = jobStatus;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setPercentageComplete(final float percentageComplete) {
        this.percentageComplete = percentageComplete;
    }

    public float getPercentageComplete() {
        return percentageComplete;
    }

    public void setCreateDate(final Instant createDate) {
        this.createDate = createDate;
    }

    public Instant getCreateDate() {
        return createDate;
    }

    public void setLastUpdateDate(final Instant lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }

    public Instant getLastUpdateDate() {
        return lastUpdateDate;
    }

    public void setFailureDetails(final String failureDetails) {
        this.failureDetails = failureDetails;
    }

    public String getFailureDetails() {
        return failureDetails;
    }

}
