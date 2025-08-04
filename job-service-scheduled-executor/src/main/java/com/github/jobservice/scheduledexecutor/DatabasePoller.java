/*
 * Copyright 2016-2025 Open Text.
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
package com.github.jobservice.scheduledexecutor;

import com.github.cafapi.common.api.Codec;
import com.github.cafapi.common.util.moduleloader.ModuleLoader;
import com.github.cafapi.common.util.moduleloader.ModuleLoaderException;
import com.rabbitmq.client.ShutdownSignalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to poll the Job Service database for jobs that can now run.
 */
public class DatabasePoller
{
    private static final Logger LOG = LoggerFactory.getLogger(DatabasePoller.class);

    public static void pollDatabaseForJobsToRun() {
        try {
            LOG.info("QueueServicesCache size: {}", QueueServicesCache.size());

            //  Poll database for prerequisite jobs that are now available to be run.
            LOG.debug("Polling Job Service database for jobs to run ...");
            final List<JobTaskData> jobsToRun = getDependentJobsToRun();

            //  Determine if there are any jobs to run.
            if (jobsToRun.size() > 0) {
                //  Load serialization class.
                LOG.debug("Loading serialization class ...");
                final Codec codec = ModuleLoader.getService(Codec.class);

                //  For each job to run, submit message to the rabbitMQ queue for further processing.
                for (final JobTaskData jtd : jobsToRun) {
                    LOG.info(MessageFormat.format("Processing job id {0} ...", jtd.getJobId()));

                    final WorkerAction workerAction = new WorkerAction();
                    workerAction.setTaskClassifier(jtd.getTaskClassifier());
                    workerAction.setTaskApiVersion(jtd.getTaskApiVersion());
                    if (jtd.getTaskData() != null) {
                        workerAction.setTaskData(new String(jtd.getTaskData(), StandardCharsets.UTF_8));
                    }
                    workerAction.setTaskPipe(jtd.getTaskPipe());
                    workerAction.setTargetPipe(jtd.getTargetPipe());
                    workerAction.setCorrelationId(jtd.getCorrelationId());
                    sendMessageToQueueMessaging(codec, jtd, workerAction);
                }
            }
        } catch (final ScheduledExecutorException e) {
            LOG.error(MessageFormat.format("Exception caught polling the Job Service database for jobs to run. {0}", e.getMessage()));
        } catch (final ModuleLoaderException e) {
            LOG.error(MessageFormat.format("Exception caught when loading the serialization class. {0}", e.getMessage()));
        }
    }

    private static void sendMessageToQueueMessaging(final Codec codec, final JobTaskData jtd, final WorkerAction workerAction)
    {
        // Create a QueueServices instance to send the message for this job to the queue
        final QueueServicesCache.Key key = new QueueServicesCache.Key(jtd.getPartitionId(),  jtd.getJobId());
        final QueueServices queueServices;
        try {
            final QueueServices existingEntry = QueueServicesCache.getIfPresent(key);
            if (existingEntry == null) {
                // Responses to this message will be handled by PublisherConfirmationAnalyzer, unless an exception
                // occurs while creating the QueueServices instance or during publishing of the message, in which case
                // the exception will be caught and handled in the catch block below.

                queueServices =
                        QueueServicesFactory.create(jtd.getPartitionId(), jtd.getJobId(), jtd.getTaskPipe(), codec);
                QueueServicesCache.put(key, queueServices);

                queueServices.sendMessage(jtd.getPartitionId(), jtd.getJobId(), workerAction);
            } else {
                LOG.warn("A QueueServices instance already exists for key={}. This means we have already sent a " +
                                "message for this job to the {} queue and are awaiting a publisher confirm. The " +
                                "new job will be processed when a publisher confirm is received for the existing job " +
                                "(or the QueueServicesCache timeout occurs).",
                        key, jtd.getTaskPipe());
            }
        } catch (final Exception e) {
            // If the exception is a wrapped ShutdownSignalException, it likely came from
            // the async listener and was re-thrown - let the async handler deal with it
            if (isAsyncListenerException(e)) {
                LOG.warn("Exception appears to be from async listener - deferring to async handler. " +
                                "[partitionId={}, jobId={}, exception={}]",
                        jtd.getPartitionId(), jtd.getJobId(), e.getClass().getSimpleName());

                return;
            }

            // Handle synchronous exceptions (connection setup, serialization, etc.)
            final PublisherConfirmationAnalyzer.FailureType failureType = JobFailureHandler.analyzeException(e);
            final String reason = MessageFormat.format("Exception during message publishing setup: {0}", e.getMessage());

            if (failureType == PublisherConfirmationAnalyzer.FailureType.NON_TRANSIENT) {
                LOG.error("NON-TRANSIENT failure detected during message publishing setup. " +
                                "Marking job as failed. [partitionId={}, jobId={}, taskPipe={}].",
                        jtd.getPartitionId(), jtd.getJobId(), jtd.getTaskPipe(), e);
                JobFailureHandler.handleNonTransientFailure(jtd.getPartitionId(), jtd.getJobId(), reason);
            } else {
                LOG.warn("TRANSIENT failure detected during message publishing setup. " +
                                "Job will be retried later. [partitionId={}, jobId={}, taskPipe={}].",
                        jtd.getPartitionId(), jtd.getJobId(), jtd.getTaskPipe(), e);
                QueueServicesCache.invalidate(key);
            }
        }
    }

