/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
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
 * Represents the expected results of an integration test.
 */
public class JobTrackingWorkerITExpectation {
    /**
     * The partition containing the job under test.
     */
    final private String partition;

    /**
     * The job task identifier of the task under test.
     */
    private String jobTaskId;

    /**
     * The queue to which the Job Tracking Worker is expected to forward the result message from the test.
     */
    private String forwardingQueue;

    /**
     * Indicates whether we expect there to be TrackingInfo on the message forwarded by the Job Tracking Worker in the test.
     */
    private boolean trackingInfoPresent;

    /**
     * The specific values expected to be reported in the Job Database for the test.
     */
    private JobReportingExpectation jobReportingExpectation;


    public JobTrackingWorkerITExpectation(
        final String partition,
        String jobTaskId,
        String forwardingQueue,
        boolean trackingInfoPresent,
        JobReportingExpectation jobReportingExpectation
    ) {
        this.partition = partition;
        this.jobTaskId = jobTaskId;
        this.forwardingQueue = forwardingQueue;
        this.trackingInfoPresent = trackingInfoPresent;
        this.jobReportingExpectation = jobReportingExpectation;
    }

    public String getPartition() {
        return partition;
    }

    public String getJobTaskId() {
        return jobTaskId;
    }


    public void setJobTaskId(String jobTaskId) {
        this.jobTaskId = jobTaskId;
    }

    public String getForwardingQueue() {
        return forwardingQueue;
    }


    public void setForwardingQueue(String forwardingQueue) {
        this.forwardingQueue = forwardingQueue;
    }


    public boolean isTrackingInfoPresent() {
        return trackingInfoPresent;
    }


    public void setTrackingInfoPresent(boolean trackingInfoPresent) {
        this.trackingInfoPresent = trackingInfoPresent;
    }

    public JobReportingExpectation getJobReportingExpectation() {
        return jobReportingExpectation;
    }


    public void setJobReportingExpectation(JobReportingExpectation jobReportingExpectation) {
        this.jobReportingExpectation = jobReportingExpectation;
    }
}
