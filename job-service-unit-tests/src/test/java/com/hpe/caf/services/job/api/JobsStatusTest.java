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

import com.hpe.caf.services.job.api.generated.model.JobStatus;
import com.hpe.caf.services.job.exceptions.BadRequestException;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import java.util.HashMap;
import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.Assert;

@ExtendWith(MockitoExtension.class)
public final class JobsStatusTest {

    @BeforeEach
    public void setup() throws Exception {

        HashMap<String, String> newEnv = new HashMap<>();
        newEnv.put("JOB_SERVICE_DATABASE_HOST", "testHost");
        newEnv.put("JOB_SERVICE_DATABASE_PORT", "8888");
        newEnv.put("JOB_SERVICE_DATABASE_NAME", "testName");
        newEnv.put("JOB_SERVICE_DATABASE_USERNAME", "testUserName");
        newEnv.put("JOB_SERVICE_DATABASE_PASSWORD", "testPassword");
        newEnv.put("JOB_SERVICE_DATABASE_APPNAME", "testAppName");

        newEnv.put("CAF_RABBITMQ_PROTOCOL", "amqp");
        newEnv.put("CAF_RABBITMQ_HOST", "localhost");
        newEnv.put("CAF_RABBITMQ_PORT", "5672");
        newEnv.put("CAF_RABBITMQ_USERNAME", "guest");
        newEnv.put("CAF_RABBITMQ_PASSWORD", "guest");
        newEnv.put("CAF_TRACKING_PIPE", "demo-jobtracking-in");
        newEnv.put("CAF_STATUS_CHECK_INTERVAL_SECONDS", "1");
        newEnv.put("CAF_WEBSERVICE_URL", "http://localhost:9090/v1");
        newEnv.put("CAF_JOB_SERVICE_RESUME_JOB_QUEUE", "testResumeJobQueue");

        TestUtil.setSystemEnvironmentFields(newEnv);
    }

    @Test
    public void testGetJobStatus_Success() throws Exception {

        try (MockedConstruction<DatabaseHelper> mockDatabaseHelper = Mockito.mockConstruction(DatabaseHelper.class, (mock, context) -> {
            when(mock.getJobStatus(any(), any())).thenReturn(JobStatus.ACTIVE);
        })) {
            //  Test successful run of job getStatus.
            final JobsStatus.JobsStatusResult jobStatusResult
                    = JobsStatus.getJobStatus("partition", "067e6162-3b6f-4ae2-a171-2470b63dff00");

            assertEquals(JobStatus.ACTIVE, jobStatusResult.jobStatus, "Unexpected job status");
            assertEquals(1, jobStatusResult.statusCheckIntervalSecs, "Unexpected status check interval secs");
            Mockito.verify(mockDatabaseHelper.constructed().get(0), Mockito.times(1))
                    .getJobStatus("partition", "067e6162-3b6f-4ae2-a171-2470b63dff00");
        }
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testGetJobStatus_Failure_EmptyJobId() throws Exception {
        //  Test failed run of job getStatus with empty job id.
        Assertions.assertThrows(BadRequestException.class, () -> JobsStatus.getJobStatus("partition", ""));
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testGetJobStatus_Failure_EmptyPartitionId() throws Exception {
        Assertions.assertThrows(BadRequestException.class, () -> JobsStatus.getJobStatus("", "067e6162-3b6f-4ae2-a171-2470b63dff00"));
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testGetJobStatus_Failure_InvalidJobId_Period() throws Exception {
        //  Test failed run of job getStatus with job id containing invalid characters.
        Assertions.assertThrows(BadRequestException.class, () -> JobsStatus.getJobStatus("partition", "067e6162-3b6f-4ae2-a171-2470b.3dff00"));
    }

    @Test
    public void testGetJobStatus_Failure_InvalidJobId_Asterisk() throws Exception {
        //  Test failed run of job getStatus with job id containing invalid characters.
        Assertions.assertThrows(BadRequestException.class, () -> JobsStatus.getJobStatus("partition", "067e6162-3b6f-4ae2-a171-2470b*3dff00"));
    }
}
