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
package com.hpe.caf.services.job.scheduled.executor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hpe.caf.api.Codec;
import com.hpe.caf.util.ModuleLoader;
import com.hpe.caf.util.ModuleLoaderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * This class is used to poll the Job Service database for jobs that can now run.
 */
public class DatabasePoller
{
    private static final Logger LOG = LoggerFactory.getLogger(DatabasePoller.class);

    private static final String JDBC_POSTGRESQL_PREFIX = "jdbc:postgresql:";
    private static final String JDBC_DRIVER = "org.postgresql.Driver";
    private static final boolean PROP_DEPENDENT_JOB_FAILURES;

    static {
        final String propDepJoFailures = System.getenv("CAF_JOB_SCHEDULER_PROPAGATE_FAILURES");
        PROP_DEPENDENT_JOB_FAILURES = Boolean.parseBoolean(propDepJoFailures);
    }

    private DatabasePoller() {}

    public static void pollDatabaseForJobsToRun() {
        try {
            //  Poll database for prerequisite jobs that are now available to be run.
            LOG.debug("Polling Job Service database for jobs to run ...");
            final List<JobTaskData> jobsToRun = getDependentJobsToRun();

            //  Determine if there are any jobs to run.
            if (!jobsToRun.isEmpty()) {
                //  Load serialization class.
                LOG.debug("Loading serialization class ...");
                final Codec codec = ModuleLoader.getService(Codec.class);
                submitMessageToRabbitMQ(jobsToRun, codec);

            }
        } catch (final ScheduledExecutorException e) {
            LOG.error(MessageFormat.format("Exception caught polling the Job Service database for jobs to run. {0}", e.getMessage()));
        } catch (final ModuleLoaderException e) {
            LOG.error(MessageFormat.format("Exception caught when loading the serialization class. {0}", e.getMessage()));
        }
    }

    private static void submitMessageToRabbitMQ(final List<JobTaskData> jobsToRun, final Codec codec) throws ScheduledExecutorException {
        //  For each job to run, submit message to the rabbitMQ queue for further processing.
        for (final JobTaskData jtd : jobsToRun) {
            LOG.info("Processing job id {} ...", jtd.getJobId());
            final WorkerAction workerAction = getWorkerAction(jtd);
            //  Get database helper instance.
            QueueServices queueServices = null;
            try {
                queueServices = QueueServicesFactory.create(jtd.getTaskPipe(), codec);

                LOG.debug("Sending task data to the target queue {} ...", workerAction);
                queueServices.sendMessage(jtd.getPartitionId(), jtd.getJobId(), workerAction);

                deleteDependentJob(jtd.getPartitionId(), jtd.getJobId());
            } catch(final Exception ex) {
                //  TODO - in future we need to consider consequence of reaching here as this means we have
                //  deleted job_task_data rows from the database. For now we will log details as part of
                //  failure details in the Job database row.

                final String errorMessage = MessageFormat.format("Failed to add task data {} to the queue.",workerAction);
                final QueueFailure f = getQueueFailure(jtd, ex);
                reportFailure(jtd, f);

                LOG.error(errorMessage);
                throw new ScheduledExecutorException(errorMessage);
            } finally {
                closeQueueServices(queueServices);
            }
        }
    }

    private static void closeQueueServices(final QueueServices queueServices) throws ScheduledExecutorException {
        if (queueServices != null) {
            try {
                queueServices.close();
            } catch (final Exception e) {
                throw new ScheduledExecutorException(e.getMessage());
            }
        }
    }

    private static void reportFailure(final JobTaskData jtd, final QueueFailure f) throws ScheduledExecutorException {
        final ObjectMapper mapper = new ObjectMapper();
        final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        mapper.setDateFormat(df);
        try {
            reportFailure(
                jtd.getPartitionId(), jtd.getJobId(), mapper.writeValueAsString(f));
        } catch (final JsonProcessingException e) {
            LOG.error("Failed to serialize the failure details.");
        }
    }

    @NotNull
    private static QueueFailure getQueueFailure(final JobTaskData jtd, final Exception ex) {
        //  Failure adding job data to queue. Update the job with the failure details.
        final QueueFailure f = new QueueFailure();
        f.setFailureId("ADD_TO_QUEUE_FAILURE");
        f.setFailureTime(new Date());
        f.failureSource(MessageFormat.format("Job Service Scheduled Executor for job id {0}", jtd.getJobId()));
        f.failureMessage(ex.getMessage());
        return f;
    }

