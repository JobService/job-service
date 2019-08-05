/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
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
package com.hpe.caf.services.job.api;

import com.hpe.caf.services.db.client.DatabaseConnectionProvider;
import com.hpe.caf.services.job.api.generated.model.Failure;
import com.hpe.caf.services.job.api.generated.model.Job;
import com.hpe.caf.services.configuration.AppConfig;
import com.hpe.caf.services.job.api.generated.model.JobSortField;
import com.hpe.caf.services.job.api.generated.model.JobSortOrder;
import com.hpe.caf.services.job.exceptions.BadRequestException;
import com.hpe.caf.services.job.exceptions.ForbiddenException;
import com.hpe.caf.services.job.exceptions.NotFoundException;
import com.hpe.caf.services.job.util.JobTaskId;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * The DatabaseHelper class is responsible for database operations.
 */
public final class DatabaseHelper
{
    private static final String FAILURE_PROPERTY_MISSING = "Unknown";

    private static AppConfig appConfig;

    private static final Logger LOG = LoggerFactory.getLogger(DatabaseHelper.class);

    /**
     * Instantiates a new DBUtil
     *
     * @param appConfig PostgreSQL database connection properties incl url (i.e. "jdbc:postgresql://PostgreSQLHost:portNumber/databaseName"), username and password
     */
    public DatabaseHelper(AppConfig appConfig)
    {
        DatabaseHelper.appConfig = appConfig;
    }

    /**
     * Returns a list of job definitions in the system.
     */
    public Job[] getJobs(final String partitionId, String jobIdStartsWith, String statusType, Integer limit, Integer offset, final JobSortField sortField, final JobSortOrder sortOrder) throws Exception {

        List<Job> jobs=new ArrayList<>();

        try (
                Connection conn = DatabaseConnectionProvider.getConnection(appConfig);
                CallableStatement stmt = conn.prepareCall("{call get_jobs(?,?,?,?,?,?,?)}")
        ) {
            if (jobIdStartsWith == null) {
                jobIdStartsWith = "";
            }
            if (statusType == null) {
                statusType = "";
            }
            if (limit == null) {
                limit = 0;
            }
            if (offset == null) {
                offset = 0;
            }
            stmt.setString(1, partitionId);
            stmt.setString(2, jobIdStartsWith);
            stmt.setString(3, statusType);
            stmt.setInt(4, limit);
            stmt.setInt(5, offset);
            stmt.setString(6, sortField.getDbField());
            stmt.setBoolean(7, sortOrder.getDbValue());

            //  Execute a query to return a list of all job definitions in the system.
            LOG.debug("Calling get_jobs() database function...");
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Job job = new Job();
                job.setId(rs.getString("job_id"));
                job.setName(rs.getString("name"));
                job.setDescription(rs.getString("description"));
                job.setExternalData(rs.getString("data"));
                job.setCreateTime(getDate(rs.getString("create_date")));
                job.setLastUpdateTime(getDate(rs.getString("last_update_date")));
                job.setStatus(Job.StatusEnum.valueOf(rs.getString("status").toUpperCase()));
                job.setPercentageComplete(rs.getFloat("percentage_complete"));

                //  Parse JSON failure sub-strings.
                String failureDetails = rs.getString("failure_details");
                if (ApiServiceUtil.isNotNullOrEmpty(failureDetails)) {
                    job.setFailures(getFailuresAsList(failureDetails));
                }

                jobs.add(job);
            }
            rs.close();
        }

        //  Convert arraylist to array of jobs.
        Job[] jobArr = new Job[jobs.size()];
        jobArr = jobs.toArray(jobArr);

