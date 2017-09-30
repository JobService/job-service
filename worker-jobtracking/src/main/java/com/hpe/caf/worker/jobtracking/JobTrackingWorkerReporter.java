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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.sql.*;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.Properties;


/**
 * Implementation of job reporting to a job-tracking Job Database, specifically supporting only JDBC/PostgreSQL connections.
 */
public class JobTrackingWorkerReporter implements JobTrackingReporter {

    private static final String JDBC_POSTGRESQL_PREFIX = "jdbc:postgresql:";
    private static final String JDBC_DRIVER = "org.postgresql.Driver";

    private static final Logger LOG = LoggerFactory.getLogger(JobTrackingWorkerReporter.class);

    @NotNull
    @Size(min = 1)
    private String jobDatabaseURL;

    /**
     * The username to use when connecting to the Job Database.
     */
    @NotNull
    @Size(min = 1)
    private String jobDatabaseUsername;

    /**
     * The password to use with the configured username when connecting to the Job Database.
     */
    @NotNull
    @Size(min = 1)
    private String jobDatabasePassword;


    public JobTrackingWorkerReporter() throws JobReportingException {
        this.jobDatabaseURL = Objects.requireNonNull(JobDatabaseProperties.getDatabaseUrl()).toLowerCase();
        if (!jobDatabaseURL.startsWith(JDBC_POSTGRESQL_PREFIX))
        {
            throw new JobReportingException("Invalid database url string format - must start with jdbc:postgresql:");
        }
        this.jobDatabaseUsername = Objects.requireNonNull(JobDatabaseProperties.getDatabaseUsername());
        this.jobDatabasePassword = Objects.requireNonNull(JobDatabaseProperties.getDatabasePassword());

        try {
            LOG.debug("Registering JDBC driver \"{}\" ...", JDBC_DRIVER);
            Class.forName(JDBC_DRIVER);
        } catch (Exception e){
            LOG.error("Failed to register JDBC driver \"{}\" ...", JDBC_DRIVER);
            throw new JobReportingException(MessageFormat.format("Failed to register JDBC driver \"{0}\". {1}", JDBC_DRIVER, e.getMessage()), e);
        }
    }

    /**
     * Reports the percentage completed of the specified task id.
     *
     * @param jobTaskId identifies the job task whose progress is to be reported
     * @param estimatedPercentageCompleted an indication of progress on the job task
     * @throws JobReportingException
     */
    @Override
    public void reportJobTaskProgress(final String jobTaskId, final int estimatedPercentageCompleted) throws JobReportingException {
        try (Connection conn = getConnection()) {
            //TODO - FUTURE: pass estimatedPercentageCompleted to the database function
            report(conn, jobTaskId, JobStatus.Active);
        } catch (SQLException se) {
            throw new JobReportingException(MessageFormat.format("Failed to report the progress of job task {0}. {1}", jobTaskId, se.getMessage()), se);
        }
    }

    /**
     * Reports the specified job task as complete.
     *
     * @param jobTaskId identifies the completed job task
     * @throws JobReportingException
     */
    @Override
    public void reportJobTaskComplete(final String jobTaskId) throws JobReportingException {
        try (Connection conn = getConnection()) {
            report(conn, jobTaskId, JobStatus.Completed);
        } catch (SQLException se) {
            throw new JobReportingException(MessageFormat.format("Failed to report the completion of job task {0}. {1}", jobTaskId, se.getMessage()), se);
        }
    }

    /**
     * Reports the job task as failure and to be retried.
     *
     * @param jobTaskId identifies the failed job task
     * @param retryDetails an explanation of the retry of this job task
     * @throws JobReportingException
     */
    @Override
    public void reportJobTaskRetry(final String jobTaskId, final String retryDetails) throws JobReportingException {
        try (Connection conn = getConnection()) {
            //TODO - Is there no way to report retryDetails?
            report(conn, jobTaskId, JobStatus.Active);
        } catch (SQLException se) {
            throw new JobReportingException(MessageFormat.format("Failed to report the failure and retry of job task {0}. {1}", jobTaskId, se.getMessage()), se);
        }
    }

    /**
     * Reports the job task as a rejected task.
     *
     * @param jobTaskId identifies the rejected job task
     * @param rejectionDetails an explanation of the failure and rejection of the job task
     * @throws JobReportingException
     */
    @Override
    public void reportJobTaskRejected(final String jobTaskId, final JobTrackingWorkerFailure rejectionDetails) throws JobReportingException {
        try (Connection conn = getConnection()) {

            ObjectMapper mapper = new ObjectMapper();
            final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            mapper.setDateFormat(df);

            reportFailure(conn, jobTaskId, mapper.writeValueAsString(rejectionDetails));

        } catch (SQLException se) {
            throw new JobReportingException(MessageFormat.format("Failed to report the failure and rejection of job task {0}. {1}", jobTaskId, se.getMessage()), se);
        } catch (JsonProcessingException e) {
            throw new JobReportingException("Cannot serialize job task failure details.",e);
        }
    }


    /**
     * Try to connect to the Job Database using the connection info provided in ctor.
     */
    @Override
    public boolean verifyJobDatabase() {
        try (Connection conn = getConnection()) {
        } catch (Exception e) {
            LOG.error("Failed to verify connection to the Job Database. ", e);
            return false;
        }
        return true;
    }


    /**
     * Creates a connection to the (PostgreSQL) Job Database.
     */
    private Connection getConnection () throws JobReportingException {
        Connection conn;

        try{
            LOG.debug("Connecting to database {} ...", jobDatabaseURL);
            Properties myProp = new Properties();
            myProp.put("user", jobDatabaseUsername);
            myProp.put("password", jobDatabasePassword);
            conn = DriverManager.getConnection(jobDatabaseURL, myProp);
        } catch(Exception e){
            LOG.error("Failed to connect to database {}. ", jobDatabaseURL, e);
            throw new JobReportingException(e.getMessage(), e);
        }

        return conn;
    }


    /**
     * Reports the status of the specified job task using the supplied Job Database connection.
     * Failure status should not be reported using this method - instead use the reportFailure method.
     * @param connection PostgreSQL connection to the Job Database
     * @param jobTaskId job task to be reported to the Job Database
     * @param status status of the job task
     * @throws SQLException
     */
    private void report(Connection connection, final String jobTaskId, final JobStatus status) throws SQLException {
        String reportProgressFnCallSQL = "{call report_progress(?,?)}";
        try (CallableStatement stmt = connection.prepareCall(reportProgressFnCallSQL)) {
            stmt.setString(1, jobTaskId);
            stmt.setObject(2, status, Types.OTHER);
            LOG.info("Reporting progress of job task {} with status {} ...", jobTaskId, status.name());
            stmt.execute();
        }
    }


    /**
     * Reports the failure of the specified job task using the supplied Job Database connection.
     * @param connection PostgreSQL connection to the Job Database
     * @param jobTaskId the failed job task to be reported to the Job Database
     * @param failureDetails description of the failure
     * @throws SQLException
     */
    private void reportFailure(Connection connection, final String jobTaskId, final String failureDetails) throws SQLException {
        String reportFailureFnCallSQL = "{call report_failure(?,?)}";
        try (CallableStatement stmt = connection.prepareCall(reportFailureFnCallSQL)) {
            stmt.setString(1, jobTaskId);
            stmt.setString(2, failureDetails);
            LOG.info("Reporting failure of job task {} ...", jobTaskId);
            stmt.execute();
        }
    }
}
