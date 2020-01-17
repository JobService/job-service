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

import static junit.framework.Assert.assertEquals;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.hpe.caf.services.job.api.generated.model.NewJob;
import com.hpe.caf.services.job.api.generated.model.RestrictedTask;
import com.hpe.caf.services.job.api.generated.model.WorkerAction;
import com.hpe.caf.services.configuration.AppConfig;
import com.hpe.caf.services.job.exceptions.BadRequestException;
import com.hpe.caf.services.job.jobtype.JobType;
import com.hpe.caf.services.job.jobtype.JobTypes;
import com.hpe.caf.services.job.queue.QueueServices;
import com.hpe.caf.services.job.queue.QueueServicesFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Arrays;
import java.util.Collections;
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

    private NewJob baseJob;
    private HashMap <String,Object> testDataObjectMap;
    private HashMap <String,String> taskMessageParams;
    private JobType basicJobType;

    /**
     * @return Simple new job with just outer values filled in - no `task` or `job`
     */
    private NewJob makeBaseJob() {
        final NewJob job = new NewJob();
        job.setName("TestName");
        job.setDescription("TestDescription");
        job.setExternalData("TestExternalData");
        return job;
    }

    /**
     * @return Simple new job with job task filled in, with string task data and no dependencies
     */
    private NewJob makeJob() {
        WorkerAction action = new WorkerAction();
        action.setTaskClassifier("TestTaskClassifier");
        action.setTaskApiVersion(1);
        action.setTaskData("TestTaskData");
        action.setTaskDataEncoding(WorkerAction.TaskDataEncodingEnum.UTF8);
        action.setTaskPipe("TaskQueue");
        action.setTargetPipe("JobServiceQueue");

        final NewJob job = makeBaseJob();
        job.setTask(action);
        return job;
    }

    /**
     * @param typeId
     * @param params
     * @return Simple new restricted job with job type and params filled in
     */
    private NewJob makeRestrictedJob(final String typeId, final JsonNode params) {
        final NewJob job = makeBaseJob();
        job.setType(typeId);
        job.setParameters(params);
        return job;
    }
   
    @Before
    public void setup() throws Exception {
        //  Mock DatabaseHelper calls.
        when(mockDatabaseHelper.createJob(
            anyString(), anyString(),anyString(),anyString(),anyString(),anyInt()
        )).thenReturn(true);
        when(mockDatabaseHelper.createJobWithDependencies(
            anyString(), anyString(),anyString(),anyString(),anyString(),anyInt(), anyString(),
            anyInt(), any(), anyString(), anyString(), any(), anyInt()
        )).thenReturn(true);
        doNothing().when(mockDatabaseHelper).deleteJob(anyString(), anyString());
        PowerMockito.whenNew(DatabaseHelper.class).withArguments(any()).thenReturn(mockDatabaseHelper);

        HashMap<String, String> newEnv  = new HashMap<>();
        newEnv.put("CAF_DATABASE_URL","testUrl");
        newEnv.put("CAF_DATABASE_USERNAME","testUserName");
        newEnv.put("CAF_DATABASE_PASSWORD","testPassword");
        newEnv.put("CAF_DATABASE_APPNAME","testAppName");
        TestUtil.setSystemEnvironmentFields(newEnv);

        // by default, set up a single job type with id 'basic'
        final JsonNode jsonObject =
            new ObjectMapper().convertValue(
                Collections.singletonMap("key", "val"),
                JsonNode.class);
        basicJobType = new JobType(
            "basic", "classifier", 2, "task pipe", "target pipe",
            (partitionId, jobId, params) -> jsonObject);
        JobTypes.initialise(() -> Collections.singletonList(basicJobType));

        //  Mock QueueServices calls.
        doNothing().when(mockQueueServices).sendMessage(any(), any(), any(), any());
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
    }
    
    @Test
    public void testCreateJob_Success_NoMatchingJobRow() throws Exception {

        when(mockDatabaseHelper.canJobBeProgressed(anyString(), anyString())).thenReturn(true);

        //  Test successful run of job creation when no matching job row exists.
        final String result = JobsPut.createOrUpdateJob(
            "partition", "067e6162-3b6f-4ae2-a171-2470b63dff00", makeJob());

        verify(mockDatabaseHelper, times(1))
            .createJob(eq("partition"), anyString(),anyString(),anyString(),anyString(),anyInt());
        verify(mockQueueServices, times(1)).sendMessage(any(), any(), any(), any());
        assertEquals("create", result);
    }

    @Test
    public void testCreateJob_Success_MatchingJobRow() throws Exception {
        when(mockDatabaseHelper.createJob(
            anyString(), anyString(),anyString(),anyString(),anyString(),anyInt()
        )).thenReturn(false);

        //  Test successful run of job creation when a matching job row already exists.
        final String result = JobsPut.createOrUpdateJob(
            "partition", "067e6162-3b6f-4ae2-a171-2470b63dff00", makeJob());

        verify(mockDatabaseHelper, times(1))
            .createJob(anyString(), anyString(),anyString(),anyString(),anyString(),anyInt());
        verify(mockQueueServices, times(0)).sendMessage(any(), any(), any(), any());
        assertEquals("update", result);
    }

    @Test(expected = BadRequestException.class)
    public void testCreateJob_Failure_JobIdNotSpecified() throws Exception {

        //  Test failed run of job creation with empty job id.
        JobsPut.createOrUpdateJob("partition", "", makeJob());
    }

    @Test(expected = BadRequestException.class)
    public void testCreateJob_Failure_InvalidJobId_Period() throws Exception {

        //  Test failed run of job creation with job id containing invalid characters.
        JobsPut.createOrUpdateJob("partition", "067e6162-3b6f-4ae2-a171-2470b6.dff00", makeJob());
    }

    @Test(expected = BadRequestException.class)
    public void testCreateJob_Failure_PartitionIdNotSpecified() throws Exception {
        JobsPut.createOrUpdateJob("", "067e6162-3b6f-4ae2-a171-2470b63dff00", makeJob());
    }

    @Test(expected = BadRequestException.class)
    public void testCreateJob_Failure_InvalidJobId_Asterisk() throws Exception {

        //  Test failed run of job creation with job id containing invalid characters.
        JobsPut.createOrUpdateJob("partition", "067e6162-3b6f-4ae2-a171-2470b6*dff00", makeJob());
    }

    @Test(expected = BadRequestException.class)
    public void testCreateRestrictedJob_Failure_typeAndTaskSpecified() throws Exception {
        final NewJob job = makeRestrictedJob("basic", null);
        job.setTask(makeJob().getTask());
        JobsPut.createOrUpdateJob("partition", "id", job);
    }

    @Test(expected = BadRequestException.class)
    public void testCreateRestrictedJob_Failure_missingType() throws Exception {
        JobsPut.createOrUpdateJob("partition", "id", makeRestrictedJob("missing", null));
    }

    @Test(expected = BadRequestException.class)
    public void testCreateRestrictedJob_Failure_invalidParams() throws Exception {
        final JobType failingJobType = new JobType(
            "id", "classifier", 2, "task pipe", "target pipe",
            (partitionId, jobId, params) -> { throw new BadRequestException("invalid params"); });
        JobTypes.initialise(() -> Collections.singletonList(failingJobType));
        JobsPut.createOrUpdateJob("partition", "id", makeRestrictedJob("basic", null));
    }

    public void testCreateRestrictedJob_Success() throws Exception {
        when(mockDatabaseHelper.canJobBeProgressed(anyString(), anyString())).thenReturn(true);
        JobsPut.createOrUpdateJob("partition", "id",
            makeRestrictedJob("basic", TextNode.valueOf("params")));

        verify(mockDatabaseHelper, times(1))
            .createJob(eq("partition"), eq("id"), anyString(), anyString(), anyString(), anyInt());
        final ArgumentCaptor<WorkerAction> workerActionCaptor =
            ArgumentCaptor.forClass(WorkerAction.class);
        verify(mockQueueServices, times(1))
            .sendMessage(eq("partition"), eq("id"), workerActionCaptor.capture(), any());

        final WorkerAction workerAction = workerActionCaptor.getValue();
        assertEquals(Collections.singletonMap("key", "val"), workerAction.getTaskData());
    }

    // null params

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
            JobsPut.createOrUpdateJob("partition", "067e6162-3b6f-4ae2-a171-2470b63dff00", job);
        } catch (BadRequestException bre) {
            assertEquals(JobsPut.ERR_MSG_TASK_DATA_NOT_SPECIFIED, bre.getMessage());
            throw bre;
        }
    }
    
    @Test
    public void testJobCreationWithTaskData_Object() throws Exception
    {
        when(mockDatabaseHelper.canJobBeProgressed(anyString(), anyString())).thenReturn(true);
        
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
        JobsPut.createOrUpdateJob("partition", "067e6162-3b6f-4ae2-a171-2470b63dff00", job);
        
        verify(mockDatabaseHelper, times(1)).createJob(
            anyString(), anyString(),anyString(),anyString(),anyString(),anyInt());
        verify(mockQueueServices, times(1)).sendMessage(any(), any(), any(), any());
    }

    @Test
    public void testJobCreationWithPrerequisites() throws Exception
    {
        when(mockDatabaseHelper.canJobBeProgressed(anyString(), anyString())).thenReturn(false);

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
        job.setPrerequisiteJobIds(Arrays.asList(new String[]{"J1", "J2"}));
        job.setDelay(0);

        //  Test creation of job when no matching job row exists and job has prereqs that have not been completed.
        final String createOrUpdateJobReturnString = JobsPut.createOrUpdateJob("partition", "067e6162-3b6f-4ae2-a171-2470b63dff00",
                job);

        assertEquals("create", createOrUpdateJobReturnString);

        verify(mockDatabaseHelper, times(1)).createJobWithDependencies(
            anyString(), anyString(),anyString(),anyString(),anyString(),anyInt(), anyString(),
            anyInt(), any(), anyString(), anyString(), any(), anyInt());
        verify(mockDatabaseHelper, times(1)).canJobBeProgressed(anyString(), anyString());
    }

    @Test
    public void testJobCreationWithPrerequisites_MatchingJobRow() throws Exception {
        when(mockDatabaseHelper.createJobWithDependencies(
            anyString(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyString(),
            anyInt(), any(), anyString(), anyString(), any(), anyInt()
        )).thenReturn(false);

        final NewJob job = makeJob();
        job.setPrerequisiteJobIds(Arrays.asList(new String[]{"J1", "J2"}));
        job.setDelay(0);

        final String result = JobsPut.createOrUpdateJob(
            "partition", "067e6162-3b6f-4ae2-a171-2470b63dff00", job);

        assertEquals("update", result);
        verify(mockDatabaseHelper, times(1)).createJobWithDependencies(
            anyString(), anyString(),anyString(),anyString(),anyString(),anyInt(), anyString(),
            anyInt(), any(), anyString(), anyString(), any(), anyInt());
        verify(mockDatabaseHelper, times(0)).canJobBeProgressed(anyString(), anyString());
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
            JobsPut.createOrUpdateJob("partition", "067e6162-3b6f-4ae2-a171-2470b63dff00", job);
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
            JobsPut.createOrUpdateJob("partition", "067e6162-3b6f-4ae2-a171-2470b63dff00", job);
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
        JobsPut.createOrUpdateJob("partition", "067e6162-3b6f-4ae2-a171-2470b63dff00", job);
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
        JobsPut.createOrUpdateJob("partition", "067e6162-3b6f-4ae2-a171-2470b63dff00", job);
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
        JobsPut.createOrUpdateJob("partition", "067e6162-3b6f-4ae2-a171-2470b63dff00", job);
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
        JobsPut.createOrUpdateJob("partition", "067e6162-3b6f-4ae2-a171-2470b63dff00", job);
    }
}
