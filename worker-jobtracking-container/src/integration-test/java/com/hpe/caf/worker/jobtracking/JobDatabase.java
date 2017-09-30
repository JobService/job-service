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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.Random;

/**
 * Provides methods wrapping access to the (PostgreSQL) Job Database.
 */
public class JobDatabase {

    private static final String JDBC_DRIVER = "org.postgresql.Driver";
    private static final Logger LOG = LoggerFactory.getLogger(JobDatabase.class);

    private Random random;


    public JobDatabase() throws Exception {
        this.random = new Random();
        LOG.info("Loading JDBC driver {}", JDBC_DRIVER);
        Class.forName(JDBC_DRIVER);
        LOG.info("Loaded JDBC driver");
    }


    public String createJobTask(final String jobDescriptor) throws Exception {
        // For jobtracking worker integration tests a simple job will do - no need for a task to be created too.
        String jobId = createJobId();
        String name = MessageFormat.format("{0}_{1}", jobDescriptor, jobId);
        String description = MessageFormat.format("{0}_{1} description", jobDescriptor, jobId);
        String data = MessageFormat.format("{0}_{1} data", jobDescriptor, jobId);
        int jobHash = random.nextInt();
        String createJobFnCallSQL = "{call create_job(?,?,?,?,?)}";

        try(Connection connection = getConnection();
            CallableStatement stmt = connection.prepareCall(createJobFnCallSQL)) {
            stmt.setString(1, jobId);
            stmt.setString(2, name);
            stmt.setString(3, description);
            stmt.setString(4, data);
            stmt.setInt(5, jobHash);
            LOG.info("Creating job {}", jobId);
            stmt.execute();
        }

        LOG.info("Created job {}", jobId);
        return jobId;
    }


    public void verifyJobStatus(String jobTaskId, JobReportingExpectation jobReportingExpectation) throws Exception {
        String getJobFnCallSQL = "{call get_job(?)}";

        try(Connection connection = getConnection();
            CallableStatement stmt = connection.prepareCall(getJobFnCallSQL)) {

            stmt.setString(1, jobTaskId);
            LOG.info("Calling get_job for job task {}", jobTaskId);
            try (ResultSet rs = stmt.executeQuery()) {
                LOG.info("Called get_job for job task {}. Verifying results against expectations...", jobTaskId);
                while (rs.next()) {
                    assertEquals(jobTaskId, rs, "job_id", jobReportingExpectation.getJobId());
                    assertEquals(jobTaskId, rs, "status", jobReportingExpectation.getStatus());
                    assertEquals(jobTaskId, rs, "percentage_complete", jobReportingExpectation.getPercentageComplete());
                    assertHasValue(jobTaskId, rs, "failure_details", jobReportingExpectation.getFailureDetailsPresent());

                    //  Parse JSON failure sub-strings.
                    String failureDetails = rs.getString("failure_details");
                    if (failureDetails != null && !failureDetails.isEmpty()) {
                        ObjectMapper mapper = new ObjectMapper();
                        JobTrackingWorkerFailure objFailure = mapper.readValue(failureDetails, JobTrackingWorkerFailure.class);
                        assertHasValue(jobTaskId, "failureId", objFailure.getFailureId(), jobReportingExpectation.getFailureDetailsIdPresent());
                        assertHasValue(jobTaskId, "failureTime", objFailure.getFailureTime().toString(), jobReportingExpectation.getFailureDetailsTimePresent());
                        assertHasValue(jobTaskId, "failureSource", objFailure.getFailureSource(), jobReportingExpectation.getFailureDetailsSourcePresent());
                        assertHasValue(jobTaskId, "failureMessage", objFailure.getFailureMessage(), jobReportingExpectation.getFailureDetailsMessagePresent());
                        }
                    }
                }
            }
    }


    private String createJobId() throws InterruptedException {
        Thread.sleep(10);
        return new StringBuilder("J").append(System.currentTimeMillis()).toString();
    }


