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
package com.hpe.caf.services.job.scheduled.executor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hpe.caf.api.Codec;
import com.hpe.caf.util.ModuleLoader;
import com.hpe.caf.util.ModuleLoaderException;
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

    private static final boolean PROP_DEPENDENT_JOB_FAILURES;

    static {
        final String propDepJoFailures = System.getenv("CAF_JOB_SCHEDULER_PROPAGATE_FAILURES");
        PROP_DEPENDENT_JOB_FAILURES = propDepJoFailures != null ? Boolean.parseBoolean(propDepJoFailures) : false;
    }

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

                    //  Get database helper instance.
                    QueueServices queueServices = null;
                    try {
                        queueServices = QueueServicesFactory.create(jtd.getTaskPipe(),codec);
                        LOG.debug(MessageFormat.format("Sending task data to the target queue {0} ...", workerAction.toString()));
                        queueServices.sendMessage(jtd.getPartitionId(), jtd.getJobId(), workerAction);
                        deleteDependentJob(jtd.getPartitionId(), jtd.getJobId());
                    } catch(final Exception ex) {
                        //  TODO - in future we need to consider consequence of reaching here as this means we have
                        //  deleted job_task_data rows from the database. For now we will log details as part of
                        //  failure details in the Job database row.

                        final String errorMessage = MessageFormat.format("Failed to add task data {0} to the queue.", workerAction.toString());

                        //  Failure adding job data to queue. Update the job with the failure details.
                        final QueueFailure f = new QueueFailure();
                        f.setFailureId("ADD_TO_QUEUE_FAILURE");
                        f.setFailureTime(new Date());
                        f.failureSource(MessageFormat.format("Job Service Scheduled Executor for job id {0}", jtd.getJobId()));
                        f.failureMessage(ex.getMessage());

                        final ObjectMapper mapper = new ObjectMapper();
                        final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                        mapper.setDateFormat(df);
                        try {
                            reportFailure(
                                jtd.getPartitionId(), jtd.getJobId(), mapper.writeValueAsString(f));
                        } catch (final JsonProcessingException e) {
                            LOG.error("Failed to serialize the failure details.");
                        }

                        LOG.error(errorMessage);
                        throw new ScheduledExecutorException(errorMessage);
                    } finally {
                        if (queueServices != null) {
                            try {
                                queueServices.close();
                            } catch (final Exception e) {
                                throw new ScheduledExecutorException(e.getMessage());
                            }
                        }
                    }
                }
            }
        } catch (final ScheduledExecutorException e) {
            LOG.error(MessageFormat.format("Exception caught polling the Job Service database for jobs to run. {0}", e.getMessage()));
        } catch (final ModuleLoaderException e) {
            LOG.error(MessageFormat.format("Exception caught when loading the serialization class. {0}", e.getMessage()));
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
    private static void reportFailure(
        final String partitionId, final String jobTaskId, final String failureDetails
    ) throws ScheduledExecutorException {
        /*
        SCMOD-6525 - FALSE POSITIVE on FORTIFY SCAN for Unreleased Resource: Database.
        */
        try (
                Connection conn = DBConnection.get();
                CallableStatement stmt = conn.prepareCall("{call report_failure(?,?,?,?)}")
        ) {
            stmt.setString(1, partitionId);
            stmt.setString(2,jobTaskId);
            stmt.setString(3,failureDetails);
            stmt.setBoolean(4, PROP_DEPENDENT_JOB_FAILURES);

            LOG.debug("Calling report_failure() database function...");
            stmt.execute();
        } catch (final SQLException e) {
            final String errorMessage = MessageFormat.format("Failed in call to report_failure() database function.{0}", e.getMessage());
            LOG.error(errorMessage);
            throw new ScheduledExecutorException(errorMessage);
        }

    }


}
