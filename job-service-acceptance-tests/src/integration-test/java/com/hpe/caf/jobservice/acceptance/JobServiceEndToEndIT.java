/*
 * Copyright 2015-2017 EntIT Software LLC, a Micro Focus company.
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
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.config.system.SystemBootstrapConfiguration;
import com.hpe.caf.naming.ServicePath;
import com.hpe.caf.services.job.client.ApiClient;
import com.hpe.caf.services.job.client.api.JobsApi;
import com.hpe.caf.services.job.client.model.NewJob;
import com.hpe.caf.services.job.client.model.WorkerAction;
import com.hpe.caf.worker.batch.BatchWorkerConstants;
import com.hpe.caf.worker.batch.BatchWorkerTask;
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
    private static final String exampleWorkerMessageOutQueue = "exampleworker-test-output-1";
    private static final String datastoreContainerId = "datastore.container.id";
    private static final String jobCorrelationId = "1";
    private static final long defaultTimeOutMs = 600000; // 10 minutes
    private static ServicePath servicePath;
    private static WorkerServices workerServices;
    private static ConfigurationSource configurationSource;
    private static RabbitWorkerQueueConfiguration rabbitConfiguration;
    private static JobsApi jobsApi;
    private static int numTestItemsToGenerate = 50;   // CAF-3677: This cannot be set any higher than 2 otherwise the job will not reach completion. Change to final variable when fixed.

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
        numTestItemsToGenerate = 50;        // CAF-3677: Remove this on fix
        testItemAssetIds = generateWorkerBatch();
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
            Assert.assertTrue(result.isSuccess());
        }
    }

    @Test
    public void testJobWithNoPrerequisiteJobs() throws Exception {
        numTestItemsToGenerate = 2;                 // CAF-3677: Remove this on fix
        testItemAssetIds = generateWorkerBatch();   // CAF-3677: Remove this on fix
        final String jobId = generateJobId();

        JobServiceEndToEndITExpectation expectation =
                new JobServiceEndToEndITExpectation(
                        false,
                        exampleWorkerMessageOutQueue,
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

            createJob(jobId, true);

            // Waits for the final result message to appear on the Example worker's output queue.
            // When we read it from this queue it should have been processed fully and its status reported to the Job Database as Completed.
            TestResult result = context.getTestResult();
            Assert.assertTrue(result.isSuccess());
        }
    }

    @Test
    public void testJobWithPrerequisiteJobsWhichHaveCompleted() throws Exception
    {
        numTestItemsToGenerate = 2;                 // CAF-3677: Remove this on fix
        testItemAssetIds = generateWorkerBatch();   // CAF-3677: Remove this on fix

        //  Generate job identifiers for test.
        final String job1Id = generateJobId();
        final String job2Id = generateJobId();
        final String job3Id = generateJobId();

        // Add a Prerequisite job 1 that should be completed
        JobServiceEndToEndITExpectation job1Expectation =
                new JobServiceEndToEndITExpectation(
                        false,
                        exampleWorkerMessageOutQueue,
                        job1Id,
                        jobCorrelationId,
                        ExampleWorkerConstants.WORKER_NAME,
                        ExampleWorkerConstants.WORKER_API_VER,
                        TaskStatus.RESULT_SUCCESS,
                        ExampleWorkerStatus.COMPLETED,
                        testItemAssetIds);

        try (QueueManager queueManager = getFinalQueueManager()) {
            ExecutionContext context = new ExecutionContext(false);
            context.initializeContext();
            getTimer(context);
            queueManager.start(new FinalOutputDeliveryHandler(workerServices.getCodec(), jobsApi, context,
                    job1Expectation));

            createJob(job1Id, true);
        }

        // Add a Prerequisite job 2 that should be completed
        JobServiceEndToEndITExpectation job2Expectation =
                new JobServiceEndToEndITExpectation(
                        false,
                        exampleWorkerMessageOutQueue,
                        job2Id,
                        jobCorrelationId,
                        ExampleWorkerConstants.WORKER_NAME,
                        ExampleWorkerConstants.WORKER_API_VER,
                        TaskStatus.RESULT_SUCCESS,
                        ExampleWorkerStatus.COMPLETED,
                        testItemAssetIds);

        try (QueueManager queueManager = getFinalQueueManager()) {
            ExecutionContext context = new ExecutionContext(false);
            context.initializeContext();
            getTimer(context);
            queueManager.start(new FinalOutputDeliveryHandler(workerServices.getCodec(), jobsApi, context,
                    job2Expectation));

            createJob(job2Id, true);
        }

        Thread.sleep(1000); // Add short delay to allow previous jobs to complete

        // Add job that has prerequisite job 1 (completed) and job 2 (completed). Also supply blank, null and empty
        // prereq job ids that should not hold the job back.
        createJobWithPrerequisites(job3Id, true, job1Id, job2Id, "", null, "           ");

        Thread.sleep(1000); // Add short delay to allow previous job to complete

        //  Verify job 3 has completed and no job dependency rows exist.
        JobServiceDatabaseUtil.assertJobStatus(job3Id, "completed");
        JobServiceDatabaseUtil.assertJobDependencyRowsDoNotExist(job3Id, job1Id);
        JobServiceDatabaseUtil.assertJobDependencyRowsDoNotExist(job3Id, job2Id);
    }

    @Test
    public void testJobWithNullAndBlankAndEmptyPrereqJobsShouldComplete() throws Exception
    {
        numTestItemsToGenerate = 1;                 // CAF-3677: Remove this on fix
        testItemAssetIds = generateWorkerBatch();   // CAF-3677: Remove this on fix
        final String job1Id = generateJobId();

        JobServiceEndToEndITExpectation job1Expectation =
                new JobServiceEndToEndITExpectation(
                        false,
                        exampleWorkerMessageOutQueue,
                        job1Id,
                        jobCorrelationId,
                        ExampleWorkerConstants.WORKER_NAME,
                        ExampleWorkerConstants.WORKER_API_VER,
                        TaskStatus.RESULT_SUCCESS,
                        ExampleWorkerStatus.COMPLETED,
                        testItemAssetIds);

        // Add a Prerequisite job 1 that should be completed
        try (QueueManager queueManager = getFinalQueueManager()) {
            ExecutionContext context = new ExecutionContext(false);
            context.initializeContext();
            getTimer(context);
            queueManager.start(new FinalOutputDeliveryHandler(workerServices.getCodec(), jobsApi, context,
                    job1Expectation));

            // Add job that has prerequisite jobs that are empty, null and blank
            createJobWithPrerequisites(job1Id, true, "", null, "           ");

            // Waits for the final result message to appear on the Example worker's output queue.
            // When we read it from this queue it should have been processed fully and its status reported to the Job Database as Completed.
            TestResult result = context.getTestResult();
            Assert.assertTrue(result.isSuccess());
        }
    }

    @Test
    public void testJobWithPrerequisiteJobsWithOneNotCompleted() throws Exception
    {
        numTestItemsToGenerate = 2;                 // CAF-3677: Remove this on fix
        testItemAssetIds = generateWorkerBatch();   // CAF-3677: Remove this on fix

        //  Generate job identifiers for test.
        final String job1Id = generateJobId();
        final String job3Id = generateJobId();
        final String job2Id = generateJobId();

        // Add a Prerequisite job 1 that should be completed
        JobServiceEndToEndITExpectation job1Expectation =
                new JobServiceEndToEndITExpectation(
                        false,
                        exampleWorkerMessageOutQueue,
                        job1Id,
                        jobCorrelationId,
                        ExampleWorkerConstants.WORKER_NAME,
                        ExampleWorkerConstants.WORKER_API_VER,
                        TaskStatus.RESULT_SUCCESS,
                        ExampleWorkerStatus.COMPLETED,
                        testItemAssetIds);

        try (QueueManager queueManager = getFinalQueueManager()) {
            ExecutionContext context = new ExecutionContext(false);
            context.initializeContext();
            getTimer(context);
            queueManager.start(new FinalOutputDeliveryHandler(workerServices.getCodec(), jobsApi, context,
                    job1Expectation));

            createJob(job1Id, true);

            // Waits for the final result message to appear on the Example worker's output queue.
            // When we read it from this queue it should have been processed fully and its status reported to the Job
            // Database as Completed.
            context.getTestResult();
        }

        Thread.sleep(1000); // Add short delay to allow previous job to complete

        // Add job that has prerequisite job 1 (completed) and job 2 (unknown)
        createJobWithPrerequisites(job3Id, true, job1Id, job2Id);

        // Verify job 3 is in waiting status and dependency rows exist as expected.
        JobServiceDatabaseUtil.assertJobStatus(job3Id, "waiting");
        JobServiceDatabaseUtil.assertJobDependencyRowsExist(job3Id, job2Id, batchWorkerMessageInQueue, exampleWorkerMessageOutQueue);
    }

    @Test
    public void testJobWithPrerequisiteJobs() throws Exception
    {
        numTestItemsToGenerate = 2;                 // CAF-3677: Remove this on fix
        testItemAssetIds = generateWorkerBatch();   // CAF-3677: Remove this on fix

        //  Generate job identifiers for test.
        final String job1Id = generateJobId();
        final String job2Id = generateJobId();
        final String job3Id = generateJobId();
        final String job4Id = generateJobId();

        //  Create job hierarchy.
        //
        //  J1
        //  -> J2
        //      -> J3
        //      -> J4
        createJobWithPrerequisites(job2Id, true, job1Id);
        //  Verify J2 is in 'waiting' state and job dependency rows exist as expected.
        JobServiceDatabaseUtil.assertJobStatus(job2Id, "waiting");
        JobServiceDatabaseUtil.assertJobDependencyRowsExist(job2Id, job1Id, batchWorkerMessageInQueue, exampleWorkerMessageOutQueue);

        createJobWithPrerequisites(job3Id, true, job2Id);
        //  Verify J3 is in 'waiting' state and job dependency rows exist as expected.
        JobServiceDatabaseUtil.assertJobStatus(job3Id, "waiting");
        JobServiceDatabaseUtil.assertJobDependencyRowsExist(job3Id, job2Id, batchWorkerMessageInQueue, exampleWorkerMessageOutQueue);

        createJobWithPrerequisites(job4Id, true, job2Id);
        //  Verify J4 is in 'waiting' state and job dependency rows exist as expected.
        JobServiceDatabaseUtil.assertJobStatus(job4Id, "waiting");
        JobServiceDatabaseUtil.assertJobDependencyRowsExist(job4Id, job2Id, batchWorkerMessageInQueue, exampleWorkerMessageOutQueue);

        //  Add a Prerequisite job 1 that should be completed. This should trigger the completion of all
        //  other jobs.
        JobServiceEndToEndITExpectation job1Expectation =
                new JobServiceEndToEndITExpectation(
                        false,
                        exampleWorkerMessageOutQueue,
                        job2Id,
                        jobCorrelationId,
                        ExampleWorkerConstants.WORKER_NAME,
                        ExampleWorkerConstants.WORKER_API_VER,
                        TaskStatus.RESULT_SUCCESS,
                        ExampleWorkerStatus.COMPLETED,
                        testItemAssetIds);

        try (QueueManager queueManager = getFinalQueueManager()) {
            ExecutionContext context = new ExecutionContext(false);
            context.initializeContext();
            getTimer(context);
            queueManager.start(new FinalOutputDeliveryHandler(workerServices.getCodec(), jobsApi, context,
                    job1Expectation));

            createJob(job1Id, true);

            // Waits for the final result message to appear on the Example worker's output queue.
            // When we read it from this queue it should have been processed fully and its status reported to the Job
            // Database as Completed.
            context.getTestResult();
        }

        Thread.sleep(2000); // Add short delay to allow previous jobs to complete

        //  Now that J1 has completed, verify this has triggered the completion of other jobs created
        //  with a prerequisite.
        JobServiceDatabaseUtil.assertJobStatus(job2Id, "completed");
        JobServiceDatabaseUtil.assertJobDependencyRowsDoNotExist(job2Id, job1Id);
        JobServiceDatabaseUtil.assertJobStatus(job3Id, "completed");
        JobServiceDatabaseUtil.assertJobDependencyRowsDoNotExist(job3Id, job2Id);
        JobServiceDatabaseUtil.assertJobStatus(job4Id, "completed");
        JobServiceDatabaseUtil.assertJobDependencyRowsDoNotExist(job4Id, job2Id);
    }

    @Test
    public void testJobCancellation() throws Exception {
        testJobCancellation(false);
    }

    @Test
    public void testJobCancellationWithTaskDataObject() throws Exception {
        testJobCancellation(true);
    }

    private void testJobCancellation(final boolean useTaskDataObject) throws Exception {
        final String jobId = generateJobId();

        JobServiceEndToEndITExpectation expectation =
                new JobServiceEndToEndITExpectation(
                        true,
                        exampleWorkerMessageOutQueue,
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

            cancelJob(jobId);

            TestResult result = context.getTestResult();
            Assert.assertTrue(result.isSuccess());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testJobServiceCaller_Success() throws ParseException, IOException, TimeoutException {

        LOG.debug("Starting testJobServiceCaller_Success() ...");

        //  Generate job identifier.
        final String jobId = generateJobId();

        List<String> testItemAssetIds = new ArrayList<>();
        testItemAssetIds.add("TestItemAssetId");

        JobServiceEndToEndITExpectation expectation =
                new JobServiceEndToEndITExpectation(
                        false,
                        exampleWorkerMessageOutQueue,
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

            //  Identify name of test data container as  we need to set up VolumesFrom to access the test data file.
            String jobDefinitionContainerName = JobServiceCallerTestsHelper.getJobDefinitionContainerName(jobDefinitionContainerJSON, dockerContainersURL);

            //  Parse the job service caller container json string. This JSON will need modified for the test.
            JSONObject createContainerObject = JobServiceCallerTestsHelper.parseJson(jobServiceCallerContainerJSON);

            //  Identify link name for the job-service container.
            String jobServiceContainerLinkName = JobServiceCallerTestsHelper.getJobServiceContainerLinkName(jobServiceImage, jobServiceAdminPort, dockerContainersURL);

            //  Before job service caller container can be started, a number of changes to the container JSON needs to be made including Cmd, HostConfig and Image.
            //  Configure Cmd (i.e. modify job identifier, web service url, polling interval and job definition file that will be passed to the containerized script).
            JSONArray cmd = new JSONArray();
            cmd.add(0, "-j");
            cmd.add(1, jobId);
            cmd.add(2, "-u");
            cmd.add(3, jobServiceCallerWebServiceLinkURL);
            cmd.add(4, "-p");
            cmd.add(5, pollingInterval);
            cmd.add(6, "-f");
            cmd.add(7, "/jobDefinition/" + jobDefinitionFile);
            createContainerObject.put("Cmd", cmd);

            //  Configure HostConfig
            JSONObject hostConfig = (JSONObject) createContainerObject.get("HostConfig");

            JSONArray links = new JSONArray();
            links.add(jobServiceContainerLinkName);
            hostConfig.put("Links", links);

            JSONArray volumesFrom = new JSONArray();
            volumesFrom.add(jobDefinitionContainerName);
            hostConfig.put("VolumesFrom", volumesFrom);
            createContainerObject.put("HostConfig", hostConfig);

            //  Configure Image.
            createContainerObject.put("Image", jobServiceCallerImage);

            //  Send POST request to create the job service caller container.
            String createContainerURL = dockerContainersURL + "create";
            String sendCreateContainerPostResponse = JobServiceCallerTestsHelper.sendPOST(createContainerURL,createContainerObject.toJSONString().replace("\\/","/"), "application/json", "gzip");

            //  Get container id from response object.
            JSONObject createContainerResponse = JobServiceCallerTestsHelper.parseJson(sendCreateContainerPostResponse);
            String id = (String) createContainerResponse.get("Id");

            //  Use container id to send POST to start the container.
            String startContainerURL = dockerContainersURL + id + "/start";
            JobServiceCallerTestsHelper.sendPOST(startContainerURL,"","text/plain", "gzip");

            //  Use container id to send POST request to wait on container response.
            String waitContainerURL = dockerContainersURL + id + "/wait";
            String sendWaitContainerPostResponse = JobServiceCallerTestsHelper.sendPOST(waitContainerURL,"", "text/plain", "gzip");

            // Get status code.
            JSONObject waitContainerResponse = JobServiceCallerTestsHelper.parseJson(sendWaitContainerPostResponse);
            long statusCode = (long) waitContainerResponse.get("StatusCode");

            //  Expecting StatusCode=0 for success.
            Assert.assertNotNull(statusCode);
            Assert.assertEquals(0L, statusCode);

            // Waits for the final result message to appear on the Example worker's output queue.
            // When we read it from this queue it should have been processed fully and its status reported to the Job Database as Completed.
            TestResult result = context.getTestResult();
            Assert.assertTrue(result.isSuccess());
        } catch (Exception e){
            LOG.error("Error while running testJobServiceCaller_Success().", e);
            throw e;
        }

        LOG.debug("Finished testJobServiceCaller_Success().");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testJobServiceCaller_Failure() throws ParseException, IOException, TimeoutException {
        LOG.debug("Starting testJobServiceCaller_Failure() ...");

        try {
            //  Generate job identifier.
            final String jobId = generateJobId();

            //  Identify name of test data container as  we need to set up VolumesFrom to access the test data file.
            String jobDefinitionContainerName = JobServiceCallerTestsHelper.getJobDefinitionContainerName(jobDefinitionContainerJSON, dockerContainersURL);

            //  Parse the job service caller container json string.
            JSONObject createContainerObject = JobServiceCallerTestsHelper.parseJson(jobServiceCallerContainerJSON);

            //  Identify link name for the job-service container.
            String jobServiceContainerLinkName = JobServiceCallerTestsHelper.getJobServiceContainerLinkName(jobServiceImage, jobServiceAdminPort, dockerContainersURL);

            //  Modify job identifier, polling interval and job definition file. Leave web service url as we want to force an error.
            JSONArray cmd = new JSONArray();
            cmd.add(0, "-j");
            cmd.add(1, jobId);
            cmd.add(2, "-p");
            cmd.add(3, pollingInterval);
            cmd.add(4, "-f");
            cmd.add(5, "/jobDefinition/" + jobDefinitionFile);
            createContainerObject.put("Cmd", cmd);

            //  Configure HostConfig
            JSONObject hostConfig = (JSONObject) createContainerObject.get("HostConfig");

            JSONArray links = new JSONArray();
            links.add(jobServiceContainerLinkName);
            hostConfig.put("Links", links);

            JSONArray volumesFrom = new JSONArray();
            volumesFrom.add(jobDefinitionContainerName);
            hostConfig.put("VolumesFrom", volumesFrom);

            createContainerObject.put("HostConfig", hostConfig);

            //  Configure Image.
            createContainerObject.put("Image", jobServiceCallerImage);

            //  Send POST request to create the job service caller container.
            String createContainerURL = dockerContainersURL + "create";
            String sendCreateContainerPostResponse = JobServiceCallerTestsHelper.sendPOST(createContainerURL, createContainerObject.toJSONString().replace("\\/", "/"), "application/json", "gzip");

            //  Get container id from response object.
            JSONObject createContainerResponse = JobServiceCallerTestsHelper.parseJson(sendCreateContainerPostResponse);
            String id = (String) createContainerResponse.get("Id");

            //  Use container id to send POST to start the container.
            String startContainerURL = dockerContainersURL + id + "/start";
            JobServiceCallerTestsHelper.sendPOST(startContainerURL, "", "text/plain", "gzip");

            //  Use container id to send POST request to wait on container response.
            String waitContainerURL = dockerContainersURL + id + "/wait";
            String sendWaitContainerPostResponse = JobServiceCallerTestsHelper.sendPOST(waitContainerURL, "", "text/plain", "gzip");

            // Get status code.
            JSONObject waitContainerResponse = JobServiceCallerTestsHelper.parseJson(sendWaitContainerPostResponse);
            long statusCode = (long) waitContainerResponse.get("StatusCode");

            //  Expecting StatusCode > 0 for failure.
            Assert.assertNotNull(statusCode);
            Assert.assertTrue(statusCode > 0);
        } catch (Exception e){
            LOG.error("Error while running testJobServiceCaller_Failure().", e);
            throw e;
        }

        LOG.debug("Finished testJobServiceCaller_Failure().");
    }

    @Test
    public void testJobDeletion() throws Exception {
        testJobDeletion(false);
    }

    @Test
    public void testJobDeletionWithTaskDataObject() throws Exception {
        testJobDeletion(true);
    }

    private void testJobDeletion(final boolean useTaskDataObject) throws Exception {
        numTestItemsToGenerate = 2;                 // CAF-3677: Remove this on fix
        testItemAssetIds = generateWorkerBatch();   // CAF-3677: Remove this on fix

        final String jobId = generateJobId();

        JobServiceEndToEndITExpectation expectation =
                new JobServiceEndToEndITExpectation(
                        false,
                        exampleWorkerMessageOutQueue,
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
            JobServiceDatabaseUtil.assertJobRowExists(jobId);

            TestResult result = context.getTestResult();
            Assert.assertTrue(result.isSuccess());
        }

        deleteJob(jobId);
        JobServiceDatabaseUtil.assertJobRowDoesNotExist(jobId);
    }

    @Test
    public void testJobDeletionWithPrerequisiteJobs() throws Exception
    {
        numTestItemsToGenerate = 2;                 // CAF-3677: Remove this on fix
        testItemAssetIds = generateWorkerBatch();   // CAF-3677: Remove this on fix

        //  Generate job identifiers for test.
        final String job1Id = generateJobId();
        final String job2Id = generateJobId();
        final String job3Id = generateJobId();
        final String job4Id = generateJobId();

        //  Create job hierarchy.
        //
        //  J1
        //  -> J2
        //      -> J3
        //      -> J4
        createJobWithPrerequisites(job2Id, true, job1Id);
        createJobWithPrerequisites(job3Id, true, job2Id);
        createJobWithPrerequisites(job4Id, true, job2Id);

        //  Delete J2.
        deleteJob(job2Id);

        //  Verify J2 rows have been removed.
        JobServiceDatabaseUtil.assertJobRowDoesNotExist(job2Id);
        JobServiceDatabaseUtil.assertJobDependencyRowsDoNotExist(job2Id, job1Id);

        //  Verify rows for J3 & J4 still exist.
        JobServiceDatabaseUtil.assertJobRowExists(job3Id);
        JobServiceDatabaseUtil.assertJobStatus(job3Id, "waiting");
        JobServiceDatabaseUtil.assertJobDependencyRowsExist(job3Id, job2Id, batchWorkerMessageInQueue, exampleWorkerMessageOutQueue);
        JobServiceDatabaseUtil.assertJobRowExists(job4Id);
        JobServiceDatabaseUtil.assertJobStatus(job4Id, "waiting");
        JobServiceDatabaseUtil.assertJobDependencyRowsExist(job4Id, job2Id, batchWorkerMessageInQueue, exampleWorkerMessageOutQueue);
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
        jobsApi.createOrUpdateJob(jobId, newJob, jobCorrelationId);
    }

    private void createJobWithPrerequisites(final String jobId, final boolean useTaskDataObject,
                                            final String... prerequisiteJobs) throws Exception {
        NewJob newJob = constructNewJob(jobId, useTaskDataObject);
        newJob.setPrerequisiteJobIds(Arrays.asList(prerequisiteJobs));
        jobsApi.createOrUpdateJob(jobId, newJob, jobCorrelationId);
    }


    private void cancelJob(final String jobId) throws Exception {
        jobsApi.cancelJob(jobId, jobCorrelationId);
    }

    private void deleteJob(final String jobId) throws Exception {
        jobsApi.deleteJob(jobId, jobCorrelationId);
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
}
