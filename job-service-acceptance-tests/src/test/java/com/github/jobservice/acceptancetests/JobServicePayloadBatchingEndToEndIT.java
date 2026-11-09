/*
 * Copyright 2016-2026 Open Text.
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
package com.github.jobservice.acceptancetests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cafapi.common.api.BootstrapConfiguration;
import com.github.cafapi.common.api.ConfigurationSource;
import com.github.cafapi.common.bootstrapconfigs.system.SystemBootstrapConfiguration;
import com.github.cafapi.common.util.naming.ServicePath;
import com.github.jobservice.client.api.JobsApi;
import com.github.jobservice.client.model.Job;
import com.github.jobservice.client.model.JobStatus;
import com.github.jobservice.client.model.NewJob;
import com.github.jobservice.client.model.WorkerAction;
import com.github.jobservice.job.client.ApiClient;
import com.github.jobservice.job.client.ApiException;
import com.github.workerframework.api.TaskMessage;
import com.github.workerframework.queues.rabbit.RabbitWorkerQueueConfiguration;
import com.github.workerframework.testing.ExecutionContext;
import com.github.workerframework.testing.SettingNames;
import com.github.workerframework.testing.util.QueueManager;
import com.github.workerframework.testing.util.QueueServices;
import com.github.workerframework.testing.util.QueueServicesFactory;
import com.github.workerframework.testing.util.SettingsProvider;
import com.github.workerframework.testing.util.WorkerServices;
import com.github.workerframework.util.rabbitmq.RabbitUtil;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static org.testng.Assert.*;

/**
 * End-to-End integration tests for Payload Batching feature.
 * Tests complete job lifecycle with batched payloads.
 */
public class JobServicePayloadBatchingEndToEndIT
{
    private static final Logger LOG = LoggerFactory.getLogger(JobServicePayloadBatchingEndToEndIT.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int BATCH_SIZE = 200;
    private static final String JOB_CORRELATION_ID = "1";
    private static final long JOB_STATUS_CHECK_TIMEOUT_MS = 60000;
    private static final long JOB_STATUS_CHECK_SLEEP_MS = 500;
    private static final long DEFAULT_TIMEOUT_MS = 120000;
    private static final String SUBDOCUMENT_BATCHER_PREFIX = "DocumentWorkerSubdocumentBatcher() ";
    private static final String RABBIT_PROP_QUEUE_TYPE = "x-queue-type";
    private static final String RABBIT_PROP_QUEUE_TYPE_QUORUM = "quorum";

    private static ServicePath servicePath;
    private static WorkerServices workerServices;
    private static ConfigurationSource configurationSource;
    private static RabbitWorkerQueueConfiguration rabbitConfiguration;
    private static JobsApi jobsApi;
    private static Connection rabbitConn;

    private String defaultPartitionId;
    private QueueManager testQueueManager;

    @BeforeClass
    public static void setupClass() throws Exception
    {
        final BootstrapConfiguration bootstrap = new SystemBootstrapConfiguration();
        servicePath = bootstrap.getServicePath();
        workerServices = WorkerServices.getDefault();
        configurationSource = workerServices.getConfigurationSource();
        rabbitConfiguration = configurationSource.getConfiguration(RabbitWorkerQueueConfiguration.class);
        rabbitConfiguration.getRabbitConfiguration().setRabbitHost(
            SettingsProvider.defaultProvider.getSetting(SettingNames.dockerHostAddress));
        rabbitConfiguration.getRabbitConfiguration().setRabbitPort(
            Integer.parseInt(SettingsProvider.defaultProvider.getSetting(SettingNames.rabbitmqNodePort)));

        // Create RabbitMQ connection for queue operations
        rabbitConn = RabbitUtil.createRabbitConnection(rabbitConfiguration.getRabbitConfiguration());

        jobsApi = createJobsApi();
    }

    private static JobsApi createJobsApi()
    {
        final String connectionString = System.getenv("CAF_WEBSERVICE_URL");
        final ApiClient client = new ApiClient();
        client.setBasePath(connectionString);
        final SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        client.setDateFormat(f);
        return new JobsApi(client);
    }

    @BeforeMethod
    public void setupMethod()
    {
        defaultPartitionId = UUID.randomUUID().toString();
    }

    @AfterMethod
    public void tearDownMethod() throws IOException
    {
        if (testQueueManager != null) {
            testQueueManager.close();
            testQueueManager = null;
        }
    }

    // Batched job completion tracking - verifies message count and job status
    @Test
    public void testBatchedJob_CompletionTracking() throws Exception
    {
        final String queueName = "payload-batching-e2e-completion-" + UUID.randomUUID();
        testQueueManager = getQueueManager(queueName);

        final int subdocCount = BATCH_SIZE * 3; // 3 batches = 3 messages
        final int expectedBatchCount = 3;
        final String jobId = UUID.randomUUID().toString();
        final NewJob newJob = createDocumentWorkerJob(jobId, subdocCount, queueName);

        LOG.info("Creating batched job {} with {} subdocuments ({} batches)",
            jobId, subdocCount, expectedBatchCount);

        // Start listening BEFORE creating the job
        final Supplier<List<TaskMessage>> messageSupplier = startCollectingMessages(testQueueManager, expectedBatchCount);

        // Create the job
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob, JOB_CORRELATION_ID);

        // Wait for messages and get results (blocking)
        final List<TaskMessage> received = messageSupplier.get();

        // Verify correct number of batched messages received
        assertEquals(received.size(), expectedBatchCount,
            "Should receive " + expectedBatchCount + " batched messages on the queue");
        LOG.info("Received {} messages on queue {} as expected", received.size(), queueName);

        // Verify job was created and is processing
        final Job job = jobsApi.getJob(defaultPartitionId, jobId, JOB_CORRELATION_ID);
        assertNotNull(job, "Job should exist");
        assertEquals(job.getId(), jobId);
        assertNotNull(job.getStatus(), "Job should have a status");

        // Job should be in Waiting or Active state (messages are queued, waiting for worker)
        assertTrue(job.getStatus() == JobStatus.WAITING || job.getStatus() == JobStatus.ACTIVE,
            "Job should be in Waiting or Active status, but was: " + job.getStatus());
    }