        return jobArr;
    }

    /**
     * Returns the number of job definitions in the system.
     */
    public long getJobsCount(final String partitionId, String jobIdStartsWith, String statusType) throws Exception {

        long jobsCount = 0;

        try (
                Connection conn = DatabaseConnectionProvider.getConnection(appConfig);
                CallableStatement stmt = conn.prepareCall("{call get_jobs_count(?,?,?)}")
        ) {
            if (jobIdStartsWith == null) {
                jobIdStartsWith = "";
            }
            if (statusType == null) {
                statusType = "";
            }
            stmt.setString(1, partitionId);
            stmt.setString(2, jobIdStartsWith);
            stmt.setString(3, statusType);

            //  Execute a query to return a count of all job definitions in the system.
            LOG.debug("Calling get_jobs_count() database function...");
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                jobsCount = rs.getLong(1);
            }

            rs.close();
        }

        return jobsCount;
    }

    /**
     * Returns the job definition for the specified job.
     */
    public Job getJob(final String partitionId, String jobId) throws Exception {

        Job job = null;

        try (
                Connection conn = DatabaseConnectionProvider.getConnection(appConfig);
                CallableStatement stmt = conn.prepareCall("{call get_job(?,?)}")
        ) {
            stmt.setString(1, partitionId);
            stmt.setString(2,jobId);

            //  Execute a query to return a list of all job definitions in the system.
            LOG.debug("Calling get_job() database function...");
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                job = new Job();
                job.setId(rs.getString("job_id"));
                job.setName(rs.getString("name"));
                job.setDescription(rs.getString("description"));
                job.setExternalData(rs.getString("data"));
                job.setCreateTime(getDate(rs.getString("create_date")));
                job.setLastUpdateTime(getDate(rs.getString("last_update_date")));
                job.setStatus(Job.StatusEnum.valueOf(rs.getString("status").toUpperCase()));
                job.setPercentageComplete(rs.getFloat("percentage_complete"));

                //  Parse JSON failure sub-strings.
                String failureDetails = rs.getString("failure_details");
                if (ApiServiceUtil.isNotNullOrEmpty(failureDetails)) {
                    job.setFailures(getFailuresAsList(failureDetails));
                }
            }
            rs.close();
        } catch (SQLException se) {
            //  Determine source of SQL exception and throw appropriate error.
            String sqlState = se.getSQLState();

            switch (sqlState) {
                case "02000":
                    //  Job id has not been provided.
                    throw new BadRequestException(se.getMessage());
                case "P0002":
                    //  No data found for the specified job id.
                    throw new NotFoundException(se.getMessage());
                default:
                    throw se;
            }
        }

        return job;
    }

    /**
     * Call one of the `create_job` database functions and parse the result.
     *
     * @param statement Statement which calls the `create_job` function
     * @return Whether the job was created
     * @throws Exception
     */
    private boolean callCreateJobFunction(final CallableStatement statement) throws Exception {
        try {
            LOG.debug("Calling create_job() database function...");
            final ResultSet rs = statement.executeQuery();
            rs.next();
            return rs.getBoolean("job_created");
        } catch (SQLException se) {
            //  Determine source of SQL exception and throw appropriate error.
            String sqlState = se.getSQLState();

            if (sqlState.equals("02000")) {
                //  Job id has not been provided.
                throw new BadRequestException(se.getMessage());
            } else if (sqlState.equals("23505")) {
                throw new ForbiddenException("Job already exists");
            } else {
                throw se;
            }
        }
    }

    /**
     * Creates the specified job.
     * @return Whether the job was created
     */
    public boolean createJob(final String partitionId, String jobId, String name, String description, String data, int jobHash) throws Exception {
        try (
                Connection conn = DatabaseConnectionProvider.getConnection(appConfig);
                CallableStatement stmt = conn.prepareCall("{call create_job(?,?,?,?,?,?)}")
        ) {
            stmt.setString(1, partitionId);
            stmt.setString(2,jobId);
            stmt.setString(3,name);
            stmt.setString(4,description);
            stmt.setString(5,data);
            stmt.setInt(6,jobHash);

            return callCreateJobFunction(stmt);
        }
    }

    /**
     * Creates the specified job.
     * @return Whether the job was created
     */
    public boolean createJobWithDependencies(final String partitionId, final String jobId, final String name, final String description,
                                          final String data, final int jobHash, final String taskClassifier,
                                          final int taskApiVersion, final byte[] taskData, final String taskPipe,
                                          final String targetPipe, final List<String> prerequisiteJobIds,
                                          final int delay) throws Exception {
        try (
                Connection conn = DatabaseConnectionProvider.getConnection(appConfig);
                CallableStatement stmt = conn.prepareCall("{call create_job(?,?,?,?,?,?,?,?,?,?,?,?,?)}")
        ) {
            final String[] prerequisiteJobIdStringArray = prerequisiteJobIds.toArray(new String[prerequisiteJobIds.size()]);
            Array prerequisiteJobIdSQLArray = conn.createArrayOf("varchar", prerequisiteJobIdStringArray);

            stmt.setString(1, partitionId);
            stmt.setString(2,jobId);
            stmt.setString(3,name);
            stmt.setString(4,description);
            stmt.setString(5,data);
            stmt.setInt(6,jobHash);
            stmt.setString(7,taskClassifier);
            stmt.setInt(8,taskApiVersion);
            stmt.setBytes(9,taskData);
            stmt.setString(10,taskPipe);
            stmt.setString(11,targetPipe);
            stmt.setArray(12,prerequisiteJobIdSQLArray);
            stmt.setInt(13,delay);

            return callCreateJobFunction(stmt);
        }
    }

    /**
     * Deletes the specified job.
     */
    public void deleteJob(final String partitionId, String jobId) throws Exception {

        try (
                Connection conn = DatabaseConnectionProvider.getConnection(appConfig);
                CallableStatement stmt = conn.prepareCall("{call delete_job(?,?,?)}")
        ) {
            stmt.setString(1, partitionId);
            stmt.setString(2,jobId);
            stmt.setString(3, new JobTaskId(partitionId, jobId).getShortId());
            LOG.debug("Calling delete_job() database function...");
            stmt.execute();
        } catch (SQLException se) {
            //  Determine source of SQL exception and throw appropriate error.
            String sqlState = se.getSQLState();

            switch (sqlState) {
                case "02000":
                    //  Job id has not been provided.
                    throw new BadRequestException(se.getMessage());
                case "P0002":
                    //  No data found for the specified job id.
                    throw new NotFoundException(se.getMessage());
                default:
                    throw se;
            }
        }
    }

    /**
     * Returns TRUE if the specified job id can be progressed, otherwise FALSE.
     */
    public boolean canJobBeProgressed(final String partitionId, final String jobId) throws Exception
    {

        boolean canBeProgressed = true;

        try (
                Connection conn = DatabaseConnectionProvider.getConnection(appConfig);
                CallableStatement stmt = conn.prepareCall("{call get_job_can_be_progressed(?,?)}")
        ) {
            stmt.setString(1, partitionId);
            stmt.setString(2, jobId);

            //  Execute a query to determine if the specified job can be progressed.
            ResultSet rs = stmt.executeQuery();
            if(rs.next()){
                canBeProgressed = rs.getBoolean("can_be_progressed");
            }

        } catch (SQLException e) {
            final String sqlState = e.getSQLState();
            if (sqlState.equals("P0002")) {
                // job missing - return false
            } else {
                throw e;
            }
        }

        return canBeProgressed;
    }

    /**
     * Returns TRUE if the specified job id is active, otherwise FALSE.
     */
    public boolean isJobActive(final String partitionId, String jobId) throws Exception {

        boolean active = false;

        try (
                Connection conn = DatabaseConnectionProvider.getConnection(appConfig);
                CallableStatement stmt = conn.prepareCall("{call get_job(?,?)}")
        ) {
            stmt.setString(1, partitionId);
            stmt.setString(2, jobId);

            //  Execute a query to determine if the specified job is active or not.
            LOG.debug("Calling get_job() database function...");
            ResultSet rs = stmt.executeQuery();
            if(rs.next()){
                final Job.StatusEnum status =
                    Job.StatusEnum.valueOf(rs.getString("status").toUpperCase());
                active = status == Job.StatusEnum.ACTIVE || status == Job.StatusEnum.WAITING;
            }

        } catch (SQLException e) {
            final String sqlState = e.getSQLState();
            if (sqlState.equals("P0002")) {
                // job missing - return false
            } else {
                throw e;
            }
        }

        return active;
    }

    /**
     * Cancels the specified job.
     */
    public void cancelJob(final String partitionId, String jobId) throws Exception {

        try (
                Connection conn = DatabaseConnectionProvider.getConnection(appConfig);
                CallableStatement stmt = conn.prepareCall("{call cancel_job(?,?,?)}")
        ) {
            stmt.setString(1, partitionId);
            stmt.setString(2,jobId);
            stmt.setString(3, new JobTaskId(partitionId, jobId).getShortId());
            LOG.debug("Calling cancel_job() database function...");
            stmt.execute();
        } catch (SQLException se) {
            //  Determine source of SQL exception and throw appropriate error.
            String sqlState = se.getSQLState();

            switch (sqlState) {
                case "02000":
                    //  Job id has not been provided.
                    throw new BadRequestException(se.getMessage());
                case "P0002":
                    //  No data found for the specified job id.
                    throw new NotFoundException(se.getMessage());
                default:
                    throw se;
            }
        }
    }

    /**
     * Creates the specified job.
     */
    public void reportFailure(final String partitionId, String jobId, String failureDetails)
        throws Exception
    {

        try (
                Connection conn = DatabaseConnectionProvider.getConnection(appConfig);
                CallableStatement stmt = conn.prepareCall("{call report_failure(?,?,?,?)}")
        ) {
            stmt.setString(1, partitionId);
            stmt.setString(2,jobId);
            stmt.setString(3, new JobTaskId(partitionId, jobId).getShortId());
            stmt.setString(4,failureDetails);

            LOG.debug("Calling report_failure() database function...");
            stmt.execute();
        }

    }

    /**
     * Parses the failure details string returned from the database and returns as a list.
     */
    private static List<Failure> getFailuresAsList (String failureDetails) throws Exception {

        List<Failure> failures = new ArrayList<>();

        //  Split on newline character.
        for (String failure: failureDetails.split("\\r?\\n")){
            if (failure.startsWith("{")) {
                JSONObject jFailure = new JSONObject(failure);
                Failure f = new Failure();
                f.setFailureId(jFailure.getString("failureId"));
                f.setFailureTime(getDate(jFailure.getString("failureTime")));
                f.failureSource(jFailure.getString("failureSource"));
                f.failureMessage(jFailure.getString("failureMessage"));
                failures.add(f);
            } else {
                //  Valid failure JSON not detected.
                Failure f = new Failure();
                f.setFailureId(FAILURE_PROPERTY_MISSING);
                f.setFailureTime(new Date());
                f.failureSource(FAILURE_PROPERTY_MISSING);
                f.failureMessage(failure);
                failures.add(f);
            }
        }

        return failures;

    }

    /**
     * Returns java.util.date from a string.
     */
    private static Date getDate(String dateString) throws ParseException {

        Instant instant = Instant.parse ( dateString );
        return java.util.Date.from( instant );

    }

}

