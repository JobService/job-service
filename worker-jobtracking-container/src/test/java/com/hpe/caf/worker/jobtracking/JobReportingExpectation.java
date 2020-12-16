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
package com.hpe.caf.worker.jobtracking;

/**
 * Holds the values expected from an integration test, to be compared against the reported state of a job in the Job Database.
 */
public class JobReportingExpectation {
    private String jobId;
    private JobStatus status;
    private int percentageComplete;
    private boolean failureDetailsPresent;
    private boolean failureDetailsIdPresent;
    private boolean failureDetailsTimePresent;
    private boolean failureDetailsSourcePresent;
    private boolean failureDetailsMessagePresent;

    public JobReportingExpectation(String jobId, JobStatus status, int percentageComplete, boolean failureDetailsPresent,
            boolean failureDetailsIdPresent, boolean failureDetailsTimePresent, boolean failureDetailsSourcePresent, boolean failureDetailsMessagePresent) {
        this.jobId = jobId;
        this.status = status;
        this.percentageComplete = percentageComplete;
        this.failureDetailsPresent = failureDetailsPresent;
        this.failureDetailsIdPresent = failureDetailsIdPresent;
        this.failureDetailsTimePresent = failureDetailsTimePresent;
        this.failureDetailsSourcePresent = failureDetailsSourcePresent;
        this.failureDetailsMessagePresent = failureDetailsMessagePresent;
    }


    public String getJobId() {
        return jobId;
    }


    public void setJobId(String jobId) {
        this.jobId = jobId;
    }


    public JobStatus getStatus() {
        return status;
    }


    public void setStatus(JobStatus status) {
        this.status = status;
    }


    public float getPercentageComplete() {
        return percentageComplete;
    }


    public void setPercentageComplete(int percentageComplete) {
        this.percentageComplete = percentageComplete;
    }

    public boolean getFailureDetailsPresent() {
        return failureDetailsPresent;
    }


    public void setFailureDetailsPresent(boolean failureDetailsPresent) {
        this.failureDetailsPresent = failureDetailsPresent;
    }

    public boolean getFailureDetailsIdPresent() {
        return failureDetailsIdPresent;
    }


    public void setFailureDetailsIdPresent(boolean failureDetailsIdPresent) {
        this.failureDetailsIdPresent = failureDetailsIdPresent;
    }

    public boolean getFailureDetailsTimePresent() {
        return failureDetailsTimePresent;
    }


    public void setFailureDetailsTimePresent(boolean failureDetailsTimePresent) {
        this.failureDetailsTimePresent = failureDetailsTimePresent;
    }

    public boolean getFailureDetailsSourcePresent() {
        return failureDetailsSourcePresent;
    }


    public void setFailureDetailsSourcePresent(boolean failureDetailsSourcePresent) {
        this.failureDetailsSourcePresent = failureDetailsSourcePresent;
    }

    public boolean getFailureDetailsMessagePresent() {
        return failureDetailsMessagePresent;
    }


    public void setFailureDetailsMessagePresent(boolean failureDetailsMessagePresent) {
        this.failureDetailsMessagePresent = failureDetailsMessagePresent;
    }
}
