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
import com.hpe.caf.services.job.api.generated.model.Job;
import com.hpe.caf.services.job.exceptions.BadRequestException;
import com.hpe.caf.services.job.queue.QueueServices;
import com.hpe.caf.services.job.queue.QueueServicesFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(PowerMockRunner.class)
@PrepareForTest({JobsResume.class, DatabaseHelper.class, AppConfig.class, QueueServices.class, QueueServicesFactory.class,})
@PowerMockIgnore("javax.management.*")
public final class JobsResumeTest {

    @Mock
    private DatabaseHelper mockDatabaseHelper;

    @Mock
    private QueueServicesFactory mockQueueServicesFactory;

    @Mock
    private QueueServices mockQueueServices;

    @Before
    public void setup() throws Exception {
        //  Mock DatabaseHelper calls.
        Mockito.doNothing().when(mockDatabaseHelper)
            .resumeJob(Mockito.anyString(), Mockito.anyString());
        PowerMockito.whenNew(DatabaseHelper.class).withArguments(Mockito.any()).thenReturn(mockDatabaseHelper);

        //  Mock QueueServices calls.
        doNothing().when(mockQueueServices).sendMessage(any(), any(), any(), any(), anyBoolean());
        PowerMockito.whenNew(QueueServices.class).withArguments(any(), any(), anyString(), any()).thenReturn(mockQueueServices);

        //  Mock QueueServicesFactory calls.
        PowerMockito.mockStatic(QueueServicesFactory.class);
        PowerMockito.doReturn(mockQueueServices).when(QueueServicesFactory.class, "create", any(), anyString(), any());

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
        Mockito.when(mockDatabaseHelper.getJobStatus(
            anyString(), anyString()
        )).thenReturn(Job.StatusEnum.PAUSED);
        
        //  Test successful run of job resume.
        JobsResume.resumeJob("partition", "067e6162-3b6f-4ae2-a171-2470b63dff00");

        Mockito.verify(mockDatabaseHelper, Mockito.times(1))
            .resumeJob("partition", "067e6162-3b6f-4ae2-a171-2470b63dff00");

        verify(mockQueueServices, times(1)).sendMessage(any(), any(), any(), any(), anyBoolean());
    }

    @Test(expected = BadRequestException.class)
    public void testResumeJob_Failure_EmptyJobId() throws Exception {
        //  Test failed run of job resume with empty job id.
        JobsResume.resumeJob("partition", "");
    }

    @Test(expected = BadRequestException.class)
    public void testResumeJob_Success_EmptyPartitionId() throws Exception {
        JobsResume.resumeJob("", "067e6162-3b6f-4ae2-a171-2470b63dff00");
    }

    @Test(expected = BadRequestException.class)
    public void testResumeJob_Failure_InvalidJobId_Period() throws Exception {
        //  Test failed run of job resume with job id containing invalid characters.
        JobsResume.resumeJob("partition", "067e6162-3b6f-4ae2-a171-2470b.3dff00");
    }

    @Test(expected = BadRequestException.class)
    public void testResumeJob_Failure_InvalidJobId_Asterisk() throws Exception {
        //  Test failed run of job resume with job id containing invalid characters.
        JobsResume.resumeJob("partition", "067e6162-3b6f-4ae2-a171-2470b*3dff00");
    }
}
