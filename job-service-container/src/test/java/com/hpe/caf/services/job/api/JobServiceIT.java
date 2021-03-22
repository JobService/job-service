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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hpe.caf.api.BootstrapConfiguration;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.config.system.SystemBootstrapConfiguration;
import com.hpe.caf.naming.ServicePath;
import com.hpe.caf.services.job.client.ApiClient;
import com.hpe.caf.services.job.client.ApiException;
import com.hpe.caf.services.job.client.api.JobsApi;
import com.hpe.caf.services.job.client.model.Job;
import com.hpe.caf.services.job.client.model.NewJob;
import com.hpe.caf.services.job.client.model.WorkerAction;
import com.hpe.caf.services.job.client.model.JobStatus;
import com.hpe.caf.util.rabbitmq.RabbitUtil;
import com.hpe.caf.worker.queue.rabbit.RabbitWorkerQueueConfiguration;
import com.hpe.caf.worker.testing.*;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.hpe.caf.services.job.api.JobServiceAssert.assertThrowsApiException;
import static org.testng.Assert.*;
import static org.testng.FileAssert.fail;

/**
 * Integration tests for the functionality of the Job Service.
 * (Not an end to end integration test.)
 */
public class JobServiceIT {

    private String connectionString;
    private String defaultPartitionId;
    private ApiClient client = new ApiClient();
    private JobsApi jobsApi;
    // cleaned up after each test, if assigned
    private QueueManager testQueueManager;
    private Connection rabbitConn;

    private static ServicePath servicePath;
    private static WorkerServices workerServices;
    private static ConfigurationSource configurationSource;
    private static RabbitWorkerQueueConfiguration rabbitConfiguration;
    private static String jobServiceOutputQueue;
    private static final long defaultTimeOutMs = 120000; // 2 minutes
    private static final ObjectMapper objectMapper = new ObjectMapper();

    final HashMap<String,Object> testDataObjectMap = new HashMap<>();
    final HashMap<String,String> taskMessageParams = new HashMap<>();

    /**
     * @param jobId
     * @param testId Used to identify which test submitted the job
     * @return Basic job definition to be submitted
     */
    private NewJob makeJob(final String jobId, final String testId) {
        String jobName = "Job_" + jobId;

        //create a WorkerAction task
        WorkerAction workerActionTask = new WorkerAction();
        workerActionTask.setTaskClassifier(jobName + "_" + testId);
        workerActionTask.setTaskApiVersion(1);
        workerActionTask.setTaskData(jobName + "_TaskClassifier Sample Test Task Data.");
        workerActionTask.setTaskDataEncoding(WorkerAction.TaskDataEncodingEnum.UTF8);
        workerActionTask.setTaskPipe("TaskQueue_" + jobId);
        workerActionTask.setTargetPipe("Queue_" + jobId);

        try {
            final Channel rabbitChannel = rabbitConn.createChannel();
            rabbitChannel.queueDeclare("TaskQueue_" + jobId, true, false, false,
                    new HashMap<>());
            rabbitChannel.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        NewJob newJob = new NewJob();
        newJob.setName(jobName);
        newJob.setDescription(jobName + " Descriptive Text.");
        newJob.setExternalData(jobName + " External data.");
        newJob.setTask(workerActionTask);

        return newJob;
    }

    /**
     * Create a job in the job-service.
     *
     * @param jobId
     * @return The job ID
     * @throws ApiException
     */
    private String createJob(final String jobId) throws ApiException {
        final NewJob job = makeJob(jobId, UUID.randomUUID().toString());
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId, job, "1");
        return jobId;
    }

    /**
     * @param jobId
     * @param typeId The job type ID
     * @param params Input for the job type task data script
     * @return Restricted job definition to be submitted
     */
    private NewJob makeRestrictedJob(final String jobId, final String typeId, final Object params) {
        final String jobName = "Job_" + jobId;

        final NewJob newJob = new NewJob();
        newJob.setName(jobName);
        newJob.setDescription(jobName + " Descriptive Text.");
        newJob.setExternalData(jobName + " External data.");
        newJob.setType(typeId);
        newJob.setParameters(params);

        return newJob;
    }

    @BeforeTest
    public void setup() throws Exception {
        connectionString = System.getenv("webserviceurl");

        //Populate maps for testing    
        taskMessageParams.put("datastorePartialReference", "sample-files");
        taskMessageParams.put("documentDataInputFolder", "/mnt/caf-datastore-root/sample-files");
        taskMessageParams.put("documentDataOutputFolder", "/mnt/bla");
        
        testDataObjectMap.put("taskClassifier", "*.txt");
        testDataObjectMap.put("batchType", "WorkerDocumentBatchPlugin");
        testDataObjectMap.put("taskMessageType", "DocumentWorkerTaskBuilder");
        testDataObjectMap.put("taskMessageParams", taskMessageParams);
        testDataObjectMap.put("targetPipe", "languageidentification-in");
        

        //set up client to connect to the web service running on docker, and call web methods from correct address.
        client.setBasePath(connectionString);

        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        client.setDateFormat(f);
        jobsApi = new JobsApi(client);

        BootstrapConfiguration bootstrap = new SystemBootstrapConfiguration();
        servicePath = bootstrap.getServicePath();
        workerServices = WorkerServices.getDefault();
        configurationSource = workerServices.getConfigurationSource();
        rabbitConfiguration = configurationSource.getConfiguration(RabbitWorkerQueueConfiguration.class);
        rabbitConfiguration.getRabbitConfiguration().setRabbitHost(SettingsProvider.defaultProvider.getSetting(SettingNames.dockerHostAddress));
        rabbitConfiguration.getRabbitConfiguration().setRabbitPort(Integer.parseInt(SettingsProvider.defaultProvider.getSetting(SettingNames.rabbitmqNodePort)));
        rabbitConn = RabbitUtil.createRabbitConnection(rabbitConfiguration.getRabbitConfiguration());
    }

    @AfterTest
    public void tearDown() throws IOException {
        if (testQueueManager != null) {
            testQueueManager.close();
        }
        if(rabbitConn != null) {
            rabbitConn.close();
        }
    }

    @BeforeMethod
    public void setupMethod() {
        defaultPartitionId = UUID.randomUUID().toString();
    }

