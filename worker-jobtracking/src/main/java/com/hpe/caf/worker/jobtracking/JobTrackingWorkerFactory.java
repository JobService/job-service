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
import com.hpe.caf.worker.tracking.report.TrackingReportConstants;
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
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
public class JobTrackingWorkerFactory implements WorkerFactory, TaskMessageForwardingEvaluator, BulkWorker {

    private static final Logger LOG = LoggerFactory.getLogger(JobTrackingWorkerFactory.class);

    private final DataStore dataStore;
    private final JobTrackingWorkerConfiguration configuration;
    private final Codec codec;
    private final Class<JobTrackingWorkerTask> taskClass;

    @NotNull
    private final JobTrackingReporter reporter;

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
        } catch (final ConfigurationException e) {
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
        } catch (final ConfigurationException e) {
            throw new WorkerException("Failed to create worker factory", e);
        }
        this.reporter = reporter;
    }

    @Override
    public final Worker getWorker(final WorkerTaskData workerTask) throws TaskRejectedException, InvalidTaskException {
        LOG.debug("Starting a single job...");

        // Reject tasks of the wrong type and tasks that require a newer version
        final String taskClassifier = workerTask.getClassifier();
        final String workerName = JobTrackingWorkerConstants.WORKER_NAME;

        switch (taskClassifier) {
            case TrackingReportConstants.TRACKING_REPORT_TASK_NAME: {
                final TrackingReportTask jobTrackingWorkerTask = getTrackingReportTask(workerTask);
                return createWorker(jobTrackingWorkerTask, workerTask);
            }
            case JobTrackingWorkerConstants.WORKER_NAME: {
                final JobTrackingWorkerTask jobTrackingWorkerTask = getJobTrackingWorkerTask(workerTask);
                return createWorker(jobTrackingWorkerTask, workerTask);
            }
            default:
                throw new InvalidTaskException("Task of type " + taskClassifier + " found on queue for " + workerName);
        }

    }

    private TrackingReportTask getTrackingReportTask(final WorkerTaskData workerTask)
        throws TaskRejectedException, InvalidTaskException
    {
        final byte[] bytes = validateVersionAndData(workerTask, TrackingReportConstants.TRACKING_REPORT_TASK_API_VER);
        return TaskValidator.deserialiseAndValidateTask(codec, TrackingReportTask.class, bytes);
    }

    private JobTrackingWorkerTask getJobTrackingWorkerTask(final WorkerTaskData workerTask)
        throws TaskRejectedException, InvalidTaskException
    {
        final byte[] data = validateVersionAndData(workerTask, JobTrackingWorkerConstants.WORKER_API_VER);
        return TaskValidator.deserialiseAndValidateTask(codec, JobTrackingWorkerTask.class, data);
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
            throws InvalidTaskException {
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
     * @return healthCheck result
     */
    @Override
    public HealthResult healthCheck() {
        try {
            final JobTrackingWorkerHealthCheck healthCheck = new JobTrackingWorkerHealthCheck(reporter);
            return healthCheck.healthCheck();
        } catch (final Exception e) {
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
    public void determineForwardingAction(final TaskMessage proxiedTaskMessage, final TaskInformation taskInformation,
                                          final Map<String, Object> headers, final WorkerCallback callback) {

        final List<JobTrackingWorkerDependency> jobDependencyList = reportProxiedTask(proxiedTaskMessage, headers);
        if (!jobDependencyList.isEmpty()) {
            // Forward any dependent jobs which are now available for processing
            try {
                forwardAvailableJobs(jobDependencyList, callback, proxiedTaskMessage.getTracking().getTrackingPipe(), taskInformation);
            } catch (final Exception e) {
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
                                                                final Map<String, Object> headers) {
        try {
            final TrackingInfo tracking = proxiedTaskMessage.getTracking();
            if (tracking == null) {
                LOG.warn("Cannot report job task progress for task {} - the task message has no tracking info", proxiedTaskMessage.getTaskId());
                return Collections.emptyList();
            }

            String jobTaskId = tracking.getJobTaskId();
            if (jobTaskId == null) {
                LOG.warn("Cannot report job task progress for task {} - the tracking info has no jobTaskId", proxiedTaskMessage.getTaskId());
                return Collections.emptyList();
            }

            final TaskStatus taskStatus = proxiedTaskMessage.getTaskStatus();
            if (taskStatus == TaskStatus.NEW_TASK || taskStatus == TaskStatus.RESULT_SUCCESS || taskStatus == TaskStatus.RESULT_FAILURE) {
                final String trackToPipe = tracking.getTrackTo();
                final String toPipe = proxiedTaskMessage.getTo();

                if ((toPipe == null && trackToPipe == null) || (trackToPipe != null && trackToPipe.equalsIgnoreCase(toPipe))) {
                    // Now returns a JobTrackingWorkerDependency[].  This ResultSet may or may not contain a list of dependent job info.
                    return reporter.reportJobTaskComplete(jobTaskId);
                } else {
                    //TODO - FUTURE: supply an accurate estimatedPercentageCompleted
                    reporter.reportJobTaskProgress(jobTaskId, 0);
                    return Collections.emptyList();
                }
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

                return Collections.emptyList();
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
        return Collections.emptyList();
    }

    /**
     * Create a JobTrackingWorkerReporter object
     */
    private JobTrackingReporter createReporter() throws TaskRejectedException {
        try {
            return new JobTrackingWorkerReporter();
        } catch (final JobReportingException e) {
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
     */
    private void forwardAvailableJobs(final List<JobTrackingWorkerDependency> jobDependencyList,
                                      final WorkerCallback callback, final String trackingPipe,
                                      final TaskInformation taskInformation)
    {
        // Walk the resultSet placing each returned job on the Rabbit Queue
        try {
            for (final JobTrackingWorkerDependency jobDependency : jobDependencyList) {
                final TaskMessage dependentJobTaskMessage =
                        JobTrackingWorkerUtil.createDependentJobTaskMessage(jobDependency, trackingPipe);

                callback.send(taskInformation, dependentJobTaskMessage);
            }
        } catch (final Exception e) {
            LOG.error("Error retrieving Dependent Job Info from the Job Service Database", e);
        }
    }

    /**
     * Main method for the bulk messages. It will process the incoming messages based on their "taskClassifier". An attempt is made to
     * group them by jobId using a customizable variable: "maxWaitingTime".
     *
     * @param bwr object supplied to CAF Workers, able to process multiple tasks together
     * @throws InterruptedException when a thread is waiting, sleeping, or otherwise occupied, and the thread is interrupted, either
     * before or during the activity
     */
    @Override
    public void processTasks(final BulkWorkerRuntime bwr) throws InterruptedException
    {
        // Configured to wait 10 seconds between two batches
        LOG.debug("Starting a bulk job...");
        final long maxBatchTime = JobTrackingWorkerUtil.getMaxBatchTime();
        final long cutoffTime = System.currentTimeMillis() + maxBatchTime;

        // Create a collection to hold the completed tasks in the tracking report messages (i.e. those which have TrackingReportTask as
        // their task classifier).  Rather than processing these immediately we are storing them here so that we can process them
        // together.  We are holding them in a TreeMap so that they are processed in a consistent order.  If a tracking report message
        // contains completed reports from multiple jobs then obviously it will result in multiple entities being added or updated in the
        // sorted map.  The last entry to be added or updated in the map will have the finalJob flag set on it, so that when the completed
        // tasks are being processed (in job order) that we know that that is the point at which the message can be acknowledged.
        final TreeMap<FullyQualifiedJobId, List<CompletedWorkerTaskEntity>> bulkItemList = new TreeMap<>();

        for (;;) {
            final long maxWaitTime = cutoffTime - System.currentTimeMillis();

            // If no more task coming in before maxWaitingTime, null is returned
            final WorkerTask workerTask = bwr.getNextWorkerTask(maxWaitTime);
            if (workerTask == null) {
                break;
            }

            final String taskClassifier = workerTask.getClassifier();

            switch (taskClassifier) {
                case TrackingReportConstants.TRACKING_REPORT_TASK_NAME:
                    processTrackingReportTask(workerTask, bulkItemList);
                    break;
                case JobTrackingWorkerConstants.WORKER_NAME:
                    processJobTrackingWorkerTask(workerTask);
                    break;
                default:
                    workerTask.setResponse(
                        new InvalidTaskException("Task of type " + taskClassifier + " found on queue for Job Tracking Worker"));
            }
        }

        LOG.info("Size of bulkItemList: {}", bulkItemList.size());

        // Process the completed TrackingReports
        processCompletedTrackingReports(bulkItemList);
    }

    /**
     *
     * @param workerTask provides access onto Worker Task Data and ability to set response
     * @param bulkItemList the lists of workerTaskEntities ordered by FullyQualifiedJobId (partition and jobId)
     */
    private void processTrackingReportTask(
        final WorkerTask workerTask,
        final TreeMap<FullyQualifiedJobId, List<CompletedWorkerTaskEntity>> bulkItemList
    ) throws InterruptedException
    {
        try {
            processTrackingReportTaskImpl(workerTask, bulkItemList);
        } catch (final InvalidTaskException ex) {
            workerTask.setResponse(ex);
        } catch (final TaskRejectedException ex) {
            workerTask.setResponse(ex);
        }
    }

    /**
     *
     * @param workerTask provides access onto Worker Task Data and ability to set response
     * @param bulkItemList the lists of workerTaskEntities ordered by FullyQualifiedJobId (partition and jobId)
     */
    private void processTrackingReportTaskImpl(
        final WorkerTask workerTask,
        final TreeMap<FullyQualifiedJobId, List<CompletedWorkerTaskEntity>> bulkItemList
    ) throws InvalidTaskException, InterruptedException, TaskRejectedException
    {
        final TrackingReportTask trackingWorkerTask = getTrackingReportTask(workerTask);

        // List containing the stripped task ids to be incremented for bulk update in db
        final List<String> completedTaskIds = new ArrayList<>();
        final JobTrackingReporterPartialProxy reporterProxy = new JobTrackingReporterPartialProxy(reporter, completedTaskIds);

        final JobTrackingReportUpdateWorker messageProcessor = new JobTrackingReportUpdateWorker(
            trackingWorkerTask,
            workerTask,
            configuration.getOutputQueue(),
            codec,
            reporterProxy);

        final WorkerResponse messageResponse = messageProcessor.doWork();

        if (messageResponse.getTaskStatus() != TaskStatus.RESULT_SUCCESS || completedTaskIds.isEmpty()) {
            workerTask.setResponse(messageResponse);
        } else {
            final TreeMap<FullyQualifiedJobId, List<JobTaskId>> taskIdsGrouped = completedTaskIds.stream()
                .map(JobTaskId::fromMessageId)
                .collect(Collectors.groupingBy(
                    taskId -> new FullyQualifiedJobId(taskId.getPartitionId(), taskId.getJobId()), TreeMap::new, Collectors.toList()));

            final Iterator<Map.Entry<FullyQualifiedJobId, List<JobTaskId>>> entryIterator
                = taskIdsGrouped.entrySet().iterator();

            boolean isNotFinalJob;
            do {
                final Map.Entry<FullyQualifiedJobId, List<JobTaskId>> entry = entryIterator.next();
                isNotFinalJob = entryIterator.hasNext();

                mergeIntoBulkItemList(bulkItemList, workerTask, entry.getKey(), entry.getValue(), !isNotFinalJob);
            } while (isNotFinalJob);
        }
    }

    /**
     *
     * @param workerTask provides access onto Worker Task Data and ability to set response
     * @param bulkItemList the lists of workerTaskEntities ordered by FullyQualifiedJobId (partition and jobId)
     * @param jobId id of the job
     * @param taskIds list of completed tasks ids belonging to the job
     * @param isFinalJob true if this is the final job that this worker task contains completed task ids for
     */
    private static void mergeIntoBulkItemList(
        final TreeMap<FullyQualifiedJobId, List<CompletedWorkerTaskEntity>> bulkItemList,
        final WorkerTask workerTask,
        final FullyQualifiedJobId jobId,
        final List<JobTaskId> taskIds,
        final boolean isFinalJob
    )
    {
        bulkItemList.merge(
            jobId,
            Collections.singletonList(new CompletedWorkerTaskEntity(workerTask, taskIds, isFinalJob)),
            (existingList, newList) -> {
                // Assert that the existing list contains elements
                assert !existingList.isEmpty();

                // Assert that the new list contains only one element (since it was created using singletonList)
                assert (newList != null && newList.size() == 1);

                // Check if the existing list is mutable
                if (existingList instanceof ArrayList) {
                    existingList.add(newList.get(0));
                    return existingList;
                } else {
                    // Assert that the existing list contains only one element
                    // (it would have already been changed to an ArrayList if this was not the case)
                    assert (existingList.size() == 1);

                    final ArrayList<CompletedWorkerTaskEntity> arrayList = new ArrayList<>();
                    arrayList.add(existingList.get(0));
                    arrayList.add(newList.get(0));
                    return arrayList;
                }
            }
        );
    }

    /**
     * Processes the completed tracking reports by reporting them to the database, then sets the response to the related workerTask and
     * triggers the related job dependencies.
     *
     * @param bulkItemList the lists of workerTaskEntities ordered by FullyQualifiedJobId (partition and jobId)
     */
    private void processCompletedTrackingReports(final TreeMap<FullyQualifiedJobId, List<CompletedWorkerTaskEntity>> bulkItemList)
    {
        // Process the jobs in the same order that they were added so that the finalJob flag is correct
        final Iterator<Map.Entry<FullyQualifiedJobId, List<CompletedWorkerTaskEntity>>> iterator = bulkItemList.entrySet().iterator();

        while (iterator.hasNext()) {
            final Map.Entry<FullyQualifiedJobId, List<CompletedWorkerTaskEntity>> entry = iterator.next();

            final FullyQualifiedJobId jobId = entry.getKey();
            LOG.debug("partition: {}; job: {}", jobId.getPartitionId(), jobId.getJobId());

            final List<CompletedWorkerTaskEntity> workerTaskEntities = entry.getValue();

            final List<String> taskIds = workerTaskEntities.stream()
                .flatMap(workerTaskObj -> workerTaskObj.getCompletedTaskIds().stream().map(JobTaskId::getId))
                .collect(Collectors.toList());

            // Actually process the list (make the call to the database)
            final List<JobTrackingWorkerDependency> jobDependencyList;
            try {
                jobDependencyList = reporter.reportJobTasksComplete(jobId.getPartitionId(), jobId.getJobId(), taskIds);
            } catch (final JobReportingTransientException ex) {
                // Get the message to include in the transient response
                final String failureMessage = ex.getMessage();

                // Respond that all tasks have failed in a transient manner and can be re-sent
                failRemainingCompletedTrackingReports(
                    workerTaskEntities,
                    iterator,
                    workerTask -> setWorkerResultTransientFailure(workerTask, failureMessage));

                return;
            } catch (final JobReportingException ex) {
                // Serialize the exception to include it in the response message
                final byte[] failureData = getFailureData(ex);

                // Respond that all remaining tasks have failed
                failRemainingCompletedTrackingReports(
                    workerTaskEntities,
                    iterator,
                    workerTask -> setWorkerResultFailure(workerTask, failureData));

                return;
            }

            // The database function may return dependencies that are now eligable to be started
            if (jobDependencyList != null && !jobDependencyList.isEmpty()) {
                // Any of the worker tasks can be used for creating the dependencies so we'll just pick the first one
                final WorkerTask workerTask = workerTaskEntities.get(0).getWorkerTask();

                // For each dependent job, create a TaskMessage object and publish to the messaging queue
                for (final JobTrackingWorkerDependency dependency : jobDependencyList) {
                    final TaskMessage dependentJobTaskMessage
                        = JobTrackingWorkerUtil.createDependentJobTaskMessage(dependency, workerTask.getTo());

                    workerTask.sendMessage(dependentJobTaskMessage);
                }
            }

            // Acknowledge the worker tasks that have been completed
            workerTaskEntities.stream()
                .filter(CompletedWorkerTaskEntity::isFinalJob)
                .map(CompletedWorkerTaskEntity::getWorkerTask)
                .forEach(JobTrackingWorkerFactory::setWorkerResultSuccess);
        }
    }

    /**
     * Utility function to be used when there is a failure from the database.<br>
     * Executes the specified failure action for the current tasks specified, and for all other tasks that can be retrieved from the
     * iterator.
     */
    private static void failRemainingCompletedTrackingReports(
        final List<CompletedWorkerTaskEntity> currentFailedWorkerTasks,
        final Iterator<Map.Entry<FullyQualifiedJobId, List<CompletedWorkerTaskEntity>>> bulkItemListIterator,
        final Consumer<WorkerTask> failureAction
    )
    {
        List<CompletedWorkerTaskEntity> failedWorkerTasks = currentFailedWorkerTasks;

        for (;;) {
            failedWorkerTasks.stream()
                .filter(CompletedWorkerTaskEntity::isFinalJob)
                .map(CompletedWorkerTaskEntity::getWorkerTask)
                .forEach(failureAction);

            if (!bulkItemListIterator.hasNext()) {
                return;
            }

            failedWorkerTasks = bulkItemListIterator.next().getValue();
        }
    }

    private static void setWorkerResultSuccess(final WorkerTask workerTask)
    {
        workerTask.setResponse(
            new WorkerResponse(null, TaskStatus.RESULT_SUCCESS, new byte[]{}, JobTrackingWorkerConstants.WORKER_NAME, 1, null));
    }

    private byte[] getFailureData(final JobReportingException ex)
    {
        try {
            return codec.serialise(
                JobTrackingWorkerUtil.createErrorResult(JobTrackingWorkerStatus.PROGRESS_UPDATE_FAILED, ex.getMessage()));
        } catch (final CodecException codecEx) {
            throw new TaskFailedException("Failed to serialise result", codecEx);
        }
    }

    private void setWorkerResultFailure(final WorkerTask workerTask, final byte[] failureData)
    {
        workerTask.setResponse(new WorkerResponse(
            configuration.getOutputQueue(),
            TaskStatus.RESULT_FAILURE,
            failureData,
            JobTrackingWorkerConstants.WORKER_NAME,
            JobTrackingWorkerConstants.WORKER_API_VER,
            null)
        );
    }

    private static void setWorkerResultTransientFailure(final WorkerTask workerTask, final String failureData)
    {
        workerTask.setResponse(new TaskRejectedException(failureData));
    }

    /**
     * @param workerTask Provides access onto Worker Task Data and ability to set response
     * @throws InterruptedException if a thread is waiting, sleeping, or otherwise occupied, and the thread is interrupted, either before
     * or during the activity.
     */
    private void processJobTrackingWorkerTask(final WorkerTask workerTask) throws InterruptedException
    {
        try {
            final JobTrackingWorkerTask jobTrackingWorkerTask = getJobTrackingWorkerTask(workerTask);

            final JobTrackingWorker messageProcessor = new JobTrackingWorker(
                jobTrackingWorkerTask,
                configuration.getOutputQueue(),
                codec,
                reporter,
                workerTask);

            final WorkerResponse messageResponse = messageProcessor.doWork();
            workerTask.setResponse(messageResponse);

        } catch (final InvalidTaskException ex) {
            LOG.warn("Invalid input message data", ex);
            workerTask.setResponse(ex);
        } catch (final TaskRejectedException ex) {
            LOG.warn("Invalid input message version", ex);
            workerTask.setResponse(ex);
        }
    }
}
