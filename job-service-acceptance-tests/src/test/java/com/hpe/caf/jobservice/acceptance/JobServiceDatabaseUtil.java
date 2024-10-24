/*
 * Copyright 2016-2024 Open Text.
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
package com.hpe.caf.jobservice.acceptance;

import com.hpe.caf.api.worker.JobStatus;
import com.hpe.caf.worker.batch.BatchWorkerConstants;

import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * This utility class provides helper methods to get database connection and assert database row states.
 */
public class JobServiceDatabaseUtil
{
    private static final Logger LOG = LoggerFactory.getLogger(JobServiceDatabaseUtil.class);

    public static void assertJobStatus(final String jobId, final String expectedStatus) throws SQLException
    {
        try (final Connection dbConnection = getDbConnection()) {

            //  Verify job status matches that expected.
            final PreparedStatement st = dbConnection.prepareStatement("SELECT status FROM job WHERE job_id = ?");
            st.setString(1, jobId);
            final ResultSet jobRS = st.executeQuery();
            jobRS.next();
            Assert.assertEquals(jobRS.getString(1).toLowerCase(Locale.ENGLISH), expectedStatus,
                    "Status of Job "+jobId+": "+jobRS.getString(1).toLowerCase(Locale.ENGLISH)+ " does not match with expected status: "+expectedStatus+".");
            jobRS.close();

            st.close();
        }
    }

    public static int getJobDelay(final String jobId) throws SQLException
    {
        try (final Connection dbConnection = getDbConnection()) {

            final PreparedStatement st = dbConnection.prepareStatement("SELECT delay FROM job WHERE job_id = ?");
            st.setString(1, jobId);
            final ResultSet jobRS = st.executeQuery();
            jobRS.next();
            final int delay = jobRS.getInt(1);
            jobRS.close();
            st.close();

            return delay;
        }
    }

    public static String getJobTaskDataEligibleRunDate(final String jobId) throws SQLException
    {
        try (final Connection dbConnection = getDbConnection()) {

            final PreparedStatement st = dbConnection.prepareStatement("SELECT eligible_to_run_date FROM job_task_data WHERE job_id = ?");
            st.setString(1, jobId);
            final ResultSet jobRS = st.executeQuery();
            jobRS.next();
            final String eligibleRunDate = jobRS.getString(1);
            jobRS.close();
            st.close();

            return eligibleRunDate;
        }
    }

    public static void assertJobRowExists(final String jobId) throws SQLException
    {
        try (final Connection dbConnection = getDbConnection()) {

            //  Verify a job row exists.
            PreparedStatement st = dbConnection.prepareStatement("SELECT * FROM job WHERE job_id = ?");
            st.setString(1, jobId);
            final ResultSet jobRS = st.executeQuery();
            jobRS.next();
            Assert.assertEquals(jobRS.getString(1), jobId, "Job "+jobId+" does not exist in job table");
            Assert.assertTrue(!jobRS.next(), "Duplicate job "+jobId+" should not exist in job table");
            st.clearBatch();
            jobRS.close();

            st.close();
        }
    }

    public static void assertJobRowDoesNotExist(final String jobId) throws SQLException
    {
        try (final Connection dbConnection = getDbConnection()) {

            //  Verify the job row has been removed.
            PreparedStatement st = dbConnection.prepareStatement("SELECT * FROM job WHERE job_id = ?");
            st.setString(1, jobId);
            final ResultSet jobRS = st.executeQuery();
            Assert.assertTrue(!jobRS.next(), "Job "+jobId+" should not exist in job table");
            jobRS.close();

            st.close();
        }
    }

