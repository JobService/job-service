/*
 * Copyright 2016-2020 Micro Focus or one of its affiliates.
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
import junit.framework.Assert;
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
@PrepareForTest({JobsActive.class, DatabaseHelper.class, AppConfig.class})
@PowerMockIgnore("javax.management.*")
public final class JobsActiveTest {

    @Mock
    private DatabaseHelper mockDatabaseHelper;

    @Before
    public void setup() throws Exception {

        //  Mock DatabaseHelper calls.
        Mockito.when(mockDatabaseHelper.isJobActive(Mockito.anyString(), Mockito.anyString()))
            .thenReturn(true);
        PowerMockito.whenNew(DatabaseHelper.class).withArguments(Mockito.any()).thenReturn(mockDatabaseHelper);

        HashMap<String, String> newEnv  = new HashMap<>();
        newEnv.put("JOB_SERVICE_DATABASE_URL","testUrl");
        newEnv.put("JOB_SERVICE_DATABASE_USERNAME","testUserName");
        newEnv.put("JOB_SERVICE_DATABASE_PASSWORD","testPassword");
        newEnv.put("JOB_SERVICE_DATABASE_APPNAME","testAppName");

        newEnv.put("CAF_RABBITMQ_HOST","localhost");
        newEnv.put("CAF_RABBITMQ_PORT","5672");
        newEnv.put("CAF_RABBITMQ_USERNAME","guest");
        newEnv.put("CAF_RABBITMQ_PASSWORD","guest");
        newEnv.put("CAF_TRACKING_PIPE","demo-jobtracking-in");
        newEnv.put("CAF_STATUS_CHECK_TIME","1");
        newEnv.put("CAF_WEBSERVICE_URL","http://localhost:9090/v1");

        TestUtil.setSystemEnvironmentFields(newEnv);
    }

    @Test
    public void testIsJobActive_Success() throws Exception {
        //  Test successful run of job isActive.
        JobsActive.JobsActiveResult result =
            JobsActive.isJobActive("partition", "067e6162-3b6f-4ae2-a171-2470b63dff00");

        Assert.assertTrue(result.active);
        Mockito.verify(mockDatabaseHelper, Mockito.times(1))
            .isJobActive("partition", "067e6162-3b6f-4ae2-a171-2470b63dff00");
    }

    @Test(expected = BadRequestException.class)
    public void testIsJobActive_Failure_EmptyJobId() throws Exception {
        //  Test failed run of job isActive with empty job id.
        JobsActive.isJobActive("partition", "");
    }

    @Test(expected = BadRequestException.class)
    public void testIsJobActive_Failure_EmptyPartitionId() throws Exception {
        JobsActive.isJobActive("", "067e6162-3b6f-4ae2-a171-2470b63dff00");
    }

    @Test(expected = BadRequestException.class)
    public void testIsJobActive_Failure_InvalidJobId_Period() throws Exception {
        //  Test failed run of job isActive with job id containing invalid characters.
        JobsActive.isJobActive("partition", "067e6162-3b6f-4ae2-a171-2470b.3dff00");
    }

    @Test(expected = BadRequestException.class)
    public void testIsJobActive_Failure_InvalidJobId_Asterisk() throws Exception {
        //  Test failed run of job isActive with job id containing invalid characters.
        JobsActive.isJobActive("partition", "067e6162-3b6f-4ae2-a171-2470b*3dff00");
    }

}