    @Test
    public void testHealthCheck() throws NoSuchAlgorithmException, KeyManagementException, IOException
    {
        final String getRequestUrl = SettingsProvider.defaultProvider.getSetting("healthcheckurl");
        final HttpGet request = new HttpGet(getRequestUrl);
        // Set up HttpClient
        final HttpClient httpClient = HttpClients.createDefault();

        System.out.println("Sending GET to HealthCheck url: " + getRequestUrl);
        final HttpResponse response = httpClient.execute(request);
        request.releaseConnection();

        if (response.getEntity() == null) {
            fail("There was no content returned from the HealthCheck HTTP Get Request");
        }

        final String expectedHealthCheckResponseContent =
                "{\"database\":{\"healthy\":\"true\"},\"queue\":{\"healthy\":\"true\"}}";
        assertEquals(IOUtils.toString(response.getEntity().getContent(), Charset.forName("UTF-8")),
                expectedHealthCheckResponseContent, "Expected HealthCheck response should match the actual response");

        System.out.println("Response code from the HealthCheck request: " + response.getStatusLine().getStatusCode());
        assertTrue(response.getStatusLine().getStatusCode() == 200);
    }

    @Test
    public void testCreateJob() throws ApiException {
        //create a job
        String jobId = UUID.randomUUID().toString();
        String jobCorrelationId = "1";
        final NewJob newJob = makeJob(jobId, "testCreateJob");

        jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob, jobCorrelationId);

        //retrieve job using web method
        Job retrievedJob = jobsApi.getJob(defaultPartitionId, jobId, jobCorrelationId);