    @NotNull
    private static WorkerAction getWorkerAction(final JobTaskData jtd) {
        final WorkerAction workerAction = new WorkerAction();
        workerAction.setTaskClassifier(jtd.getTaskClassifier());
        workerAction.setTaskApiVersion(jtd.getTaskApiVersion());
        if (jtd.getTaskData() != null) {
            workerAction.setTaskData(new String(jtd.getTaskData(), StandardCharsets.UTF_8));
        }
        workerAction.setTaskPipe(jtd.getTaskPipe());
        workerAction.setTargetPipe(jtd.getTargetPipe());
        return workerAction;
    }

    /**
     * Deletes the supplied job from the job_task_data database table.
     */
    private static void deleteDependentJob(final String partitionId, final String jobId) throws ScheduledExecutorException
    {
        try (
                Connection connection = getConnection();
                CallableStatement stmt = connection.prepareCall("{call delete_dependent_job(?,?)}")) {
            stmt.setString(1, partitionId);
            stmt.setString(2, jobId);
            LOG.info("Calling delete_dependent_job({},{}) database function ...", partitionId, jobId);
            stmt.execute();
        } catch (final SQLException e) {
            final String errorMessage = MessageFormat.format("Failed in call to delete_dependent_job({},{}) database function.{}",
                    partitionId, jobId,e.getMessage());
            LOG.error(errorMessage);
            throw new ScheduledExecutorException(errorMessage);
        }
    }

    /**
     * Deletes the supplied job from the job_task_data database table.
     */
    private static void deleteDependentJob(final String partitionId, final String jobId) throws ScheduledExecutorException
    {
        try (
                Connection connection = getConnection();
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
                Connection connection = getConnection();
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
        final String partitionId, final String jobId, final String failureDetails
    ) throws ScheduledExecutorException {
        /*
        SCMOD-6525 - FALSE POSITIVE on FORTIFY SCAN for Unreleased Resource: Database.
        */
        try (
                Connection conn = getConnection();
                CallableStatement stmt = conn.prepareCall("{call report_failure(?,?,?,?)}")
        ) {
            stmt.setString(1, partitionId);
            stmt.setString(2,jobId);
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

    /**
     * Creates a connection to the PostgreSQL database.
     */
    private static Connection getConnection() throws ScheduledExecutorException
    {
        final String databaseUrl = ScheduledExecutorConfig.getDatabaseURL();
        final String dbUser = ScheduledExecutorConfig.getDatabaseUsername();
        final String dbPass = ScheduledExecutorConfig.getDatabasePassword();
        final String appName = ScheduledExecutorConfig.getApplicationName() != null ? ScheduledExecutorConfig.getApplicationName()
                                 : "Job Service Scheduled Executor";

        // Only JDBC/PostgreSQL connections are supported.
        if ( !databaseUrl.startsWith(JDBC_POSTGRESQL_PREFIX) )
        {
            throw new ScheduledExecutorException("Invalid database url string format - must start with jdbc:postgresql:");
        }

        try {
            LOG.debug("Registering JDBC driver \"{}\" ...", JDBC_DRIVER);
        } catch (final Exception e){
            final String errorMessage = MessageFormat.format("Failed to register JDBC driver \"{0}\". {1}.", JDBC_DRIVER, e.getMessage());
            LOG.error(errorMessage);
            throw new ScheduledExecutorException(errorMessage, e);
        }

        final Connection conn;
        try {
            final Properties myProp = new Properties();
            myProp.put("user", dbUser);
            myProp.put("password", dbPass);
            myProp.put("ApplicationName", appName);
            LOG.debug("Connecting to database {} with username {} and password {} ...", databaseUrl, dbUser, dbPass);
            conn = DriverManager.getConnection(databaseUrl, myProp);
            LOG.debug("Connected to database.");
        } catch (final SQLException se) {
            final String errorMessage = MessageFormat.format("Failed to connect to database {0} with username {1} and password {2}.", databaseUrl, dbUser, dbPass);
            /*
            SCMOD-6525 - FALSE POSITIVE on FORTIFY SCAN for Log forging. The values of databaseUrl, dbUser, dbPass are all set using
            properties or env variables.
            */
            LOG.error(errorMessage);
            throw new ScheduledExecutorException(errorMessage);
        }

        return conn;
    }


}
