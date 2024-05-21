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

import com.hpe.caf.services.configuration.AppConfig;
import com.hpe.caf.services.job.api.generated.model.JobStatus;
import com.hpe.caf.services.job.exceptions.BadRequestException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public final class JobsStatusTest {

    @Mock
    private DatabaseHelper mockDatabaseHelper;

    private MockedStatic<DatabaseHelperFactory> databaseHelperFactoryMockedStatic;

    @Before
    public void setup() throws Exception {

        //  Mock DatabaseHelper calls.
        final JobStatus jobStatus = JobStatus.ACTIVE;
        mockDatabaseHelper = Mockito.mock(DatabaseHelper.class);
        databaseHelperFactoryMockedStatic =
                Mockito.mockStatic(DatabaseHelperFactory.class);
        when(DatabaseHelperFactory.createDatabaseHelper(any())).thenReturn(mockDatabaseHelper);
        Mockito.when(mockDatabaseHelper.getJobStatus(Mockito.anyString(), Mockito.anyString()))
            .thenReturn(jobStatus);

        HashMap<String, String> newEnv  = new HashMap<>();
        newEnv.put("JOB_SERVICE_DATABASE_HOST","testHost");
        newEnv.put("JOB_SERVICE_DATABASE_PORT", "8888");
        newEnv.put("JOB_SERVICE_DATABASE_NAME","testName");
        newEnv.put("JOB_SERVICE_DATABASE_USERNAME","testUserName");
        newEnv.put("JOB_SERVICE_DATABASE_PASSWORD","testPassword");
        newEnv.put("JOB_SERVICE_DATABASE_APPNAME","testAppName");

        newEnv.put("CAF_RABBITMQ_PROTOCOL", "amqp");
        newEnv.put("CAF_RABBITMQ_HOST","localhost");
        newEnv.put("CAF_RABBITMQ_PORT","5672");
        newEnv.put("CAF_RABBITMQ_USERNAME","guest");
        newEnv.put("CAF_RABBITMQ_PASSWORD","guest");
        newEnv.put("CAF_TRACKING_PIPE","demo-jobtracking-in");
        newEnv.put("CAF_STATUS_CHECK_INTERVAL_SECONDS","1");
        newEnv.put("CAF_WEBSERVICE_URL","http://localhost:9090/v1");
        newEnv.put("CAF_JOB_SERVICE_RESUME_JOB_QUEUE", "testResumeJobQueue");

        TestUtil.setSystemEnvironmentFields(newEnv);
    }

    @After
    public void cleanUp() throws Exception {
        databaseHelperFactoryMockedStatic.close();
    }

    @Test
    public void testGetJobStatus_Success() throws Exception {
        //  Test successful run of job getStatus.
        final JobsStatus.JobsStatusResult jobStatusResult =
            JobsStatus.getJobStatus("partition", "067e6162-3b6f-4ae2-a171-2470b63dff00");

        Assert.assertEquals("Unexpected job status", JobStatus.ACTIVE, jobStatusResult.jobStatus);
        Assert.assertEquals("Unexpected status check interval secs", 1, jobStatusResult.statusCheckIntervalSecs);
        Mockito.verify(mockDatabaseHelper, Mockito.times(1))
            .getJobStatus("partition", "067e6162-3b6f-4ae2-a171-2470b63dff00");
    }

    @Test(expected = BadRequestException.class)
    public void testGetJobStatus_Failure_EmptyJobId() throws Exception {
        //  Test failed run of job getStatus with empty job id.
        JobsStatus.getJobStatus("partition", "");
    }

    @Test(expected = BadRequestException.class)
    public void testGetJobStatus_Failure_EmptyPartitionId() throws Exception {
        JobsStatus.getJobStatus("", "067e6162-3b6f-4ae2-a171-2470b63dff00");
    }

    @Test(expected = BadRequestException.class)
    public void testGetJobStatus_Failure_InvalidJobId_Period() throws Exception {
        //  Test failed run of job getStatus with job id containing invalid characters.
        JobsStatus.getJobStatus("partition", "067e6162-3b6f-4ae2-a171-2470b.3dff00");
    }

    @Test(expected = BadRequestException.class)
    public void testGetJobStatus_Failure_InvalidJobId_Asterisk() throws Exception {
        //  Test failed run of job getStatus with job id containing invalid characters.
        JobsStatus.getJobStatus("partition", "067e6162-3b6f-4ae2-a171-2470b*3dff00");
    }
}
