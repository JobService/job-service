package com.hpe.caf.services.job.api;

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
@PrepareForTest({JobsGet.class})
@PowerMockIgnore("javax.management.*")
public class JobsGetTest {

    @Mock
    private DatabaseHelper mockDatabaseHelper;

    @Before
    public void setup() throws Exception {
        //  Mock DatabaseHelper calls.
        Mockito.doNothing().when(mockDatabaseHelper).deleteJob(Mockito.anyString());
        PowerMockito.whenNew(DatabaseHelper.class).withArguments(Mockito.any()).thenReturn(mockDatabaseHelper);

        HashMap<String, String> newEnv  = new HashMap<>();
        newEnv.put("database.url","testUrl");
        newEnv.put("database.username","testUserName");
        newEnv.put("database.password","testPassword");
        TestUtil.setSystemEnvironmentFields(newEnv);
    }

    @Test
    public void testGetJob_Success() throws Exception {
        //  Test successful run of job retrieval.
        JobsGet.getJobs();

        Mockito.verify(mockDatabaseHelper, Mockito.times(1)).getJobs();
    }

}
