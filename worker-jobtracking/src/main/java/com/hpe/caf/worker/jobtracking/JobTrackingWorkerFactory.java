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

import com.hpe.caf.api.*;
import com.hpe.caf.api.worker.*;
import com.hpe.caf.util.rabbitmq.RabbitHeaders;
import com.hpe.caf.worker.AbstractWorkerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Factory class for creating a JobTrackingWorker.
 * The Job Tracking Worker application is special in that it is both a normal Worker application that receives
 * messages that were intended for it (although they are Event Messages rather than Document Messages), and it also
 * acts as a Proxy, routing messages that were not ultimately intended for it to the correct Worker (although the
 * actual message forwarding will be done by Worker Framework code). Messages will typically arrive at the
 * Job Tracking Worker because the pipe that it is consuming messages from is specified as the trackingPipe
 * (which will trigger the Worker Framework to re-route output messages).
 * The Job Tracking Worker reports the progress of the task to the Job Database and, if the job is active,
 * it will be forwarded to the correct destination pipe.
 */
public class JobTrackingWorkerFactory extends AbstractWorkerFactory<JobTrackingWorkerConfiguration, JobTrackingWorkerTask> implements TaskMessageForwardingEvaluator {

    private static final Logger LOG = LoggerFactory.getLogger(JobTrackingWorkerFactory.class);

    @NotNull
    private JobTrackingReporter reporter;

    /**
     * Constructor for JobTrackingWorkerFactory called by JobTrackingWorkerFactoryProvider
     */
    public JobTrackingWorkerFactory(ConfigurationSource configSource, DataStore store, Codec codec) throws WorkerException {
        super(configSource, store, codec, JobTrackingWorkerConfiguration.class, JobTrackingWorkerTask.class);
        this.reporter = createReporter();
    }

    /**
     * Constructor for JobTrackingWorkerFactory called by JobTrackingWorkerFactoryProvider
     */
    public JobTrackingWorkerFactory(ConfigurationSource configSource, DataStore store, Codec codec, JobTrackingReporter reporter) throws WorkerException {
        super(configSource, store, codec, JobTrackingWorkerConfiguration.class, JobTrackingWorkerTask.class);
        this.reporter = reporter;
    }


    @Override
    protected String getWorkerName() {
        return JobTrackingWorkerConstants.WORKER_NAME;
    }


    @Override
    protected int getWorkerApiVersion() {
        return JobTrackingWorkerConstants.WORKER_API_VER;
    }

    /**
     * Create a worker to process the given task.
     */
    @Override
    public Worker createWorker(JobTrackingWorkerTask task) throws TaskRejectedException, InvalidTaskException {
        return new JobTrackingWorker(task, getConfiguration().getOutputQueue(), getCodec(), reporter);
    }


    @Override
    public String getInvalidTaskQueue() {
        return getConfiguration().getOutputQueue();
    }


    @Override
    public int getWorkerThreads() {
        return getConfiguration().getThreads();
    }

    /**
     * Health check which returns healthy if the Job Tracking Worker components are available.
     * @return
     */
    @Override
    public HealthResult healthCheck() {
        try {
            JobTrackingWorkerHealthCheck healthCheck = new JobTrackingWorkerHealthCheck(reporter);
            return healthCheck.healthCheck();
        } catch (Exception e) {
            return new HealthResult(HealthStatus.UNHEALTHY, "Failed to perform Job Tracking Worker health check. " + e.getMessage());
        }
    }


    /**
     * Reports the progress of the proxied task message, then forwards it to its destination.
     * @param proxiedTaskMessage the proxied task message being diverted via the Job Tracking Worker
     * @param queueMessageId the reference to the message this task arrived on
     * @param headers the map of key/value paired headers to be stamped on the message
     * @param callback worker callback to enact the forwarding action determined by the worker
     */
    @Override
    public void determineForwardingAction(TaskMessage proxiedTaskMessage, String queueMessageId, Map<String, Object> headers, WorkerCallback callback) {
        
        ResultSet resultSet = reportProxiedTask(proxiedTaskMessage, headers);

        // Forward any dependent jobs which are now available for processing
        try {
            forwardAvailableJobs(resultSet, callback, proxiedTaskMessage.getTracking().getTrackingPipe());
        } catch (Exception e) {
            LOG.error("Failed to create dependent jobs.");
            throw new RuntimeException("Failed to create dependent jobs.", e);
        }

        LOG.debug("Forwarding task {}", proxiedTaskMessage.getTaskId());
        callback.forward(queueMessageId, proxiedTaskMessage.getTo(), proxiedTaskMessage, headers);
    }

