/*
 * Copyright 2016-2021 Micro Focus or one of its affiliates.
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
package com.hpe.caf.services.job.api;

import java.util.Date;

/**
 * The expectation object
 */
public class JobServiceTrackingInfoExpectation {

    /**
     * The partition identifier of the task under test.
     */
    private final String partitionId;

    /**
     * The job task identifier of the task under test.
     */
    private String jobTaskId;

    /**
     * The last time the status of the task was checked
     */
    private Date lastStatusCheckTime;

    /**
     * The interval in milliseconds to wait between task status checks
     */
    private long statusCheckIntervalMillis;

    /**
     * The full URL address to access the status job service web method
     */
    private String statusCheckUrl;

    /**
     * The queue that job tracking worker is listening to
     */
    private String trackingPipe;

    /**
     * The final destination queue, when it is here the tracking info is removed.
     * This is also the target pipe.
     */
    private String trackingTo;

    /**
     * Indicates whether we expect there to be TrackingInfo on the message forwarded by the Job Tracking Worker in the test.
     */
    private boolean trackingInfoPresent;

    public JobServiceTrackingInfoExpectation(
        final String partitionId,
        String jobTaskId,
        Date lastStatusCheckTime,
        long statusCheckIntervalMillis,
        String statusCheckUrl,
        String trackingPipe,
        String trackingTo,
        boolean trackingInfoPresent
    ) {
        this.partitionId = partitionId;
        this.jobTaskId = jobTaskId;
        this.lastStatusCheckTime = lastStatusCheckTime;
        this.statusCheckIntervalMillis = statusCheckIntervalMillis;
        this.statusCheckUrl = statusCheckUrl;
        this.trackingPipe = trackingPipe;
        this.trackingTo = trackingTo;
        this.trackingInfoPresent = trackingInfoPresent;
    }

    public String getPartitionId() {
        return partitionId;
    }

    public String getJobTaskId() {
        return jobTaskId;
    }

    public void setJobTaskId(String jobTaskId) {
        this.jobTaskId = jobTaskId;
    }

    public Date getLastStatusCheckTime() {
        return lastStatusCheckTime;
    }

    public void setLastStatusCheckTime(Date lastStatusCheckTime) {
        this.lastStatusCheckTime = lastStatusCheckTime;
    }

    public long getStatusCheckIntervalMillis(){
        return statusCheckIntervalMillis;
    }

    public void setStatusCheckIntervalMillis(long statusCheckIntervalMillis){
        this.statusCheckIntervalMillis = statusCheckIntervalMillis;
    }

    public String getStatusCheckUrl() {
        return statusCheckUrl;
    }

    public void setStatusCheckUrl(String statusCheckUrl) {
        this.statusCheckUrl = statusCheckUrl;
    }

    public String getTrackingPipe() {
        return trackingPipe;
    }

    public void setTrackingPipe(String trackingPipe) {
        this.trackingPipe = trackingPipe;
    }

    public String getTrackingTo() {
        return trackingTo;
    }

    public void setTrackingTo(String trackingTo) {
        this.trackingTo = trackingTo;
    }

    public boolean isTrackingInfoPresent() {
        return trackingInfoPresent;
    }

    public void setTrackingInfoPresent(boolean trackingInfoPresent) {
        this.trackingInfoPresent = trackingInfoPresent;
    }
}