    private Connection getConnection () throws SQLException {
        try {
            Connection conn;
            Properties myProp = new Properties();
            myProp.put("user", JobDatabaseProperties.getDatabaseUsername());
            myProp.put("password", JobDatabaseProperties.getDatabasePassword());
            LOG.info("Connecting to database {} with username {} and password {}", JobDatabaseProperties.getDatabaseUrl(), JobDatabaseProperties.getDatabaseUsername(), JobDatabaseProperties.getDatabasePassword());
            conn = DriverManager.getConnection(JobDatabaseProperties.getDatabaseUrl(), myProp);
            LOG.info("Connected to database");
            return conn;
        } catch (Exception e) {
            LOG.error("ERROR connecting to database {} with username {} and password {}. ", JobDatabaseProperties.getDatabaseUrl(), JobDatabaseProperties.getDatabaseUsername(), JobDatabaseProperties.getDatabasePassword(), e);
            throw e;
        }
    }


    private void assertEquals(final String jobTaskId, ResultSet rs, final String column, final String expected) throws Exception {
        String actual = rs.getString(column);
        LOG.info("Job task {} has {} = {}", jobTaskId, column, actual == null ? "null" : actual);
        if (!expected.equals(actual)) {
            LOG.error("Job task {} does not have the expected {} in the Job Database. Expected {}. Found {}.", jobTaskId, column, expected, actual);
            throw new Exception(MessageFormat.format("Job task {0} does not have the expected {1} in the Job Database. Expected {2}. Found {3}.", jobTaskId, column, expected, actual));
        }
    }


    private void assertEquals(final String jobTaskId, ResultSet rs, final String column, final float expected) throws Exception {
        float actual = rs.getFloat(column);
        LOG.info("Job task {} has {} = {}", jobTaskId, column, actual);
        if (expected != actual) {
            LOG.error("Job task {} does not have the expected {} in the Job Database. Expected {}. Found {}.", jobTaskId, column, expected, actual);
            throw new Exception(MessageFormat.format("Job task {0} does not have the expected {1} in the Job Database. Expected {2}. Found {3}.", jobTaskId, column, expected, actual));
        }
    }


    private void assertEquals(final String jobTaskId, ResultSet rs, final String column, final JobStatus expected) throws Exception {
        JobStatus actual = JobStatus.valueOf(rs.getString(column));
        LOG.info("Job task {} has {} = {}", jobTaskId, column, actual);
        if (expected != actual) {
            LOG.error("Job task {} does not have the expected {} in the Job Database. Expected {}. Found {}.", jobTaskId, column, expected, actual);
            throw new Exception(MessageFormat.format("Job task {0} does not have the expected {1} in the Job Database. Expected {2}. Found {3}.", jobTaskId, column, expected, actual));
        }
    }


    private void assertHasValue(final String jobTaskId, ResultSet rs, final String column, final boolean expectedHasValue) throws Exception {
        String actual = rs.getString(column);
        LOG.info("Job task {} has {} = {}", jobTaskId, column, actual == null ? "null" : actual);
        boolean actualHasValue = (actual != null) && !actual.isEmpty();
        if (expectedHasValue != actualHasValue) {
            LOG.error("Job task {} does not have the expected {} in the Job Database. Expected {}. Found \"{}\".", jobTaskId, column, expectedHasValue ? "a value" : "no value", actual == null ? "null" : actual);
            throw new Exception(MessageFormat.format("Job task {0} does not have the expected {1} in the Job Database. Expected {2}. Found \"{3}\".", jobTaskId, column, expectedHasValue ? "a value" : "no value", actual == null ? "null" : actual));
        }
    }

    private void assertHasValue(final String jobTaskId, final  String column, final String actual, final boolean expectedHasValue) throws Exception {
        LOG.info("Job task {} has {} = {}", jobTaskId, column, actual == null ? "null" : actual);
        boolean actualHasValue = (actual != null) && !actual.isEmpty();
        if (expectedHasValue != actualHasValue) {
            LOG.error("Job task {} does not have the expected {} in the Job Database. Expected {}. Found \"{}\".", jobTaskId, column, expectedHasValue ? "a value" : "no value", actual == null ? "null" : actual);
            throw new Exception(MessageFormat.format("Job task {0} does not have the expected {1} in the Job Database. Expected {2}. Found \"{3}\".", jobTaskId, column, expectedHasValue ? "a value" : "no value", actual == null ? "null" : actual));
        }
    }
}