    /**
     * Report the task's status to the job database.
     *
     * @param proxiedTaskMessage the task to be reported
     * @param headers task headers such as if the task was rejected
     * @return ResultSet containing any dependent jobs that are now available for processing else
     *         returns null
     */
    private ResultSet reportProxiedTask(final TaskMessage proxiedTaskMessage, Map<String, Object> headers) {
        ResultSet resultSet = null;
        try {
            TrackingInfo tracking = proxiedTaskMessage.getTracking();
            if (tracking == null) {
                LOG.warn("Cannot report job task progress for task {} - the task message has no tracking info", proxiedTaskMessage.getTaskId());
                return null;
            }

            String jobTaskId = tracking.getJobTaskId();
            if (jobTaskId == null) {
                LOG.warn("Cannot report job task progress for task {} - the tracking info has no jobTaskId", proxiedTaskMessage.getTaskId());
                return null;
            }

            String trackToPipe = tracking.getTrackTo();
            if (trackToPipe == null) {
                LOG.warn("Cannot evaluate job task progress for job task {} in worker task {} - the tracking info has no trackTo pipe", jobTaskId, proxiedTaskMessage.getTaskId());
                return null;
            }

            TaskStatus taskStatus = proxiedTaskMessage.getTaskStatus();

            if (taskStatus == TaskStatus.NEW_TASK || taskStatus == TaskStatus.RESULT_SUCCESS || taskStatus == TaskStatus.RESULT_FAILURE) {
                String toPipe = proxiedTaskMessage.getTo();
                if (trackToPipe.equalsIgnoreCase(toPipe)) {
                    // Now returns a ResultSet.  This ResultSet may or may not contain a list of dependent job info.
                    resultSet = reporter.reportJobTaskComplete(jobTaskId);
                } else {
                    //TODO - FUTURE: supply an accurate estimatedPercentageCompleted
                    reporter.reportJobTaskProgress(jobTaskId, 0);
                }
                return resultSet;
            }

            if (taskStatus == TaskStatus.RESULT_EXCEPTION || taskStatus == TaskStatus.INVALID_TASK) {

                //  Failed to execute job task.
                JobTrackingWorkerFailure f = new JobTrackingWorkerFailure();
                f.setFailureId(taskStatus.toString());
                f.setFailureTime(new Date());
                f.setFailureSource(getWorkerName(proxiedTaskMessage));

                final byte[] taskData = proxiedTaskMessage.getTaskData();
                if (taskData != null) {
                    f.setFailureMessage(new String(taskData, StandardCharsets.UTF_8));
                }

                reporter.reportJobTaskRejected(jobTaskId, f);

                return null;
            }

            boolean rejected = headers.getOrDefault(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_REJECTED, null) != null;
            int retries = Integer.parseInt(String.valueOf(headers.getOrDefault(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_RETRY, "0")));
            if (rejected) {
                String rejectedHeader = String.valueOf(headers.get(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_REJECTED));
                String rejectionDetails = MessageFormat.format("{0}. Execution of this job task was retried {1} times.", rejectedHeader, retries);

                //  Failed to execute job task.
                JobTrackingWorkerFailure f = new JobTrackingWorkerFailure();
                f.setFailureId(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_REJECTED);
                f.setFailureTime(new Date());
                f.setFailureSource(getWorkerName(proxiedTaskMessage));
                f.setFailureMessage(rejectionDetails);

                reporter.reportJobTaskRejected(jobTaskId, f);

            } else {
                String retryDetails = MessageFormat.format("This job task encountered a problem and will be retried. This will be retry attempt number {0} for this job task.", retries);
                reporter.reportJobTaskRetry(jobTaskId, retryDetails);
            }
        } catch (JobReportingException e) {
            LOG.warn("Error reporting task {} progress to the Job Database: ", proxiedTaskMessage.getTaskId(), e);
            //TODO - should this ex be rethrown?
        }
        return null;
    }

