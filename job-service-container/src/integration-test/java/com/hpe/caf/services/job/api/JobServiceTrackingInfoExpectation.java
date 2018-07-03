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
package com.hpe.caf.services.job.api;

/**
 * The expectation object
 */
public class JobServiceTrackingInfoExpectation {

    /**
     * The job task identifier of the task under test.
     */
    private String jobTaskId;

    /**
     * The status check time
     */
    private String statusCheckTime;

    /**
     * The full URL address to access the isActive job service web method
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

    public JobServiceTrackingInfoExpectation(String jobTaskId, String statusCheckTime, String statusCheckUrl, String trackingPipe, String trackingTo, boolean trackingInfoPresent) {
        this.jobTaskId = jobTaskId;
        this.statusCheckTime = statusCheckTime;
        this.statusCheckUrl = statusCheckUrl;
        this.trackingPipe = trackingPipe;
        this.trackingTo = trackingTo;
        this.trackingInfoPresent = trackingInfoPresent;
    }

    public String getJobTaskId() {
        return jobTaskId;
    }

    public void setJobTaskId(String jobTaskId) {
        this.jobTaskId = jobTaskId;
    }

    public String getStatusCheckTime() {
        return statusCheckTime;
    }

    public void setStatusCheckTime(String statusCheckTime) {
        this.statusCheckTime = statusCheckTime;
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
