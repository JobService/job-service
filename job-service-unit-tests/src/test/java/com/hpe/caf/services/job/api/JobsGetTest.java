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
package com.hpe.caf.services.job.api;

import com.hpe.caf.services.job.api.generated.model.JobSortField;
import com.hpe.caf.services.job.api.generated.model.SortDirection;
import com.hpe.caf.services.job.exceptions.BadRequestException;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.util.HashMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public final class JobsGetTest {

    @BeforeEach
    public void setup() throws Exception {

        HashMap<String, String> newEnv  = new HashMap<>();
        newEnv.put("JOB_SERVICE_DATABASE_HOST","testHost");
        newEnv.put("JOB_SERVICE_DATABASE_PORT", "8888");
        newEnv.put("JOB_SERVICE_DATABASE_NAME","testName");
        newEnv.put("JOB_SERVICE_DATABASE_USERNAME","testUserName");
        newEnv.put("JOB_SERVICE_DATABASE_PASSWORD","testPassword");
        newEnv.put("JOB_SERVICE_DATABASE_APPNAME","testAppName");
        newEnv.put("CAF_JOB_SERVICE_RESUME_JOB_QUEUE", "testResumeJobQueue");
        TestUtil.setSystemEnvironmentFields(newEnv);
    }

    @Test
    public void testGetJob_Success() throws Exception {
        try (MockedConstruction<DatabaseHelper> mockDatabaseHelper = Mockito.mockConstruction(DatabaseHelper.class)){
            //  Test successful run of job retrieval.
            JobsGet.getJobs("partition", "", null, 0, 0, null, null, null);

            Mockito.verify(mockDatabaseHelper.constructed().get(0), Mockito.times(1)).getJobs(
                    "partition", "", null, 0, 0, JobSortField.CREATE_DATE, SortDirection.DESCENDING, null, null);
        }
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testGetJobs_Failure_EmptyPartitionId() throws Exception {
         Assertions.assertThrows(BadRequestException.class, () -> JobsGet.getJobs("", "", null, 0, 0, null, null, null));
    }

    @Test
    public void testGetJobs_Success_WithSort() throws Exception {
        try (MockedConstruction<DatabaseHelper> mockDatabaseHelper = Mockito.mockConstruction(DatabaseHelper.class)) {
            JobsGet.getJobs("partition", "", null, 0, 0, "jobId:asc", null, null);
            Mockito.verify(mockDatabaseHelper.constructed().get(0), Mockito.times(1)).getJobs(
                    "partition", "", null, 0, 0, JobSortField.JOB_ID, SortDirection.ASCENDING, null, null);
        }
    }

    @Test
    public void testGetJobs_Success_WithNameSort() throws Exception {
        try (MockedConstruction<DatabaseHelper> mockDatabaseHelper = Mockito.mockConstruction(DatabaseHelper.class)) {
            JobsGet.getJobs("partition", "", null, 0, 0, "name:asc", null, null);
            Mockito.verify(mockDatabaseHelper.constructed().get(0), Mockito.times(1)).getJobs(
                    "partition", "", null, 0, 0, JobSortField.NAME, SortDirection.ASCENDING, null, null);
        }
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testGetJobs_Failure_InvalidSort() throws Exception {
        Assertions.assertThrows(BadRequestException.class, () -> JobsGet.getJobs("partition", "", null, 0, 0, "invalid", null, null));
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testGetJobs_Failure_InvalidSortField() throws Exception {
        Assertions.assertThrows(BadRequestException.class, () -> JobsGet.getJobs("partition", "", null, 0, 0, "unknown:desc", null, null));
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testGetJobs_Failure_InvalidSortDirection() throws Exception {
        Assertions.assertThrows(BadRequestException.class, () -> JobsGet.getJobs("partition", "", null, 0, 0, "jobId:random", null, null));
    }
}
