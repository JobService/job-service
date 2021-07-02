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
package com.hpe.caf.jobservice.acceptance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hpe.caf.api.BootstrapConfiguration;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.worker.DataStoreException;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.config.system.SystemBootstrapConfiguration;
import com.hpe.caf.naming.ServicePath;
import com.hpe.caf.services.job.client.ApiClient;
import com.hpe.caf.services.job.client.ApiException;
import com.hpe.caf.services.job.client.api.JobsApi;
import com.hpe.caf.services.job.client.model.Job;
import com.hpe.caf.services.job.client.model.NewJob;
import com.hpe.caf.services.job.client.model.WorkerAction;
import com.hpe.caf.services.job.client.model.JobStatus;
import com.hpe.caf.worker.batch.BatchWorkerConstants;
import com.hpe.caf.worker.batch.BatchWorkerTask;
import com.hpe.caf.worker.document.DocumentWorkerDocumentTask;
import com.hpe.caf.worker.example.ExampleWorkerConstants;
import com.hpe.caf.worker.example.ExampleWorkerStatus;
import com.hpe.caf.worker.queue.rabbit.RabbitWorkerQueueConfiguration;
import com.hpe.caf.worker.testing.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeoutException;



/**
 * End to end integration test of the full Job Service solution, including
 * the Job Service itself, Job Service Database, Batch Worker, and Job Tracking Worker.
 * The data processing worker used in this test is the Example Worker.
 */
public class JobServiceEndToEndIT {
    private static final String batchWorkerMessageInQueue = "batchworker-test-input-1";
    private static final String exampleWorkerMessageInQueue = "exampleworker-test-input-1";
    private static final String resumeJobQueue = "worker-taskunstowing-in";
    private static String exampleWorkerMessageOutQueue;
    private static final String datastoreContainerId = "datastore.container.id";
    private static final String jobCorrelationId = "1";
    private static final long defaultTimeOutMs = 600000; // 10 minutes
    private static final long JOB_STATUS_CHECK_TIMEOUT_MS = 30000; // 30 seconds
    private static final long JOB_STATUS_CHECK_SLEEP_MS = 500; // 0.5 seconds
    private static ServicePath servicePath;
    private static WorkerServices workerServices;
    private static ConfigurationSource configurationSource;
    private static RabbitWorkerQueueConfiguration rabbitConfiguration;
    private static JobsApi jobsApi;
    private static int numTestItemsToGenerate = 50;   // CAF-3677: This cannot be set any higher than 2 otherwise the job will not reach completion. Change to final variable when fixed.

    private String defaultPartitionId;
    private List<String> testItemAssetIds;

    //  Scripted Job Service testing.
    private static final String CREATE_JOB_DEFN_CONTAINER_JSON_FILENAME = "create_job_definition_container.json";
    private static final String CREATE_JOB_SERVICE_CALLER_CONTAINER_JSON_FILENAME = "create_job_service_caller_container.json";
    private static String jobDefinitionContainerJSON;
    private static String jobServiceCallerContainerJSON;
    private static final String pollingInterval = "10";
    private static final String jobDefinitionFile = "test_job_definition.json";
    private static String dockerContainersURL;
    private static String jobServiceCallerImage;
    private static String jobServiceCallerWebServiceLinkURL;
    private static String jobServiceImage;
    private static String jobServiceAdminPort;

    private static final Logger LOG = LoggerFactory.getLogger(JobServiceEndToEndIT.class);

    @BeforeClass
    public static void setup() throws Exception {
        BootstrapConfiguration bootstrap = new SystemBootstrapConfiguration();
        servicePath = bootstrap.getServicePath();
        workerServices = WorkerServices.getDefault();
        configurationSource = workerServices.getConfigurationSource();
        rabbitConfiguration = configurationSource.getConfiguration(RabbitWorkerQueueConfiguration.class);
        rabbitConfiguration.getRabbitConfiguration().setRabbitHost(SettingsProvider.defaultProvider.getSetting(SettingNames.dockerHostAddress));
        rabbitConfiguration.getRabbitConfiguration().setRabbitPort(Integer.parseInt(SettingsProvider.defaultProvider.getSetting(SettingNames.rabbitmqNodePort)));
        jobsApi = createJobsApi();
        dockerContainersURL = System.getenv("CAF_DOCKER_HOST") + "/v" + System.getenv("CAF_DOCKER_VERSION") + "/containers/";
        jobServiceImage = System.getenv("CAF_JOB_SERVICE_IMAGE");
        jobServiceAdminPort = System.getenv("CAF_JOB_SERVICE_ADMIN_PORT");
        jobDefinitionContainerJSON = JobServiceCallerTestsHelper.getJSONFromFile(CREATE_JOB_DEFN_CONTAINER_JSON_FILENAME);
        jobServiceCallerContainerJSON = JobServiceCallerTestsHelper.getJSONFromFile(CREATE_JOB_SERVICE_CALLER_CONTAINER_JSON_FILENAME);
        jobServiceCallerImage = System.getenv("CAF_JOB_SERVICE_CALLER_IMAGE");
        jobServiceCallerWebServiceLinkURL = System.getenv("CAF_JOB_SERVICE_CALLER_WEBSERVICE_LINK_URL");
    }

    private static JobsApi createJobsApi() {
        String connectionString = System.getenv("CAF_WEBSERVICE_URL");
        ApiClient client = new ApiClient();
        client.setBasePath(connectionString);
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        client.setDateFormat(f);
        return new JobsApi(client);
    }


    @BeforeMethod
    public void testSetup() throws Exception {
        defaultPartitionId = UUID.randomUUID().toString();
        numTestItemsToGenerate = 50;        // CAF-3677: Remove this on fix
        testItemAssetIds = generateWorkerBatch();
        exampleWorkerMessageOutQueue = "exampleworker-test-output-1";
    }

//    @Test
//    public void testJobCompletionWithTaskDataObject() throws Exception {
//        testJobCompletion(true);
//    }

