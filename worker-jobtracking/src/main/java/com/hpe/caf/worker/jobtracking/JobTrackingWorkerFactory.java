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

import com.hpe.caf.api.*;
import com.hpe.caf.api.worker.*;
import com.hpe.caf.services.job.util.JobTaskId;
import com.hpe.caf.util.rabbitmq.RabbitHeaders;
import com.hpe.caf.worker.tracking.report.TrackingReport;
import com.hpe.caf.worker.tracking.report.TrackingReportConstants;
import com.hpe.caf.worker.tracking.report.TrackingReportStatus;
import com.hpe.caf.worker.tracking.report.TrackingReportTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
public class JobTrackingWorkerFactory
        implements WorkerFactory, TaskMessageForwardingEvaluator, BulkWorker {

    private static final Logger LOG = LoggerFactory.getLogger(JobTrackingWorkerFactory.class);

    private final DataStore dataStore;
    private final JobTrackingWorkerConfiguration configuration;
    private final Codec codec;
    private final Class<JobTrackingWorkerTask> taskClass;

    @NotNull
    private JobTrackingReporter reporter;

    /**
     * Constructor for JobTrackingWorkerFactory called by JobTrackingWorkerFactoryProvider
     */
    public JobTrackingWorkerFactory(final ConfigurationSource configSource, final DataStore store, final Codec codec)
            throws WorkerException {
        this.codec = Objects.requireNonNull(codec);
        this.taskClass = Objects.requireNonNull(JobTrackingWorkerTask.class);
        this.dataStore = Objects.requireNonNull(store);
        try {
            this.configuration = configSource.getConfiguration(JobTrackingWorkerConfiguration.class);
        } catch (ConfigurationException e) {
            throw new WorkerException("Failed to create worker factory", e);
        }
        this.reporter = createReporter();
    }

    /**
     * Constructor for JobTrackingWorkerFactory called by JobTrackingWorkerFactoryProvider
     */
    public JobTrackingWorkerFactory(final ConfigurationSource configSource, final DataStore store, final Codec codec,
                                    final JobTrackingReporter reporter) throws WorkerException {
        this.codec = Objects.requireNonNull(codec);
        this.taskClass = Objects.requireNonNull(JobTrackingWorkerTask.class);
        this.dataStore = Objects.requireNonNull(store);
        try {
            this.configuration = configSource.getConfiguration(JobTrackingWorkerConfiguration.class);
        } catch (ConfigurationException e) {
            throw new WorkerException("Failed to create worker factory", e);
        }
        this.reporter = reporter;
    }

    @Override
    public final Worker getWorker(final WorkerTaskData workerTask) throws TaskRejectedException, InvalidTaskException {

        // Reject tasks of the wrong type and tasks that require a newer version
        final String taskClassifier = workerTask.getClassifier();
        final String workerName = JobTrackingWorkerConstants.WORKER_NAME;

        switch (taskClassifier) {
            case TrackingReportConstants.TRACKING_REPORT_TASK_NAME: {
                final byte[] data = validateVersionAndData(workerTask,
                        TrackingReportConstants.TRACKING_REPORT_TASK_API_VER);
                final TrackingReportTask jobTrackingWorkerTask
                        = TaskValidator.deserialiseAndValidateTask(codec, TrackingReportTask.class, data);
                return createWorker(jobTrackingWorkerTask, workerTask);
            }
            case JobTrackingWorkerConstants.WORKER_NAME: {
                final byte[] data = validateVersionAndData(workerTask, JobTrackingWorkerConstants.WORKER_API_VER);
                final JobTrackingWorkerTask jobTrackingWorkerTask
                        = TaskValidator.deserialiseAndValidateTask(codec, JobTrackingWorkerTask.class, data);
                return createWorker(jobTrackingWorkerTask, workerTask);
            }
            default:
                throw new InvalidTaskException("Task of type " + taskClassifier + " found on queue for " + workerName);
        }

    }

    private static byte[] validateVersionAndData(final WorkerTaskData workerTask, final int workerApiVersion)
            throws InvalidTaskException, TaskRejectedException
    {
        final int version = workerTask.getVersion();
        if (workerApiVersion < version) {
            throw new TaskRejectedException("Found task version " + version + ", which is newer than " +
                    workerApiVersion);
        }

        final byte[] data = workerTask.getData();
        if (data == null) {
            throw new InvalidTaskException("Invalid input message: task not specified");
        }

        return data;
    }

    /**
     * The purpose of this static nested class is just to delay the creation of the validator object until it is
     * required.
     */
    private static class TaskValidator
    {
        /**
         * Used to validate that the message being processed complies with the constraints that are declared.
         */
        private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        /**
         * Deserialise the given data into the specified class, and validate that any constraints specified have
         * been met.
         */
        public static <T> T deserialiseAndValidateTask(final Codec codec, final Class<T> taskType, final byte[] data)
                throws InvalidTaskException
        {
            final T jobTrackingWorkerTask;
            try {
                jobTrackingWorkerTask = codec.deserialise(data, taskType, DecodeMethod.STRICT);
            } catch (final CodecException e) {
                throw new InvalidTaskException("Invalid input message", e);
            }

            if (jobTrackingWorkerTask == null) {
                throw new InvalidTaskException("Invalid input message: no result from deserialisation");
            }

            final Set<ConstraintViolation<T>> violations = validator.validate(jobTrackingWorkerTask);
            if (violations.size() > 0) {
                LOG.error("Task of type {} failed validation due to: {}", taskType.getSimpleName(), violations);
                throw new InvalidTaskException("Task failed validation");
            }

            return jobTrackingWorkerTask;
        }
    }

    protected String getWorkerName() {
        return JobTrackingWorkerConstants.WORKER_NAME;
    }


    protected int getWorkerApiVersion() {
        return JobTrackingWorkerConstants.WORKER_API_VER;
    }

    /**
     * Create a worker to process the given JobTrackingWorkerTask task.
     */
    private Worker createWorker(final JobTrackingWorkerTask task, final WorkerTaskData workerTaskData) throws TaskRejectedException, InvalidTaskException {
        return new JobTrackingWorker(task, configuration.getOutputQueue(), codec, reporter, workerTaskData);
    }

    /**
     * Create a worker to process the given TrackingReportTask task.
     */
    private Worker createWorker(final TrackingReportTask task, final WorkerTaskData workerTask)
            throws TaskRejectedException, InvalidTaskException {
        return new JobTrackingReportUpdateWorker(task, workerTask, configuration.getOutputQueue(), codec, reporter);
    }

    @Override
    public String getInvalidTaskQueue() {
        return configuration.getOutputQueue();
    }


    @Override
    public int getWorkerThreads() {
        return configuration.getThreads();
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
            return new HealthResult(HealthStatus.UNHEALTHY, "Failed to perform Job Tracking Worker health check. " +
                    e.getMessage());
        }
    }


    /**
     * Reports the progress of the proxied task message, then forwards it to its destination.
     * @param proxiedTaskMessage the proxied task message being diverted via the Job Tracking Worker
     * @param taskInformation the reference to the message this task arrived on
     * @param headers the map of key/value paired headers to be stamped on the message
     * @param callback worker callback to enact the forwarding action determined by the worker
     */
    @Override
    public void determineForwardingAction(TaskMessage proxiedTaskMessage, TaskInformation taskInformation,
                                          Map<String, Object> headers, WorkerCallback callback) {

        List<JobTrackingWorkerDependency> jobDependencyList = reportProxiedTask(proxiedTaskMessage, headers);
        if (jobDependencyList != null && jobDependencyList.size() > 0) {
            // Forward any dependent jobs which are now available for processing
            try {
                forwardAvailableJobs(jobDependencyList, callback, proxiedTaskMessage.getTracking().getTrackingPipe(), taskInformation);
            } catch (Exception e) {
                LOG.error("Failed to create dependent jobs.");
                throw new RuntimeException("Failed to create dependent jobs.", e);
            }
        }

        LOG.debug("Forwarding task {}", proxiedTaskMessage.getTaskId());
        callback.forward(taskInformation, proxiedTaskMessage.getTo(), proxiedTaskMessage, headers);
    }

    /**
     * Report the task's status to the job database.
     *
     * @param proxiedTaskMessage the task to be reported
     * @param headers task headers such as if the task was rejected
     * @return List<JobTrackingWorkerDependency> containing any dependent jobs that are now available for processing
     *         else returns null
     */
    private List<JobTrackingWorkerDependency> reportProxiedTask(final TaskMessage proxiedTaskMessage,
                                                                Map<String, Object> headers) {
        List<JobTrackingWorkerDependency> jobDependencyList = null;
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

            final TaskStatus taskStatus = proxiedTaskMessage.getTaskStatus();
            if (taskStatus == TaskStatus.NEW_TASK || taskStatus == TaskStatus.RESULT_SUCCESS || taskStatus == TaskStatus.RESULT_FAILURE) {
                final String trackToPipe = tracking.getTrackTo();
                final String toPipe = proxiedTaskMessage.getTo();

                if ((toPipe == null && trackToPipe == null) || (trackToPipe != null && trackToPipe.equalsIgnoreCase(toPipe))) {
                    // Now returns a JobTrackingWorkerDependency[].  This ResultSet may or may not contain a list of dependent job info.
                    jobDependencyList = reporter.reportJobTaskComplete(jobTaskId);
                } else {
                    //TODO - FUTURE: supply an accurate estimatedPercentageCompleted
                    reporter.reportJobTaskProgress(jobTaskId, 0);
                }
                return jobDependencyList;
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
     * @param jobDependencyList containing any dependent jobs that are now available for processing
     * @param callback worker callback to enact the forwarding action determined by the worker
     * @param trackingPipe target pipe where dependent jobs should be forwarded to.
     * @throws Exception
     */
    private void forwardAvailableJobs(final List<JobTrackingWorkerDependency> jobDependencyList,
                                      final WorkerCallback callback, final String trackingPipe,
                                      TaskInformation taskInformation) throws Exception
    {
        // Walk the resultSet placing each returned job on the Rabbit Queue
        try {
            for (final JobTrackingWorkerDependency jobDependency : jobDependencyList) {
                final TaskMessage dependentJobTaskMessage =
                        JobTrackingWorkerUtil.createDependentJobTaskMessage(jobDependency, trackingPipe);

                callback.send(taskInformation, dependentJobTaskMessage);
            }
        } catch (final Exception e) {
            LOG.error("Error retrieving Dependent Job Info from the Job Service Database {}", e);
        }
    }

    @Override
    public void processTasks(final BulkWorkerRuntime bwr) throws InterruptedException
    {
        // TODO should come from configuration
        // Calculate a batch timeframe
        // Let's say 10 seconds
        final long cutoffTime = System.currentTimeMillis() + 10000;
        final List<WorkerTask> workerTasksThatWillNeedToGoToDatabase = new ArrayList<>();
        String partition = "";
        String jobId = "";

        // Reject tasks of the wrong type and tasks that require a newer version
        final List<String> completedTrackingReports = new ArrayList<>();

        for (;;) {
            final long maxWaitTime = cutoffTime - System.currentTimeMillis();
            final WorkerTask workerTask = bwr.getNextWorkerTask(maxWaitTime);
            if (workerTask == null) {
                break;
            }

            final String taskClassifier = workerTask.getClassifier();

            // Some of these tasks can be processed right here
            // (i.e. those that don't need to go to the database)
            // Get those out of the way here
            
            // Reject tasks of the wrong type and tasks that require a newer version
            if (taskClassifier.equals(TrackingReportConstants.TRACKING_REPORT_TASK_NAME)) {

                try {
                    final byte[] data = validateVersionAndData(workerTask,
                                                  TrackingReportConstants.TRACKING_REPORT_TASK_API_VER);

                    final TrackingReportTask jobTrackingWorkerTask
                        = TaskValidator.deserialiseAndValidateTask(codec, TrackingReportTask.class, data);

                    for (final TrackingReport report : jobTrackingWorkerTask.trackingReports) {
                        final String taskId = report.jobTaskId;
                        final String currentPartition = JobTaskId.fromMessageId(taskId).getPartitionId();
                        final String currentJobId = taskId.substring(taskId.indexOf(":") + 1, taskId.indexOf("."));

                        // if report status is failed, process
                        if (report.status == TrackingReportStatus.Failed) {
                            processFailureTrackingReports(report);
                        } // if report status is complete,
                        else if (report.status == TrackingReportStatus.Complete) {

                            // if we are processing a new job,
                            if (!currentPartition.equals(partition) || !currentJobId.equals(jobId)) {

                                // process the existing list if not empty
                                if (completedTrackingReports.size() > 0) {
                                    processCompletedTrackingReports(completedTrackingReports,
                                                                    workerTask);
                                }

                                // clear the list
                                completedTrackingReports.clear();

                                // replace partition and jobId
                                partition = currentPartition;
                                jobId = currentJobId;
                            }
                            // add to the current list
                            completedTrackingReports
                                .add(report.jobTaskId);
                        } else {
                            // If status equals "retry" or "progress", add to the logs
                            String status = report.status.toString().toLowerCase();
                            LOG.trace("Received " + status + " report message for task "
                                + "{}; taking no"
                                + " action",
                                      report.jobTaskId);
                        }
                    }

                    // This task must be added to the list to deal with later
                    workerTasksThatWillNeedToGoToDatabase.add(workerTask);

                } catch (final InvalidTaskException e) {
                    LOG.warn("Invalid task received", e);
                    workerTask.setResponse(e);
                } catch (final TaskRejectedException e) {
                    LOG.warn("Task rejected", e);
                    workerTask.setResponse(e);
                }

            } // If the worker is a JobTrackingWorker, then we simply log the tasks and set the response
            else if (taskClassifier.equals(JobTrackingWorkerConstants.WORKER_NAME)) {
                processJobTrackingWorker(workerTask);

            } else {
                try {
                    throw new InvalidTaskException(
                        "Task of type " + taskClassifier + " found on queue for " + JobTrackingWorkerConstants.WORKER_NAME);
                } catch (InvalidTaskException e) {
                    e.printStackTrace();
                }
            }
        }

        // Process the completed TrackingReports
        processCompletedTrackingReports(completedTrackingReports, null);

        // xxx
        for (final WorkerTask wTask : workerTasksThatWillNeedToGoToDatabase) {
            wTask.setResponse(new WorkerResponse(null,
                                                 TaskStatus.RESULT_SUCCESS,
                                                 new byte[]{}, JobTrackingWorkerConstants.WORKER_NAME, 1, null));
        }
    }

    private void processJobTrackingWorker(WorkerTask workerTask)
    {
        final byte[] data;
        try {
            data = validateVersionAndData(workerTask, JobTrackingWorkerConstants.WORKER_API_VER);
            final JobTrackingWorkerTask jobTrackingWorkerTask
                = TaskValidator.deserialiseAndValidateTask(codec, JobTrackingWorkerTask.class, data);
            LOG.trace("Received progress update message for task {}; taking no action",
                      jobTrackingWorkerTask.getJobTaskId());

            workerTask.setResponse(new WorkerResponse(null,
                                                      TaskStatus.RESULT_SUCCESS,
                                                      new byte[]{}, JobTrackingWorkerConstants.WORKER_NAME, 1, null));
        } catch (InvalidTaskException | TaskRejectedException e) {
            LOG.warn("Error reporting task progress to the Job Database: ", e.getStackTrace());
        }
    }

    private void processFailureTrackingReports(final TrackingReport trackingReport)
    {
        final JobTrackingWorkerFailure f = new JobTrackingWorkerFailure();
        f.setFailureId(trackingReport.failure.failureId);
        f.setFailureTime(trackingReport.failure.failureTime);
        f.setFailureSource(trackingReport.failure.failureSource);
        f.setFailureMessage(trackingReport.failure.failureMessage);
        try {
            reporter.reportJobTaskRejected(trackingReport.jobTaskId, f);
        } catch (final JobReportingException e) {
            e.printStackTrace();
        }
    }

    private void processCompletedTrackingReports(List<String> completedTrackingReports, WorkerTask workerTask)
    {
        try {
            // Actually process the list (make the call to the database)
            final List<JobTrackingWorkerDependency> jobDependencyList = reporter.reportJobTasksComplete(completedTrackingReports);

            if (jobDependencyList != null && !jobDependencyList.isEmpty()) {
                //  For each dependent job, create a TaskMessage object and publish to the
                //  messaging queue.
                for (final JobTrackingWorkerDependency dependency : jobDependencyList) {
                    final TaskMessage dependentJobTaskMessage
                        = JobTrackingWorkerUtil.createDependentJobTaskMessage(dependency, workerTask.getTo());
                    workerTask.sendMessage(dependentJobTaskMessage);
                }
            }
        } catch (JobReportingException e) {
            LOG.warn("Error reporting task progress to the Job Database: ", e);
            workerTask.setResponse(new WorkerResponse(null,
                                                      TaskStatus.RESULT_FAILURE,
                                                      new byte[]{}, JobTrackingWorkerConstants.WORKER_NAME, 1, null));
        }
    }
}