    /**
     * Determines if an exception likely originated from an async listener callback.
     * These exceptions should be handled by the async listener, not the synchronous catch block.
     */
    private static boolean isAsyncListenerException(final Exception exception) {
        // Look for IOException wrapping ShutdownSignalException - this is typically
        // what happens when RabbitMQ async callbacks throw exceptions that get
        // propagated back to synchronous code
        if (exception instanceof IOException) {
            Throwable cause = exception.getCause();
            while (cause != null) {
                if (cause instanceof ShutdownSignalException) {
                    // This is likely from an async callback
                    return true;
                }
                cause = cause.getCause();
            }
        }

        // Direct ShutdownSignalException in sync context is unusual - likely async
        return exception instanceof ShutdownSignalException;
    }

    /**
     * Deletes the supplied job from the job_task_data database table.
     */
    public static void deleteDependentJob(final String partitionId, final String jobId) throws ScheduledExecutorException
    {
        try (
                Connection connection = DBConnection.get();
                CallableStatement stmt = connection.prepareCall("{call delete_dependent_job(?,?)}")) {
            stmt.setString(1, partitionId);
            stmt.setString(2, jobId);
            LOG.info(MessageFormat.format("Calling delete_dependent_job({0},{1}) database function ...", partitionId, jobId));
            stmt.execute();
        } catch (final SQLException e) {
            final String errorMessage = MessageFormat.format("Failed in call to delete_dependent_job({0},{1}) database function.{3}",
                    partitionId, jobId,e.getMessage());
            LOG.error(errorMessage);
            throw new ScheduledExecutorException(errorMessage);
        }
    }

    /**
     * Returns a list of dependent jobs that are now available to run.
     */
    private static List<JobTaskData> getDependentJobsToRun() throws ScheduledExecutorException
    {
        /*
        SCMOD-6525 - FALSE POSITIVE on FORTIFY SCAN for Unreleased Resource: Database.
        */
        try (
                Connection connection = DBConnection.get();
                CallableStatement stmt = connection.prepareCall("{call get_dependent_jobs()}")
        ) {
            LOG.debug("Calling get_dependent_jobs() database function ...");
            stmt.execute();

            final List<JobTaskData> jobTaskDataList = new ArrayList<>();
            final ResultSet rs = stmt.getResultSet();
            while (rs.next()) {
                final JobTaskData dependency = new JobTaskData();
                dependency.setPartitionId(stmt.getResultSet().getString(1));
                dependency.setJobId(stmt.getResultSet().getString(2));
                dependency.setTaskClassifier(stmt.getResultSet().getString(3));
                dependency.setTaskApiVersion(stmt.getResultSet().getInt(4));
                dependency.setTaskData(stmt.getResultSet().getBytes(5));
                dependency.setTaskPipe(stmt.getResultSet().getString(6));
                dependency.setTargetPipe(stmt.getResultSet().getString(7));
                dependency.setCorrelationId(stmt.getResultSet().getString(8));
                jobTaskDataList.add(dependency);
            }

            return jobTaskDataList;
        } catch (final SQLException e) {
            final String errorMessage = MessageFormat.format("Failed in call to get_dependent_jobs() database function.{0}", e.getMessage());
            LOG.error(errorMessage);
            throw new ScheduledExecutorException(errorMessage);
        }
    }

    /**
     * Reports failure for the specified job identifier.
     */
    public static void reportFailure(
            final String partitionId,
            final String jobId,
            final String failureDetails) throws ScheduledExecutorException {
        /*
        SCMOD-6525 - FALSE POSITIVE on FORTIFY SCAN for Unreleased Resource: Database.
        */
        try (
                Connection conn = DBConnection.get();
                CallableStatement stmt = conn.prepareCall("{call report_failure(?,?,?)}")
        ) {
            stmt.setString(1, partitionId);
            stmt.setString(2, jobId);
            stmt.setString(3, failureDetails);

            LOG.info("Calling report_failure() database function with partitionId={}, jobId={}, failureDetails={} ...",
                    partitionId, jobId, failureDetails);
            stmt.execute();
        } catch (final SQLException e) {
            final String errorMessage = MessageFormat.format(
                    "Failed in call to report_failure() database function with " +
                            "partitionId={0}, jobId={1}, failureDetails={2}. {3}",
                    partitionId, jobId, failureDetails, e.getMessage());
            LOG.error(errorMessage);
            throw new ScheduledExecutorException(errorMessage);
        }
    }
}