    private void testJobCompletion(final boolean useTaskDataObject) throws Exception {
        final String jobId = generateJobId();

        JobServiceEndToEndITExpectation expectation =
                new JobServiceEndToEndITExpectation(
                        false,
                        exampleWorkerMessageOutQueue,
                    defaultPartitionId,
                        jobId,
                        jobCorrelationId,
                        ExampleWorkerConstants.WORKER_NAME,
                        ExampleWorkerConstants.WORKER_API_VER,
                        TaskStatus.RESULT_SUCCESS,
                        ExampleWorkerStatus.COMPLETED,
                        testItemAssetIds);

        try (QueueManager queueManager = getFinalQueueManager()) {
            ExecutionContext context = new ExecutionContext(false);
            context.initializeContext();
            Timer timer = getTimer(context);
            Thread thread = queueManager.start(new FinalOutputDeliveryHandler(workerServices.getCodec(), jobsApi, context, expectation));

            createJob(jobId, useTaskDataObject);

            // Waits for the final result message to appear on the Example worker's output queue.
            // When we read it from this queue it should have been processed fully and its status reported to the Job Database as Completed.
            TestResult result = context.getTestResult();
            Assert.assertTrue(result.isSuccess(), "Job "+jobId+" was not completed successfully");
        }
    }

//    @Test(enabled = false) // See https://portal.digitalsafe.net/browse/SCMOD-13004
//    public void testTargetPipeNull()throws Exception{
//        //Set null output queue. Which is valid.
//        testTargetPipeForJobWithNoAndWithCompletedPrerequisiteJobs(null);
//    }
//    @Test
//    public void testTargetPipeEmpty()throws Exception{
//        //Set empty output queue. Which is invalid.
//        try {
//            testTargetPipeForJobWithNoAndWithCompletedPrerequisiteJobs("");
//        }catch (ApiException apiEx){
//            Assert.assertEquals(apiEx.getMessage(), "{\"message\":\"The target queue name has not been specified.\"}", "ApiException is not thrown for not specifying Target Pipe");
//        }
//    }
//    public void testTargetPipeForJobWithNoAndWithCompletedPrerequisiteJobs(final String targetPipe) throws Exception{
//
//        //set output queue
//        exampleWorkerMessageOutQueue=targetPipe;
//        numTestItemsToGenerate = 2;                 // CAF-3677: Remove this on fix
//        testItemAssetIds = generateWorkerBatch();   // CAF-3677: Remove this on fix
//
//        //  Generate job identifiers for test.
//        final String job1Id = generateJobId();
//        final String job2Id = generateJobId();
//        final String job3Id = generateJobId();
//
//        // Add a Prerequisite job 1 that should be completed
//        JobServiceEndToEndITExpectation job1Expectation =
//                new JobServiceEndToEndITExpectation(
//                        false,
//                        exampleWorkerMessageOutQueue,
//                    defaultPartitionId,
//                        job1Id,
//                        jobCorrelationId,
//                        ExampleWorkerConstants.WORKER_NAME,
//                        ExampleWorkerConstants.WORKER_API_VER,
//                        TaskStatus.RESULT_SUCCESS,
//                        ExampleWorkerStatus.COMPLETED,
//                        testItemAssetIds);
//
//        try (QueueManager queueManager = getFinalQueueManager()) {
//            ExecutionContext context = new ExecutionContext(false);
//            context.initializeContext();
//            getTimer(context);
//            queueManager.start(new FinalOutputDeliveryHandler(workerServices.getCodec(), jobsApi, context,
//                    job1Expectation));
//
//            createJob(job1Id, true);
//        }
//
//        // Add a Prerequisite job 2 that should be completed
//        JobServiceEndToEndITExpectation job2Expectation =
//                new JobServiceEndToEndITExpectation(
//                        false,
//                        exampleWorkerMessageOutQueue,
//                    defaultPartitionId,
//                        job2Id,
//                        jobCorrelationId,
//                        ExampleWorkerConstants.WORKER_NAME,
//                        ExampleWorkerConstants.WORKER_API_VER,
//                        TaskStatus.RESULT_SUCCESS,
//                        ExampleWorkerStatus.COMPLETED,
//                        testItemAssetIds);
//
//        try (QueueManager queueManager = getFinalQueueManager()) {
//            ExecutionContext context = new ExecutionContext(false);
//            context.initializeContext();
//            getTimer(context);
//            queueManager.start(new FinalOutputDeliveryHandler(workerServices.getCodec(), jobsApi, context,
//                    job2Expectation));
//
//            createJob(job2Id, true);
//        }
//
//        //Wait for the jobs to complete
//        waitUntilJobCompletes(job1Id);
//        waitUntilJobCompletes(job2Id);
//
//        // Add job that has prerequisite job 1 (completed) and job 2 (completed). Also supply blank, null and empty
//        // prereq job ids that should not hold the job back.
//        createJobWithPrerequisites(job3Id, true, 0, job1Id, job2Id, "", null, "           ");
//
//        //Wait for the job to complete
//        waitUntilJobCompletes(job3Id);
//
//        // Call getJob to trigger the subtask completion
//        jobsApi.getJob(defaultPartitionId, job3Id, jobCorrelationId);
//
//        //  Verify job 3 has completed and no job dependency rows exist.
//        JobServiceDatabaseUtil.assertJobStatus(job3Id, "completed");
//        JobServiceDatabaseUtil.assertJobDependencyRowsDoNotExist(job3Id, job1Id);
//        JobServiceDatabaseUtil.assertJobDependencyRowsDoNotExist(job3Id, job2Id);
//    }
//
//    @Test(enabled = false) // See https://portal.digitalsafe.net/browse/SCMOD-13004
//    public void testJobWithNoPrerequisiteJobs() throws Exception {
//        numTestItemsToGenerate = 2;                 // CAF-3677: Remove this on fix
//        testItemAssetIds = generateWorkerBatch();   // CAF-3677: Remove this on fix
//        final String jobId = generateJobId();
//
//        JobServiceEndToEndITExpectation expectation =
//                new JobServiceEndToEndITExpectation(
//                        false,
//                        exampleWorkerMessageOutQueue,
//                    defaultPartitionId,
//                        jobId,
//                        jobCorrelationId,
//                        ExampleWorkerConstants.WORKER_NAME,
//                        ExampleWorkerConstants.WORKER_API_VER,
//                        TaskStatus.RESULT_SUCCESS,
//                        ExampleWorkerStatus.COMPLETED,
//                        testItemAssetIds);
//
//        try (QueueManager queueManager = getFinalQueueManager()) {
//            ExecutionContext context = new ExecutionContext(false);
//            context.initializeContext();
//            Timer timer = getTimer(context);
//            Thread thread = queueManager.start(new FinalOutputDeliveryHandler(workerServices.getCodec(), jobsApi, context, expectation));
//
//            createJob(jobId, true);
//
//            // Waits for the final result message to appear on the Example worker's output queue.
//            // When we read it from this queue it should have been processed fully and its status reported to the Job Database as Completed.
//            TestResult result = context.getTestResult();
//            Assert.assertTrue(result.isSuccess(), "Job "+jobId+" was not completed successfully");
//        }
//    }
//
//    @Test(enabled = false) // See https://portal.digitalsafe.net/browse/SCMOD-13004
//    public void testJobWithPrerequisiteJobsWhichHaveCompleted() throws Exception
//    {
//        numTestItemsToGenerate = 2;                 // CAF-3677: Remove this on fix
//        testItemAssetIds = generateWorkerBatch();   // CAF-3677: Remove this on fix
//
//        //  Generate job identifiers for test.
//        final String job1Id = generateJobId();
//        final String job2Id = generateJobId();
//        final String job3Id = generateJobId();
//
//        // Add a Prerequisite job 1 that should be completed
//        JobServiceEndToEndITExpectation job1Expectation =
//                new JobServiceEndToEndITExpectation(
//                        false,
//                        exampleWorkerMessageOutQueue,
//                    defaultPartitionId,
//                        job1Id,
//                        jobCorrelationId,
//                        ExampleWorkerConstants.WORKER_NAME,
//                        ExampleWorkerConstants.WORKER_API_VER,
//                        TaskStatus.RESULT_SUCCESS,
//                        ExampleWorkerStatus.COMPLETED,
//                        testItemAssetIds);
//
//        try (QueueManager queueManager = getFinalQueueManager()) {
//            ExecutionContext context = new ExecutionContext(false);
//            context.initializeContext();
//            getTimer(context);
//            queueManager.start(new FinalOutputDeliveryHandler(workerServices.getCodec(), jobsApi, context,
//                    job1Expectation));
//
//            createJob(job1Id, true);
//        }
//
//        // Add a Prerequisite job 2 that should be completed
//        JobServiceEndToEndITExpectation job2Expectation =
//                new JobServiceEndToEndITExpectation(
//                        false,
//                        exampleWorkerMessageOutQueue,
//                    defaultPartitionId,
//                        job2Id,
//                        jobCorrelationId,
//                        ExampleWorkerConstants.WORKER_NAME,
//                        ExampleWorkerConstants.WORKER_API_VER,
//                        TaskStatus.RESULT_SUCCESS,
//                        ExampleWorkerStatus.COMPLETED,
//                        testItemAssetIds);
//
//        try (QueueManager queueManager = getFinalQueueManager()) {
//            ExecutionContext context = new ExecutionContext(false);
//            context.initializeContext();
//            getTimer(context);
//            queueManager.start(new FinalOutputDeliveryHandler(workerServices.getCodec(), jobsApi, context,
//                    job2Expectation));
//
//            createJob(job2Id, true);
//        }
//
//        //Wait for the job to complete
//        waitUntilJobCompletes(job1Id);
//        waitUntilJobCompletes(job2Id);
//
//        // Add job that has prerequisite job 1 (completed) and job 2 (completed). Also supply blank, null and empty
//        // prereq job ids that should not hold the job back.
//        createJobWithPrerequisites(job3Id, true, 0, job1Id, job2Id, "", null, "           ");
//
//        //Wait for the job to complete
//        waitUntilJobCompletes(job3Id);
//
//        // Call getJob to trigger the subtask completion
//        jobsApi.getJob(defaultPartitionId, job3Id, jobCorrelationId);
//
//        //  Verify job 3 has completed and no job dependency rows exist.
//        JobServiceDatabaseUtil.assertJobStatus(job3Id, "completed");
//        JobServiceDatabaseUtil.assertJobDependencyRowsDoNotExist(job3Id, job1Id);
//        JobServiceDatabaseUtil.assertJobDependencyRowsDoNotExist(job3Id, job2Id);
//    }
//
//    @Test(enabled = false) // See https://portal.digitalsafe.net/browse/SCMOD-13004
//    public void testJobWithNullAndBlankAndEmptyPrereqJobsShouldComplete() throws Exception
//    {
//        numTestItemsToGenerate = 1;                 // CAF-3677: Remove this on fix
//        testItemAssetIds = generateWorkerBatch();   // CAF-3677: Remove this on fix
//        final String job1Id = generateJobId();
//
//        JobServiceEndToEndITExpectation job1Expectation =
//                new JobServiceEndToEndITExpectation(
//                        false,
//                        exampleWorkerMessageOutQueue,
//                    defaultPartitionId,
//                        job1Id,
//                        jobCorrelationId,
//                        ExampleWorkerConstants.WORKER_NAME,
//                        ExampleWorkerConstants.WORKER_API_VER,
//                        TaskStatus.RESULT_SUCCESS,
//                        ExampleWorkerStatus.COMPLETED,
//                        testItemAssetIds);
//
//        // Add a Prerequisite job 1 that should be completed
//        try (QueueManager queueManager = getFinalQueueManager()) {
//            ExecutionContext context = new ExecutionContext(false);
//            context.initializeContext();
//            getTimer(context);
//            queueManager.start(new FinalOutputDeliveryHandler(workerServices.getCodec(), jobsApi, context,
//                    job1Expectation));
//
//            // Add job that has prerequisite jobs that are empty, null and blank
//            createJobWithPrerequisites(job1Id, true, 0, "", null, "           ");
//
//            // Waits for the final result message to appear on the Example worker's output queue.
//            // When we read it from this queue it should have been processed fully and its status reported to the Job Database as Completed.
//            TestResult result = context.getTestResult();
//            Assert.assertTrue(result.isSuccess(), "Job "+job1Id+" was not completed successfully");
//        }
//    }
//
//    @Test(enabled = false) // See https://portal.digitalsafe.net/browse/SCMOD-13004
//    public void testJobWithPrerequisiteJobsWithOneNotCompleted() throws Exception
//    {
//        numTestItemsToGenerate = 2;                 // CAF-3677: Remove this on fix
//        testItemAssetIds = generateWorkerBatch();   // CAF-3677: Remove this on fix
//
//        //  Generate job identifiers for test.
//        final String job1Id = generateJobId();
//        final String job3Id = generateJobId();
//        final String job2Id = generateJobId();
//
//        // Add a Prerequisite job 1 that should be completed
//        JobServiceEndToEndITExpectation job1Expectation =
//                new JobServiceEndToEndITExpectation(
//                        false,
//                        exampleWorkerMessageOutQueue,
//                    defaultPartitionId,
//                        job1Id,
//                        jobCorrelationId,
//                        ExampleWorkerConstants.WORKER_NAME,
//                        ExampleWorkerConstants.WORKER_API_VER,
//                        TaskStatus.RESULT_SUCCESS,
//                        ExampleWorkerStatus.COMPLETED,
//                        testItemAssetIds);
//
//        try (QueueManager queueManager = getFinalQueueManager()) {
//            ExecutionContext context = new ExecutionContext(false);
//            context.initializeContext();
//            getTimer(context);
//            queueManager.start(new FinalOutputDeliveryHandler(workerServices.getCodec(), jobsApi, context,
//                    job1Expectation));
//
//            createJob(job1Id, true);
//
//            // Waits for the final result message to appear on the Example worker's output queue.
//            // When we read it from this queue it should have been processed fully and its status reported to the Job
//            // Database as Completed.
//            context.getTestResult();
//        }
//
//        //Wait for the job to complete
//        waitUntilJobCompletes(job1Id);
//
//        // Add job that has prerequisite job 1 (completed) and job 2 (unknown)
//        createJobWithPrerequisites(job3Id, true, 0, job1Id, job2Id);
//
//        // Verify job 3 is in waiting status and dependency rows exist as expected.
//        JobServiceDatabaseUtil.assertJobStatus(job3Id, "waiting");
//        JobServiceDatabaseUtil.assertJobDependencyRowsExist(job3Id, job2Id, batchWorkerMessageInQueue, exampleWorkerMessageOutQueue);
//    }
//
//    // testing creation of 2 job-dependency rows for the same parent job
//    @Test
//    public void testJobWithPrerequisiteJobsNotCompleted() throws Exception
//    {
//        numTestItemsToGenerate = 2;                 // CAF-3677: Remove this on fix
//        testItemAssetIds = generateWorkerBatch();   // CAF-3677: Remove this on fix
//
//        final String parentJobId = generateJobId();
//        final String child1JobId = generateJobId();
//        final String child2JobId = generateJobId();
//        createJobWithPrerequisites(parentJobId, true, 0, child1JobId, child2JobId);
//
//        JobServiceDatabaseUtil.assertJobStatus(parentJobId, "waiting");
//    }
//
//    @Test(enabled = false) // See https://portal.digitalsafe.net/browse/SCMOD-13004
//    public void testJobWithPrerequisiteJobs() throws Exception
//    {
//        numTestItemsToGenerate = 2;                 // CAF-3677: Remove this on fix
//        testItemAssetIds = generateWorkerBatch();   // CAF-3677: Remove this on fix
//
//        //  Generate job identifiers for test.
//        final String job1Id = generateJobId();
//        final String job2Id = generateJobId();
//        final String job3Id = generateJobId();
//        final String job4Id = generateJobId();
//
//        //  Create job hierarchy.
//        //
//        //  J1
//        //  -> J2
//        //      -> J3
//        //      -> J4
//        createJobWithPrerequisites(job2Id, true, 0, job1Id);
//        //  Verify J2 is in 'waiting' state and job dependency rows exist as expected.
//        JobServiceDatabaseUtil.assertJobStatus(job2Id, "waiting");
//        JobServiceDatabaseUtil.assertJobDependencyRowsExist(job2Id, job1Id, batchWorkerMessageInQueue, exampleWorkerMessageOutQueue);
//
//        createJobWithPrerequisites(job3Id, true, 0, job2Id);
//        //  Verify J3 is in 'waiting' state and job dependency rows exist as expected.
//        JobServiceDatabaseUtil.assertJobStatus(job3Id, "waiting");
//        JobServiceDatabaseUtil.assertJobDependencyRowsExist(job3Id, job2Id, batchWorkerMessageInQueue, exampleWorkerMessageOutQueue);
//
//        createJobWithPrerequisites(job4Id, true, 0, job2Id);
//        //  Verify J4 is in 'waiting' state and job dependency rows exist as expected.
//        JobServiceDatabaseUtil.assertJobStatus(job4Id, "waiting");
//        JobServiceDatabaseUtil.assertJobDependencyRowsExist(job4Id, job2Id, batchWorkerMessageInQueue, exampleWorkerMessageOutQueue);
//
//        //  Add a Prerequisite job 1 that should be completed. This should trigger the completion of all
//        //  other jobs.
//        JobServiceEndToEndITExpectation job1Expectation =
//                new JobServiceEndToEndITExpectation(
//                        false,
//                        exampleWorkerMessageOutQueue,
//                    defaultPartitionId,
//                        job2Id,
//                        jobCorrelationId,
//                        ExampleWorkerConstants.WORKER_NAME,
//                        ExampleWorkerConstants.WORKER_API_VER,
//                        TaskStatus.RESULT_SUCCESS,
//                        ExampleWorkerStatus.COMPLETED,
//                        testItemAssetIds);
//
//        try (QueueManager queueManager = getFinalQueueManager()) {
//            ExecutionContext context = new ExecutionContext(false);
//            context.initializeContext();
//            getTimer(context);
//            queueManager.start(new FinalOutputDeliveryHandler(workerServices.getCodec(), jobsApi, context,
//                    job1Expectation));
//
//            createJob(job1Id, true);
//
//            // Waits for the final result message to appear on the Example worker's output queue.
//            // When we read it from this queue it should have been processed fully and its status reported to the Job
//            // Database as Completed.
//            context.getTestResult();
//        }
//
//        //Wait for the jobs to complete
//        waitUntilJobCompletes(job1Id);
//        waitUntilJobCompletes(job2Id);
//        waitUntilJobCompletes(job3Id);
//        waitUntilJobCompletes(job4Id);
//
//        // Call getJob to trigger the subtask completion
//        jobsApi.getJob(defaultPartitionId, job3Id, jobCorrelationId);
//        jobsApi.getJob(defaultPartitionId, job4Id, jobCorrelationId);
//
//        //  Now that J1 has completed, verify this has triggered the completion of other jobs created
//        //  with a prerequisite.
//        JobServiceDatabaseUtil.assertJobStatus(job2Id, "completed");
//        JobServiceDatabaseUtil.assertJobDependencyRowsDoNotExist(job2Id, job1Id);
//        JobServiceDatabaseUtil.assertJobStatus(job3Id, "completed");
//        JobServiceDatabaseUtil.assertJobDependencyRowsDoNotExist(job3Id, job2Id);
//        JobServiceDatabaseUtil.assertJobStatus(job4Id, "completed");
//        JobServiceDatabaseUtil.assertJobDependencyRowsDoNotExist(job4Id, job2Id);
//    }
//
//    @Test(enabled = false) // See https://portal.digitalsafe.net/browse/SCMOD-13004
//    public void testJobWithPrerequisiteJobsAndDelays() throws Exception
//    {
//        numTestItemsToGenerate = 2;                 // CAF-3677: Remove this on fix
//        testItemAssetIds = generateWorkerBatch();   // CAF-3677: Remove this on fix
//
//        //  Generate job identifiers for test.
//        final String job1Id = generateJobId();
//        final String job2Id = generateJobId();
//        final String job3Id = generateJobId();
//
//        //  Create job hierarchy.
//        //
//        //  J1
//        //  -> J2 (delay=2s)
//        //      -> J3 (delay=10s)
//        createJobWithPrerequisites(job2Id, true, 2, job1Id);
//        //  Verify J2 is in 'waiting' state and job dependency rows exist as expected.
//        JobServiceDatabaseUtil.assertJobStatus(job2Id, "waiting");
//        JobServiceDatabaseUtil.assertJobDependencyRowsExist(job2Id, job1Id, batchWorkerMessageInQueue, exampleWorkerMessageOutQueue);
//        Assert.assertTrue(JobServiceDatabaseUtil.getJobDelay(job2Id) == 2, "Job "+job2Id+" was not delayed by 2s");
//        Assert.assertTrue(JobServiceDatabaseUtil.getJobTaskDataEligibleRunDate(job2Id) == null, "Job "+job2Id+" eligible_to_run_date value is not null");
//
//        createJobWithPrerequisites(job3Id, true, 10, job2Id);
//        //  Verify J3 is in 'waiting' state and job dependency rows exist as expected.
//        JobServiceDatabaseUtil.assertJobStatus(job3Id, "waiting");
//        JobServiceDatabaseUtil.assertJobDependencyRowsExist(job3Id, job2Id, batchWorkerMessageInQueue, exampleWorkerMessageOutQueue);
//        Assert.assertTrue(JobServiceDatabaseUtil.getJobDelay(job3Id) == 10, "Job "+job3Id+" was not delayed by 10s");
//        Assert.assertTrue(JobServiceDatabaseUtil.getJobTaskDataEligibleRunDate(job3Id) == null, "Job "+job3Id+" eligible_to_run_date value is not null");
//
//        //  Add a Prerequisite job 1 that should be completed. This should trigger the completion of all
//        //  other jobs eventually after all the delays have been respected.
//        JobServiceEndToEndITExpectation job1Expectation =
//                new JobServiceEndToEndITExpectation(
//                        false,
//                        exampleWorkerMessageOutQueue,
//                    defaultPartitionId,
//                        job2Id,
//                        jobCorrelationId,
//                        ExampleWorkerConstants.WORKER_NAME,
//                        ExampleWorkerConstants.WORKER_API_VER,
//                        TaskStatus.RESULT_SUCCESS,
//                        ExampleWorkerStatus.COMPLETED,
//                        testItemAssetIds);
//
//        try (QueueManager queueManager = getFinalQueueManager()) {
//            ExecutionContext context = new ExecutionContext(false);
//            context.initializeContext();
//            getTimer(context);
//            queueManager.start(new FinalOutputDeliveryHandler(workerServices.getCodec(), jobsApi, context,
//                    job1Expectation));
//
//            createJob(job1Id, true);
//
//            // Waits for the final result message to appear on the Example worker's output queue.
//            // When we read it from this queue it should have been processed fully and its status reported to the Job
//            // Database as Completed.
//            context.getTestResult();
//        }
//
//        //Wait for the jobs to complete
//        waitUntilJobCompletes(job1Id);
//        waitUntilJobCompletes(job2Id);
//
//        //  Verify J2 is complete but J3 is still waiting.
//        JobServiceDatabaseUtil.assertJobStatus(job2Id, "completed");
//        JobServiceDatabaseUtil.assertJobStatus(job3Id, "waiting");
//        Assert.assertTrue(JobServiceDatabaseUtil.getJobTaskDataEligibleRunDate(job3Id) != null, "Job "+job3Id+" eligible_to_run_date value is null");
//
//        //Wait for the job to complete
//        waitUntilJobCompletes(job3Id);
//
//        // Call getJob to trigger the subtask completion
//        jobsApi.getJob(defaultPartitionId, job3Id, jobCorrelationId);
//
//        //  Verify J3 is complete.
//        JobServiceDatabaseUtil.assertJobStatus(job3Id, "completed");
//        JobServiceDatabaseUtil.assertJobDependencyRowsDoNotExist(job3Id, job2Id);
//
//        //  Verify dependency rows do not exist.
//        JobServiceDatabaseUtil.assertJobDependencyRowsDoNotExist(job2Id, job1Id);
//        JobServiceDatabaseUtil.assertJobDependencyRowsDoNotExist(job3Id, job2Id);
//    }
//
//    @Test
//    public void testCreateJobLongDelay() throws Exception {
//        //create a job for a suspended partition
//        final String jobId = generateJobId();
//        final String jobCorrelationId = "1";
//        final NewJob newJob = constructNewJob(jobId, true);
//        newJob.setDelay(6000);
//        final String partitionId = "tenant-abc-com";
//        jobsApi.createOrUpdateJob(partitionId, jobId, newJob, jobCorrelationId);
//
//        //retrieve job using web method
//        final Job retrievedJob = jobsApi.getJob(partitionId, jobId, jobCorrelationId);
//        final boolean canRun = JobServiceDatabaseUtil.isJobEligibleToRun(retrievedJob.getId());
//        LOG.info("--testCreateJobLongDelay job {} in partition: {}, canRun? {}", retrievedJob.getId(), partitionId, canRun);
//        assertEquals(canRun, false, "Job "+jobId+" is eligible to run despite a configured delay");
//    }
//
//    @Test
//    public void testCreateJobNoDelayNoPreReq() throws Exception {
//        //create a job eligible for running
//        final String jobId = generateJobId();
//        final String jobCorrelationId = "1";
//        final NewJob newJob = constructNewJob(jobId, true);
//        final String partitionId = "tenant-allclear-com";
//        jobsApi.createOrUpdateJob(partitionId, jobId, newJob, jobCorrelationId);
//
//        //retrieve job using web method
//        final Job retrievedJob = jobsApi.getJob(partitionId, jobId, jobCorrelationId);
//        LOG.info("--testCreateJobNoDelayNoPreReq job {} in partition: {}", retrievedJob.getId(), partitionId);
//        JobServiceDatabaseUtil.assertJobTaskDataRowDoesNotExist(retrievedJob.getId());
//    }
//
//    @Test
//    public void testCreateJobNoDelayAndSomePreReq() throws Exception {
//        //create a job with some pre req
//        final String preReqJobId = generateJobId();
//        final String job1Id = generateJobId();
//        final String jobCorrelationId = "1";
//        final String partitionId = "tenant-hasprereq-com";
//        createJobWithPrerequisites(partitionId, job1Id, true, preReqJobId);
//        //  Verify J1 is in 'waiting' state and job dependency rows exist as expected.
//        JobServiceDatabaseUtil.assertJobStatus(job1Id, "waiting");
//        JobServiceDatabaseUtil.assertJobDependencyRowsExist(job1Id, preReqJobId, batchWorkerMessageInQueue, exampleWorkerMessageOutQueue);
//        Assert.assertTrue(JobServiceDatabaseUtil.getJobDelay(job1Id) == 0, "Job "+job1Id+" has an unexpected delay configured");
//        final String job1EligibleRunDate = JobServiceDatabaseUtil.getJobTaskDataEligibleRunDate(job1Id);
//        LOG.info("--testCreateJobNoDelayAndSomePreReq : job1EligibleRunDate: ", job1EligibleRunDate);
//        Assert.assertTrue(job1EligibleRunDate == null, "Job "+job1Id+" eligible_to_run_date value is not null");
//
//        final NewJob preReqJobJob = constructNewJob(preReqJobId, true);
//        jobsApi.createOrUpdateJob(partitionId, preReqJobId, preReqJobJob, jobCorrelationId);
//
//        final boolean canRun = JobServiceDatabaseUtil.isJobEligibleToRun(job1Id);
//        LOG.info("--testCreateJobNoDelayAndSomePreReq job {} in partition: {}, canRun? {}", job1Id, partitionId, canRun);
//        assertEquals(canRun, false, "Job "+job1Id+" is eligible to run despite incomplete prerequisite jobs");
//
//        JobServiceDatabaseUtil.assertJobTaskDataRowDoesNotExist(preReqJobId);
//    }
//
//    @Test
//    public void testCreateJobNoDelayAndSomePreReqWithDelay() throws Exception {
//        //create a job with some pre req which has a delay
//        final String preReqJobId = generateJobId();
//        final String job1Id = generateJobId();
//        final String partitionId = "tenant-prereqdelay-com";
//        createJobWithPrerequisites(partitionId, job1Id, true, preReqJobId);
//        //  Verify J1 is in 'waiting' state and job dependency rows exist as expected.
//        JobServiceDatabaseUtil.assertJobStatus(job1Id, "waiting");
//        JobServiceDatabaseUtil.assertJobDependencyRowsExist(job1Id, preReqJobId, batchWorkerMessageInQueue, exampleWorkerMessageOutQueue);
//        Assert.assertTrue(JobServiceDatabaseUtil.getJobDelay(job1Id) == 0, "Job "+job1Id+" has an unexpected delay configured.");
//        final String job1EligibleRunDate = JobServiceDatabaseUtil.getJobTaskDataEligibleRunDate(job1Id);
//        LOG.info("--testCreateJobNoDelayAndSomePreReqWithDelay : job1EligibleRunDate: ", job1EligibleRunDate);
//        Assert.assertTrue(job1EligibleRunDate == null, "Job "+job1Id+" eligible_to_run_date value is not null");
//
//        createJobWithDelay(partitionId, preReqJobId, true, 15);
//
//        final boolean canRun = JobServiceDatabaseUtil.isJobEligibleToRun(job1Id);
//        LOG.info("--testCreateJobNoDelayAndSomePreReqWithDelay job {} in partition: {}, canRun? {}", job1Id, partitionId, canRun);
//        assertEquals(canRun, false, "Job "+job1Id+" is eligible to run despite incomplete prerequisite jobs");
//
//        final boolean preReqJobCanRun = JobServiceDatabaseUtil.isJobEligibleToRun(preReqJobId);
//        LOG.info("--testCreateJobNoDelayAndSomePreReqWithDelay job {} in partition: {}, canRun? {}",
//                preReqJobId, partitionId, preReqJobCanRun);
//        assertEquals(preReqJobCanRun, false, "Job "+preReqJobId+" is eligible to run despite configured delay");
//    }
//
//    @Test
//    public void testCreateJobSuspendedPartition() throws Exception {
//        //create a job for a suspended partition
//        final String jobId = generateJobId();
//        final String jobCorrelationId = "1";
//        final NewJob newJob = constructNewJob(jobId, true);
//        final String partitionId = "tenant-acme-com";
//        jobsApi.createOrUpdateJob(partitionId, jobId, newJob, jobCorrelationId);
//
//        //retrieve job using web method
//        final Job retrievedJob = jobsApi.getJob(partitionId, jobId, jobCorrelationId);
//        final boolean canRun = JobServiceDatabaseUtil.isJobEligibleToRun(retrievedJob.getId());
//        LOG.info("--testCreateJobSuspendedPartition job {} in partition: {}, canRun? {}", retrievedJob.getId(), partitionId, canRun);
//        assertEquals(canRun, false, "Job "+jobId+" is eligible to run despite being in a suspended partition");
//    }
//
//    @Test
//    public void testCreateJobWithPreReqSuspendedPartition() throws Exception {
//        //create a job with pre reqs for a suspended partition
//        final String jobId = generateJobId();
//        final String jobCorrelationId = "1";
//        final NewJob newJob = constructNewJob(jobId, true);
//        newJob.setPrerequisiteJobIds(Collections.singletonList(UUID.randomUUID().toString()));
//        final String partitionId = "tenant-acme-co";
//        jobsApi.createOrUpdateJob(partitionId, jobId, newJob, jobCorrelationId);
//
//        //retrieve job using web method
//        final Job retrievedJob = jobsApi.getJob(partitionId, jobId, jobCorrelationId);
//        final boolean canRun = JobServiceDatabaseUtil.isJobEligibleToRun(retrievedJob.getId());
//        LOG.info("--testCreateJobWithPreReqSuspendedPartition job {} in partition: {}, canRun? {}",
//                retrievedJob.getId(), partitionId, canRun);
//        assertEquals(canRun, false, "Job "+jobId+" is eligible to run despite being in a suspended partition and incomplete prerequisite jobs");
//    }
//
//    @Test
//    public void testCreateJobWithDelaySuspendedPartition() throws Exception {
//        //create a job with delay for a suspended partition
//        final String jobId = generateJobId();
//        final String jobCorrelationId = "1";
//        final NewJob newJob = constructNewJob(jobId, true);
//        newJob.setDelay(600); // 600s
//        final String partitionId = "tenant-acme-com";
//        jobsApi.createOrUpdateJob(partitionId, jobId, newJob, jobCorrelationId);
//
//        //retrieve job using web method
//        final Job retrievedJob = jobsApi.getJob(partitionId, jobId, jobCorrelationId);
//        final boolean canRun = JobServiceDatabaseUtil.isJobEligibleToRun(retrievedJob.getId());
//        LOG.info("--testCreateJobWithDelaySuspendedPartition job {} in partition: {}, canRun? {}",
//                retrievedJob.getId(), partitionId, canRun);
//        assertEquals(canRun, false, "Job "+jobId+" is eligible to run despite being in a suspended partition and a configured delay");
//    }
//
//    @Test
//    public void testSuspendedJobWithPrerequisiteJobsAndDelays() throws Exception
//    {
//        numTestItemsToGenerate = 2;                 // CAF-3677: Remove this on fix
//        testItemAssetIds = generateWorkerBatch();   // CAF-3677: Remove this on fix
//
//        final String partitionId = "tenant-acme-corp";
//
//        //  Generate job identifiers for test.
//        final String job1Id = generateJobId();
//        final String job2Id = generateJobId();
//        final String job3Id = generateJobId();
//
//        //  Create job hierarchy.
//        //
//        //  J1
//        //  -> J2 (delay=2s)
//        //      -> J3 (delay=10s)
//        createJobWithPrerequisitesAndDelay(partitionId, job2Id, true, 2, job1Id);
//        //  Verify J2 is in 'waiting' state and job dependency rows exist as expected.
//        JobServiceDatabaseUtil.assertJobStatus(job2Id, "waiting");
//        JobServiceDatabaseUtil.assertJobDependencyRowsExist(job2Id, job1Id, batchWorkerMessageInQueue, exampleWorkerMessageOutQueue);
//        Assert.assertTrue(JobServiceDatabaseUtil.getJobDelay(job2Id) == 2, "Job "+job2Id+" was not delayed for 2s");
//        final String job2EligibleRunDate = JobServiceDatabaseUtil.getJobTaskDataEligibleRunDate(job2Id);
//        LOG.info("--testSuspendedJobWithPrerequisiteJobsAndDelays : job2EligibleRunDate: ", job2EligibleRunDate);
//        Assert.assertTrue(job2EligibleRunDate == null, "Job "+job2Id+" eligible_to_run_date value is not null");
//
//        createJobWithPrerequisitesAndDelay(partitionId, job3Id, true, 10, job2Id);
//        //  Verify J3 is in 'waiting' state and job dependency rows exist as expected.
//        JobServiceDatabaseUtil.assertJobStatus(job3Id, "waiting");
//        JobServiceDatabaseUtil.assertJobDependencyRowsExist(job3Id, job2Id, batchWorkerMessageInQueue, exampleWorkerMessageOutQueue);
//        Assert.assertTrue(JobServiceDatabaseUtil.getJobDelay(job3Id) == 10, "Job "+job3Id+" was not delayed for 10s");
//        String job3EligibleRunDate = JobServiceDatabaseUtil.getJobTaskDataEligibleRunDate(job3Id);
//        LOG.info("--testSuspendedJobWithPrerequisiteJobsAndDelays : job3EligibleRunDate: ", job3EligibleRunDate);
//        Assert.assertTrue(job3EligibleRunDate == null, "Job "+job3Id+" eligible_to_run_date value is not null");
//
//        final Map<String, String> labels = new HashMap<>();
//        labels.put("tag:4", "4");
//        labels.put("tag:7", "7");
//
//        createJobWithLabels(partitionId, job1Id, true, labels);
//
//        LOG.info("--testSuspendedJobWithPrerequisiteJobsAndDelays check if task_data rows are created for jobs in partition: {}",
//                partitionId);
//
//        JobServiceDatabaseUtil.assertJobTaskDataRowExists(job1Id);
//        JobServiceDatabaseUtil.assertJobTaskDataRowExists(job2Id);
//        JobServiceDatabaseUtil.assertJobTaskDataRowExists(job3Id);
//
//        final boolean canRunJob1 = JobServiceDatabaseUtil.isJobEligibleToRun(job1Id);
//        LOG.info("--testSuspendedJobWithPrerequisiteJobsAndDelays job1 {} in partition: {}, canRun? {}",
//                job1Id, partitionId, canRunJob1);
//        final boolean canRunJob2 = JobServiceDatabaseUtil.isJobEligibleToRun(job2Id);
//        LOG.info("--testSuspendedJobWithPrerequisiteJobsAndDelays job2 {} in partition: {}, canRun? {}",
//                job2Id, partitionId, canRunJob2);
//        final boolean canRunJob3 = JobServiceDatabaseUtil.isJobEligibleToRun(job3Id);
//        LOG.info("--testSuspendedJobWithPrerequisiteJobsAndDelays job3 {} in partition: {}, canRun? {}",
//                job3Id, partitionId, canRunJob3);
//        assertEquals(canRunJob1, false, "Job "+job1Id+" is eligible to run despite being in a suspended partition");
//        assertEquals(canRunJob2, false, "Job "+job2Id+" is eligible to run despite prerequisite "+job1Id+" failed");
//        assertEquals(canRunJob3, false, "Job "+job3Id+" is eligible to run despite prerequisite "+job2Id+" failed");
//    }
//
//    @Test(enabled = false) // See https://portal.digitalsafe.net/browse/SCMOD-13004
//    public void testJobCancellation() throws Exception {
//        testJobCancellation(false);
//    }
//
//    @Test(enabled = false) // See https://portal.digitalsafe.net/browse/SCMOD-13004
//    public void testJobCancellationWithTaskDataObject() throws Exception {
//        testJobCancellation(true);
//    }
//
//    private void testJobCancellation(final boolean useTaskDataObject) throws Exception {
//        final String jobId = generateJobId();
//
//        JobServiceEndToEndITExpectation expectation =
//                new JobServiceEndToEndITExpectation(
//                        true,
//                        exampleWorkerMessageOutQueue,
//                    defaultPartitionId,
//                        jobId,
//                        jobCorrelationId,
//                        ExampleWorkerConstants.WORKER_NAME,
//                        ExampleWorkerConstants.WORKER_API_VER,
//                        TaskStatus.RESULT_SUCCESS,
//                        ExampleWorkerStatus.COMPLETED,
//                        testItemAssetIds);
//
//        try (QueueManager queueManager = getFinalQueueManager()) {
//            ExecutionContext context = new ExecutionContext(false);
//            context.initializeContext();
//            Timer timer = getTimer(context);
//            Thread thread = queueManager.start(new FinalOutputDeliveryHandler(workerServices.getCodec(), jobsApi, context, expectation));
//
//            createJob(jobId, useTaskDataObject);
//
//            cancelJob(jobId);
//
//            TestResult result = context.getTestResult();
//            Assert.assertTrue(result.isSuccess(), "Job "+jobId+ "was not cancelled" );
//        }
//    }
//
//    @Test(enabled = false) // See https://portal.digitalsafe.net/browse/SCMOD-13004
//    @SuppressWarnings("unchecked")
//    public void testJobServiceCaller_Success() throws ParseException, IOException, TimeoutException {
//
//        LOG.debug("Starting testJobServiceCaller_Success() ...");
//
//        //  Generate job identifier.
//        final String jobId = generateJobId();
//
//        List<String> testItemAssetIds = new ArrayList<>();
//        testItemAssetIds.add("TestItemAssetId");
//
//        JobServiceEndToEndITExpectation expectation =
//                new JobServiceEndToEndITExpectation(
//                        false,
//                        exampleWorkerMessageOutQueue,
//                    defaultPartitionId,
//                        jobId,
//                        jobCorrelationId,
//                        ExampleWorkerConstants.WORKER_NAME,
//                        ExampleWorkerConstants.WORKER_API_VER,
//                        TaskStatus.RESULT_SUCCESS,
//                        ExampleWorkerStatus.COMPLETED,
//                        testItemAssetIds);
//
//        try (QueueManager queueManager = getFinalQueueManager()) {
//            ExecutionContext context = new ExecutionContext(false);
//            context.initializeContext();
//            Timer timer = getTimer(context);
//            Thread thread = queueManager.start(new FinalOutputDeliveryHandler(workerServices.getCodec(), jobsApi, context, expectation));
//
//            //  Identify name of test data container as  we need to set up VolumesFrom to access the test data file.
//            String jobDefinitionContainerName = JobServiceCallerTestsHelper.getJobDefinitionContainerName(jobDefinitionContainerJSON, dockerContainersURL);
//
//            //  Parse the job service caller container json string. This JSON will need modified for the test.
//            JSONObject createContainerObject = JobServiceCallerTestsHelper.parseJson(jobServiceCallerContainerJSON);
//
//            //  Identify link name for the job-service container.
//            String jobServiceContainerLinkName = JobServiceCallerTestsHelper.getJobServiceContainerLinkName(jobServiceImage, jobServiceAdminPort, dockerContainersURL);
//
//            //  Before job service caller container can be started, a number of changes to the container JSON needs to be made including Cmd, HostConfig and Image.
//            //  Configure Cmd (i.e. modify job identifier, web service url, polling interval and job definition file that will be passed to the containerized script).
//            JSONArray cmd = new JSONArray();
//            cmd.add(0, "-P");
//            cmd.add(1, defaultPartitionId);
//            cmd.add(2, "-j");
//            cmd.add(3, jobId);
//            cmd.add(4, "-u");
//            cmd.add(5, jobServiceCallerWebServiceLinkURL);
//            cmd.add(6, "-p");
//            cmd.add(7, pollingInterval);
//            cmd.add(8, "-f");
//            cmd.add(9, "/jobDefinition/" + jobDefinitionFile);
//            createContainerObject.put("Cmd", cmd);
//
//            //  Configure HostConfig
//            JSONObject hostConfig = (JSONObject) createContainerObject.get("HostConfig");
//
//            JSONArray links = new JSONArray();
//            links.add(jobServiceContainerLinkName);
//            hostConfig.put("Links", links);
//
//            JSONArray volumesFrom = new JSONArray();
//            volumesFrom.add(jobDefinitionContainerName);
//            hostConfig.put("VolumesFrom", volumesFrom);
//            createContainerObject.put("HostConfig", hostConfig);
//
//            //  Configure Image.
//            createContainerObject.put("Image", jobServiceCallerImage);
//
//            //  Send POST request to create the job service caller container.
//            String createContainerURL = dockerContainersURL + "create";
//            String sendCreateContainerPostResponse = JobServiceCallerTestsHelper.sendPOST(createContainerURL,createContainerObject.toJSONString().replace("\\/","/"), "application/json", "gzip");
//
//            //  Get container id from response object.
//            JSONObject createContainerResponse = JobServiceCallerTestsHelper.parseJson(sendCreateContainerPostResponse);
//            String id = (String) createContainerResponse.get("Id");
//
//            //  Use container id to send POST to start the container.
//            String startContainerURL = dockerContainersURL + id + "/start";
//            JobServiceCallerTestsHelper.sendPOST(startContainerURL,"","text/plain", "gzip");
//
//            //  Use container id to send POST request to wait on container response.
//            String waitContainerURL = dockerContainersURL + id + "/wait";
//            String sendWaitContainerPostResponse = JobServiceCallerTestsHelper.sendPOST(waitContainerURL,"", "text/plain", "gzip");
//
//            // Get status code.
//            JSONObject waitContainerResponse = JobServiceCallerTestsHelper.parseJson(sendWaitContainerPostResponse);
//            long statusCode = (long) waitContainerResponse.get("StatusCode");
//
//            //  Expecting StatusCode=0 for success.
//            Assert.assertNotNull(statusCode, "Job "+jobId+" status code is null");
//            Assert.assertEquals(statusCode, 0L, "Status code for Job "+jobId+" is not 0.");
//
//            // Waits for the final result message to appear on the Example worker's output queue.
//            // When we read it from this queue it should have been processed fully and its status reported to the Job Database as Completed.
//            TestResult result = context.getTestResult();
//            Assert.assertTrue(result.isSuccess(), "Job "+jobId+" was not completed successfully.");
//        } catch (Exception e){
//            LOG.error("Error while running testJobServiceCaller_Success().", e);
//            throw e;
//        }
//
//        LOG.debug("Finished testJobServiceCaller_Success().");
//    }
//
//    @Test
//    @SuppressWarnings("unchecked")
//    public void testJobServiceCaller_Failure() throws ParseException, IOException, TimeoutException {
//        LOG.debug("Starting testJobServiceCaller_Failure() ...");
//
//        try {
//            //  Generate job identifier.
//            final String jobId = generateJobId();
//
//            //  Identify name of test data container as  we need to set up VolumesFrom to access the test data file.
//            String jobDefinitionContainerName = JobServiceCallerTestsHelper.getJobDefinitionContainerName(jobDefinitionContainerJSON, dockerContainersURL);
//
//            //  Parse the job service caller container json string.
//            JSONObject createContainerObject = JobServiceCallerTestsHelper.parseJson(jobServiceCallerContainerJSON);
//
//            //  Identify link name for the job-service container.
//            String jobServiceContainerLinkName = JobServiceCallerTestsHelper.getJobServiceContainerLinkName(jobServiceImage, jobServiceAdminPort, dockerContainersURL);
//
//            //  Modify job identifier, polling interval and job definition file. Leave web service url as we want to force an error.
//            JSONArray cmd = new JSONArray();
//            cmd.add(0, "-j");
//            cmd.add(1, jobId);
//            cmd.add(2, "-p");
//            cmd.add(3, pollingInterval);
//            cmd.add(4, "-f");
//            cmd.add(5, "/jobDefinition/" + jobDefinitionFile);
//            createContainerObject.put("Cmd", cmd);
//
//            //  Configure HostConfig
//            JSONObject hostConfig = (JSONObject) createContainerObject.get("HostConfig");
//
//            JSONArray links = new JSONArray();
//            links.add(jobServiceContainerLinkName);
//            hostConfig.put("Links", links);
//
//            JSONArray volumesFrom = new JSONArray();
//            volumesFrom.add(jobDefinitionContainerName);
//            hostConfig.put("VolumesFrom", volumesFrom);
//
//            createContainerObject.put("HostConfig", hostConfig);
//
//            //  Configure Image.
//            createContainerObject.put("Image", jobServiceCallerImage);
//
//            //  Send POST request to create the job service caller container.
//            String createContainerURL = dockerContainersURL + "create";
//            String sendCreateContainerPostResponse = JobServiceCallerTestsHelper.sendPOST(createContainerURL, createContainerObject.toJSONString().replace("\\/", "/"), "application/json", "gzip");
//
//            //  Get container id from response object.
//            JSONObject createContainerResponse = JobServiceCallerTestsHelper.parseJson(sendCreateContainerPostResponse);
//            String id = (String) createContainerResponse.get("Id");
//
//            //  Use container id to send POST to start the container.
//            String startContainerURL = dockerContainersURL + id + "/start";
//            JobServiceCallerTestsHelper.sendPOST(startContainerURL, "", "text/plain", "gzip");
//
//            //  Use container id to send POST request to wait on container response.
//            String waitContainerURL = dockerContainersURL + id + "/wait";
//            String sendWaitContainerPostResponse = JobServiceCallerTestsHelper.sendPOST(waitContainerURL, "", "text/plain", "gzip");
//
//            // Get status code.
//            JSONObject waitContainerResponse = JobServiceCallerTestsHelper.parseJson(sendWaitContainerPostResponse);
//            long statusCode = (long) waitContainerResponse.get("StatusCode");
//
//            //  Expecting StatusCode > 0 for failure.
//            Assert.assertNotNull(statusCode, "Job "+jobId+" status code is null.");
//            Assert.assertTrue(statusCode > 0, "Job "+jobId+" status code is not greater than 0.");
//        } catch (Exception e){
//            LOG.error("Error while running testJobServiceCaller_Failure().", e);
//            throw e;
//        }
//
//        LOG.debug("Finished testJobServiceCaller_Failure().");
//    }
//
//    @Test(enabled = false) // See https://portal.digitalsafe.net/browse/SCMOD-13004
//    public void testJobDeletion() throws Exception {
//        testJobDeletion(false);
//    }
//
//    @Test(enabled = false) // See https://portal.digitalsafe.net/browse/SCMOD-13004
//    public void testJobDeletionWithTaskDataObject() throws Exception {
//        testJobDeletion(true);
//    }
//
//    private void testJobDeletion(final boolean useTaskDataObject) throws Exception {
//        numTestItemsToGenerate = 2;                 // CAF-3677: Remove this on fix
//        testItemAssetIds = generateWorkerBatch();   // CAF-3677: Remove this on fix
//
//        final String jobId = generateJobId();
//
//        JobServiceEndToEndITExpectation expectation =
//                new JobServiceEndToEndITExpectation(
//                        false,
//                        exampleWorkerMessageOutQueue,
//                    defaultPartitionId,
//                        jobId,
//                        jobCorrelationId,
//                        ExampleWorkerConstants.WORKER_NAME,
//                        ExampleWorkerConstants.WORKER_API_VER,
//                        TaskStatus.RESULT_SUCCESS,
//                        ExampleWorkerStatus.COMPLETED,
//                        testItemAssetIds);
//
//        try (QueueManager queueManager = getFinalQueueManager()) {
//            ExecutionContext context = new ExecutionContext(false);
//            context.initializeContext();
//            Timer timer = getTimer(context);
//            Thread thread = queueManager.start(new FinalOutputDeliveryHandler(workerServices.getCodec(), jobsApi, context, expectation));
//
//            createJob(jobId, useTaskDataObject);
//            JobServiceDatabaseUtil.assertJobRowExists(jobId);
//
//            TestResult result = context.getTestResult();
//            Assert.assertTrue(result.isSuccess(), "Job "+jobId+" was not completed successfully");
//        }
//
//        deleteJob(jobId);
//        JobServiceDatabaseUtil.assertJobRowDoesNotExist(jobId);
//    }
//
//    @Test
//    public void testJobDeletionWithPrerequisiteJobs() throws Exception
//    {
//        numTestItemsToGenerate = 2;                 // CAF-3677: Remove this on fix
//        testItemAssetIds = generateWorkerBatch();   // CAF-3677: Remove this on fix
//
//        //  Generate job identifiers for test.
//        final String job1Id = generateJobId();
//        final String job2Id = generateJobId();
//        final String job3Id = generateJobId();
//        final String job4Id = generateJobId();
//
//        //  Create job hierarchy.
//        //
//        //  J1
//        //  -> J2
//        //      -> J3
//        //      -> J4
//        createJobWithPrerequisites(job2Id, true, 0, job1Id);
//        createJobWithPrerequisites(job3Id, true, 0, job2Id);
//        createJobWithPrerequisites(job4Id, true, 0, job2Id);
//
//        //  Delete J2.
//        deleteJob(job2Id);
//
//        //  Verify J2 rows have been removed.
//        JobServiceDatabaseUtil.assertJobRowDoesNotExist(job2Id);
//        JobServiceDatabaseUtil.assertJobDependencyRowsDoNotExist(job2Id, job1Id);
//
//        //  Verify rows for J3 & J4 still exist.
//        JobServiceDatabaseUtil.assertJobRowExists(job3Id);
//        JobServiceDatabaseUtil.assertJobStatus(job3Id, "waiting");
//        JobServiceDatabaseUtil.assertJobDependencyRowsExist(job3Id, job2Id, batchWorkerMessageInQueue, exampleWorkerMessageOutQueue);
//        JobServiceDatabaseUtil.assertJobRowExists(job4Id);
//        JobServiceDatabaseUtil.assertJobStatus(job4Id, "waiting");
//        JobServiceDatabaseUtil.assertJobDependencyRowsExist(job4Id, job2Id, batchWorkerMessageInQueue, exampleWorkerMessageOutQueue);
//    }
//
//    @Test(enabled = false) // See https://portal.digitalsafe.net/browse/SCMOD-13004
//    public void testJobCompletionInsertsRecordsInDeleteLog() throws Exception
//    {
//        numTestItemsToGenerate = 2;
//        testItemAssetIds = generateWorkerBatch();
//
//        //  Generate job identifiers for test.
//        final String job1Id = generateJobId();
//        final String job2Id = generateJobId();
//
//        //  Job hierarchy.
//        //
//        //  J1
//        //  -> J2
//
//        // Add a Prerequisite job 2 that should be completed
//        JobServiceEndToEndITExpectation job2Expectation =
//                new JobServiceEndToEndITExpectation(
//                        false,
//                        exampleWorkerMessageOutQueue,
//                        defaultPartitionId,
//                        job2Id,
//                        jobCorrelationId,
//                        ExampleWorkerConstants.WORKER_NAME,
//                        ExampleWorkerConstants.WORKER_API_VER,
//                        TaskStatus.RESULT_SUCCESS,
//                        ExampleWorkerStatus.COMPLETED,
//                        testItemAssetIds);
//
//        try (QueueManager queueManager = getFinalQueueManager()) {
//            ExecutionContext context = new ExecutionContext(false);
//            context.initializeContext();
//            getTimer(context);
//            queueManager.start(new FinalOutputDeliveryHandler(workerServices.getCodec(), jobsApi, context,
//                    job2Expectation));
//
//            createJobWithPrerequisites(job2Id, true, 0, job1Id);
//        }
//
//        JobServiceEndToEndITExpectation job1Expectation =
//                new JobServiceEndToEndITExpectation(
//                        false,
//                        exampleWorkerMessageOutQueue,
//                        defaultPartitionId,
//                        job1Id,
//                        jobCorrelationId,
//                        ExampleWorkerConstants.WORKER_NAME,
//                        ExampleWorkerConstants.WORKER_API_VER,
//                        TaskStatus.RESULT_SUCCESS,
//                        ExampleWorkerStatus.COMPLETED,
//                        testItemAssetIds);
//
//        try (QueueManager queueManager = getFinalQueueManager()) {
//            ExecutionContext context = new ExecutionContext(false);
//            context.initializeContext();
//            getTimer(context);
//            queueManager.start(new FinalOutputDeliveryHandler(workerServices.getCodec(), jobsApi, context,
//                    job1Expectation));
//
//            createJob(job1Id, true);
//        }
//
//        waitUntilJobCompletes(job1Id);
//        waitUntilJobCompletes(job2Id);
//
//        //assert
//        JobServiceDatabaseUtil.assertDeleteLogNotEmpty();
//    }
//
//    @Test
//    public void testJobDeletionWithLabels() throws Exception {
//        numTestItemsToGenerate = 2;                 // CAF-3677: Remove this on fix
//        testItemAssetIds = generateWorkerBatch();   // CAF-3677: Remove this on fix
//
//        //  Generate job identifiers for test.
//        final String job1Id = generateJobId();
//
//        final Map<String, String> labels = new HashMap<>();
//        labels.put("tag:4", "4");
//        labels.put("tag:7", "7");
//        createJobWithLabels(job1Id, true, labels);
//
//        //  Delete J1.
//        deleteJob(job1Id);
//
//        //  Verify J1 rows have been removed.
//        JobServiceDatabaseUtil.assertJobRowDoesNotExist(job1Id);
//
//        // Verify J1 label rows have been removed.
//        JobServiceDatabaseUtil.assertJobLabelRowsDoNotExist(job1Id);
//    }
//
//    @Test
//    public void testPauseWaitingJobIsSuccessful() throws Exception {
//        final String jobId = generateJobId();
//        createJob(jobId, true);
//        waitUntilJobStatusIs(JobStatus.Waiting, jobId);
//        pauseJob(jobId);
//        waitUntilJobStatusIs(JobStatus.Paused, jobId);
//    }
//
//    @Test
//    public void testPausePausedJobIsSuccessful() throws Exception {
//        final String jobId = generateJobId();
//        createJob(jobId, true);
//        waitUntilJobStatusIs(JobStatus.Waiting, jobId);
//        pauseJob(jobId);
//        waitUntilJobStatusIs(JobStatus.Paused, jobId);
//        pauseJob(jobId);
//        waitUntilJobStatusIs(JobStatus.Paused, jobId);
//    }
//
//    @Test(enabled = false) // See https://portal.digitalsafe.net/browse/SCMOD-13004
//    public void testPauseCompletedJobIsNotAllowed() throws Exception {
//        exampleWorkerMessageOutQueue = null; // Setting this to null means the job completes rather than being stuck in a 'Waiting' state
//        final String jobId = generateJobId();
//        createJob(jobId, true);
//        waitUntilJobCompletes(jobId);
//        try {
//            pauseJob(jobId);
//        } catch (final ApiException e) {
//            Assert.assertEquals(e.getCode(), 400, "Unexpected HTTP response code");
//            Assert.assertTrue(e.getMessage().contains("job_id {" + jobId + "} cannot be paused as it has a status of {Completed}. "
//                + "Only jobs with a status of Active or Waiting can be paused."),
//                                                      "Error message returned in HTTP 400 response does not contain the expected text");
//        }
//    }
//
//    @Test
//    public void testPauseCancelledJobIsNotAllowed() throws Exception {
//        final String jobId = generateJobId();
//        createJob(jobId, true);
//        waitUntilJobStatusIs(JobStatus.Waiting, jobId);
//        cancelJob(jobId);
//        waitUntilJobStatusIs(JobStatus.Cancelled, jobId);
//        try {
//            pauseJob(jobId);
//        } catch (final ApiException e) {
//            Assert.assertEquals(e.getCode(), 400, "Unexpected HTTP response code");
//            Assert.assertTrue(e.getMessage().contains("job_id {" + jobId + "} cannot be paused as it has a status of {Cancelled}. "
//                + "Only jobs with a status of Active or Waiting can be paused."),
//                                                      "Error message returned in HTTP 400 response does not contain the expected text");
//        }
//    }