    public static void assertJobDependencyRowsExist(final String jobId, final String dependentJobId,
                                                    final String batchWorkerMessageInQueue,
                                                    final String exampleWorkerMessageOutQueue) throws SQLException
    {
        try (final Connection dbConnection = getDbConnection()) {

            //  Verify job task data row exists.
            PreparedStatement st = dbConnection.prepareStatement("SELECT * FROM job_task_data WHERE job_id = ?");
            st.setString(1, jobId);
            final ResultSet jobTaskDataRS = st.executeQuery();
            jobTaskDataRS.next();
            Assert.assertEquals(jobTaskDataRS.getString(1), jobId, "Job "+jobId+" does not exist in job_task_data table");
            Assert.assertEquals(jobTaskDataRS.getString(2), BatchWorkerConstants.WORKER_NAME, "Worker "+BatchWorkerConstants.WORKER_NAME+" does not exist in job_task_data table");
            Assert.assertEquals(jobTaskDataRS.getInt(3), BatchWorkerConstants.WORKER_API_VERSION, "Worker API Version "+BatchWorkerConstants.WORKER_API_VERSION+" does not exist in job_task_data table");
            Assert.assertTrue(jobTaskDataRS.getBytes(4).length > 0, jobTaskDataRS.getBytes(4)+" array is empty");
            Assert.assertEquals(jobTaskDataRS.getString(5), batchWorkerMessageInQueue, "batchWorkerMessageInQueue does not match");
            Assert.assertEquals(jobTaskDataRS.getString(6), exampleWorkerMessageOutQueue, "exampleWorkerMessageOutQueue does not match");
            Assert.assertTrue(!jobTaskDataRS.next(), "Duplicate result for Job "+jobId+" should not exist in job_task_data table");
            st.clearBatch();
            jobTaskDataRS.close();

            //  Verify job dependency row exists.
            st = dbConnection.prepareStatement("SELECT * FROM job_dependency WHERE job_id = ?");
            st.setString(1, jobId);
            final ResultSet jobDependencyRS = st.executeQuery();
            jobDependencyRS.next();
            Assert.assertEquals(jobDependencyRS.getString(1), jobId, "Job "+jobId+" does not exist in job_dependency table");
            Assert.assertEquals(jobDependencyRS.getString(2), dependentJobId, "Dependent Job "+dependentJobId+" does not exist in job_dependency table");
            Assert.assertTrue(!jobDependencyRS.next(), "Duplicate result for Job "+jobId+" should not exist in job_dependency table");
            jobDependencyRS.close();

            st.close();
        }
    }

    public static void assertJobTaskDataRowExists(final String jobId)
            throws SQLException
    {
        try (final Connection dbConnection = getDbConnection();
             final PreparedStatement st = dbConnection.prepareStatement("SELECT * FROM job_task_data WHERE job_id = ?")) {

            // Verify job task data row exists.
            st.setString(1, jobId);
            final ResultSet jobTaskDataRS = st.executeQuery();
            jobTaskDataRS.next();
            Assert.assertEquals(jobTaskDataRS.getString(1), jobId, "Job "+jobId+" does not exist in job_task_data table");
            Assert.assertTrue(!jobTaskDataRS.next(), "Duplicate Job "+jobId+" should not exist in job_task_data table");
            st.clearBatch();
            jobTaskDataRS.close();
        }
    }

    public static void assertJobTaskDataRowDoesNotExist(final String jobId)
            throws SQLException
    {
        try (final Connection dbConnection = getDbConnection();
             final PreparedStatement st = dbConnection.prepareStatement("SELECT * FROM job_task_data WHERE job_id = ?")) {

            // Verify job task data row does not exist.
            st.setString(1, jobId);
            final ResultSet jobTaskDataRS = st.executeQuery();
            Assert.assertTrue(!jobTaskDataRS.next(), "Job "+jobId+" should not exist in job_task_data table");
            jobTaskDataRS.close();
        }
    }

    public static void assertJobDependencyRowsDoNotExist(final String jobId, final String dependentJobId)
            throws SQLException
    {
        try (final Connection dbConnection = getDbConnection()) {

            //  Verify job task data row has been removed.
            PreparedStatement st = dbConnection.prepareStatement("SELECT * FROM job_task_data WHERE job_id = ?");
            st.setString(1, jobId);
            final ResultSet jobTaskDataRS = st.executeQuery();
            Assert.assertTrue(!jobTaskDataRS.next(), "Job "+jobId+" was not removed from job_task_data table.");
            jobTaskDataRS.close();

            //  Verify job dependency row does not exist.
            st = dbConnection.prepareStatement("SELECT * FROM job_dependency WHERE job_id = ? AND dependent_job_id = ?");
            st.setString(1, jobId);
            st.setString(2, dependentJobId);
            final ResultSet jobDependencyRS = st.executeQuery();
            Assert.assertTrue(!jobDependencyRS.next(), "Job "+dependentJobId+" should not exist in job_dependency table");
            jobDependencyRS.close();

            st.close();
        }
    }

    public static void assertJobLabelRowsDoNotExist(final String jobId) throws SQLException {
        try (final Connection dbConnection = getDbConnection()) {

            //  Verify job task data row has been removed.
            try(final PreparedStatement st = dbConnection.prepareStatement("SELECT count(*) as result FROM public.label WHERE job_id = ?")) {
                st.setString(1, jobId);
                try (final ResultSet rs = st.executeQuery()) {
                    rs.next();
                    Assert.assertEquals(rs.getInt("result"), 0, "Job "+jobId+" row was not removed from label table");
                }
            }
        }
    }