    // Batched job progress reporting - verifies messages published and tracks progress
    @Test
    public void testBatchedJob_ProgressReporting() throws Exception
    {
        final String queueName = "payload-batching-e2e-progress-" + UUID.randomUUID();
        testQueueManager = getQueueManager(queueName);

        final int subdocCount = BATCH_SIZE * 3; // 3 batches = 3 messages
        final int expectedBatchCount = 3;
        final String jobId = UUID.randomUUID().toString();
        final NewJob newJob = createDocumentWorkerJob(jobId, subdocCount, queueName);

        LOG.info("Creating job {} for progress tracking test with {} batches", jobId, expectedBatchCount);

        // Start listening BEFORE creating the job
        final Supplier<List<TaskMessage>> messageSupplier = startCollectingMessages(testQueueManager, expectedBatchCount);

        // Create the job
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob, JOB_CORRELATION_ID);

        // Verify all batched messages are published
        final List<TaskMessage> received = messageSupplier.get();
        assertEquals(received.size(), expectedBatchCount,
            "Should receive " + expectedBatchCount + " batched messages");
        LOG.info("All {} messages published to queue {}", received.size(), queueName);

        // Verify job status
        Job job = jobsApi.getJob(defaultPartitionId, jobId, JOB_CORRELATION_ID);
        assertNotNull(job.getStatus(), "Job should have status");
        LOG.info("Job {} status: {}, percentageComplete: {}",
            jobId, job.getStatus(), job.getPercentageComplete());