    @Test
    public void testResumePausedJobThatWasPreviouslyWaitingMovesJobToActiveStatus() throws Exception
    {
        try (final IntegrationTestQueueServices integrationTestQueueServices = new IntegrationTestQueueServices()) {
            // given a job with a status of waiting
            integrationTestQueueServices.startListening();
            final String jobId = generateJobId();
            createJob(jobId, true);
            waitUntilJobStatusIs(JobStatus.Waiting, jobId);

            // when the job is paused then resumed
            pauseJob(jobId);
            waitUntilJobStatusIs(JobStatus.Paused, jobId);
            resumeJob(jobId);

            // then the status of the job should now be active
            waitUntilJobStatusIs(JobStatus.Active, jobId);

            // and a message should be sent to the resume job queue
            integrationTestQueueServices.waitForMessages(1, 30000, IntegrationTestQueueServices.RESUME_JOB_QUEUE_NAME);
            final List<String> resumeJobQueueMessages
                = integrationTestQueueServices.getMessages(IntegrationTestQueueServices.RESUME_JOB_QUEUE_NAME);
            Assert.assertEquals(resumeJobQueueMessages.size(), 1,
                "Expected 1 message to have been sent to the " + IntegrationTestQueueServices.RESUME_JOB_QUEUE_NAME + " queue");

            final TaskMessage resumeJobQueueTaskMessage
                = workerServices.getCodec().deserialise(resumeJobQueueMessages.get(0).getBytes(), TaskMessage.class);
            Assert.assertEquals(
                resumeJobQueueTaskMessage.getTaskApiVersion(),
                1,
                "Message sent to the " + IntegrationTestQueueServices.RESUME_JOB_QUEUE_NAME
                + " has unexpected value for it's 'taskApiVersion' property");
            Assert.assertEquals(
                resumeJobQueueTaskMessage.getTaskClassifier(),
                "DocumentWorkerTask",
                "Message sent to the " + IntegrationTestQueueServices.RESUME_JOB_QUEUE_NAME
                + " has unexpected value for it's 'taskClassifier' property");
            Assert.assertEquals(
                resumeJobQueueTaskMessage.getContext(),
                Collections.EMPTY_MAP,
                "Message sent to the " + IntegrationTestQueueServices.RESUME_JOB_QUEUE_NAME
                + " has unexpected value for it's 'context' property");
            Assert.assertEquals(
                resumeJobQueueTaskMessage.getTo(),
                IntegrationTestQueueServices.RESUME_JOB_QUEUE_NAME,
                "Message sent to the " + IntegrationTestQueueServices.RESUME_JOB_QUEUE_NAME
                + " has unexpected value for its 'to' property");

            final DocumentWorkerDocumentTask documentWorkerDocumentTask
                = workerServices.getCodec().deserialise(((String)resumeJobQueueTaskMessage.getTaskData()).getBytes(StandardCharsets.UTF_8),
                    DocumentWorkerDocumentTask.class);
            Assert.assertNull(
                documentWorkerDocumentTask.document,
                "Message sent to the " + IntegrationTestQueueServices.RESUME_JOB_QUEUE_NAME
                + " should have a 'null' 'document' property");
            Assert.assertEquals(
                documentWorkerDocumentTask.customData.size(),
                2,
                "Message sent to the " + IntegrationTestQueueServices.RESUME_JOB_QUEUE_NAME
                + " should have 2 customData properties");
            Assert.assertEquals(
                documentWorkerDocumentTask.customData.get("partitionId"),
                defaultPartitionId,
                "Message sent to the " + IntegrationTestQueueServices.RESUME_JOB_QUEUE_NAME
                + " has unexpected value for it's 'customData.partitionId' property");
            Assert.assertEquals(
                documentWorkerDocumentTask.customData.get("jobId"),
                jobId,
                "Message sent to the " + IntegrationTestQueueServices.RESUME_JOB_QUEUE_NAME
                + " has unexpected value for it's 'customData.jobId' property");
            Assert.assertNull(
                resumeJobQueueTaskMessage.getSourceInfo(),
                "Message sent to the " + IntegrationTestQueueServices.RESUME_JOB_QUEUE_NAME
                + " should have a null 'sourceInfo' property");
            Assert.assertNull(
                resumeJobQueueTaskMessage.getPriority(),
                "Message sent to the " + IntegrationTestQueueServices.RESUME_JOB_QUEUE_NAME
                + " should have a null 'priority' property");
            Assert.assertEquals(
                resumeJobQueueTaskMessage.getCorrelationId(),
                "1",
                "Message sent to the " + IntegrationTestQueueServices.RESUME_JOB_QUEUE_NAME
                + " has unexpected value for it's 'correlationId' property");
        }
    }

//    @Test(enabled = false) // See https://portal.digitalsafe.net/browse/SCMOD-13004
    public void testResumeCompletedJobIsNotAllowed() throws Exception {
        exampleWorkerMessageOutQueue = null; // Setting this to null means the job completes rather than being stuck in a 'Waiting' state
        final String jobId = generateJobId();
        createJob(jobId, true);
        waitUntilJobCompletes(jobId);
        waitUntilJobStatusIs(JobStatus.Completed, jobId);
        try {
            resumeJob(jobId);
        } catch (final ApiException e) {
            Assert.assertEquals(e.getCode(), 400, "Unexpected HTTP response code");
            Assert.assertTrue(e.getMessage().contains("job_id {" + jobId + "} cannot be resumed as it has a status of {Completed}. "
                + "Only jobs with a status of Paused can be resumed."),
                                                      "Error message returned in HTTP 400 response does not contain the expected text");
        }
    }

//    @Test
    public void testResumeCancelledJobIsNotAllowed() throws Exception {
        final String jobId = generateJobId();
        createJob(jobId, true);
        waitUntilJobStatusIs(JobStatus.Waiting, jobId);
        cancelJob(jobId);
        waitUntilJobStatusIs(JobStatus.Cancelled, jobId);
        try {
            resumeJob(jobId);
        } catch (final ApiException e) {
            Assert.assertEquals(e.getCode(), 400, "Unexpected HTTP response code");
            Assert.assertTrue(e.getMessage().contains("job_id {" + jobId + "} cannot be resumed as it has a status of {Cancelled}. "
                + "Only jobs with a status of Paused can be resumed."),
                                                      "Error message returned in HTTP 400 response does not contain the expected text");
        }
    }

//    @Test
    public void testResumeWaitingJobIsNotAllowed() throws Exception {
        final String jobId = generateJobId();
        createJob(jobId, true);
        waitUntilJobStatusIs(JobStatus.Waiting, jobId);
        try {
            resumeJob(jobId);
        } catch (final ApiException e) {
            Assert.assertEquals(e.getCode(), 400, "Unexpected HTTP response code");
            Assert.assertTrue(e.getMessage().contains("job_id {" + jobId + "} cannot be resumed as it has a status of {Waiting}. "
                + "Only jobs with a status of Paused can be resumed."),
                                                      "Error message returned in HTTP 400 response does not contain the expected text");
        }
    }

//    @Test
    public void testGetJobStatus() throws Exception {
        final String jobId = generateJobId();
        createJob(jobId, true);
        waitUntilJobStatusIs(JobStatus.Waiting, jobId);
    }

//    @Test
    public void testGetJobStatusForUnknownJob() throws Exception {
        try {
            jobsApi.getJobStatus(defaultPartitionId, "unknown-job-id", jobCorrelationId);
        } catch (final ApiException e) {
            Assert.assertEquals(e.getCode(), 404, "Unexpected HTTP response code");
            Assert.assertTrue(e.getMessage().contains("job_id {unknown-job-id} not found"),
                              "Error message returned in HTTP 400 response does not contain the expected text");
        }
    }