    /**
     * Create a JobTrackingWorkerReporter object
     */
    private JobTrackingReporter createReporter() throws TaskRejectedException {
        try {
            return new JobTrackingWorkerReporter();
        } catch (JobReportingException e) {
            throw new TaskRejectedException("Failed to create Job Database reporter for Job Tracking Worker. ", e);
        }
    }

    /**
     * Returns the worker name from the source information in the task message or an "Unknown" string if it is not present.
     *
     * @param taskMessage the task message to be examined
     * @return the name of the worker that created the task message
     */
    private static String getWorkerName(final TaskMessage taskMessage)
    {
        final TaskSourceInfo sourceInfo = taskMessage.getSourceInfo();
        if (sourceInfo == null) {
            return "Unknown - no source info";
        }

        final String workerName = sourceInfo.getName();
        if (workerName == null) {
            return "Unknown - worker name not set";
        }

        return workerName;
    }

    /**
     *  Start the list of jobs that are now available to be run.
     *
     * @param resultSet containing any dependent jobs that are now available for processing
     * @param callback worker callback to enact the forwarding action determined by the worker
     * @param trackingPipe target pipe where dependent jobs should be forwarded to.
     * @throws Exception
     */
    private void forwardAvailableJobs(ResultSet resultSet, WorkerCallback callback, String trackingPipe) throws Exception
    {
        // Walk the resultSet placing each returned job on the Rabbit Queue
        try {
            //	Set up string for statusCheckUrl
            String statusCheckUrlPrefix = System.getenv("CAF_WEBSERVICE_URL") +"/jobs/";

            while (resultSet != null && resultSet.next()) {
                // Retrieve the Json Text and other necessary data which represents the Job to be
                // place on the Rabbit Queue
                // Columns
                // 1. jtd.job_id
                // 2. jtd.task_classifier
                // 3. jtd.task_api_version
                // 4. jtd.task_data
                // 5. jtd.task_data_encoding
                // 6. jtd.task_pipe
                // 7. jtd.target_pipe

                //  Generate a random task id.
                String taskId = UUID.randomUUID().toString();

                String jobId = resultSet.getString(0);
                String taskClassifier = resultSet.getString(1);
                int taskApiVersion = resultSet.getInt(2);
                byte[] taskData = resultSet.getBytes(3);
                String taskPipe = resultSet.getString(5);
                String targetPipe = resultSet.getString(6);

                String statusCheckUrl = statusCheckUrlPrefix + URLEncoder.encode(jobId, "UTF-8") +"/isActive";

                //  Construct the task message.
                final TrackingInfo trackingInfo = new TrackingInfo(jobId, calculateStatusCheckDate(System.getenv("CAF_STATUS_CHECK_TIME")),
                        statusCheckUrl, trackingPipe, targetPipe);

                final TaskMessage taskMessage = new TaskMessage(
                        taskId,
                        taskClassifier,
                        taskApiVersion,
                        taskData,
                        TaskStatus.NEW_TASK,
                        null,
                        taskPipe,
                        trackingInfo);

                callback.send("-1", taskMessage);
            }
        } catch (SQLException e) {
            LOG.error("Error retrieving Dependent Job Info from the Job Service Database {}", e);
        }
    }

    /**
     * Calculates the date of the next status check to be performed.
     *
     * @param statusCheckTime - This is the number of seconds after which it is appropriate to try to confirm that the
     * task has not been cancelled or aborted.
     */
    private Date calculateStatusCheckDate(String statusCheckTime){
        //  Make sure statusCheckTime is a valid long
        long seconds = 0;
        try{
            seconds = Long.parseLong(statusCheckTime);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Please provide a valid integer for statusCheckTime in seconds. " +e);
        }

        //  Set up date for statusCheckTime. Get current date-time and add statusCheckTime seconds.
        Instant now = Instant.now();
        Instant later = now.plusSeconds(seconds);
        return java.util.Date.from( later );
    }

}
