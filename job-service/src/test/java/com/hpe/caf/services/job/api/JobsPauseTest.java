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

import java.util.HashMap;

@RunWith(PowerMockRunner.class)
@PrepareForTest({JobsPause.class, DatabaseHelper.class, AppConfig.class})
@PowerMockIgnore("javax.management.*")
public final class JobsPauseTest {

    @Mock
    private DatabaseHelper mockDatabaseHelper;

    @Before
    public void setup() throws Exception {
        //  Mock DatabaseHelper calls.
        Mockito.doNothing().when(mockDatabaseHelper)
            .pauseJob(Mockito.anyString(), Mockito.anyString());
        PowerMockito.whenNew(DatabaseHelper.class).withArguments(Mockito.any()).thenReturn(mockDatabaseHelper);

        HashMap<String, String> newEnv  = new HashMap<>();
        newEnv.put("JOB_SERVICE_DATABASE_URL","testUrl");
        newEnv.put("JOB_SERVICE_DATABASE_USERNAME","testUserName");
        newEnv.put("JOB_SERVICE_DATABASE_PASSWORD","testPassword");
        newEnv.put("JOB_SERVICE_DATABASE_APPNAME","testAppName");
        TestUtil.setSystemEnvironmentFields(newEnv);
    }

    @Test
    public void testPauseJob_Success() throws Exception {
        //  Test successful run of job pause.
        JobsPause.pauseJob("partition", "067e6162-3b6f-4ae2-a171-2470b63dff00");

        Mockito.verify(mockDatabaseHelper, Mockito.times(1))
            .pauseJob("partition", "067e6162-3b6f-4ae2-a171-2470b63dff00");
    }

    @Test(expected = BadRequestException.class)
    public void testPauseJob_Failure_EmptyJobId() throws Exception {
        //  Test failed run of job pause with empty job id.
        JobsPause.pauseJob("partition", "");
    }

    @Test(expected = BadRequestException.class)
    public void testPauseJob_Success_EmptyPartitionId() throws Exception {
        JobsPause.pauseJob("", "067e6162-3b6f-4ae2-a171-2470b63dff00");
    }

    @Test(expected = BadRequestException.class)
    public void testPauseJob_Failure_InvalidJobId_Period() throws Exception {
        //  Test failed run of job pause with job id containing invalid characters.
        JobsPause.pauseJob("partition", "067e6162-3b6f-4ae2-a171-2470b.3dff00");
    }

    @Test(expected = BadRequestException.class)
    public void testPauseJob_Failure_InvalidJobId_Asterisk() throws Exception {
        //  Test failed run of job pause with job id containing invalid characters.
        JobsPause.pauseJob("partition", "067e6162-3b6f-4ae2-a171-2470b*3dff00");
    }
}