    private List<String> generateWorkerBatch() throws DataStoreException {
        List<String> assetIds = new ArrayList<>();
        String containerId = getContainerId();
        for (int testItemNumber = 0; testItemNumber < numTestItemsToGenerate; testItemNumber++) {
            String itemContent = "TestItem_" + String.valueOf(testItemNumber);
            InputStream is = new ByteArrayInputStream(itemContent.getBytes(StandardCharsets.UTF_8));
            String reference = workerServices.getDataStore().store(is, containerId);
            assetIds.add(reference);
        }
        return assetIds;
    }


    private String toBatchDefinition(final List<String> assetIds) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(assetIds);
    }


    private String getContainerId() {
        String containerId = System.getProperty(datastoreContainerId);
        if (containerId == null) {
            containerId = System.getenv(datastoreContainerId);
        }
        return containerId;
    }


    /**
     * Creates a new job in the Job Database.
     * @param jobId the new job should be created with this id
     * @param useTaskDataObject indicates if the new job task should be created with taskDataObject
     */
    private void createJob(final String jobId, final boolean useTaskDataObject) throws Exception {
        NewJob newJob = constructNewJob(jobId, useTaskDataObject);
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob, jobCorrelationId);
    }

    private void createJobWithPrerequisites(final String jobId, final boolean useTaskDataObject, final int delay,
                                            final String... prerequisiteJobs) throws Exception {
        NewJob newJob = constructNewJob(jobId, useTaskDataObject);
        newJob.setPrerequisiteJobIds(Arrays.asList(prerequisiteJobs));
        newJob.setDelay(delay);
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob, jobCorrelationId);
    }

    private void createJobWithLabels(final String jobId, final boolean useTaskDataObject,
                                     final Map<String, String> labels) throws Exception {
        final NewJob newJob = constructNewJob(jobId, useTaskDataObject);
        newJob.getLabels().putAll(labels);
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob, jobCorrelationId);
    }

    private void createJobWithPrerequisites(final String partitionId, final String jobId,
            final boolean useTaskDataObject, final String... prerequisiteJobs) throws Exception {
        final NewJob newJob = constructNewJob(jobId, useTaskDataObject);
        newJob.setPrerequisiteJobIds(Arrays.asList(prerequisiteJobs));
        jobsApi.createOrUpdateJob(partitionId, jobId, newJob, jobCorrelationId);
    }

    private void createJobWithDelay(final String partitionId, final String jobId,
            final boolean useTaskDataObject, final int delay) throws Exception {
        final NewJob newJob = constructNewJob(jobId, useTaskDataObject);
        newJob.setDelay(delay);
        jobsApi.createOrUpdateJob(partitionId, jobId, newJob, jobCorrelationId);
    }

    private void createJobWithPrerequisitesAndDelay(final String partitionId, final String jobId,
            final boolean useTaskDataObject, final int delay,
            final String... prerequisiteJobs) throws Exception {
        final NewJob newJob = constructNewJob(jobId, useTaskDataObject);
        newJob.setPrerequisiteJobIds(Arrays.asList(prerequisiteJobs));
        newJob.setDelay(delay);
        jobsApi.createOrUpdateJob(partitionId, jobId, newJob, jobCorrelationId);
    }

    private void createJobWithLabels(final String partitionId, final String jobId, final boolean useTaskDataObject,
            final Map<String, String> labels) throws Exception {
        final NewJob newJob = constructNewJob(jobId, useTaskDataObject);
        newJob.getLabels().putAll(labels);
        jobsApi.createOrUpdateJob(partitionId, jobId, newJob, jobCorrelationId);
    }

    private void cancelJob(final String jobId) throws Exception {
        jobsApi.cancelJob(defaultPartitionId, jobId, jobCorrelationId);
    }

    private void deleteJob(final String jobId) throws Exception {
        jobsApi.deleteJob(defaultPartitionId, jobId, jobCorrelationId);
    }

    private void pauseJob(final String jobId) throws Exception{
        jobsApi.pauseJob(defaultPartitionId, jobId, jobCorrelationId);
    }

    private void resumeJob(final String jobId) throws Exception {
        jobsApi.resumeJob(defaultPartitionId, jobId, jobCorrelationId);
    }

    private NewJob constructNewJob(String jobId, final boolean useTaskDataObject) throws Exception {
        WorkerAction batchWorkerAction = constructBatchWorkerAction(useTaskDataObject);
        String jobName = "Job_" + jobId;
        NewJob newJob = new NewJob();
        newJob.setName(jobName);
        newJob.setDescription(jobName + " description");
        newJob.setExternalData(jobName + " external data");
        newJob.setTask(batchWorkerAction);
        return newJob;
    }


    private WorkerAction constructBatchWorkerAction(final boolean useTaskDataObject) throws Exception {
        WorkerAction batchWorkerAction = new WorkerAction();
        batchWorkerAction.setTaskClassifier(BatchWorkerConstants.WORKER_NAME);
        batchWorkerAction.setTaskApiVersion(BatchWorkerConstants.WORKER_API_VERSION);

        if (useTaskDataObject) {
            batchWorkerAction.setTaskData(constructBatchWorkerTask());
        } else {
            batchWorkerAction.setTaskData(constructSerialisedBatchWorkerTask());
            batchWorkerAction.setTaskDataEncoding(WorkerAction.TaskDataEncodingEnum.UTF8);
        }

        batchWorkerAction.setTaskPipe(batchWorkerMessageInQueue);
        batchWorkerAction.setTargetPipe(exampleWorkerMessageOutQueue);
        return batchWorkerAction;
    }


    private String constructSerialisedBatchWorkerTask() throws Exception {
        final BatchWorkerTask task = constructBatchWorkerTask();
        return new String(workerServices.getCodec().serialise(task), StandardCharsets.UTF_8);
    }

    private BatchWorkerTask constructBatchWorkerTask() throws Exception {
        Map<String, String> exampleWorkerTaskMessageParams = new HashMap<>();
        exampleWorkerTaskMessageParams.put("datastorePartialReference", getContainerId());
        exampleWorkerTaskMessageParams.put("action", "CAPITALISE");

        BatchWorkerTask task = new BatchWorkerTask();
        task.batchDefinition = toBatchDefinition(testItemAssetIds);
        task.batchType = "AssetIdBatchPlugin";
        task.taskMessageType = "ExampleWorkerTaskBuilder";
        task.taskMessageParams = exampleWorkerTaskMessageParams;
        task.targetPipe = exampleWorkerMessageInQueue;
        return task;
    }

    /**
     * Generate a unique Job Id.
     *
     * @return a pseudo-random job id
     */
    private String generateJobId()
    {
        return "J" + System.currentTimeMillis() + "-" + (int) (Math.random() * 10000);
    }

    private QueueManager getFinalQueueManager() throws IOException, TimeoutException {
        // The end-to-end test should ultimately result in an example worker result message
        // on the exampleWorkerMessageOutQueue, so we'll consume from there.
        QueueServices queueServices = QueueServicesFactory.create(rabbitConfiguration, exampleWorkerMessageOutQueue, workerServices.getCodec());
        boolean debugEnabled = SettingsProvider.defaultProvider.getBooleanSetting(SettingNames.createDebugMessage,false);
        return new QueueManager(queueServices, workerServices, debugEnabled);
    }

    private Timer getTimer(ExecutionContext context) {
        String timeoutSetting = SettingsProvider.defaultProvider.getSetting(SettingNames.timeOutMs);
        long timeout = timeoutSetting == null ? defaultTimeOutMs : Long.parseLong(timeoutSetting);
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                context.testRunsTimedOut();
            }
        }, timeout);
        return timer;
    }

    /**
     * Desc: Waits till the job is Completed
     * @param jobId
     * @throws ApiException
     * @throws InterruptedException
     */
    private void waitUntilJobCompletes(String jobId) throws ApiException, InterruptedException {
        waitUntilJobStatusIs(JobStatus.Completed, jobId);
    }

    private void waitUntilJobStatusIs(final JobStatus expectedJobStatus, final String jobId) throws ApiException, InterruptedException {
        long deadline = System.currentTimeMillis() + JOB_STATUS_CHECK_TIMEOUT_MS;
        JobStatus currentJobStatus = jobsApi.getJobStatus(defaultPartitionId, jobId, jobCorrelationId);
        while (currentJobStatus != expectedJobStatus) {
            Thread.sleep(JOB_STATUS_CHECK_SLEEP_MS);
            long remaining = deadline - System.currentTimeMillis();
            if (remaining < 0) {
                Assert.fail("Job " + jobId + " has unexpected status: " + currentJobStatus + " (expected: " + expectedJobStatus + ")");
            }
            currentJobStatus = jobsApi.getJobStatus(defaultPartitionId, jobId, jobCorrelationId);
        }
    }
}
