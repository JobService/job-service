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
@PrepareForTest({JobsCancel.class})
@PowerMockIgnore("javax.management.*")
public class JobsCancelTest {

    @Mock
    private DatabaseHelper mockDatabaseHelper;

    @Before
    public void setup() throws Exception {
        //  Mock DatabaseHelper calls.
        Mockito.doNothing().when(mockDatabaseHelper).cancelJob(Mockito.anyString());
        PowerMockito.whenNew(DatabaseHelper.class).withArguments(Mockito.any()).thenReturn(mockDatabaseHelper);

        HashMap<String, String> newEnv  = new HashMap<>();
        newEnv.put("database.url","testUrl");
        newEnv.put("database.username","testUserName");
        newEnv.put("database.password","testPassword");
        TestUtil.setSystemEnvironmentFields(newEnv);
    }

    @Test
    public void testCancelJob_Success() throws Exception {
        //  Test successful run of job cancellation.
        JobsCancel.cancelJob("067e6162-3b6f-4ae2-a171-2470b63dff00");

        Mockito.verify(mockDatabaseHelper, Mockito.times(1)).cancelJob(Mockito.anyString());
    }

    @Test(expected = BadRequestException.class)
    public void testCancelJob_Failure_EmptyJobId() throws Exception {
        //  Test failed run of job cancellation with empty job id.
        JobsCancel.cancelJob("");

        Mockito.verify(mockDatabaseHelper, Mockito.times(0)).cancelJob(Mockito.anyString());
    }

    @Test(expected = BadRequestException.class)
    public void testCancelJob_Failure_InvalidJobId_Period() throws Exception {
        //  Test failed run of job cancellation with job id containing invalid characters.
        JobsCancel.cancelJob("067e6162-3b6f-4ae2-a171-2470b.3dff00");

        Mockito.verify(mockDatabaseHelper, Mockito.times(0)).cancelJob(Mockito.anyString());
    }

    @Test(expected = BadRequestException.class)
    public void testCancelJob_Failure_InvalidJobId_Asterisk() throws Exception {
        //  Test failed run of job cancellation with job id containing invalid characters.
        JobsCancel.cancelJob("067e6162-3b6f-4ae2-a171-2470b*3dff00");

        Mockito.verify(mockDatabaseHelper, Mockito.times(0)).cancelJob(Mockito.anyString());
    }

}
