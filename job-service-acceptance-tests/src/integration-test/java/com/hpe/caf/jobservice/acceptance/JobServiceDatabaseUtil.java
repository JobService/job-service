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
package com.hpe.caf.jobservice.acceptance;

import com.hpe.caf.worker.batch.BatchWorkerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Properties;

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
            Assert.assertEquals(jobRS.getString(1).toLowerCase(Locale.ENGLISH), expectedStatus);
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
            Assert.assertEquals(jobRS.getString(1), jobId);
            Assert.assertTrue(!jobRS.next());
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
            Assert.assertTrue(!jobRS.next());
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
            Assert.assertEquals(jobTaskDataRS.getString(1), jobId);
            Assert.assertEquals(jobTaskDataRS.getString(2), BatchWorkerConstants.WORKER_NAME);
            Assert.assertEquals(jobTaskDataRS.getInt(3), BatchWorkerConstants.WORKER_API_VERSION);
            Assert.assertTrue(jobTaskDataRS.getBytes(4).length > 0);
            Assert.assertEquals(jobTaskDataRS.getString(5), batchWorkerMessageInQueue);
            Assert.assertEquals(jobTaskDataRS.getString(6), exampleWorkerMessageOutQueue);
            Assert.assertTrue(!jobTaskDataRS.next());
            st.clearBatch();
            jobTaskDataRS.close();

            //  Verify job dependency row exists.
            st = dbConnection.prepareStatement("SELECT * FROM job_dependency WHERE job_id = ?");
            st.setString(1, jobId);
            final ResultSet jobDependencyRS = st.executeQuery();
            jobDependencyRS.next();
            Assert.assertEquals(jobDependencyRS.getString(1), jobId);
            Assert.assertEquals(jobDependencyRS.getString(2), dependentJobId);
            Assert.assertTrue(!jobDependencyRS.next());
            jobDependencyRS.close();

            st.close();
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
            Assert.assertTrue(!jobTaskDataRS.next());
            jobTaskDataRS.close();

            //  Verify job dependency row does not exist.
            st = dbConnection.prepareStatement("SELECT * FROM job_dependency WHERE job_id = ? AND dependent_job_id = ?");
            st.setString(1, jobId);
            st.setString(2, dependentJobId);
            final ResultSet jobDependencyRS = st.executeQuery();
            Assert.assertTrue(!jobDependencyRS.next());
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
                    Assert.assertEquals(rs.getInt("result"), 0);
                }
            }
        }
    }

    private static Connection getDbConnection() throws SQLException
    {
        final String databaseUrl = System.getProperty("CAF_DATABASE_URL", System.getenv("CAF_DATABASE_URL"));
        final String dbUser = System.getProperty("CAF_DATABASE_USERNAME", System.getenv("CAF_DATABASE_USERNAME"));
        final String dbPass = System.getProperty("CAF_DATABASE_PASSWORD", System.getenv("CAF_DATABASE_PASSWORD"));
        final String appName = System.getProperty("CAF_DATABASE_APPNAME", System.getenv("CAF_DATABASE_APPNAME"));
        try {
            final Connection conn;
            final Properties myProp = new Properties();
            myProp.put("user", dbUser);
            myProp.put("password", dbPass);
            myProp.put("ApplicationName", appName != null ? appName : "Job Service Acceptance");
            LOG.info("Connecting to database " + databaseUrl + " with username " + dbUser + " and password " + dbPass);
            conn = DriverManager.getConnection(databaseUrl, myProp);
            LOG.info("Connected to database");
            return conn;
        } catch (final Exception e) {
            LOG.error("ERROR connecting to database " + databaseUrl + " with username " + dbUser + " and password "
                    + dbPass);
            throw e;
        }
    }

}
