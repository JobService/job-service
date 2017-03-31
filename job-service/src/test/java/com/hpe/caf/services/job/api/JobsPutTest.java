package com.hpe.caf.services.job.api;

import static junit.framework.Assert.assertEquals;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

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

    private HashMap <String,Object> testDataObjectMap;
    private HashMap <String,String> taskMessageParams;
   
    @Before
    public void setup() throws Exception {
        //  Mock DatabaseHelper calls.
        doNothing().when(mockDatabaseHelper).createJob(anyString(),anyString(),anyString(),anyString(),anyInt());
        doNothing().when(mockDatabaseHelper).deleteJob(anyString());
        PowerMockito.whenNew(DatabaseHelper.class).withArguments(any()).thenReturn(mockDatabaseHelper);

        HashMap<String, String> newEnv  = new HashMap<>();
        newEnv.put("CAF_DATABASE_URL","testUrl");
        newEnv.put("CAF_DATABASE_USERNAME","testUserName");
        newEnv.put("CAF_DATABASE_PASSWORD","testPassword");
        TestUtil.setSystemEnvironmentFields(newEnv);

        //  Mock QueueServices calls.
        doNothing().when(mockQueueServices).sendMessage(any(), any(), any());
        PowerMockito.whenNew(QueueServices.class).withArguments(any(),any(),anyString(),any()).thenReturn(mockQueueServices);

        //  Mock QueueServicesFactory calls.
        PowerMockito.mockStatic(QueueServicesFactory.class);
        PowerMockito.doReturn(mockQueueServices).when(QueueServicesFactory.class, "create", any(), anyString(), any());
        
        testDataObjectMap = new HashMap<>();
        taskMessageParams = new HashMap<>();
        
         //Populate the maps
        taskMessageParams.put("datastorePartialReference", "sample-files");
        taskMessageParams.put("documentDataInputFolder", "/mnt/caf-datastore-root/sample-files");
        taskMessageParams.put("documentDataOutputFolder", "/mnt/bla");
        
        testDataObjectMap.put("taskClassifier", "*.txt");
        testDataObjectMap.put("batchType", "WorkerDocumentBatchPlugin");
        testDataObjectMap.put("taskMessageType", "DocumentWorkerTaskBuilder");
        testDataObjectMap.put("taskMessageParams", taskMessageParams);
        testDataObjectMap.put("targetPipe", "languageidentification-in");
        
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
        action.setTaskPipe("TaskQueue");
        action.setTargetPipe("JobServiceQueue");

        validJob.setTask(action); 
    }
    
    @Test
    public void testCreateJob_Success_NoMatchingJobRow() throws Exception {

        when(mockDatabaseHelper.doesJobAlreadyExist(anyString(), anyInt())).thenReturn(false);

        //  Test successful run of job creation when no matching job row exists.
        JobsPut.createOrUpdateJob("067e6162-3b6f-4ae2-a171-2470b63dff00", validJob);

        verify(mockDatabaseHelper, times(1)).doesJobAlreadyExist(anyString(), anyInt());
        verify(mockDatabaseHelper, times(1)).createJob(anyString(),anyString(),anyString(),anyString(),anyInt());
        verify(mockQueueServices, times(1)).sendMessage(any(), any(), any());
    }

    @Test
    public void testCreateJob_Success_MatchingJobRow() throws Exception {

        when(mockDatabaseHelper.doesJobAlreadyExist(anyString(), anyInt())).thenReturn(true);

        //  Test successful run of job creation when a matching job row already exists.
        JobsPut.createOrUpdateJob("067e6162-3b6f-4ae2-a171-2470b63dff00", validJob);

        verify(mockDatabaseHelper, times(1)).doesJobAlreadyExist(anyString(), anyInt());
        verify(mockDatabaseHelper, times(0)).createJob(anyString(),anyString(),anyString(),anyString(),anyInt());
        verify(mockQueueServices, times(0)).sendMessage(any(), any(), any());
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
        action.setTaskPipe("TaskQueue");
        action.setTargetPipe("JobServiceQueue");
        job.setTask(action);

        //  Test failed run of job creation where task data has not been specified.
        try {
            JobsPut.createOrUpdateJob("067e6162-3b6f-4ae2-a171-2470b63dff00", job);
        } catch (BadRequestException bre) {
            assertEquals(JobsPut.ERR_MSG_TASK_DATA_NOT_SPECIFIED, bre.getMessage());
            throw bre;
        }
    }
    
    @Test
    public void testJobCreationWithTaskData_Object() throws Exception
    {
        when(mockDatabaseHelper.doesJobAlreadyExist(anyString(), anyInt())).thenReturn(false);
        
        NewJob job = new NewJob();
        WorkerAction action = new WorkerAction();
        job.setName("TestName");
        job.setDescription("TestDescription");
        job.setExternalData("TestExternalData");
        action.setTaskClassifier("TestTaskClassifier");
        action.setTaskApiVersion(1);
        action.setTaskData(testDataObjectMap);
        action.setTaskPipe("TaskQueue");
        action.setTargetPipe("JobServiceQueue");
        job.setTask(action);
        
        //  Test successful run of job creation when no matching job row exists.
        JobsPut.createOrUpdateJob("067e6162-3b6f-4ae2-a171-2470b63dff00", job);
        
        verify(mockDatabaseHelper, times(1)).doesJobAlreadyExist(anyString(), anyInt());
        verify(mockDatabaseHelper, times(1)).createJob(anyString(),anyString(),anyString(),anyString(),anyInt());
        verify(mockQueueServices, times(1)).sendMessage(any(), any(), any());
    }
    
    
    @Test(expected = BadRequestException.class)
    public void testCreateJob_Failure_TaskData_unsupportedType() throws Exception
    {
        NewJob job = new NewJob();
        WorkerAction action = new WorkerAction();
        job.setName("TestName");
        job.setDescription("TestDescription");
        job.setExternalData("TestExternalData");
        action.setTaskClassifier("TestTaskClassifier");
        action.setTaskApiVersion(1);
        action.setTaskData(1);
        action.setTaskPipe("TaskQueue");
        action.setTargetPipe("JobServiceQueue");
        job.setTask(action);

        //  Test failed run of job creation where taskData is an unsupported datatype
        try {
            JobsPut.createOrUpdateJob("067e6162-3b6f-4ae2-a171-2470b63dff00", job);
        } catch (BadRequestException bre) {
            assertEquals(JobsPut.ERR_MSG_TASK_DATA_DATATYPE_ERROR, bre.getMessage());
            throw bre;
        }
    }

    @Test(expected = BadRequestException.class)
    public void testCreateJob_Failure_TaskDataObjectAndEncodingConflict() throws Exception {
        NewJob job = new NewJob();
        WorkerAction action = new WorkerAction();
        job.setName("TestName");
        job.setDescription("TestDescription");
        job.setExternalData("TestExternalData");
        action.setTaskClassifier("TestTaskClassifier");
        action.setTaskApiVersion(1);
        action.setTaskData(testDataObjectMap);
        action.setTaskDataEncoding(WorkerAction.TaskDataEncodingEnum.UTF8);
        action.setTaskPipe("TaskQueue");
        action.setTargetPipe("JobServiceQueue");
        job.setTask(action);
        
        try {
            JobsPut.createOrUpdateJob("067e6162-3b6f-4ae2-a171-2470b63dff00", job);
        } catch (BadRequestException bre) {
            assertEquals(JobsPut.ERR_MSG_TASK_DATA_OBJECT_ENCODING_CONFLICT, bre.getMessage());
            throw bre;
        }
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
        action.setTaskPipe("TaskQueue");
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
        action.setTaskPipe("TaskQueue");
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
        action.setTaskPipe("TaskQueue");
        action.setTargetPipe("");
        job.setTask(action);

        //  Test failed run of job creation where target queue has not been specified.
        JobsPut.createOrUpdateJob("067e6162-3b6f-4ae2-a171-2470b63dff00", job);
    }

    @Test(expected = BadRequestException.class)
    public void testCreateJob_Failure_TaskQueueNotSpecified() throws Exception {

        NewJob job = new NewJob();
        WorkerAction action = new WorkerAction();
        job.setName("TestName");
        job.setDescription("TestDescription");
        job.setExternalData("TestExternalData");
        action.setTaskClassifier("TestTaskClassifier");
        action.setTaskApiVersion(0);
        action.setTaskData("TestTaskData");
        action.setTaskDataEncoding(WorkerAction.TaskDataEncodingEnum.UTF8);
        action.setTaskPipe("");
        action.setTargetPipe("JobServiceQueue");
        job.setTask(action);

        //  Test failed run of job creation where target queue has not been specified.
        JobsPut.createOrUpdateJob("067e6162-3b6f-4ae2-a171-2470b63dff00", job);
    }
}
