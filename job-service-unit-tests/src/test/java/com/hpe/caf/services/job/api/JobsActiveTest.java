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

import com.hpe.caf.services.job.exceptions.BadRequestException;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.util.HashMap;
import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public final class JobsActiveTest {

    @BeforeEach
    public void setup() throws Exception {

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

    @Test
    public void testIsJobActive_Success() throws Exception {
        try (MockedConstruction<DatabaseHelper> mockDatabaseHelper = Mockito.mockConstruction(DatabaseHelper.class, (mock, context) -> {
                    Mockito.when(mock.isJobActive(Mockito.anyString(), Mockito.anyString()))
                            .thenReturn(true);
                })) {
            //  Test successful run of job isActive.
            JobsActive.JobsActiveResult result =
                    JobsActive.isJobActive("partition", "067e6162-3b6f-4ae2-a171-2470b63dff00");

            assertTrue(result.active);
            Mockito.verify(mockDatabaseHelper.constructed().get(0), Mockito.times(1))
                    .isJobActive("partition", "067e6162-3b6f-4ae2-a171-2470b63dff00");
        }
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testIsJobActive_Failure_EmptyJobId() throws Exception {
        //  Test failed run of job isActive with empty job id.
        Assertions.assertThrows(BadRequestException.class, () -> JobsActive.isJobActive("partition", ""));
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testIsJobActive_Failure_EmptyPartitionId() throws Exception {
        Assertions.assertThrows(BadRequestException.class, () -> JobsActive.isJobActive("", "067e6162-3b6f-4ae2-a171-2470b63dff00"));
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testIsJobActive_Failure_InvalidJobId_Period() throws Exception {
        //  Test failed run of job isActive with job id containing invalid characters.
        Assertions.assertThrows(BadRequestException.class, () -> JobsActive.isJobActive("partition", "067e6162-3b6f-4ae2-a171-2470b.3dff00"));
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testIsJobActive_Failure_InvalidJobId_Asterisk() throws Exception {
        //  Test failed run of job isActive with job id containing invalid characters.
        Assertions.assertThrows(BadRequestException.class, () -> JobsActive.isJobActive("partition", "067e6162-3b6f-4ae2-a171-2470b*3dff00"));
    }
}
