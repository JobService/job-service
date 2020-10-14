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

import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.api.worker.TrackingInfo;
import com.hpe.caf.services.job.util.JobTaskId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

public final class JobTrackingWorkerUtil
{
    private JobTrackingWorkerUtil(){}

    private static final Logger LOG = LoggerFactory.getLogger(JobTrackingWorkerUtil.class);

    /**
     *  Start the list of jobs that are now available to be run.
     *
     * @param jobDependency dependent job that is now available for processing
     * @param trackingPipe target pipe where dependent jobs should be forwarded to.
     */
    public static TaskMessage createDependentJobTaskMessage(final JobTrackingWorkerDependency jobDependency,
                                                            final String trackingPipe)
    {
        //  Generate a random task id.
        final String taskId = UUID.randomUUID().toString();

        //  Set up string for statusCheckUrl
        final String statusCheckUrl = UriBuilder.fromUri(System.getenv("CAF_WEBSERVICE_URL") )
            .path("partitions").path(jobDependency.getPartitionId())
            .path("jobs").path(jobDependency.getJobId())
            .path("isActive").build().toString();

        //  Construct the task message.
        String statusCheckTime = System.getenv("CAF_STATUS_CHECK_TIME");
        if (null == statusCheckTime) {
            // Default to 5 if the environment variable is not present.  This is to avoid introducing a breaking change.
            statusCheckTime = "5";
        }

        final TrackingInfo trackingInfo = new TrackingInfo(
                new JobTaskId(jobDependency.getPartitionId(), jobDependency.getJobId()).getMessageId(),
                calculateStatusCheckDate(statusCheckTime), statusCheckUrl, trackingPipe, jobDependency.getTargetPipe());

        return new TaskMessage(
                taskId,
                jobDependency.getTaskClassifier(),
                jobDependency.getTaskApiVersion(),
                jobDependency.getTaskData(),
                TaskStatus.NEW_TASK,
                Collections.<String, byte[]>emptyMap(),
                jobDependency.getTaskPipe(),
                trackingInfo);
    }

    /**
     * If an error in the worker occurs, create a new JobTrackingWorkerResult with the corresponding worker
     * failure status.
     *
     * @param status indicates whether or not the worker processed the task.
     */
    public static JobTrackingWorkerResult createErrorResult(final JobTrackingWorkerStatus status){
        final JobTrackingWorkerResult workerResult = new JobTrackingWorkerResult();
        workerResult.setStatus(status);
        return workerResult;
    }

    /**
     * If an error in the worker occurs, create a new JobTrackingWorkerResult with the corresponding worker
     * failure status and message.
     *
     * @param status indicates whether or not the worker processed the task.
     * @param message a note about the result.
     */
    public static JobTrackingWorkerResult createErrorResult(final JobTrackingWorkerStatus status,
                                                            final String message){
        final JobTrackingWorkerResult workerResult = new JobTrackingWorkerResult();
        workerResult.setStatus(status);
        workerResult.setMessage(message);
        return workerResult;
    }

    public static final String getmaxWaitingTime() {
        String maxWaitingTime = System.getenv("CAF_MAX_WAITING_TIME");
        if (null == maxWaitingTime) {
            // Default to 10000 if the environment variable is not present.  This is to avoid introducing a breaking change.
            maxWaitingTime = "10000";
        }
        return maxWaitingTime;
    }

    /**
     * Calculates the date of the next status check to be performed.
     *
     * @param statusCheckTime - This is the number of seconds after which it is appropriate to try to confirm that the
     * task has not been cancelled or aborted.
     */
    public static Date calculateStatusCheckDate(final String statusCheckTime){
        //  Make sure statusCheckTime is a valid long
        long seconds = 0;
        try{
            seconds = Long.parseLong(statusCheckTime);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Please provide a valid integer for statusCheckTime in seconds. " + e);
        }

        //  Set up date for statusCheckTime. Get current date-time and add statusCheckTime seconds.
        final Instant now = Instant.now();
        final Instant later = now.plusSeconds(seconds);
        return java.util.Date.from( later );
    }

}
