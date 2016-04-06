package com.hpe.caf.services.job.api;

import com.hpe.caf.services.job.api.generated.model.NewJob;
import com.hpe.caf.services.job.api.generated.model.WorkerAction;
import com.hpe.caf.services.job.configuration.AppConfig;
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

@RunWith(PowerMockRunner.class)
@PrepareForTest({JobsPut.class,QueueServices.class, QueueServicesFactory.class, DatabaseHelper.class, AppConfig.class})
@PowerMockIgnore("javax.management.*")
public final class JobsPutTest {

    @Mock
    private DatabaseHelper mockDatabaseHelper;
    @Mock
    private QueueServices mockQueueServices;
    @Mock
    private QueueServicesFactory mockQueueServicesFactory;

    private NewJob validJob;

    @Before
    public void setup() throws Exception {
        //  Mock DatabaseHelper calls.
        Mockito.doNothing().when(mockDatabaseHelper).createJob(Mockito.anyString(),Mockito.anyString(),Mockito.anyString(),Mockito.anyString(),Mockito.anyInt());
        Mockito.doNothing().when(mockDatabaseHelper).deleteJob(Mockito.anyString());
        PowerMockito.whenNew(DatabaseHelper.class).withArguments(Mockito.any()).thenReturn(mockDatabaseHelper);

        HashMap<String, String> newEnv  = new HashMap<>();
        newEnv.put("CAF_DATABASE_URL","testUrl");
        newEnv.put("CAF_DATABASE_USERNAME","testUserName");
        newEnv.put("CAF_DATABASE_PASSWORD","testPassword");
        TestUtil.setSystemEnvironmentFields(newEnv);

        //  Mock QueueServices calls.
        Mockito.doNothing().when(mockQueueServices).sendMessage(Mockito.any(), Mockito.any(), Mockito.any());
        PowerMockito.whenNew(QueueServices.class).withArguments(Mockito.any(),Mockito.any(),Mockito.anyString(),Mockito.any()).thenReturn(mockQueueServices);

        //  Mock QueueServicesFactory calls.
        PowerMockito.mockStatic(QueueServicesFactory.class);
        PowerMockito.doReturn(mockQueueServices).when(QueueServicesFactory.class, "create", Mockito.any(), Mockito.anyString(), Mockito.any());

        //  Create job object for testing.
        validJob = new NewJob();
        WorkerAction action = new WorkerAction();
        validJob.setName("TestName");
        validJob.setDescription("TestDescription");
        validJob.setExternalData("TestExternalData");
        action.setTaskClassifier("TestTaskClassifier");
        action.setTaskApiVersion(1);
        action.setTaskData("TestTaskData");
        action.setTaskDataEncoding(WorkerAction.TaskDataEncodingEnum.UTF8);
        action.setTargetPipe("JobServiceQueue");

        validJob.setTask(action);


    }

