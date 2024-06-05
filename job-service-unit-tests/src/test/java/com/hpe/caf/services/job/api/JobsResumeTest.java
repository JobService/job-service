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
import com.hpe.caf.services.job.queue.QueueServices;
import com.hpe.caf.services.job.queue.QueueServicesFactory;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.util.HashMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public final class JobsResumeTest {

    @Mock
    private DatabaseHelper mockDatabaseHelper;

    @Mock
    private QueueServices mockQueueServices;

    @BeforeEach
    public void setup() throws Exception {
        mockQueueServices = Mockito.mock(QueueServices.class);
        doNothing().when(mockQueueServices).sendMessage(any(), any(), any(), any(), anyBoolean());

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
    public void testResumeJob_Success() throws Exception {

        try (MockedConstruction<DatabaseHelper> mockDatabaseHelper = Mockito.mockConstruction(DatabaseHelper.class, (mock, context) -> {
                Mockito.when(mock.getJobStatus(anyString(), anyString())).thenReturn(JobStatus.PAUSED);
                Mockito.doNothing().when(mock).resumeJob(Mockito.anyString(), Mockito.anyString());
            });
            MockedStatic<QueueServicesFactory> queueServicesFactoryMockedStatic = Mockito.mockStatic(QueueServicesFactory.class)){

                when(QueueServicesFactory.create(any(),anyString(),any())).thenReturn(mockQueueServices);

            //  Test successful run of job resume.
                JobsResume.resumeJob("partition", "067e6162-3b6f-4ae2-a171-2470b63dff00");

                Mockito.verify(mockDatabaseHelper.constructed().get(0), Mockito.times(1))
                        .resumeJob("partition", "067e6162-3b6f-4ae2-a171-2470b63dff00");

                verify(mockQueueServices, times(1)).sendMessage(any(), any(), any(), any(), anyBoolean());
            }
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testResumeJob_Failure_EmptyJobId() throws Exception {
        //  Test failed run of job resume with empty job id.
        Assertions.assertThrows(BadRequestException.class, () -> JobsResume.resumeJob("partition", ""));
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testResumeJob_Success_EmptyPartitionId() throws Exception {
        Assertions.assertThrows(BadRequestException.class, () -> JobsResume.resumeJob("", "067e6162-3b6f-4ae2-a171-2470b63dff00"));
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testResumeJob_Failure_InvalidJobId_Period() throws Exception {
        //  Test failed run of job resume with job id containing invalid characters.
        Assertions.assertThrows(BadRequestException.class, () -> JobsResume.resumeJob("partition", "067e6162-3b6f-4ae2-a171-2470b.3dff00"));
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testResumeJob_Failure_InvalidJobId_Asterisk() throws Exception {
        //  Test failed run of job resume with job id containing invalid characters.
       Assertions.assertThrows(BadRequestException.class, () ->  JobsResume.resumeJob("partition", "067e6162-3b6f-4ae2-a171-2470b*3dff00"));
    }
}
