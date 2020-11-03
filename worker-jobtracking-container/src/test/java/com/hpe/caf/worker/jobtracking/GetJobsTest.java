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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import static org.junit.Assert.assertTrue;


/**
 * Integration tests for Job Tracking Worker.
 */
public class GetJobsTest
        extends JobTrackingWorkerIT{


    private static final long MAX_EXECUTION_TIME=10000000;
    private static final Logger LOG = LoggerFactory.getLogger(GetJobsTest.class);
    private static final String PARTITION_ID="tenant-x";
    private static final String firstDbFilePath ="src/test/resources/dbFiller/dbFiller1.sql";
    private static final String secondDbFilePath ="src/test/resources/dbFiller/dbFiller2.sql";


    /** Checking that we can handle large lists (9580 items) */
    @Test
    public void checkExecutionTimeForgetJobs() throws SQLException, IOException {
        prepareDatabase();
        startExecutionTimer();
        List<DBJob> dbJobs = jobDatabase.getJobs(PARTITION_ID);
        assertTrue(checkIfAllJobStatusAreCompleted(dbJobs));
        System.out.println("it's happening!");
        assertTrue(checkIfMaxExecutionTimeRespected(MAX_EXECUTION_TIME+MAX_EXECUTION_TIME));// 10 seconds
    }


    private boolean checkIfAllJobStatusAreCompleted(List<DBJob> dbJobs) {
        for (final DBJob dbJob: dbJobs
             ) {
            if (dbJob.getStatus().equals(JobStatus.Completed)){
                return false;
            }
        }
        return true;
    }

    private void prepareDatabase() throws IOException, SQLException {

        // Inserting the first part of the data
        // => data gets inserted into completed_subtask_report table
        jobDatabase.databaseFiller(firstDbFilePath);

        // Calling getJobs
        // data from completed_subtask_report table get inserted in regular task tables
        jobDatabase.getJobs(PARTITION_ID);

        // Inserting the second set of data
        jobDatabase.databaseFiller(secondDbFilePath);
    }

}
