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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;

@RunWith(MockitoJUnitRunner.class)
public final class JobsDeleteTest {

    @Before
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
    public void testDeleteJob_Success() throws Exception {
        try (MockedConstruction<DatabaseHelper> mockDatabaseHelper = Mockito.mockConstruction(DatabaseHelper.class, (mock, context) -> {
                    Mockito.doNothing().when(mock).deleteJob(Mockito.anyString(), Mockito.anyString());
                })) {
            //  Test successful run of job deletion.
            JobsDelete.deleteJob("partition", "067e6162-3b6f-4ae2-a171-2470b63dff00");

            Mockito.verify(mockDatabaseHelper.constructed().get(0), Mockito.times(1))
                    .deleteJob("partition", "067e6162-3b6f-4ae2-a171-2470b63dff00");
        }
    }

    @Test(expected = BadRequestException.class)
    public void testDeleteJob_Failure_EmptyJobId() throws Exception {
        //  Test failed run of job deletion with empty job id.
        JobsDelete.deleteJob("partition", "");
    }

    @Test(expected = BadRequestException.class)
    public void testDeletxeJob_Success_EmptyPartitionId() throws Exception {
        JobsDelete.deleteJob("", "067e6162-3b6f-4ae2-a171-2470b63dff00");
    }

    @Test(expected = BadRequestException.class)
    public void testDeleteJob_Failure_InvalidJobId_Period() throws Exception {
        //  Test failed run of job deletion with job id containing invalid characters.
        JobsDelete.deleteJob("partition", "067e6162-3b6f-4ae2-a171-2470b.3dff00");
    }

    @Test(expected = BadRequestException.class)
    public void testDeleteJob_Failure_InvalidJobId_Asterisk() throws Exception {
        //  Test failed run of job deletion with job id containing invalid characters.
        JobsDelete.deleteJob("partition", "067e6162-3b6f-4ae2-a171-2470b*3dff00");
    }
}