        assertEquals(retrievedJob.getId(), jobId);
        assertEquals(retrievedJob.getName(), newJob.getName());
        assertEquals(retrievedJob.getDescription(), newJob.getDescription());
        assertEquals(retrievedJob.getExternalData(), newJob.getExternalData());
        assertEquals(retrievedJob.getCreateTime(), retrievedJob.getLastUpdateTime(),
            "initial last-update-time should be create-time");
    }

    @Test
    public void testCreateJobTwice() throws ApiException {
        final String jobId = UUID.randomUUID().toString();
        final String correlationId = "1";
        final NewJob newJob = makeJob(jobId, "testCreateJobTwice");

        jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob, correlationId);
        final Job retrievedJobBefore = jobsApi.getJob(defaultPartitionId, jobId, correlationId);
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob, correlationId);
        final Job retrievedJobAfter = jobsApi.getJob(defaultPartitionId, jobId, correlationId);

        assertEquals(retrievedJobAfter.getName(), newJob.getName(), "job name should be unchanged");
        assertEquals(retrievedJobAfter.getLastUpdateTime(), retrievedJobBefore.getLastUpdateTime(),
            "job last update time should be unchanged");
    }

    @Test
    public void testCreateJobTwiceWithDeps() throws ApiException {
        final String jobId = UUID.randomUUID().toString();
        final String correlationId = "1";
        final NewJob newJob = makeJob(jobId, "testCreateJobTwiceWithDeps");
        newJob.setPrerequisiteJobIds(Collections.singletonList(UUID.randomUUID().toString()));

        jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob, correlationId);
        final Job retrievedJobBefore = jobsApi.getJob(defaultPartitionId, jobId, correlationId);
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob, correlationId);
        final Job retrievedJobAfter = jobsApi.getJob(defaultPartitionId, jobId, correlationId);

        assertEquals(retrievedJobAfter.getName(), newJob.getName(), "job name should be unchanged");
        assertEquals(retrievedJobAfter.getLastUpdateTime(), retrievedJobBefore.getLastUpdateTime(),
            "job last update time should be unchanged");
    }

    // regression test for SCMOD-7065
    @Test
    public void testCreateJobTwiceInParallel() throws Throwable {
        final String jobId = UUID.randomUUID().toString();
        final String correlationId = "1";
        final NewJob newJob = makeJob(jobId, "testCreateJobTwiceInParallel");
        final JobServiceAssert.TestThread req1 = new JobServiceAssert.TestThread(
            () -> jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob, correlationId));
        final JobServiceAssert.TestThread req2 = new JobServiceAssert.TestThread(
            () -> jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob, correlationId));

        // just checking the requests succeed
        req1.start();
        req2.start();
        req1.join();
        req2.join();
        req1.handleThrown();
        req2.handleThrown();
    }

    // regression test for SCMOD-7065
    @Test
    public void testCreateJobTwiceInParallelWithDeps() throws Throwable {
        final String jobId = UUID.randomUUID().toString();
        final String correlationId = "1";
        final NewJob newJob = makeJob(jobId, "testCreateJobTwiceInParallelWithDeps");
        newJob.setPrerequisiteJobIds(Collections.singletonList(UUID.randomUUID().toString()));
        final JobServiceAssert.TestThread req1 = new JobServiceAssert.TestThread(
            () -> jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob, correlationId));
        final JobServiceAssert.TestThread req2 = new JobServiceAssert.TestThread(
            () -> jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob, correlationId));

        // just checking the requests succeed
        req1.start();
        req2.start();
        req1.join();
        req2.join();
        req1.handleThrown();
        req2.handleThrown();
    }

    @Test
    public void testUpdateJobName() throws ApiException {
        final String jobId = UUID.randomUUID().toString();
        final String correlationId = "1";
        final NewJob newJob1 = makeJob(jobId, "testUpdateJobName");
        final NewJob newJob2 = makeJob(jobId, "testUpdateJobName");
        newJob2.setName(newJob2.getName() + " updated");

        jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob1, correlationId);
        assertThrowsApiException(Response.Status.FORBIDDEN,
            () -> jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob2, correlationId));

        final Job retrievedJob = jobsApi.getJob(defaultPartitionId, jobId, correlationId);
        assertEquals(retrievedJob.getName(), newJob1.getName(), "job name should be unchanged");
    }

    @Test
    public void testUpdateJobTask() throws ApiException {
        final String jobId = UUID.randomUUID().toString();
        final String correlationId = "1";
        final NewJob newJob1 = makeJob(jobId, "testUpdateJobTask");
        final NewJob newJob2 = makeJob(jobId, "testUpdateJobTask");
        newJob2.getTask().setTaskApiVersion(newJob2.getTask().getTaskApiVersion() + 7);

        jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob1, correlationId);
        assertThrowsApiException(Response.Status.FORBIDDEN,
            () -> jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob2, correlationId));
    }

    @Test
    public void testJobIsActive() throws ApiException {
        //create a job
        String jobId = UUID.randomUUID().toString();
        String jobCorrelationId = "1";
        final NewJob newJob = makeJob(jobId, "testJobIsActive");

        jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob, jobCorrelationId);

        // Check if job is active.
        boolean isActive = jobsApi.getJobActive(defaultPartitionId, jobId, jobCorrelationId);

        // Job will be in a 'Waiting' state, which is assumed as being Active.
        assertTrue(isActive);
    }

    @Test
    public void testDeleteJob() throws ApiException {
        //create a job
        String jobId = UUID.randomUUID().toString();
        String jobCorrelationId = "1";
        final NewJob newJob = makeJob(jobId, "testDeleteJob");

        jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob, jobCorrelationId);

        //make sure the job is there
        Job retrievedJob = jobsApi.getJob(defaultPartitionId, jobId, jobCorrelationId);
        assertEquals(retrievedJob.getId(), jobId);

        //delete the job
        jobsApi.deleteJob(defaultPartitionId, jobId, jobCorrelationId);

        //make sure the job does not exist
        try {
            jobsApi.getJob(defaultPartitionId, jobId, jobCorrelationId).getDescription();
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("\"message\":\"ERROR: job_id {" +jobId +"} not found"),
                    "Exception Message should return JobId not found");
        }

    }
    
    @Test
    public void testCreateJobWithTaskData_Object() throws ApiException {
        //create a job
        String jobId = UUID.randomUUID().toString();
        String jobCorrelationId = "1";
        final NewJob newJob = makeJob(jobId, "testObjectJob");

        jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob, jobCorrelationId);

        //retrieve job using web method
        Job retrievedJob = jobsApi.getJob(defaultPartitionId, jobId, jobCorrelationId);

        assertEquals(retrievedJob.getId(), jobId);
        assertEquals(retrievedJob.getName(), newJob.getName());
        assertEquals(retrievedJob.getDescription(), newJob.getDescription());
        assertEquals(retrievedJob.getExternalData(), newJob.getExternalData());
    }

    @Test
    public void testRetrieveMultipleJobs() throws ApiException {
        String randomUUID = UUID.randomUUID().toString();
        for(int i=0; i<10; i++){
            String jobId = randomUUID +"_"+i;
            String jobName = "Job_"+randomUUID +"_"+i;
            String jobDesc = jobName +" Descriptive Text.";
            String jobCorrelationId = "100";
            String jobExternalData = jobName +" External data.";

            WorkerAction workerActionTask = new WorkerAction();
            workerActionTask.setTaskClassifier(jobName +"_TaskClassifier");
            workerActionTask.setTaskApiVersion(1);
            workerActionTask.setTaskData("Sample Test Task Data.");
            workerActionTask.setTaskDataEncoding(WorkerAction.TaskDataEncodingEnum.UTF8);
            workerActionTask.setTaskPipe("TaskQueue_" + randomUUID);
            workerActionTask.setTargetPipe("Queue_" +randomUUID);

            try {
                Channel rabbitChannel = rabbitConn.createChannel();
                rabbitChannel.queueDeclare("TaskQueue_" + randomUUID, true, false, false,
                        new HashMap<>());
                rabbitChannel.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            NewJob newJob = new NewJob();
            newJob.setName(jobName);
            newJob.setDescription(jobDesc);
            newJob.setExternalData(jobExternalData);
            newJob.setTask(workerActionTask);

            jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob, jobCorrelationId);
        }

        List<Job> retrievedJobs = jobsApi.getJobs(
            defaultPartitionId, "100",null,null,null,null,null, null, null);
        assertEquals(retrievedJobs.size(), 10);

        for(int i=0; i<10; i++) {
            String expectedId = randomUUID +"_" +i;
            String expectedName = "Job_" +randomUUID +"_" +i;
            String expectedDescription = expectedName + " Descriptive Text.";
            String expectedExternalData = expectedName +" External data.";

            final int resultIndex = 9 - i; // default order is by create date, descending
            assertEquals(retrievedJobs.get(resultIndex).getId(), expectedId);
            assertEquals(retrievedJobs.get(resultIndex).getName(), expectedName);
            assertEquals(retrievedJobs.get(resultIndex).getDescription(), expectedDescription);
            assertEquals(retrievedJobs.get(resultIndex).getExternalData(), expectedExternalData);
        }
    }

    @Test
    public void testGetJobsFilterNotFinished() throws ApiException {
        final String correlationId = "1";

        final String canceledJobId = UUID.randomUUID().toString();
        final NewJob canceledJob = makeJob(canceledJobId, "testGetJobsFilterNotFinished");
        jobsApi.createOrUpdateJob(defaultPartitionId, canceledJobId, canceledJob, correlationId);
        jobsApi.cancelJob(defaultPartitionId,  canceledJobId, correlationId);

        final String waitingJobId = UUID.randomUUID().toString();
        final NewJob waitingJob = makeJob(waitingJobId, "testGetJobsFilterNotFinished");
        jobsApi.createOrUpdateJob(defaultPartitionId, waitingJobId, waitingJob, correlationId);

        final List<Job> jobs = jobsApi.getJobs(
            defaultPartitionId, correlationId, null, "NotFinished", null, null, null, null, null);
        assertEquals(jobs.size(), 1);
        assertEquals(jobs.get(0).getId(), waitingJobId);
    }

    @Test
    public void testGetJobsCountFilterNotFinished() throws ApiException {
        final String correlationId = "1";

        final String canceledJobId = UUID.randomUUID().toString();
        final NewJob canceledJob = makeJob(canceledJobId, "testGetJobsCountFilterNotFinished");
        jobsApi.createOrUpdateJob(defaultPartitionId, canceledJobId, canceledJob, correlationId);
        jobsApi.cancelJob(defaultPartitionId,  canceledJobId, correlationId);

        final String waitingJobId = UUID.randomUUID().toString();
        final NewJob waitingJob = makeJob(waitingJobId, "testGetJobsCountFilterNotFinished");
        jobsApi.createOrUpdateJob(defaultPartitionId, waitingJobId, waitingJob, correlationId);

        final long count = jobsApi.getJobsCount(
            defaultPartitionId, correlationId, null, "NotFinished", null);
        assertEquals(count, 1);
    }

    @Test
    public void testGetJobsWithSort() throws ApiException {
        final String jobId = UUID.randomUUID().toString();
        final String job1Id = createJob(jobId + "C");
        final String job2Id = createJob(jobId + "A");
        final String job3Id = createJob(jobId + "b");

        final List<Job> jobs = jobsApi.getJobs(
            defaultPartitionId, "1", null, null, null, null, "jobId:asc", null, null);
        final List<String> resultJobIds =
            jobs.stream().map(job -> job.getId()).collect(Collectors.toList());
        assertEquals(resultJobIds, Arrays.asList(jobId + "A", jobId + "b", jobId + "C"),
            "should sort case-insensitively by ascending job ID");
    }

    @Test
    public void testGetJobsWithSortByName() throws ApiException {
        final String jobId = UUID.randomUUID().toString();
        final String job1Id = createJob(jobId + "C");
        final String job2Id = createJob(jobId + "A");
        final String job3Id = createJob(jobId + "B");

        final List<Job> jobs = jobsApi.getJobs(
            defaultPartitionId, "1", null, null, null, null, "name:asc", null, null);
        final List<String> resultJobIds =
            jobs.stream().map(job -> job.getName()).collect(Collectors.toList());
        assertEquals(resultJobIds, Arrays.asList("Job_"+jobId + "A", "Job_"+jobId + "B", "Job_"+jobId + "C"),
            "should sort case-insensitively by ascending job name");
    }

    @Test
    public void testGetJobsWithSortByLabel() throws ApiException {
        final String jobId1 = UUID.randomUUID().toString();
        final String jobId2 = UUID.randomUUID().toString();
        final String jobId3 = UUID.randomUUID().toString();
        final String correlationId = "1";
        final NewJob job = makeJob(jobId1, "testFilterJobsByLabel");
        job.getLabels().put("tag1", "someTag");
        job.getLabels().put("tag2", "bbb");
        final NewJob job2 = makeJob(jobId2, "testFilterJobsByLabel");
        job2.getLabels().put("tag1", "aaa");
        job2.getLabels().put("owner", "bob");
        final NewJob job3 = makeJob(jobId3, "testFilterJobsByLabel");
        job3.getLabels().put("random", "label");
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId1, job, correlationId);
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId2, job2, correlationId);
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId3, job3, correlationId);

        //retrieve job using web method
        List<Job> jobs = jobsApi.getJobs(defaultPartitionId, correlationId, null, null,
                null, null, "labels.tag1:asc", null, null);
        assertEquals(jobs.stream().map(Job::getId).collect(Collectors.toSet()),
                                                    new HashSet<>(Arrays.asList(jobId2, jobId1, jobId3)));

        final List<String> sortJobIds =
            jobs.stream().map(e -> e.getId()).collect(Collectors.toList());
        assertEquals(sortJobIds, Arrays.asList(jobId2, jobId1, jobId3),
            "should sort case-insensitively by ascending job labels for the label-key: tag1");

        jobs = jobsApi.getJobs(defaultPartitionId, correlationId, null, null, null,null,
                "labels.owner:asc", "owner", null);
        assertEquals(jobs.stream().map(Job::getId).collect(Collectors.toSet()),
                                                new HashSet<>(Arrays.asList(jobId2)));
    }

    @Test
    public void testCancelJob() throws ApiException {
        //create a job
        String jobId = UUID.randomUUID().toString();
        String jobCorrelationId = "1";
        final NewJob newJob = makeJob(jobId, "testCancelJob");

        jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob, jobCorrelationId);

        final Job initialJob = jobsApi.getJob(defaultPartitionId, jobId, jobCorrelationId);

        jobsApi.cancelJob(defaultPartitionId, jobId, jobCorrelationId);

        Job cancelledJob = jobsApi.getJob(defaultPartitionId, jobId, jobCorrelationId);

        assertEquals(cancelledJob.getStatus(), JobStatus.Cancelled);
        assertTrue(new Date(cancelledJob.getLastUpdateTime()).after(new Date(initialJob.getLastUpdateTime())),
            "last-update-time should be updated on cancel");
    }

    /**
     * This tests cancelling the same job twice, which should succeed without changing the status.
     */
    @Test
    public void testCancelJobTwice() throws ApiException {
        String jobId = UUID.randomUUID().toString();
        String jobCorrelationId = "1";
        final NewJob newJob = makeJob(jobId, "testCancelJobTwice");

        jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob, jobCorrelationId);
        final Job initialJob = jobsApi.getJob(defaultPartitionId, jobId, jobCorrelationId);

        jobsApi.cancelJob(defaultPartitionId, jobId, jobCorrelationId);
        final Job cancelledJob = jobsApi.getJob(defaultPartitionId, jobId, jobCorrelationId);

        jobsApi.cancelJob(defaultPartitionId, jobId, jobCorrelationId); // shouldn't throw
        final Job cancelledAgainJob = jobsApi.getJob(defaultPartitionId, jobId, jobCorrelationId);

        assertEquals(cancelledJob.getStatus(), JobStatus.Cancelled,
            "status should remain cancelled");
        assertEquals(cancelledJob.getLastUpdateTime(), cancelledAgainJob.getLastUpdateTime(),
            "last-update-time should not be updated on second cancel");
    }

    @Test
    public void testGetJobFromDifferentPartition() throws ApiException {
        final String jobId = UUID.randomUUID().toString();
        final String jobCorrelationId = "1";
        final NewJob newJob = makeJob(jobId, "testGetJobFromDifferentPartition");

        jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob, jobCorrelationId);
        assertThrowsApiException(Response.Status.NOT_FOUND,
            () -> jobsApi.getJob(UUID.randomUUID().toString(), jobId, jobCorrelationId));
    }

    @Test
    public void testGetJobWithDepsFromDifferentPartition() throws ApiException {
        final String jobId = UUID.randomUUID().toString();
        final String jobCorrelationId = "1";
        final NewJob newJob = makeJob(jobId, "testGetJobWithDepsFromDifferentPartition");
        newJob.setPrerequisiteJobIds(Collections.singletonList(UUID.randomUUID().toString()));

        jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob, jobCorrelationId);
        assertThrowsApiException(Response.Status.NOT_FOUND,
            () -> jobsApi.getJob(UUID.randomUUID().toString(), jobId, jobCorrelationId));
    }

    @Test
    public void testCreateJobInMultiplePartitions() throws ApiException {
        final String jobId = UUID.randomUUID().toString();
        final String jobCorrelationId = "1";
        final NewJob newJob1 = makeJob(jobId, "testCreateJobInMultiplePartitions-1");
        final String partitionId2 = UUID.randomUUID().toString();
        final NewJob newJob2 = makeJob(jobId, "testCreateJobInMultiplePartitions-2");

        jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob1, jobCorrelationId);
        // shouldn't throw
        jobsApi.createOrUpdateJob(partitionId2, jobId, newJob2, jobCorrelationId);
        jobsApi.getJob(partitionId2, jobId, jobCorrelationId);
    }

    @Test
    public void testCreateJobWithDepsInMultiplePartitions() throws ApiException {
        final String jobId = UUID.randomUUID().toString();
        final String jobCorrelationId = "1";
        final NewJob newJob1 = makeJob(jobId, "testCreateJobWithDepsInMultiplePartitions-1");
        final String partitionId2 = UUID.randomUUID().toString();
        final NewJob newJob2 = makeJob(jobId, "testCreateJobWithDepsInMultiplePartitions-2");
        newJob2.setPrerequisiteJobIds(Collections.singletonList(UUID.randomUUID().toString()));

        jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob1, jobCorrelationId);
        // shouldn't throw
        jobsApi.createOrUpdateJob(partitionId2, jobId, newJob2, jobCorrelationId);
        jobsApi.getJob(partitionId2, jobId, jobCorrelationId);
    }

    @Test
    public void testDeleteJobFromDifferentPartition() throws ApiException {
        final String jobId = UUID.randomUUID().toString();
        final String jobCorrelationId = "1";
        final NewJob newJob = makeJob(jobId, "testDeleteJobFromDifferentPartition");

        jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob, jobCorrelationId);
        assertThrowsApiException(Response.Status.NOT_FOUND,
            () -> jobsApi.deleteJob(UUID.randomUUID().toString(), jobId, jobCorrelationId));
        final Job job = jobsApi.getJob(defaultPartitionId, jobId, jobCorrelationId);
        assertEquals(job.getStatus(), JobStatus.Waiting, "job should still be waiting");
    }

    @Test
    public void testCancelJobFromDifferentPartition() throws ApiException {
        final String jobId = UUID.randomUUID().toString();
        final String jobCorrelationId = "1";
        final NewJob newJob = makeJob(jobId, "testCancelJobFromDifferentPartition");

        jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob, jobCorrelationId);
        assertThrowsApiException(Response.Status.NOT_FOUND,
            () -> jobsApi.cancelJob(UUID.randomUUID().toString(), jobId, jobCorrelationId));
        final Job job = jobsApi.getJob(defaultPartitionId, jobId, jobCorrelationId);
        assertEquals(job.getStatus(), JobStatus.Waiting, "job should still be waiting");
    }

    @Test
    public void testGetJobActiveFromDifferentPartition() throws ApiException {
        final String jobId = UUID.randomUUID().toString();
        final String jobCorrelationId = "1";
        final NewJob newJob = makeJob(jobId, "testGetJobActiveFromDifferentPartition");

        jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob, jobCorrelationId);
        assertFalse(
            jobsApi.getJobActive(UUID.randomUUID().toString(), jobId, jobCorrelationId),
            "should not be active");
    }

    @Test
    public void testGetJobsFromDifferentPartition() throws ApiException {
        final String jobId = UUID.randomUUID().toString();
        final String jobCorrelationId = "1";
        final NewJob newJob = makeJob(jobId, "testGetJobsFromDifferentPartition");

        jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob, jobCorrelationId);
        final List<Job> jobs = jobsApi.getJobs(
            UUID.randomUUID().toString(), jobCorrelationId, null, null, null, null, null, null, null);
        assertEquals(jobs.size(), 0, "job list should be empty");
    }

    @Test
    public void testGetJobsCountFromDifferentPartition() throws ApiException {
        final String jobId = UUID.randomUUID().toString();
        final String jobCorrelationId = "1";
        final NewJob newJob = makeJob(jobId, "testGetJobsCountFromDifferentPartition");

        jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob, jobCorrelationId);
        final long count =
            jobsApi.getJobsCount(UUID.randomUUID().toString(), jobCorrelationId, null, null, null);
        assertEquals(count, 0, "job count should be zero");
    }

    /**
     * This test will create a job with tracking info, and using the jobservice client it will create a new job using the
     * appropriate web method. It then consumes the message from the queue and asserts the result is as expected.
     * @throws Exception
     */
    @Test
    public void testMessagesOnRabbit() throws Exception {
        //Create a job first
        String jobId = UUID.randomUUID().toString();
        System.out.println("JobID: " +jobId);
        String jobName = "Job_" +jobId;
        String jobDesc = jobName + " Descriptive Text.";
        String jobCorrelationId = "1";
        String jobExternalData = jobName +" External data.";
        String testQueue = "jobservice-test-input-1";
        String trackingToQueue = "tracking-to-queue";

        //create the worker action including target pipe
        WorkerAction workerActionTask = new WorkerAction();
        workerActionTask.setTaskClassifier(jobName + "_TaskClassifier");
        workerActionTask.setTaskApiVersion(1);
        workerActionTask.setTaskData(jobName + "_TaskClassifier Sample Test Task Data.");
        workerActionTask.setTaskDataEncoding(WorkerAction.TaskDataEncodingEnum.UTF8);
        workerActionTask.setTaskPipe(testQueue);
        workerActionTask.setTargetPipe(trackingToQueue);

        //create a job
        NewJob newJob = new NewJob();
        newJob.setName(jobName);
        newJob.setDescription(jobDesc);
        newJob.setExternalData(jobExternalData);
        newJob.setTask(workerActionTask);

        //get values of environment variables stored in the task message, making sure
        String statusCheckUrl = System.getenv("CAF_WEBSERVICE_URL");
        if(statusCheckUrl!=null) {
            statusCheckUrl = statusCheckUrl +
                "/partitions/" + defaultPartitionId + "/jobs/" + jobId + "/status";
        } else {
            throw new Exception("CAF_WEBSERVICE_URL environment variable is null.");
        }

        String trackingPipe = System.getenv("CAF_TRACKING_PIPE");
        if(trackingPipe==null)
            throw new Exception("CAF_TRACKING_PIPE environment variable is null.");

        String statusCheckTime = System.getenv("CAF_STATUS_CHECK_TIME");
        if(statusCheckTime==null)
            throw new Exception("CAF_STATUS_CHECK_TIME environment variable is null.");

        //create expectation object for comparing message on RabbitMQ
        JobServiceTrackingInfoExpectation expectation = new JobServiceTrackingInfoExpectation(
            defaultPartitionId, jobId, null, Long.parseLong(statusCheckTime) * 1000, statusCheckUrl,
            trackingPipe, trackingToQueue, true);

        testMessagesPutOnQueue(
            testQueue, expectation, defaultPartitionId, jobId, newJob, jobCorrelationId);
    }

    @Test
    public void testCreateRestrictedJob() throws Exception {
        testQueueManager = getQueueManager("basic task-pipe");
        final Supplier<TaskMessage> messageRetriever = getMessageFromQueue(testQueueManager);
        final String jobId = UUID.randomUUID().toString();
        final String correlationId = "1";
        final NewJob newJob = makeRestrictedJob(jobId, "basic", null);
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob, correlationId);

        final Job databaseJob = jobsApi.getJob(defaultPartitionId, jobId, correlationId);
        assertEquals(databaseJob.getId(), jobId, "job ID in database should be correct");
        assertEquals(databaseJob.getName(), newJob.getName(), "name in database should be correct");
        assertEquals(databaseJob.getDescription(), newJob.getDescription(),
            "description in database should be correct");
        assertEquals(databaseJob.getExternalData(), newJob.getExternalData(),
            "external data in database should be correct");

        final TaskMessage messageTask = messageRetriever.get();
        assertEquals(messageTask.getTaskClassifier(), "basic classifier",
            "classifier in message should come from job type definition");
        assertEquals(messageTask.getTaskApiVersion(), 74,
            "API version in message should come from job type definition");
        assertEquals(messageTask.getTo(), "basic task-pipe",
            "task pipe in message should come from configuration");
        assertEquals(messageTask.getTracking().getTrackTo(), "basic target-pipe",
            "target pipe in message should come from configuration");

        final JobTypeTestTaskData messageTaskData =
            objectMapper.readValue(messageTask.getTaskData(), JobTypeTestTaskData.class);
        assertEquals(messageTaskData.config.size(), 2,
            "configuration passed to task data script should contain 2 items: TASK_PIPE and TARGET_PIPE");
        assertEquals(messageTaskData.taskQueue, "basic task-pipe",
            "task pipe passed to task data script should come from configuration");
        assertEquals(messageTaskData.targetQueue, "basic target-pipe",
            "target pipe passed to task data script should come from configuration");
        assertEquals(messageTaskData.partitionIdent, defaultPartitionId,
            "partition ID passed to task data script should come from request");
        assertEquals(messageTaskData.jobIdent, jobId,
            "job ID passed to task data script should come from request");
        assertNull(messageTaskData.reqParams,
            "parameters passed to task data script should be null when not provided with request");
    }

    @Test
    public void testCreateRestrictedJobWithMissingType() throws Exception {
        final String jobId = UUID.randomUUID().toString();
        final String correlationId = "1";
        final NewJob newJob = makeRestrictedJob(jobId, "missing", null);
        assertThrowsApiException(Response.Status.BAD_REQUEST,
            () -> jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob, correlationId));
    }

    @Test
    public void testCreateRestrictedJobWithWrongCaseType() throws Exception {
        final String jobId = UUID.randomUUID().toString();
        final String correlationId = "1";
        final NewJob newJob = makeRestrictedJob(jobId, "BASIC", null);
        assertThrowsApiException(Response.Status.BAD_REQUEST,
            () -> jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob, correlationId));
    }

    @Test
    public void testCreateRestrictedJobWithTask() throws Exception {
        final String jobId = UUID.randomUUID().toString();
        final String correlationId = "1";
        final NewJob newJob = makeRestrictedJob(jobId, "basic", null);
        final NewJob newJobWithTask = makeJob(jobId, "testCreateRestrictedJobWithTask");
        newJob.setTask(newJobWithTask.getTask());
        assertThrowsApiException(Response.Status.BAD_REQUEST,
            () -> jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob, correlationId));
    }

    // the definition's extension isn't '.yaml', so it should be missing
    @Test
    public void testCreateRestrictedJobWithWrongDefinitionExtension() throws Exception {
        final String jobId = UUID.randomUUID().toString();
        final String correlationId = "1";
        final NewJob newJob = makeRestrictedJob(jobId, "wrong-ext", null);
        assertThrowsApiException(Response.Status.BAD_REQUEST,
            () -> jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob, correlationId));
    }

    @Test
    public void testCreateRestrictedJobWithInvalidParamsWithDefaultSchema() throws Exception {
        final String jobId = UUID.randomUUID().toString();
        final String correlationId = "1";
        final NewJob newJob = makeRestrictedJob(jobId, "basic", "params which should be null");
        assertThrowsApiException(Response.Status.BAD_REQUEST,
            () -> jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob, correlationId));
    }

    @Test
    public void testCreateRestrictedJobWithConfig() throws Exception {
        testQueueManager = getQueueManager("config task-pipe");
        final Supplier<TaskMessage> messageRetriever = getMessageFromQueue(testQueueManager);
        final String jobId = UUID.randomUUID().toString();
        final String correlationId = "1";
        final NewJob newJob = makeRestrictedJob(jobId, "config", null);
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob, correlationId);

        final JobTypeTestTaskData messageTaskData
            = objectMapper.readValue(messageRetriever.get().getTaskData(), JobTypeTestTaskData.class);
        assertEquals(messageTaskData.config.get("CAF_JOB_SERVICE_JOB_TYPE_CONFIG_UPPER"), "upper value",
                     "property with upper-case name should be passed to task data script");
        assertEquals(messageTaskData.config.get("CAF_JOB_SERVICE_JOB_TYPE_CONFIG_lower"), "lower value",
                     "property with lower-case name should be passed to task data script");
        assertEquals(messageTaskData.config.get("CAF_JOB_SERVICE_JOB_TYPE_CONFIG_multiple"), "multiple value",
                     "property specified with multiple cases should be passed to task data script "
                     + "in lower-case variant");
        assertEquals(messageTaskData.config.get("CAF_JOB_SERVICE_JOB_TYPE_CONFIG_MULTIPLE"), "multiple value",
                     "property specified with multiple cases should be passed to task data script "
                     + "in upper-case variant");
        assertEquals(messageTaskData.config.get("CAF_JOB_SERVICE_JOB_TYPE_CONFIG_nodesc"), "nodesc value",
                     "property with no description should be passed to task data script");
        assertEquals(messageTaskData.config.get("CAF_JOB_SERVICE_JOB_TYPE_CONFIG_number"), "123",
                     "property with numeric value should be passed to task data script as string");
    }

    @Test
    public void testCreateRestrictedJobWithParams() throws Exception {
        final Map<String, Object> params = new HashMap<>();
        params.put("s", "some value");
        params.put("a", 289);
        params.put("b", 4913);

        testQueueManager = getQueueManager("params task-pipe");
        final Supplier<TaskMessage> messageRetriever = getMessageFromQueue(testQueueManager);
        final String jobId = UUID.randomUUID().toString();
        final String correlationId = "1";
        final NewJob newJob = makeRestrictedJob(jobId, "params", params);
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob, correlationId);

        final JobTypeTestTaskData messageTaskData =
            objectMapper.readValue(messageRetriever.get().getTaskData(), JobTypeTestTaskData.class);
        assertEquals(messageTaskData.reqParams.get("s"), "some value",
            "param s should be passed to task data script");
        assertEquals(messageTaskData.reqParams.get("a"), 289,
            "param a should be passed to task data script");
        assertEquals(messageTaskData.reqParams.get("b"), 4913,
            "param b should be passed to task data script");
    }

    @Test
    public void testCreateRestrictedJobWithInvalidParams() throws Exception {
        final Map<String, Object> params = new HashMap<>();
        params.put("s", "some value");
        params.put("a", "not a number");
        params.put("b", 4913);

        final String jobId = UUID.randomUUID().toString();
        final String correlationId = "1";
        final NewJob newJob = makeRestrictedJob(jobId, "params", params);
        assertThrowsApiException(Response.Status.BAD_REQUEST,
            () -> jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob, correlationId));
    }

    @Test
    public void testCreateRestrictedJobWithNullTargetPipe() throws Exception {
        testQueueManager = getQueueManager("null-target-pipe task-pipe");
        final Supplier<TaskMessage> messageRetriever = getMessageFromQueue(testQueueManager);
        final String jobId = UUID.randomUUID().toString();
        final String correlationId = "1";
        final NewJob newJob = makeRestrictedJob(jobId, "null-target-pipe", null);
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob, correlationId);

        final TaskMessage messageTask = messageRetriever.get();
        assertNull(messageTask.getTracking().getTrackTo(),
            "target pipe should be missing from message");

        final JobTypeTestTaskData messageTaskData =
            objectMapper.readValue(messageTask.getTaskData(), JobTypeTestTaskData.class);
        assertNull(messageTaskData.targetQueue,
            "target pipe should not be passed to task data script");
    }

    @Test
    public void testCreateRestrictedJobWithComplexTransform() throws Exception {
        final Map<String, Object> params = new HashMap<>();
        params.put("forJoin", Arrays.asList("first", "second", "third"));
        params.put("forJsonConcat", "things\"which should\\be escaped");
        final Map<String, Number> paramsNumbers = new HashMap<>();
        paramsNumbers.put("a", -5);
        paramsNumbers.put("b", 0);
        params.put("forObjectMap", paramsNumbers);

        testQueueManager = getQueueManager("complex task-pipe");
        final Supplier<TaskMessage> messageRetriever = getMessageFromQueue(testQueueManager);
        final String jobId = UUID.randomUUID().toString();
        final String correlationId = "1";
        final NewJob newJob = makeRestrictedJob(jobId, "complex-transform", params);
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob, correlationId);

        final JobTypeTestTaskData messageTaskData =
            objectMapper.readValue(messageRetriever.get().getTaskData(), JobTypeTestTaskData.class);
        assertEquals(messageTaskData.reqParams.get("joined"), "first>second>third",
            "join function should work");
        assertEquals(messageTaskData.reqParams.get("concatenatedJson"),
            "{ \"start\": \"things\\\"which should\\\\be escaped_end\" }",
            "JSON escaping should work");
        final Map<String, Integer> messageTaskDataNumbers =
            (Map<String, Integer>) messageTaskData.reqParams.get("mappedObject");
        assertEquals(messageTaskDataNumbers.get("incremented:a").intValue(), -4,
            "number a should be incremented");
        assertEquals(messageTaskDataNumbers.get("incremented:b").intValue(), 1,
            "number b should be incremented");
    }

    // the definition is invalid (its task data script produces non-object), so expect HTTP 500
    @Test
    public void testCreateRestrictedJobWithInvalidTaskDataOutput() throws Exception {
        final String jobId = UUID.randomUUID().toString();
        final String correlationId = "1";
        final NewJob newJob = makeRestrictedJob(jobId, "invalid-output", null);
        assertThrowsApiException(Response.Status.INTERNAL_SERVER_ERROR,
            () -> jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob, correlationId));
    }

    @Test
    public void testCreateJobWithLabels() throws ApiException {
        final String jobId = UUID.randomUUID().toString();
        final String correlationId = "1";
        final NewJob job = makeJob(jobId, "testCreateJobWithLabels");
        job.getLabels().put("tag:1", "1");
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId, job, correlationId);

        //retrieve job using web method
        final Job retrievedJob = jobsApi.getJob(defaultPartitionId, jobId, correlationId);

        assertEquals(retrievedJob.getId(), jobId);
        assertEquals(retrievedJob.getName(), job.getName());
        assertTrue(retrievedJob.getLabels().containsKey("tag:1"));

        assertEquals(job.getLabels().get("tag:1"), retrievedJob.getLabels().get("tag:1"));
    }

    @Test
    public void testFilterJobsByLabel() throws ApiException {
        final String jobId1 = UUID.randomUUID().toString();
        final String jobId2 = UUID.randomUUID().toString();
        final String jobId3 = UUID.randomUUID().toString();
        final String correlationId = "1";
        final NewJob job = makeJob(jobId1, "testFilterJobsByLabel");
        job.getLabels().put("tag:1", "1");
        job.getLabels().put("tag:2", "2");
        final NewJob job2 = makeJob(jobId2, "testFilterJobsByLabel");
        job2.getLabels().put("tag:1", "1");
        job2.getLabels().put("owner", "bob");
        final NewJob job3 = makeJob(jobId3, "testFilterJobsByLabel");
        job3.getLabels().put("random", "label");
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId1, job, correlationId);
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId2, job2, correlationId);
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId3, job3, correlationId);

        //retrieve job using web method
        List<Job> jobs = jobsApi.getJobs(defaultPartitionId, correlationId, null, null,
                null, null, null, "tag:1", null);
        assertEquals(jobs.stream().map(Job::getId).collect(Collectors.toSet()), new HashSet<>(Arrays.asList(jobId1, jobId2)));

        //Assert all labels are returned, not just the ones used to filter the jobs
        Job dbJob1 = jobs.stream().filter(j -> j.getId().equals(jobId1)).findFirst().orElse(null);
        assertNotNull(dbJob1);
        assertTrue(dbJob1.getLabels().containsKey("tag:1"));
        assertTrue(dbJob1.getLabels().containsKey("tag:2"));

        Job dbJob2 = jobs.stream().filter(j -> j.getId().equals(jobId2)).findFirst().orElse(null);
        assertNotNull(dbJob2);
        assertTrue(dbJob2.getLabels().containsKey("tag:1"));
        assertTrue(dbJob2.getLabels().containsKey("owner"));

        jobs = jobsApi.getJobs(defaultPartitionId, correlationId, null, null, null,
                null, null, "tag:1,random", null);
        assertEquals(jobs.stream().map(Job::getId).collect(Collectors.toSet()), new HashSet<>(Arrays.asList(jobId1, jobId2, jobId3)));

        jobs = jobsApi.getJobs(defaultPartitionId, correlationId, null, null, null,
                null, null, "owner", null);
        assertEquals(jobs.stream().map(Job::getId).collect(Collectors.toSet()), new HashSet<>(Collections.singletonList(jobId2)));
    }

    @Test
    public void testPagingWithLabels() throws ApiException {
        final String jobId1 = UUID.randomUUID().toString();
        final String jobId2 = UUID.randomUUID().toString();
        final String jobId3 = UUID.randomUUID().toString();
        final String correlationId = UUID.randomUUID().toString();
        final NewJob job = makeJob(jobId1, "testPagingWithLabels");
        job.getLabels().put("tag:1", "1");
        job.getLabels().put("tag:2", "2");
        final NewJob job2 = makeJob(jobId2, "testPagingWithLabels");
        job2.getLabels().put("tag:1", "1");
        job2.getLabels().put("owner", "bob");
        final NewJob job3 = makeJob(jobId3, "testPagingWithLabels");
        job3.getLabels().put("random", "label");
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId1, job, correlationId);
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId2, job2, correlationId);
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId3, job3, correlationId);

        //retrieve job using web method
        List<Job> jobs = jobsApi.getJobs(defaultPartitionId, correlationId, null, null,
                2, 0, "createTime:asc", null, null);
        assertEquals(jobs.size(), 2);
        //Assert all labels are returned
        Job dbJob1 = jobs.stream().filter(j -> j.getId().equals(jobId1)).findFirst().orElse(null);
        assertNotNull(dbJob1);
        assertTrue(dbJob1.getLabels().containsKey("tag:1"));
        assertTrue(dbJob1.getLabels().containsKey("tag:2"));

        jobs = jobsApi.getJobs(defaultPartitionId, correlationId, null, null,
                5, 0, "createTime:asc", null, null);
        assertEquals(jobs.size(), 3);

        jobs = jobsApi.getJobs(defaultPartitionId, correlationId, null, null,
                2, 2, "createTime:asc", null, null);
        assertEquals(jobs.size(), 1);
    }

    @Test
    public void testInvalidLabelFormat() {
        final String jobId = UUID.randomUUID().toString();
        final String correlationId = "1";
        final NewJob job = makeJob(jobId, "testFilterJobsByLabel");
        job.getLabels().put(", ", "1");
        boolean exceptionThrown = false;
        try {
            jobsApi.createOrUpdateJob(defaultPartitionId, jobId, job, correlationId);
        } catch (final ApiException e) {
            exceptionThrown = true;
            assertTrue(e.getMessage().contains("A provided label name contains an invalid character, only alphanumeric, '-' and '_' are supported"));
        }
        assertTrue(exceptionThrown);
        exceptionThrown = false;
        job.getLabels().clear();
        job.getLabels().put("    ", "asd");
        try {
            jobsApi.createOrUpdateJob(defaultPartitionId, jobId, job, correlationId);
        } catch (final ApiException e) {
            exceptionThrown = true;
            assertTrue(e.getMessage().contains("A provided label name contains an invalid character, only alphanumeric, '-' and '_' are supported"));
        }
        assertTrue(exceptionThrown);
        exceptionThrown = false;
        job.getLabels().clear();
        job.getLabels().put("tag name", "value");
        try {
            jobsApi.createOrUpdateJob(defaultPartitionId, jobId, job, correlationId);
        } catch (final ApiException e) {
            exceptionThrown = true;
            assertTrue(e.getMessage().contains("A provided label name contains an invalid character, only alphanumeric, '-' and '_' are supported"));
        }
        assertTrue(exceptionThrown);
    }

    public void testMessagesPutOnQueue(
        final String taskQueue,
        final JobServiceTrackingInfoExpectation expectation,
        final String partitionId,
        String jobId,
        NewJob newJob,
        String jobCorrelationId
    ) throws Exception {
        try (QueueManager queueManager = getQueueManager(taskQueue)) {
            ExecutionContext context = new ExecutionContext(false);
            context.initializeContext();
            Timer timer = getTimer(context);
            Thread thread = queueManager.start(new JobServiceOutputDeliveryHandler(context, expectation));

            //call web method to create the new job and put message on queue
            jobsApi.createOrUpdateJob(partitionId, jobId, newJob, jobCorrelationId);

            TestResult result = context.getTestResult();
            assertTrue(result.isSuccess());
        }
    }

    /**
     * Retrieve a single message from a queue.  Call this before triggering the message publish.
     *
     * @param queueManager Configured with a specific queue
     * @return Function to call which waits for the message and returns it.  Throws
     *         {@link AssertionError} on timeout.
     * @throws Exception
     */
    private static Supplier<TaskMessage> getMessageFromQueue(final QueueManager queueManager)
        throws Exception
    {
        final ExecutionContext context = new ExecutionContext(false);
        context.initializeContext();
        final Timer timer = getTimer(context);

        final List<TaskMessage> result = new ArrayList<>();
        queueManager.start(message -> {
            result.add(message);
            context.finishedSuccessfully();
        });

        return () -> {
            assertTrue(context.getTestResult().isSuccess(),
                "should publish a message to the queue");
            return result.get(0);
        };
    }

    private static QueueManager getQueueManager(final String queueName) throws IOException, TimeoutException {
        //Test messages are published to the target pipe specified in the test (jobservice-test-input-1).
        //The test will consume these messages and assert that the results are as expected.
        QueueServices queueServices = QueueServicesFactory.create(rabbitConfiguration, queueName, workerServices.getCodec());
        boolean debugEnabled = SettingsProvider.defaultProvider.getBooleanSetting(SettingNames.createDebugMessage,false);
        return new QueueManager(queueServices, workerServices, debugEnabled);
    }

    private static Timer getTimer(ExecutionContext context) {
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
     * Format of task data produced by all test job type definitions.  We use a common structure to
     * simplify assertions.  Fields purposely have different values from `taskDataScript` input to
     * ensure we're getting the output and not the input.
     */
    final static class JobTypeTestTaskData {
        public Map<String, String> config;
        public String taskQueue;
        public String targetQueue;
        public String partitionIdent;
        public String jobIdent;
        public Map<String, Object> reqParams;
    }

}
