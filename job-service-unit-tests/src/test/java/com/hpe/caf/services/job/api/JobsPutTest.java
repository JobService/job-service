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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.hpe.caf.services.job.api.generated.model.NewJob;
import com.hpe.caf.services.job.api.generated.model.WorkerAction;
import com.hpe.caf.services.job.exceptions.BadRequestException;
import com.hpe.caf.services.job.jobtype.JobType;
import com.hpe.caf.services.job.jobtype.JobTypes;
import org.mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public final class JobsPutTest {

    public static final String TEST_TASK_DATA = "{\"data\" : \"TestTaskData\"}";

    @Mock
    private DatabaseHelper mockDatabaseHelper;

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
        action.setTaskData(TEST_TASK_DATA);
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
   
    @BeforeEach
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

        // by default, set up a single job type with id 'basic'
        final JsonNode jsonObject =
            new ObjectMapper().convertValue(
                Collections.singletonMap("key", "val"),
                JsonNode.class);
        basicJobType = new JobType(
            "basic",
            (partitionId, jobId, params) -> jsonObject);
        JobTypes.initialise(() -> Collections.singletonList(basicJobType));

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

        try (MockedConstruction<DatabaseHelper> mockDatabaseHelper = Mockito.mockConstruction(DatabaseHelper.class, (mock, context) -> {
            when(mock.createJob(
                    anyString(), anyString(),anyString(),anyString(),anyString(),anyInt(), anyString(),
                    anyInt(), any(), anyString(), anyString(), anyInt(), anyMap())).thenReturn(true);
            when(mock.createJobWithDependencies(
                    anyString(), anyString(),anyString(),anyString(),anyString(),anyInt(), anyString(),
                    anyInt(), any(), anyString(), anyString(), any(), anyInt(), anyMap(), eq(false)
            )).thenReturn(true);
        })) {

            //  Test successful run of job creation when no matching job row exists.
            final String result = JobsPut.createOrUpdateJob(
                    "partition", "067e6162-3b6f-4ae2-a171-2470b63dff00", makeJob());

            verify(mockDatabaseHelper.constructed().get(0), times(1))
                    .createJob(eq("partition"), anyString(),anyString(),anyString(),anyString(),anyInt(), anyString(),
                            anyInt(), any(), anyString(), anyString(), anyInt(), anyMap());
            assertEquals("create", result);
        }

    }

    @Test
    public void testCreateJob_Success_MatchingJobRow() throws Exception {
        try (MockedConstruction<DatabaseHelper> mockDatabaseHelper = Mockito.mockConstruction(DatabaseHelper.class, (mock, context) -> {
            when(mock.createJob(
                    anyString(), anyString(),anyString(),anyString(),anyString(),anyInt(), anyString(),
                    anyInt(), any(), anyString(), anyString(), anyInt(), anyMap())).thenReturn(false);
            when(mock.createJobWithDependencies(
                    anyString(), anyString(),anyString(),anyString(),anyString(),anyInt(), anyString(),
                    anyInt(), any(), anyString(), anyString(), any(), anyInt(), anyMap(), eq(false)
            )).thenReturn(true);
        })) {
            //  Test successful run of job creation when a matching job row already exists.
            final String result = JobsPut.createOrUpdateJob(
                    "partition", "067e6162-3b6f-4ae2-a171-2470b63dff00", makeJob());

            verify(mockDatabaseHelper.constructed().get(0), times(1))
                    .createJob(
                            anyString(), anyString(),anyString(),anyString(),anyString(),anyInt(), anyString(),
                            anyInt(), any(), anyString(), anyString(), anyInt(), anyMap());
            assertEquals("update", result);
        }
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testCreateJob_Failure_JobIdNotSpecified() throws Exception {

        //  Test failed run of job creation with empty job id.
        Assertions.assertThrows(BadRequestException.class, () -> JobsPut.createOrUpdateJob("partition", "", makeJob()));
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testCreateJob_Failure_InvalidJobId_Period() throws Exception {

        //  Test failed run of job creation with job id containing invalid characters.
        Assertions.assertThrows(BadRequestException.class, () -> JobsPut.createOrUpdateJob("partition", "067e6162-3b6f-4ae2-a171-2470b6.dff00", makeJob()));
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testCreateJob_Failure_PartitionIdNotSpecified() throws Exception {
        Assertions.assertThrows(BadRequestException.class, () -> JobsPut.createOrUpdateJob("", "067e6162-3b6f-4ae2-a171-2470b63dff00", makeJob()));
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testCreateJob_Failure_InvalidJobId_Asterisk() throws Exception {

        //  Test failed run of job creation with job id containing invalid characters.
        Assertions.assertThrows(BadRequestException.class, () -> JobsPut.createOrUpdateJob("partition", "067e6162-3b6f-4ae2-a171-2470b6*dff00", makeJob()));
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testCreateRestrictedJob_Failure_typeAndTaskSpecified() throws Exception {
        final NewJob job = makeRestrictedJob("basic", null);
        job.setTask(makeJob().getTask());
        Assertions.assertThrows(BadRequestException.class, () -> JobsPut.createOrUpdateJob("partition", "id", job));
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testCreateRestrictedJob_Failure_missingType() throws Exception {
        Assertions.assertThrows(BadRequestException.class, () -> JobsPut.createOrUpdateJob("partition", "id", makeRestrictedJob("missing", null)));
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testCreateRestrictedJob_Failure_invalidParams() throws Exception {
        final JobType failingJobType = new JobType(
            "id",
            (partitionId, jobId, params) -> {
                throw new BadRequestException("invalid params");
            });
        JobTypes.initialise(() -> Collections.singletonList(failingJobType));
        Assertions.assertThrows(BadRequestException.class, () -> JobsPut.createOrUpdateJob("partition", "id", makeRestrictedJob("basic", null)));
    }

    public void testCreateRestrictedJob_Success() throws Exception {
        JobsPut.createOrUpdateJob("partition", "id",
            makeRestrictedJob("basic", TextNode.valueOf("params")));

        verify(mockDatabaseHelper, times(1))
            .createJob(eq("partition"), eq("id"), anyString(),anyString(),anyString(),anyInt(), anyString(),
                    anyInt(), any(), anyString(), anyString(), anyInt(), anyMap());
        final ArgumentCaptor<WorkerAction> workerActionCaptor =
            ArgumentCaptor.forClass(WorkerAction.class);

        final WorkerAction workerAction = workerActionCaptor.getValue();
        assertEquals(Collections.singletonMap("key", "val"), workerAction.getTaskData());
    }

    // null params

    @Test
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
        Exception bre = Assertions.assertThrows(BadRequestException.class, () -> JobsPut.createOrUpdateJob("partition", "067e6162-3b6f-4ae2-a171-2470b63dff00", job));
        assertEquals(JobsPut.ERR_MSG_TASK_DATA_NOT_SPECIFIED, bre.getMessage());
    }
    
    @Test
    public void testJobCreationWithTaskData_Object() throws Exception
    {
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
        try (MockedConstruction<DatabaseHelper> mockDatabaseHelper = Mockito.mockConstruction(DatabaseHelper.class)) {
            JobsPut.createOrUpdateJob("partition", "067e6162-3b6f-4ae2-a171-2470b63dff00", job);

            verify(mockDatabaseHelper.constructed().get(0), times(1)).createJob(
                    anyString(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyString(),
                    anyInt(), any(), anyString(), anyString(), anyInt(), anyMap());
        }
    }

    @Test
    public void testJobCreationWithPrerequisites() throws Exception
    {
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

        try (MockedConstruction<DatabaseHelper> mockDatabaseHelper = Mockito.mockConstruction(DatabaseHelper.class, (mock, context) -> {
            when(mock.createJob(
                    anyString(), anyString(),anyString(),anyString(),anyString(),anyInt(), anyString(),
                    anyInt(), any(), anyString(), anyString(), anyInt(), anyMap())).thenReturn(true);
            when(mock.createJobWithDependencies(
                    anyString(), anyString(),anyString(),anyString(),anyString(),anyInt(), anyString(),
                    anyInt(), any(), anyString(), anyString(), any(), anyInt(), anyMap(), eq(false)
            )).thenReturn(true);
        })) {
            //  Test creation of job when no matching job row exists and job has prereqs that have not been completed.
            final String createOrUpdateJobReturnString = JobsPut.createOrUpdateJob("partition", "067e6162-3b6f-4ae2-a171-2470b63dff00",
                    job);

            assertEquals("create", createOrUpdateJobReturnString);

            verify(mockDatabaseHelper.constructed().get(0), times(1)).createJobWithDependencies(
                    anyString(), anyString(),anyString(),anyString(),anyString(),anyInt(), anyString(),
                    anyInt(), any(), anyString(), anyString(), any(), anyInt(), anyMap(), eq(false));
        }
    }

    @Test
    public void testJobCreationWithPrerequisites_MatchingJobRow() throws Exception {

        try (MockedConstruction<DatabaseHelper> mockDatabaseHelper = Mockito.mockConstruction(DatabaseHelper.class, (mock, context) -> {
            when(mock.createJob(
                    anyString(), anyString(),anyString(),anyString(),anyString(),anyInt(), anyString(),
                    anyInt(), any(), anyString(), anyString(), anyInt(), anyMap())).thenReturn(true);
            when(mock.createJobWithDependencies(
                    anyString(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyString(),
                    anyInt(), any(), anyString(), anyString(), any(), anyInt(), anyMap(), eq(false)
            )).thenReturn(false);
        })) {
            final NewJob job = makeJob();
            job.setPrerequisiteJobIds(Arrays.asList(new String[]{"J1", "J2"}));
            job.setDelay(0);

            final String result = JobsPut.createOrUpdateJob(
                    "partition", "067e6162-3b6f-4ae2-a171-2470b63dff00", job);

            assertEquals("update", result);
            verify(mockDatabaseHelper.constructed().get(0), times(1)).createJobWithDependencies(
                    anyString(), anyString(),anyString(),anyString(),anyString(),anyInt(), anyString(),
                    anyInt(), any(), anyString(), anyString(), any(), anyInt(), anyMap(), eq(false));
        }
    }

    
    @Test
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
        Exception bre = Assertions.assertThrows(BadRequestException.class, () -> JobsPut.createOrUpdateJob("partition", "067e6162-3b6f-4ae2-a171-2470b63dff00", job));
        assertEquals(JobsPut.ERR_MSG_TASK_DATA_DATATYPE_ERROR, bre.getMessage());
    }

    @Test
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

        try (MockedConstruction<DatabaseHelper> mockDatabaseHelper = Mockito.mockConstruction(DatabaseHelper.class, (mock, context) -> {
            when(mock.createJob(
                    anyString(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyString(),
                    anyInt(), any(), anyString(), anyString(), anyInt(), anyMap())).thenReturn(true);
            when(mock.createJobWithDependencies(
                    anyString(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyString(),
                    anyInt(), any(), anyString(), anyString(), any(), anyInt(), anyMap(), eq(false)
            )).thenReturn(true);
        })) {
            Exception bre = Assertions.assertThrows(BadRequestException.class, () -> JobsPut.createOrUpdateJob("partition", "067e6162-3b6f-4ae2-a171-2470b63dff00", job));
            assertEquals(JobsPut.ERR_MSG_TASK_DATA_OBJECT_ENCODING_CONFLICT, bre.getMessage());
        }

    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testCreateJob_Failure_TaskClassifierNotSpecified() throws Exception {

        NewJob job = new NewJob();
        WorkerAction action = new WorkerAction();
        job.setName("TestName");
        job.setDescription("TestDescription");
        job.setExternalData("TestExternalData");
        action.setTaskClassifier("");
        action.setTaskApiVersion(1);
        action.setTaskData(TEST_TASK_DATA);
        action.setTaskDataEncoding(WorkerAction.TaskDataEncodingEnum.UTF8);
        action.setTaskPipe("TaskQueue");
        action.setTargetPipe("JobServiceQueue");
        job.setTask(action);

        //  Test failed run of job creation where task classifier has not been specified.
        Assertions.assertThrows(BadRequestException.class, () -> JobsPut.createOrUpdateJob("partition", "067e6162-3b6f-4ae2-a171-2470b63dff00", job));
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testCreateJob_Failure_TaskApiVersionNotSpecified() throws Exception {

        NewJob job = new NewJob();
        WorkerAction action = new WorkerAction();
        job.setName("TestName");
        job.setDescription("TestDescription");
        job.setExternalData("TestExternalData");
        action.setTaskClassifier("TestTaskClassifier");
        action.setTaskApiVersion(0);
        action.setTaskData(TEST_TASK_DATA);
        action.setTaskDataEncoding(WorkerAction.TaskDataEncodingEnum.UTF8);
        action.setTaskPipe("TaskQueue");
        action.setTargetPipe("JobServiceQueue");
        job.setTask(action);

        //  Test failed run of job creation where task api version has not been specified.
        Assertions.assertThrows(BadRequestException.class, () -> JobsPut.createOrUpdateJob("partition", "067e6162-3b6f-4ae2-a171-2470b63dff00", job));
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testCreateJob_Failure_TargetQueueNotSpecified() throws Exception {

        NewJob job = new NewJob();
        WorkerAction action = new WorkerAction();
        job.setName("TestName");
        job.setDescription("TestDescription");
        job.setExternalData("TestExternalData");
        action.setTaskClassifier("TestTaskClassifier");
        action.setTaskApiVersion(0);
        action.setTaskData(TEST_TASK_DATA);
        action.setTaskDataEncoding(WorkerAction.TaskDataEncodingEnum.UTF8);
        action.setTaskPipe("TaskQueue");
        action.setTargetPipe("");
        job.setTask(action);

        //  Test failed run of job creation where target queue has not been specified.
        Assertions.assertThrows(BadRequestException.class, () -> JobsPut.createOrUpdateJob("partition", "067e6162-3b6f-4ae2-a171-2470b63dff00", job));
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testCreateJob_Failure_TaskQueueNotSpecified() throws Exception {

        NewJob job = new NewJob();
        WorkerAction action = new WorkerAction();
        job.setName("TestName");
        job.setDescription("TestDescription");
        job.setExternalData("TestExternalData");
        action.setTaskClassifier("TestTaskClassifier");
        action.setTaskApiVersion(0);
        action.setTaskData(TEST_TASK_DATA);
        action.setTaskDataEncoding(WorkerAction.TaskDataEncodingEnum.UTF8);
        action.setTaskPipe("");
        action.setTargetPipe("JobServiceQueue");
        job.setTask(action);

        //  Test failed run of job creation where target queue has not been specified.
        Assertions.assertThrows(BadRequestException.class, () -> JobsPut.createOrUpdateJob("partition", "067e6162-3b6f-4ae2-a171-2470b63dff00", job));
    }
}