    public static boolean isJobEligibleToRun(final String jobId) throws SQLException
    {
        try (
                final Connection connection = getDbConnection();
                final CallableStatement stmt = connection.prepareCall("{call get_dependent_jobs()}")
        ) {
            LOG.debug("Calling get_dependent_jobs() database function ...");
            stmt.execute();

            final List<String> jobTaskDataList = new ArrayList<>();
            final ResultSet rs = stmt.getResultSet();
            while (rs.next()) {
                jobTaskDataList.add(stmt.getResultSet().getString(2));
            }
            rs.close();

            return jobTaskDataList.contains(jobId);
        }
    }

    private static Connection getDbConnection() throws SQLException
    {
        final String dbHost = Objects.requireNonNull(getPropertyOrEnvVar("JOB_SERVICE_DATABASE_HOST"));
        final String dbPortString = Objects.requireNonNull(getPropertyOrEnvVar("JOB_SERVICE_DATABASE_PORT"));
        final String dbName = Objects.requireNonNull(getPropertyOrEnvVar("JOB_SERVICE_DATABASE_NAME"));
        final String dbUser = Objects.requireNonNull(getPropertyOrEnvVar("JOB_SERVICE_DATABASE_USERNAME"));
        final String dbPass = Objects.requireNonNull(getPropertyOrEnvVar("JOB_SERVICE_DATABASE_PASSWORD"));
        final String appName = getPropertyOrEnvVar("JOB_SERVICE_DATABASE_APPNAME") != null ? getPropertyOrEnvVar(
                "JOB_SERVICE_DATABASE_APPNAME") : "Job Service Acceptance";
        try {
            final int dbPort = Integer.parseInt(dbPortString);
            final Connection conn;
            final PGSimpleDataSource dbSource = new PGSimpleDataSource();
            dbSource.setServerNames(new String[]{dbHost});
            dbSource.setPortNumbers(new int[]{dbPort});
            dbSource.setDatabaseName(dbName);
            dbSource.setUser(dbUser);
            dbSource.setPassword(dbPass);
            dbSource.setApplicationName(appName);
            LOG.info("Connecting to database {} with host {}, port {}, username {} and password {}", dbName, dbHost, dbPort,
                    dbUser, dbPass);
            conn = dbSource.getConnection();
            LOG.info("Connected to database");
            return conn;
        } catch (final Exception e) {
            LOG.error("ERROR connecting to database {} with host {}, port {}, username {} and password {}", dbName, dbHost, dbPortString,
                    dbUser, dbPass);
            throw e;
        }
    }
    
    private static String getPropertyOrEnvVar(final String key)
    {
        final String propertyValue = System.getProperty(key);
        return (propertyValue != null) ? propertyValue : System.getenv(key);
    }
    
    public static void assertDeleteLogNotEmpty() throws SQLException
    {
        try(final Connection dbConnection = getDbConnection();
            final PreparedStatement st = dbConnection.prepareStatement("SELECT count(*) as result FROM public.delete_log ");
            final ResultSet rs = st.executeQuery())
        {
            rs.next();
            Assert.assertNotEquals(rs.getInt("result"), 0, "Soft deleted table names not present. ");
        }
    }

    public static void insertRowIntoJobTable(
            final String jobId,
            final String partitionId,
            final JobStatus jobStatus) throws SQLException
    {
        final String sql = "INSERT INTO public.job ("
            + "job_id, "
            + "name, "
            + "description, "
            + "\"data\", "
            + "create_date, "
            + "status, "
            + "percentage_complete, "
            + "failure_details, "
            + "job_hash, "
            + "delay, "
            + "last_update_date, "
            + "partition_id) "
            + "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (final Connection dbConnection = getDbConnection()) {
            PreparedStatement preparedStatement = dbConnection.prepareStatement(sql);

            preparedStatement.setString(1, jobId); // job_id
            preparedStatement.setString(2, ""); // name
            preparedStatement.setString(3, ""); // description
            preparedStatement.setString(4, ""); // data
            preparedStatement.setTimestamp(5, new Timestamp(System.currentTimeMillis())); // create_data
            preparedStatement.setObject(6, jobStatus, Types.OTHER); // status
            preparedStatement.setDouble(7, 0.00); // percentage_complete
            preparedStatement.setString(8, ""); // failure_details
            preparedStatement.setInt(9, 0); // job_hash
            preparedStatement.setInt(10, 0); // delay
            preparedStatement.setTimestamp(11, new Timestamp(System.currentTimeMillis())); // last_update_date
            preparedStatement.setString(12, partitionId); // partition_id

            // Execute the SQL statement
            int rowsAffected = preparedStatement.executeUpdate();
            System.out.println(rowsAffected + " rows affected.");
        }
    }
}
