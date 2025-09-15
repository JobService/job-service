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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cafapi.common.api.Codec;
import com.github.cafapi.common.util.moduleloader.ModuleLoader;
import com.github.cafapi.common.util.moduleloader.ModuleLoaderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This class is used to poll the Job Service database for jobs that can now run.
 */
public class DatabasePoller
{
    private static final Logger LOG = LoggerFactory.getLogger(DatabasePoller.class);
    private static final String FAILURE_ID = "ADD_TO_QUEUE_FAILURE";
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final ObjectMapper MAPPER = new ObjectMapper().setDateFormat(DATE_FORMAT);

    public static void pollDatabaseForJobsToRun() {
        try {
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

    private static void sendMessageToQueueMessaging(final Codec codec, final JobTaskData jtd,
                                                    final WorkerAction workerAction) {
        final String partitionId = jtd.getPartitionId();
        final String jobId = jtd.getJobId();
        final String taskPipe = jtd.getTaskPipe();
        final String context = MessageFormat.format("[partitionId={0}, jobId={1}, taskPipe={2}]",
                partitionId, jobId, taskPipe);

        try (final QueueServices queueServices = QueueServicesFactory.create(taskPipe, partitionId, codec)) {

            queueServices.sendMessage(partitionId, jobId, workerAction);

            try {
                deleteDependentJob(partitionId, jobId);
            } catch (final ScheduledExecutorException | RuntimeException e) {
                LOG.error("Message successfully published but failed to remove job from retry table. " +
                                "Job will be retried. {}: {}",
                        context, e.getMessage(), e);
            }
        } catch (final Exception sendMessageException) {
            final ExceptionType exceptionType = ExceptionAnalyzer.analyzeException(sendMessageException);

            switch (exceptionType) {
                case TRANSIENT:
                    LOG.warn("Transient RabbitMQ failure occurred while publishing message. " +
                            "Job will be retried. {}: {}",
                            context, sendMessageException.getMessage(), sendMessageException);
                    break;

                case PERMANENT:
                    final String reason = MessageFormat.format(
                            "Permanent RabbitMQ failure occurred while publishing message. {0}: {1}",
                            context, sendMessageException.getMessage());

                    LOG.error(reason, sendMessageException);

                    try {
                        reportFailureAndDeleteDependentJob(partitionId, jobId, reason);
                    } catch (final ScheduledExecutorException | JsonProcessingException | SQLException reportFailureException) {
                        LOG.error("Failed to mark job as failed. Job will remain in retry table " +
                                "and be retried. {}: {}",
                                context, reportFailureException.getMessage(), reportFailureException);
                    }
                    break;

                default:
                    throw new RuntimeException("Unexpected exception type: " + exceptionType, sendMessageException);
            }
        }
    }

    /**
     * Deletes the supplied job from the job_task_data database table.
     */
    private static void deleteDependentJob(final String partitionId, final String jobId) throws ScheduledExecutorException
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

    private static void reportFailureAndDeleteDependentJob(
            final String partitionId,
            final String jobId,
            final String reason)
            throws JsonProcessingException, ScheduledExecutorException, SQLException
    {
        final QueueFailure failure = new QueueFailure();
        failure.setFailureId(FAILURE_ID);
        failure.setFailureTime(new Date());
        failure.failureSource(MessageFormat.format("Job Service Scheduled Executor for job id {0}", jobId));
        failure.failureMessage(reason);

        final String failureJson = MAPPER.writeValueAsString(failure);

        try (final Connection conn = DBConnection.get();
             final CallableStatement stmt = conn.prepareCall("{call report_failure_and_delete_dependent_job(?,?,?)}")) {
            stmt.setString(1, partitionId);
            stmt.setString(2, jobId);
            stmt.setString(3, failureJson);

            LOG.info("Calling report_failure_and_delete_dependent_job() database function with " +
                            "partitionId={}, jobId={}, failureDetails={} ...",
                    partitionId, jobId, failureJson);
            stmt.execute();
        }
    }
}
