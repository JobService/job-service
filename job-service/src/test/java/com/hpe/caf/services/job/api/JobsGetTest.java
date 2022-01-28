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
package com.hpe.caf.services.job.api;

import com.hpe.caf.services.configuration.AppConfig;
import com.hpe.caf.services.job.api.generated.model.JobSortField;
import com.hpe.caf.services.job.api.generated.model.SortDirection;
import com.hpe.caf.services.job.exceptions.BadRequestException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

@RunWith(PowerMockRunner.class)
@PrepareForTest({JobsGet.class, DatabaseHelper.class, AppConfig.class})
@PowerMockIgnore("javax.management.*")
public final class JobsGetTest {

    @Mock
    private DatabaseHelper mockDatabaseHelper;

    @Before
    public void setup() throws Exception {
        //  Mock DatabaseHelper calls.
        Mockito.doNothing().when(mockDatabaseHelper)
            .deleteJob(Mockito.anyString(), Mockito.anyString());
        PowerMockito.whenNew(DatabaseHelper.class).withArguments(Mockito.any()).thenReturn(mockDatabaseHelper);

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
        //  Test successful run of job retrieval.
        JobsGet.getJobs("partition", "", null, 0, 0, null, null, null);

        Mockito.verify(mockDatabaseHelper, Mockito.times(1)).getJobs(
            "partition", "", null, 0, 0, JobSortField.CREATE_DATE, SortDirection.DESCENDING, null, null);
    }

    @Test(expected = BadRequestException.class)
    public void testGetJobs_Failure_EmptyPartitionId() throws Exception {
        JobsGet.getJobs("", "", null, 0, 0, null, null, null);
    }

    @Test
    public void testGetJobs_Success_WithSort() throws Exception {
        JobsGet.getJobs("partition", "", null, 0, 0, "jobId:asc", null, null);
        Mockito.verify(mockDatabaseHelper, Mockito.times(1)).getJobs(
            "partition", "", null, 0, 0, JobSortField.JOB_ID, SortDirection.ASCENDING, null, null);
    }

    @Test
    public void testGetJobs_Success_WithNameSort() throws Exception {
        JobsGet.getJobs("partition", "", null, 0, 0, "name:asc", null, null);
        Mockito.verify(mockDatabaseHelper, Mockito.times(1)).getJobs(
            "partition", "", null, 0, 0, JobSortField.NAME, SortDirection.ASCENDING, null, null);
    }

    @Test(expected = BadRequestException.class)
    public void testGetJobs_Failure_InvalidSort() throws Exception {
        JobsGet.getJobs("partition", "", null, 0, 0, "invalid", null, null);
    }

    @Test(expected = BadRequestException.class)
    public void testGetJobs_Failure_InvalidSortField() throws Exception {
        JobsGet.getJobs("partition", "", null, 0, 0, "unknown:desc", null, null);
    }

    @Test(expected = BadRequestException.class)
    public void testGetJobs_Failure_InvalidSortDirection() throws Exception {
        JobsGet.getJobs("partition", "", null, 0, 0, "jobId:random", null, null);
    }
}
