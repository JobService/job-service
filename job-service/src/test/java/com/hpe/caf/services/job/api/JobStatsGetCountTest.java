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
@PrepareForTest({JobsStatsGetCount.class, DatabaseHelper.class, AppConfig.class})
@PowerMockIgnore("javax.management.*")
public final class JobStatsGetCountTest {

    @Mock
    private DatabaseHelper mockDatabaseHelper;

    @Before
    public void setup() throws Exception {
        //  Mock DatabaseHelper calls.
        Mockito.doNothing().when(mockDatabaseHelper)
            .deleteJob(Mockito.anyString(), Mockito.anyString());
        PowerMockito.whenNew(DatabaseHelper.class).withArguments(Mockito.any()).thenReturn(mockDatabaseHelper);

        HashMap<String, String> newEnv  = new HashMap<>();
        newEnv.put("JOB_SERVICE_DATABASE_URL","testUrl");
        newEnv.put("JOB_SERVICE_DATABASE_USERNAME","testUserName");
        newEnv.put("JOB_SERVICE_DATABASE_PASSWORD","testPassword");
        newEnv.put("JOB_SERVICE_DATABASE_APPNAME","testAppName");
        TestUtil.setSystemEnvironmentFields(newEnv);
    }

    @Test
    public void testGetJobCount_Success() throws Exception {
        //  Test successful run of job count retrieval.
        JobsStatsGetCount.getJobsCount("partition", "", null, null);

        Mockito.verify(mockDatabaseHelper, Mockito.times(1)).getJobsCount("partition", "", null, null);
    }

    @Test(expected = BadRequestException.class)
    public void testGetJobCount_Success_EmptyPartitionId() throws Exception {
        JobsStatsGetCount.getJobsCount("", "", null, null);
    }

    @Test(expected = ServiceUnavailableException.class)
    public void testGetJobCount_Failure_DatabaseConnection() throws Exception {
        Mockito.when(mockDatabaseHelper.getJobsCount(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
            .thenThrow(ServiceUnavailableException.class);

        JobsStatsGetCount.getJobsCount("partition", "", null, null);
    }
}
