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

import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.api.worker.TrackingInfo;
import com.hpe.caf.services.job.util.JobTaskId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.UriBuilder;
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
                                                            final String trackingPipe,
                                                            final String correlationId)
    {
        //  Generate a random task id.
        final String taskId = UUID.randomUUID().toString();

        //  Set up string for statusCheckUrl
        final String statusCheckUrl = UriBuilder.fromUri(System.getenv("CAF_WEBSERVICE_URL") )
            .path("partitions").path(jobDependency.getPartitionId())
            .path("jobs").path(jobDependency.getJobId())
            .path("status").build().toString();

        //  Construct the task message.
        String statusCheckIntervalSeconds = System.getenv("CAF_STATUS_CHECK_INTERVAL_SECONDS");
        if (null == statusCheckIntervalSeconds) {
            // Default to 5 if the environment variable is not present.  This is to avoid introducing a breaking change.
            statusCheckIntervalSeconds = "5";
        }

        final TrackingInfo trackingInfo = new TrackingInfo(
                new JobTaskId(jobDependency.getPartitionId(), jobDependency.getJobId()).getMessageId(),
                new Date(), getStatusCheckIntervalMillis(statusCheckIntervalSeconds), statusCheckUrl, trackingPipe, jobDependency.getTargetPipe());

        return new TaskMessage(
                taskId,
                jobDependency.getTaskClassifier(),
                jobDependency.getTaskApiVersion(),
                jobDependency.getTaskData(),
                TaskStatus.NEW_TASK,
                Collections.<String, byte[]>emptyMap(),
                jobDependency.getTaskPipe(),
                trackingInfo,
                null,
                correlationId);
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

    public static long getMaxBatchTime()
    {
        final String maxBatchTime = System.getenv("CAF_WORKER_MAX_BATCH_TIME");

        // Default to 10000 if the environment variable is not present.
        if (null == maxBatchTime) {
            return 10000;
        }

        return Long.parseLong(maxBatchTime);
    }

    private static long getStatusCheckIntervalMillis(final String statusCheckIntervalSeconds)
    {
        try {
            return Long.parseLong(statusCheckIntervalSeconds) * 1000;
        } catch (NumberFormatException e) {
            throw new RuntimeException("Please provide a valid integer for statusCheckIntervalSeconds. " + e);
        }
    }

}
