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

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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


    public void createJobTask(
        final String partitionId, final String jobId, final String jobDescriptor
    ) throws Exception {
        // For jobtracking worker integration tests a simple job will do - no need for a task to be created too.
        String name = MessageFormat.format("{0}_{1}", jobDescriptor, jobId);
        String description = MessageFormat.format("{0}_{1} description", jobDescriptor, jobId);
        String data = MessageFormat.format("{0}_{1} data", jobDescriptor, jobId);
        int jobHash = random.nextInt();

        try(Connection connection = getConnection();
            CallableStatement stmt = connection.prepareCall("{call create_job(?,?,?,?,?,?)}")) {
            stmt.setString(1, partitionId);
            stmt.setString(2, jobId);
            stmt.setString(3, name);
            stmt.setString(4, description);
            stmt.setString(5, data);
            stmt.setInt(6, jobHash);
            LOG.info("Creating job {}", jobId);
            stmt.execute();
        }

        LOG.info("Created job {}", jobId);
    }

    /**
     * Retrieve the job with the given ID from the job-service database.
     */
    public DBJob getJob(final String partitionId, final String jobTaskId) throws SQLException {
        final DBJob jobStatus = new DBJob();
        try(Connection connection = getConnection();
            CallableStatement stmt = connection.prepareCall("{call get_job(?,?)}")) {

            stmt.setString(1, partitionId);
            stmt.setString(2, jobTaskId);
            LOG.info("Calling get_job for job task {}", jobTaskId);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next(); // expect exactly 1 record

                jobStatus.setJobId(rs.getString("job_id"));
                jobStatus.setStatus(JobStatus.valueOf(rs.getString("status")));
                jobStatus.setPercentageComplete(rs.getFloat("percentage_complete"));
                jobStatus.setCreateDate(Instant.parse(rs.getString("create_date")));
                jobStatus.setLastUpdateDate(Instant.parse(rs.getString("last_update_date")));
                jobStatus.setFailureDetails(rs.getString("failure_details"));

            }
        }
        return jobStatus;
    }

    public List<DBJob> getJobs(final String partitionId) throws SQLException {
        final List<DBJob> dbJobs = new ArrayList<>();
        try (Connection connection = getConnection();
             CallableStatement stmt = connection.prepareCall("{call get_jobs(?,?,?,?,?,?,?,?,?,?)}")) {
            stmt.setString(1, partitionId);
            stmt.setString(2, null);
            stmt.setString(3, null);
            stmt.setInt(4, 200); // in_limit
            stmt.setInt(5, -1);
            stmt.setString(6, null);
            stmt.setString(7, null);
            stmt.setBoolean(8, true);
            stmt.setString(9, null);
            stmt.setString(10, null);


            LOG.info("Calling get_jobs for partition {}", partitionId);
            try (ResultSet rs = stmt.executeQuery()) {
                final DBJob jobStatus = new DBJob();
                rs.next(); // expect exactly 1 record

                jobStatus.setJobId(rs.getString("job_id"));
                jobStatus.setStatus(JobStatus.valueOf(rs.getString("status")));
                jobStatus.setPercentageComplete(rs.getFloat("percentage_complete"));
                jobStatus.setCreateDate(Instant.parse(rs.getString("create_date")));
                jobStatus.setLastUpdateDate(Instant.parse(rs.getString("last_update_date")));
                jobStatus.setFailureDetails(rs.getString("failure_details"));
                dbJobs.add(jobStatus);
            }
        }
        return dbJobs;
    }
    /**
     * Takes in two arrays. The first has the brut values and the second, the expected result after processing
     * We process the brut values with the function then compare the result against the expected result provided
     * And return a boolean (true if success, false otherwise)
     */
    public boolean taskCollapseTest( final String[] input, final String[] output) throws SQLException {

        try(final Connection connection = getConnection();
            final CallableStatement stmt = connection.prepareCall("{call task_collapse(?, ?)}")) {
            LOG.info("Calling task_collapse for job task ");
            final Array inputArray = connection.createArrayOf("VARCHAR", input);
            final Array outputArray = connection.createArrayOf("VARCHAR", output);
            stmt.setArray(1, inputArray);
            stmt.setArray(2, outputArray);
            try (final ResultSet rs = stmt.executeQuery()) {
                return rs.next(); // expect exactly 1 record
            }
        }
    }


    protected void databaseFiller(String scriptFilePath) throws SQLException, IOException {
        BufferedReader reader = null;
        Statement statement = null;
        final Connection connection=getConnection();
        try {
            // create statement object
            statement = connection.createStatement();
            // initialize file reader
            reader = new BufferedReader(new FileReader(scriptFilePath));
            String line = null;
            // read script line by line
            while ((line = reader.readLine()) != null) {
                // execute query
                statement.execute(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // close file reader
            if (reader != null) {
                reader.close();
            }
            // close db connection
            if (connection != null) {
                connection.close();
            }
        }
    }




    public void verifyJobStatus(
        final String partitionId, String jobTaskId, JobReportingExpectation jobReportingExpectation
    ) throws Exception {
        final DBJob job = getJob(partitionId, jobTaskId);
        LOG.info("Called get_job for job task {}. Verifying results against expectations...", jobTaskId);
        assertEquals(jobTaskId, "job_id", job.getJobId(), jobReportingExpectation.getJobId());
        assertEquals(jobTaskId, "status", job.getStatus(), jobReportingExpectation.getStatus());
        assertEquals(jobTaskId, "percentage_complete", job.getPercentageComplete(), jobReportingExpectation.getPercentageComplete());
        assertHasValue(jobTaskId, "failure_details", job.getFailureDetails(), jobReportingExpectation.getFailureDetailsPresent());

        //  Parse JSON failure sub-strings.
        String failureDetails = job.getFailureDetails();
        if (failureDetails != null && !failureDetails.isEmpty()) {
            JSONObject json = new JSONObject(failureDetails);
            assertHasValue(jobTaskId, "failureId", json.getString("failureId"), jobReportingExpectation.getFailureDetailsIdPresent());
            assertHasValue(jobTaskId, "failureTime", json.getString("failureTime"), jobReportingExpectation.getFailureDetailsTimePresent());
            assertHasValue(jobTaskId, "failureSource", json.getString("failureSource"), jobReportingExpectation.getFailureDetailsSourcePresent());
            assertHasValue(jobTaskId, "failureMessage", json.getString("failureMessage"), jobReportingExpectation.getFailureDetailsMessagePresent());
        }
    }


    public String createJobId() throws InterruptedException {
        Thread.sleep(10);
        return new StringBuilder("J").append(System.currentTimeMillis()).toString();
    }


    private Connection getConnection () throws SQLException {
        try {
            final String appName = JobDatabaseProperties.getApplicationName() != null ? JobDatabaseProperties.getApplicationName() : "Job Tracking Worker";
            Connection conn;
            Properties myProp = new Properties();
            myProp.put("user", JobDatabaseProperties.getDatabaseUsername());
            myProp.put("password", JobDatabaseProperties.getDatabasePassword());
            myProp.put("ApplicationName", appName);
            LOG.info("Connecting to database {} with username {} and password {}", JobDatabaseProperties.getDatabaseUrl(), JobDatabaseProperties.getDatabaseUsername(), JobDatabaseProperties.getDatabasePassword());
            conn = DriverManager.getConnection(JobDatabaseProperties.getDatabaseUrl(), myProp);
            LOG.info("Connected to database");
            return conn;
        } catch (Exception e) {
            LOG.error("ERROR connecting to database {} with username {} and password {}. ", JobDatabaseProperties.getDatabaseUrl(), JobDatabaseProperties.getDatabaseUsername(), JobDatabaseProperties.getDatabasePassword(), e);
            throw e;
        }
    }


    private void assertEquals(final String jobTaskId, final String column, final String actual, final String expected) throws Exception {
        LOG.info("Job task {} has {} = {}", jobTaskId, column, actual == null ? "null" : actual);
        if (!expected.equals(actual)) {
            LOG.error("Job task {} does not have the expected {} in the Job Database. Expected {}. Found {}.", jobTaskId, column, expected, actual);
            throw new Exception(MessageFormat.format("Job task {0} does not have the expected {1} in the Job Database. Expected {2}. Found {3}.", jobTaskId, column, expected, actual));
        }
    }


    private void assertEquals(final String jobTaskId, final String column, final float actual, final float expected) throws Exception {
        LOG.info("Job task {} has {} = {}", jobTaskId, column, actual);
        if (expected != actual) {
            LOG.error("Job task {} does not have the expected {} in the Job Database. Expected {}. Found {}.", jobTaskId, column, expected, actual);
            throw new Exception(MessageFormat.format("Job task {0} does not have the expected {1} in the Job Database. Expected {2}. Found {3}.", jobTaskId, column, expected, actual));
        }
    }


    private void assertEquals(final String jobTaskId, final String column, final JobStatus actual, final JobStatus expected) throws Exception {
        LOG.info("Job task {} has {} = {}", jobTaskId, column, actual);
        if (expected != actual) {
            LOG.error("Job task {} does not have the expected {} in the Job Database. Expected {}. Found {}.", jobTaskId, column, expected, actual);
            throw new Exception(MessageFormat.format("Job task {0} does not have the expected {1} in the Job Database. Expected {2}. Found {3}.", jobTaskId, column, expected, actual));
        }
    }


    private void assertHasValue(final String jobTaskId, final String column, final String actual, final boolean expectedHasValue) throws Exception {
        LOG.info("Job task {} has {} = {}", jobTaskId, column, actual == null ? "null" : actual);
        boolean actualHasValue = (actual != null) && !actual.isEmpty();
        if (expectedHasValue != actualHasValue) {
            LOG.error("Job task {} does not have the expected {} in the Job Database. Expected {}. Found \"{}\".", jobTaskId, column, expectedHasValue ? "a value" : "no value", actual == null ? "null" : actual);
            throw new Exception(MessageFormat.format("Job task {0} does not have the expected {1} in the Job Database. Expected {2}. Found \"{3}\".", jobTaskId, column, expectedHasValue ? "a value" : "no value", actual == null ? "null" : actual));
        }
    }
}