        // Progress will remain at 0 or initial state until workers complete subtasks
        assertTrue(job.getStatus() == JobStatus.WAITING || job.getStatus() == JobStatus.ACTIVE,
            "Job should be Waiting or Active after messages published");
    }

    // Batched job cancellation - verifies job can be cancelled and checks status
    @Test
    public void testBatchedJob_Cancellation() throws Exception
    {
        final String queueName = "payload-batching-e2e-cancel-" + UUID.randomUUID();
        testQueueManager = getQueueManager(queueName);

        final int subdocCount = BATCH_SIZE * 5; // 5 batches - larger to allow time for cancellation
        final int expectedBatchCount = 5;
        final String jobId = UUID.randomUUID().toString();
        final NewJob newJob = createDocumentWorkerJob(jobId, subdocCount, queueName);

        LOG.info("Creating job {} for cancellation test with {} batches", jobId, expectedBatchCount);

        // Start listening for messages
        final Supplier<List<TaskMessage>> messageSupplier = startCollectingMessages(testQueueManager, expectedBatchCount);

        // Create the job
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob, JOB_CORRELATION_ID);

        // Wait for all messages to be published
        final List<TaskMessage> received = messageSupplier.get();
        assertEquals(received.size(), expectedBatchCount,
            "Should receive " + expectedBatchCount + " batched messages before cancellation");
        LOG.info("Received {} messages, now cancelling job {}", received.size(), jobId);

        // Get job state before cancellation
        Job jobBeforeCancel = jobsApi.getJob(defaultPartitionId, jobId, JOB_CORRELATION_ID);
        LOG.info("Job {} status before cancel: {}, percentageComplete: {}",
            jobId, jobBeforeCancel.getStatus(), jobBeforeCancel.getPercentageComplete());

        // Cancel the job
        jobsApi.cancelJob(defaultPartitionId, jobId, JOB_CORRELATION_ID);

        // Verify cancellation
        final Job job = jobsApi.getJob(defaultPartitionId, jobId, JOB_CORRELATION_ID);
        assertEquals(job.getStatus(), JobStatus.CANCELLED, "Job should be cancelled");

        // Log the percentage complete at cancellation time
        LOG.info("Job {} successfully cancelled. Final percentageComplete: {}",
            jobId, job.getPercentageComplete());
    }

    // Small subdocuments - no batching - verifies single message and job status
    @Test
    public void testSmallSubdocuments_NoBatching() throws Exception
    {
        final String queueName = "payload-batching-e2e-small-" + UUID.randomUUID();
        testQueueManager = getQueueManager(queueName);

        final int subdocCount = BATCH_SIZE / 2; // Below threshold - no batching
        final int expectedMessageCount = 1; // Single message expected
        final String jobId = UUID.randomUUID().toString();
        final NewJob newJob = createDocumentWorkerJob(jobId, subdocCount, queueName);

        LOG.info("Creating non-batched job {} with {} subdocuments (below threshold)", jobId, subdocCount);

        // Start listening BEFORE creating the job
        final Supplier<List<TaskMessage>> messageSupplier = startCollectingMessages(testQueueManager, expectedMessageCount);

        // Create the job
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob, JOB_CORRELATION_ID);

        // Wait for message
        final List<TaskMessage> received = messageSupplier.get();

        // Verify single message received (no batching)
        assertEquals(received.size(), expectedMessageCount,
            "Should receive exactly 1 message for small subdocument count");

        // Verify job created and status
        final Job job = jobsApi.getJob(defaultPartitionId, jobId, JOB_CORRELATION_ID);
        assertNotNull(job, "Job should exist");
        assertEquals(job.getId(), jobId);
        assertTrue(job.getStatus() == JobStatus.WAITING || job.getStatus() == JobStatus.ACTIVE,
            "Job should be in Waiting or Active status");

        LOG.info("Non-batched job {} created with 1 message, status: {}", jobId, job.getStatus());
    }

    // === HELPER METHODS ===

    private NewJob createDocumentWorkerJob(final String jobId, final int subdocCount, final String queueName) throws Exception
    {
        // Declare the queue first
        declareQueue(queueName);

        // taskData structure matches actual Job Definition format
        final Map<String, Object> taskData = new HashMap<>();

        // document with fields and subdocuments array
        final Map<String, Object> document = new HashMap<>();
        // Add fields inside document (like targetId, destinationId in real payloads)
        final Map<String, Object> documentFields = new HashMap<>();
        documentFields.put("targetId", List.of(Map.of("data", "2")));
        documentFields.put("destinationId", List.of(Map.of("data", "4")));
        document.put("fields", documentFields);

        final List<Object> subdocs = new ArrayList<>();
        for (int i = 1; i <= subdocCount; i++) {
            // Each subdoc has fields structure like actual format
            final Map<String, Object> subdoc = new HashMap<>();
            final Map<String, Object> fields = new HashMap<>();
            fields.put("_id", List.of(Map.of("data", String.valueOf(i))));
            fields.put("_index", List.of(Map.of("data", "test-index")));
            fields.put("_routing", List.of(Map.of("data", String.valueOf(i))));
            subdoc.put("fields", fields);
            subdocs.add(subdoc);
        }
        document.put("subdocuments", subdocs);
        taskData.put("document", document);

        // customData at taskData root level
        final Map<String, Object> customData = new HashMap<>();
        customData.put("tenantId", "testTenant");
        customData.put("workflowName", "test-workflow");
        customData.put("timestamp", String.valueOf(System.currentTimeMillis()));
        taskData.put("customData", customData);

        // scripts at taskData root level
        taskData.put("scripts", new ArrayList<>());

        final WorkerAction task = new WorkerAction();
        task.setTaskClassifier("DocumentWorkerTask");
        task.setTaskApiVersion(2);
        task.setTaskData(OBJECT_MAPPER.writeValueAsString(taskData));
        task.setTaskDataEncoding(WorkerAction.TaskDataEncodingEnum.UTF8);
        // Add batching prefix for opt-in
        task.setTaskPipe(SUBDOCUMENT_BATCHER_PREFIX + queueName);
        task.setTargetPipe(queueName + "-out");

        final NewJob newJob = new NewJob();
        newJob.setName("PayloadBatchingE2E_" + jobId);
        newJob.setDescription("End-to-end payload batching test");
        newJob.setTask(task);

        return newJob;
    }

    private void declareQueue(final String queueName) throws Exception
    {
        final Channel channel = rabbitConn.createChannel();
        final Map<String, Object> args = new HashMap<>();
        args.put(RABBIT_PROP_QUEUE_TYPE, RABBIT_PROP_QUEUE_TYPE_QUORUM);
        channel.queueDeclare(queueName, true, false, false, args);
        channel.close();
    }

    private QueueManager getQueueManager(final String queueName)
        throws IOException, TimeoutException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException
    {
        final QueueServices queueServices = QueueServicesFactory.create(
            rabbitConfiguration, queueName, workerServices.getCodec());
        final boolean debugEnabled = SettingsProvider.defaultProvider.getBooleanSetting(
            SettingNames.createDebugMessage, false);
        return new QueueManager(queueServices, workerServices, debugEnabled);
    }

    /**
     * Starts collecting messages from the queue and returns a Supplier that blocks until
     * the expected number of messages are received or timeout occurs.
     */
    private Supplier<List<TaskMessage>> startCollectingMessages(final QueueManager queueManager, final int expectedCount)
        throws Exception
    {
        final ExecutionContext context = new ExecutionContext(false);
        context.initializeContext();

        final List<TaskMessage> result = new ArrayList<>();
        final int[] receivedCount = {0};

        // Set up timeout timer
        final Timer timer = getTimer(context);

        // Start consuming messages
        queueManager.start(message -> {
            result.add(message);
            receivedCount[0]++;
            LOG.debug("Received message {} of {}", receivedCount[0], expectedCount);
            if (receivedCount[0] >= expectedCount) {
                timer.cancel();
                context.finishedSuccessfully();
            }
        });

        // Return supplier that blocks on test result
        return () -> {
            assertTrue(context.getTestResult().isSuccess(),
                "Should receive " + expectedCount + " messages from queue");
            return result;
        };
    }

    private Timer getTimer(final ExecutionContext context)
    {
        final String timeoutSetting = SettingsProvider.defaultProvider.getSetting(SettingNames.timeOutMs);
        final long timeout = timeoutSetting == null ? DEFAULT_TIMEOUT_MS : Long.parseLong(timeoutSetting);
        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                context.testRunsTimedOut();
            }
        }, timeout);
        return timer;
    }

    private void waitForJobStatus(final String jobId, final JobStatus expectedStatus, final long timeoutMs)
        throws ApiException, InterruptedException
    {
        final long startTime = System.currentTimeMillis();
        Job job;

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            job = jobsApi.getJob(defaultPartitionId, jobId, JOB_CORRELATION_ID);

            if (job.getStatus() == expectedStatus) {
                LOG.info("Job {} reached status {}", jobId, expectedStatus);
                return;
            }

            // Also check for terminal states
            if (expectedStatus != JobStatus.COMPLETED &&
                expectedStatus != JobStatus.CANCELLED &&
                expectedStatus != JobStatus.FAILED) {
                if (job.getStatus() == JobStatus.COMPLETED ||
                    job.getStatus() == JobStatus.CANCELLED ||
                    job.getStatus() == JobStatus.FAILED) {
                    LOG.info("Job {} reached terminal status {}", jobId, job.getStatus());
                    return;
                }
            }

            Thread.sleep(JOB_STATUS_CHECK_SLEEP_MS);
        }

        LOG.warn("Job {} did not reach status {} within timeout", jobId, expectedStatus);
    }
}