    @Test
    public void testCreateJob_Success_NoMatchingJobRow() throws Exception {

        Mockito.when(mockDatabaseHelper.doesJobAlreadyExist(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);

        //  Test successful run of job creation when no matching job row exists.
        JobsPut.createOrUpdateJob("067e6162-3b6f-4ae2-a171-2470b63dff00", validJob);

        Mockito.verify(mockDatabaseHelper, Mockito.times(1)).doesJobAlreadyExist(Mockito.anyString(), Mockito.anyInt());
        Mockito.verify(mockDatabaseHelper, Mockito.times(1)).createJob(Mockito.anyString(),Mockito.anyString(),Mockito.anyString(),Mockito.anyString(),Mockito.anyInt());
        Mockito.verify(mockQueueServices, Mockito.times(1)).sendMessage(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void testCreateJob_Success_MatchingJobRow() throws Exception {

        Mockito.when(mockDatabaseHelper.doesJobAlreadyExist(Mockito.anyString(), Mockito.anyInt())).thenReturn(true);

        //  Test successful run of job creation when a matching job row already exists.
        JobsPut.createOrUpdateJob("067e6162-3b6f-4ae2-a171-2470b63dff00", validJob);

        Mockito.verify(mockDatabaseHelper, Mockito.times(1)).doesJobAlreadyExist(Mockito.anyString(), Mockito.anyInt());
        Mockito.verify(mockDatabaseHelper, Mockito.times(0)).createJob(Mockito.anyString(),Mockito.anyString(),Mockito.anyString(),Mockito.anyString(),Mockito.anyInt());
        Mockito.verify(mockQueueServices, Mockito.times(0)).sendMessage(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test(expected = BadRequestException.class)
    public void testCreateJob_Failure_JobIdNotSpecified() throws Exception {

        //  Test failed run of job creation with empty job id.
        JobsPut.createOrUpdateJob("", validJob);
    }

    @Test(expected = BadRequestException.class)
    public void testCreateJob_Failure_InvalidJobId_Period() throws Exception {

        //  Test failed run of job creation with job id containing invalid characters.
        JobsPut.createOrUpdateJob("067e6162-3b6f-4ae2-a171-2470b6.dff00", validJob);
    }

    @Test(expected = BadRequestException.class)
    public void testCreateJob_Failure_InvalidJobId_Asterisk() throws Exception {

        //  Test failed run of job creation with job id containing invalid characters.
        JobsPut.createOrUpdateJob("067e6162-3b6f-4ae2-a171-2470b6*dff00", validJob);
    }

    @Test(expected = BadRequestException.class)
    public void testCreateJob_Failure_TaskDataNotSpecified() throws Exception {

        NewJob job = new NewJob();
        WorkerAction action = new WorkerAction();
        job.setName("TestName");
        job.setDescription("TestDescription");
        job.setExternalData("TestExternalData");
        action.setTaskClassifier("TestTaskClassifier");
        action.setTaskApiVersion(1);
        action.setTaskData("");
        action.setTaskDataEncoding(WorkerAction.TaskDataEncodingEnum.UTF8);
        action.setTargetPipe("JobServiceQueue");
        job.setTask(action);

        //  Test failed run of job creation where task data has not been specified.
        JobsPut.createOrUpdateJob("067e6162-3b6f-4ae2-a171-2470b63dff00", job);
    }

    @Test(expected = BadRequestException.class)
    public void testCreateJob_Failure_TaskClassifierNotSpecified() throws Exception {

        NewJob job = new NewJob();
        WorkerAction action = new WorkerAction();
        job.setName("TestName");
        job.setDescription("TestDescription");
        job.setExternalData("TestExternalData");
        action.setTaskClassifier("");
        action.setTaskApiVersion(1);
        action.setTaskData("TestTaskData");
        action.setTaskDataEncoding(WorkerAction.TaskDataEncodingEnum.UTF8);
        action.setTargetPipe("JobServiceQueue");
        job.setTask(action);

        //  Test failed run of job creation where task classifier has not been specified.
        JobsPut.createOrUpdateJob("067e6162-3b6f-4ae2-a171-2470b63dff00", job);
    }

    @Test(expected = BadRequestException.class)
    public void testCreateJob_Failure_TaskApiVersionNotSpecified() throws Exception {

        NewJob job = new NewJob();
        WorkerAction action = new WorkerAction();
        job.setName("TestName");
        job.setDescription("TestDescription");
        job.setExternalData("TestExternalData");
        action.setTaskClassifier("TestTaskClassifier");
        action.setTaskApiVersion(0);
        action.setTaskData("TestTaskData");
        action.setTaskDataEncoding(WorkerAction.TaskDataEncodingEnum.UTF8);
        action.setTargetPipe("JobServiceQueue");
        job.setTask(action);

        //  Test failed run of job creation where task api version has not been specified.
        JobsPut.createOrUpdateJob("067e6162-3b6f-4ae2-a171-2470b63dff00", job);
    }

    @Test(expected = BadRequestException.class)
    public void testCreateJob_Failure_TargetQueueNotSpecified() throws Exception {

        NewJob job = new NewJob();
        WorkerAction action = new WorkerAction();
        job.setName("TestName");
        job.setDescription("TestDescription");
        job.setExternalData("TestExternalData");
        action.setTaskClassifier("TestTaskClassifier");
        action.setTaskApiVersion(0);
        action.setTaskData("TestTaskData");
        action.setTaskDataEncoding(WorkerAction.TaskDataEncodingEnum.UTF8);
        action.setTargetPipe("");
        job.setTask(action);

        //  Test failed run of job creation where target queue has not been specified.
        JobsPut.createOrUpdateJob("067e6162-3b6f-4ae2-a171-2470b63dff00", job);
    }

}
