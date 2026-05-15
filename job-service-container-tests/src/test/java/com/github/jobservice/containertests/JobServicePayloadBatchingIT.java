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
package com.github.jobservice.containertests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cafapi.common.api.BootstrapConfiguration;
import com.github.cafapi.common.bootstrapconfigs.system.SystemBootstrapConfiguration;
import com.github.cafapi.common.util.naming.ServicePath;
import com.github.jobservice.client.api.JobsApi;
import com.github.jobservice.client.model.Job;
import com.github.jobservice.client.model.NewJob;
import com.github.jobservice.client.model.WorkerAction;
import com.github.jobservice.job.client.ApiClient;
import com.github.workerframework.api.TaskMessage;
import com.github.workerframework.api.TrackingInfo;
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
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.testng.Assert.*;

/**
 * Integration tests for Payload Batching feature.
 * Tests message delivery with containers for batched DocumentWorkerTask payloads.
 */
public class JobServicePayloadBatchingIT
{
    private static final Logger LOG = LoggerFactory.getLogger(JobServicePayloadBatchingIT.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int BATCH_SIZE = 200;
    private static final long DEFAULT_TIMEOUT_MS = 120000;
    private static final Pattern SUBTASK_ID_PATTERN = Pattern.compile(".*\\.\\d+\\*?$");
    private static final String SUBDOCUMENT_BATCHER_PREFIX = "DocumentWorkerSubdocumentBatcher() ";

    private String connectionString;
    private String defaultPartitionId;
    private ApiClient client = new ApiClient();
    private JobsApi jobsApi;
    private QueueManager testQueueManager;
    private Connection rabbitConn;

    private static ServicePath servicePath;
    private static WorkerServices workerServices;
    private static RabbitWorkerQueueConfiguration rabbitConfiguration;

    @BeforeTest
    public void setup() throws Exception
    {
        connectionString = System.getenv("webserviceurl");
        client.setBasePath(connectionString);
        final SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        client.setDateFormat(f);
        jobsApi = new JobsApi(client);

        final BootstrapConfiguration bootstrap = new SystemBootstrapConfiguration();
        servicePath = bootstrap.getServicePath();
        workerServices = WorkerServices.getDefault();
        rabbitConfiguration = workerServices.getConfigurationSource()
            .getConfiguration(RabbitWorkerQueueConfiguration.class);
        rabbitConfiguration.getRabbitConfiguration().setRabbitHost(
            SettingsProvider.defaultProvider.getSetting(SettingNames.dockerHostAddress));
        rabbitConfiguration.getRabbitConfiguration().setRabbitPort(
            Integer.parseInt(SettingsProvider.defaultProvider.getSetting(SettingNames.rabbitmqNodePort)));
        rabbitConn = RabbitUtil.createRabbitConnection(rabbitConfiguration.getRabbitConfiguration());
    }

    @AfterTest
    public void tearDown() throws IOException
    {
        if (testQueueManager != null) {
            testQueueManager.close();
        }
        if (rabbitConn != null) {
            rabbitConn.close();
        }
    }

    @BeforeMethod
    public void setupMethod()
    {
        defaultPartitionId = UUID.randomUUID().toString();
    }

    // Batched messages received on queue
    @Test
    public void testBatchedMessages_ReceivedOnQueue() throws Exception
    {
        final String queueName = "payload-batching-test-" + UUID.randomUUID();
        testQueueManager = getQueueManager(queueName);

        final int subdocCount = BATCH_SIZE * 3 + 50; // 3.25 batches = 4 messages
        final String jobId = UUID.randomUUID().toString();
        final NewJob newJob = createDocumentWorkerJob(jobId, subdocCount, queueName, true);

        // Start listening BEFORE creating the job
        final Supplier<List<TaskMessage>> messageSupplier = startCollectingMessages(testQueueManager, 4);

        // Create the job (messages will be published)
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob, "1");

        // Wait for messages and get results (blocking)
        final List<TaskMessage> received = messageSupplier.get();
        assertEquals(received.size(), 4, "Should receive 4 batched messages");
    }

    // Batched messages have correct subtask IDs in BOTH fields
    @Test
    public void testBatchedMessages_CorrectSubtaskIds() throws Exception
    {
        final String queueName = "payload-batching-subtaskid-test-" + UUID.randomUUID();
        testQueueManager = getQueueManager(queueName);

        final int subdocCount = BATCH_SIZE * 2 + 50; // 2.25 batches = 3 messages
        final String jobId = UUID.randomUUID().toString();
        final NewJob newJob = createDocumentWorkerJob(jobId, subdocCount, queueName, true);

        // Start listening BEFORE creating the job
        final Supplier<List<TaskMessage>> messageSupplier = startCollectingMessages(testQueueManager, 3);

        // Create the job (messages will be published)
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob, "1");

        // Wait for messages and get results (blocking)
        final List<TaskMessage> received = messageSupplier.get();
        assertEquals(received.size(), 3);

        // Verify subtask IDs in both fields
        for (int i = 0; i < received.size(); i++) {
            final TaskMessage msg = received.get(i);
            final String taskId = msg.getTaskId();
            final TrackingInfo tracking = msg.getTracking();
            final String jobTaskId = tracking.getJobTaskId();

            // Both should have subtask suffix
            assertTrue(SUBTASK_ID_PATTERN.matcher(taskId).matches(),
                "TaskMessage.taskId should have subtask suffix: " + taskId);
            assertTrue(SUBTASK_ID_PATTERN.matcher(jobTaskId).matches(),
                "TrackingInfo.jobTaskId should have subtask suffix: " + jobTaskId);

            // Extract and compare suffixes
            final String taskSuffix = taskId.substring(taskId.lastIndexOf('.'));
            final String jobTaskSuffix = jobTaskId.substring(jobTaskId.lastIndexOf('.'));
            assertEquals(taskSuffix, jobTaskSuffix, "Suffixes should match");

            // Last batch should have asterisk
            if (i == received.size() - 1) {
                assertTrue(taskId.endsWith("*"), "Last taskId should end with *");
                assertTrue(jobTaskId.endsWith("*"), "Last jobTaskId should end with *");
            } else {
                assertFalse(taskId.endsWith("*"), "Non-final taskId should not end with *");
                assertFalse(jobTaskId.endsWith("*"), "Non-final jobTaskId should not end with *");
            }
        }
    }

    // Batched messages have valid payload structure
    @Test
    @SuppressWarnings("unchecked")
    public void testBatchedMessages_PayloadStructure() throws Exception
    {
        final String queueName = "payload-batching-payload-test-" + UUID.randomUUID();
        testQueueManager = getQueueManager(queueName);

        final int subdocCount = BATCH_SIZE + 50; // 2 messages
        final String jobId = UUID.randomUUID().toString();
        final NewJob newJob = createDocumentWorkerJob(jobId, subdocCount, queueName, true);

        // Start listening BEFORE creating the job
        final Supplier<List<TaskMessage>> messageSupplier = startCollectingMessages(testQueueManager, 2);

        // Create the job (messages will be published)
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob, "1");

        // Wait for messages and get results (blocking)
        final List<TaskMessage> received = messageSupplier.get();
        assertEquals(received.size(), 2);

        // Expected document fields (same as created in createDocumentWorkerJob)
        final Map<String, Object> expectedDocumentFields = new HashMap<>();
        expectedDocumentFields.put("targetId", List.of(Map.of("data", "2")));
        expectedDocumentFields.put("destinationId", List.of(Map.of("data", "4")));

        // Expected customData (same as created in createDocumentWorkerJob)
        final String expectedTenantId = "testTenant";
        final String expectedWorkflowName = "test-workflow";

        int totalSubdocs = 0;
        for (final TaskMessage msg : received) {
            // Parse taskData
            final Map<String, Object> taskData = OBJECT_MAPPER.readValue(msg.getTaskData(), Map.class);
            assertNotNull(taskData, "TaskData should not be null");
            assertTrue(taskData.containsKey("document"), "Should have document field");

            final Map<String, Object> document = (Map<String, Object>) taskData.get("document");

            // Verify fields inside document are preserved with correct values
            assertTrue(document.containsKey("fields"), "Document should have fields");
            final Map<String, Object> documentFields = (Map<String, Object>) document.get("fields");
            assertNotNull(documentFields, "Document fields should not be null");
            assertEquals(documentFields, expectedDocumentFields, "Document fields should match original values");

            // Verify customData is preserved with correct values
            assertTrue(taskData.containsKey("customData"), "Should have customData field");
            final Map<String, Object> customData = (Map<String, Object>) taskData.get("customData");
            assertNotNull(customData, "customData should not be null");
            assertEquals(customData.get("tenantId"), expectedTenantId, "tenantId should match");
            assertEquals(customData.get("workflowName"), expectedWorkflowName, "workflowName should match");
            assertNotNull(customData.get("timestamp"), "timestamp should be present");

            // Verify scripts is preserved
            assertTrue(taskData.containsKey("scripts"), "Should have scripts field");
            final List<Object> scripts = (List<Object>) taskData.get("scripts");
            assertNotNull(scripts, "scripts should not be null");
            assertTrue(scripts.isEmpty(), "scripts should be empty list");

            // Verify subdocuments
            assertTrue(document.containsKey("subdocuments"), "Should have subdocuments");

            final List<Object> subdocs = (List<Object>) document.get("subdocuments");
            assertNotNull(subdocs, "Subdocuments should not be null");
            assertTrue(subdocs.size() <= BATCH_SIZE, "Batch size should not exceed " + BATCH_SIZE);

            totalSubdocs += subdocs.size();
        }

        assertEquals(totalSubdocs, subdocCount, "Total subdocuments should match original");
    }

    // Non-DocumentWorkerTask job - single message
    @Test
    public void testNonBatchedJob_SingleMessage() throws Exception
    {
        final String queueName = "payload-batching-nonbatch-test-" + UUID.randomUUID();
        testQueueManager = getQueueManager(queueName);

        final String jobId = UUID.randomUUID().toString();
        final NewJob newJob = createNonBatchedJob(jobId, queueName);

        // Start listening BEFORE creating the job
        final Supplier<List<TaskMessage>> messageSupplier = startCollectingMessages(testQueueManager, 1);

        // Create the job (messages will be published)
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob, "1");

        // Wait for messages and get results (blocking)
        final List<TaskMessage> received = messageSupplier.get();
        assertEquals(received.size(), 1, "Should receive single message");

        // Verify no subtask suffix
        final String taskId = received.get(0).getTaskId();
        assertFalse(taskId.contains("."), "Non-batched taskId should not have subtask suffix");
    }

    // Small DocumentWorkerTask - single message
    @Test
    public void testSmallDocumentWorkerTask_SingleMessage() throws Exception
    {
        final String queueName = "payload-batching-small-test-" + UUID.randomUUID();
        testQueueManager = getQueueManager(queueName);

        final int subdocCount = BATCH_SIZE / 2; // Below threshold
        final String jobId = UUID.randomUUID().toString();
        final NewJob newJob = createDocumentWorkerJob(jobId, subdocCount, queueName, true);

        // Start listening BEFORE creating the job
        final Supplier<List<TaskMessage>> messageSupplier = startCollectingMessages(testQueueManager, 1);

        // Create the job (messages will be published)
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob, "1");

        // Wait for messages and get results (blocking)
        final List<TaskMessage> received = messageSupplier.get();
        assertEquals(received.size(), 1, "Should receive single message for small subdocs");

        // Verify no subtask suffix
        final String taskId = received.get(0).getTaskId();
        assertFalse(taskId.contains(".") && SUBTASK_ID_PATTERN.matcher(taskId).matches(),
            "Small DocumentWorkerTask should not have subtask suffix");
    }

    // DocumentWorkerTask WITHOUT prefix but with large subdocs - single message (not opted in)
//    @Test
//    public void testDocumentWorkerTask_NoPrefix_SingleMessage() throws Exception
//    {
//        final String queueName = "payload-batching-noprefix-test-" + UUID.randomUUID();
//        testQueueManager = getQueueManager(queueName);
//
//        final int subdocCount = BATCH_SIZE * 3; // Above threshold but no prefix
//        final String jobId = UUID.randomUUID().toString();
//        final NewJob newJob = createDocumentWorkerJob(jobId, subdocCount, queueName, false); // WITHOUT prefix
//
//        // Start listening BEFORE creating the job
//        final Supplier<List<TaskMessage>> messageSupplier = startCollectingMessages(testQueueManager, 1);
//
//        // Create the job (messages will be published)
//        jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob, "1");
//
//        // Wait for messages and get results (blocking)
//        final List<TaskMessage> received = messageSupplier.get();
//        assertEquals(received.size(), 1, "Should receive single message when prefix not present (not opted in)");
//
//        // Verify no subtask suffix
//        final String taskId = received.get(0).getTaskId();
//        assertFalse(SUBTASK_ID_PATTERN.matcher(taskId).matches(),
//            "DocumentWorkerTask without prefix should not have subtask suffix");
//    }

    // Verify task pipe in batch messages has prefix stripped
    @Test
    public void testBatchedMessages_TaskPipeStripped() throws Exception
    {
        final String queueName = "payload-batching-stripped-test-" + UUID.randomUUID();
        testQueueManager = getQueueManager(queueName);

        final int subdocCount = BATCH_SIZE + 50; // 2 messages
        final String jobId = UUID.randomUUID().toString();
        final NewJob newJob = createDocumentWorkerJob(jobId, subdocCount, queueName, true); // WITH prefix

        // Start listening BEFORE creating the job
        final Supplier<List<TaskMessage>> messageSupplier = startCollectingMessages(testQueueManager, 2);

        // Create the job (messages will be published)
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId, newJob, "1");

        // Wait for messages and get results (blocking)
        final List<TaskMessage> received = messageSupplier.get();
        assertEquals(received.size(), 2);

        // Verify the task pipe in each message does NOT have the prefix
        for (final TaskMessage msg : received) {
            final String toQueue = msg.getTo();
            assertFalse(toQueue.startsWith(SUBDOCUMENT_BATCHER_PREFIX),
                "Task pipe should have prefix stripped, got: " + toQueue);
            assertEquals(toQueue, queueName, "Task pipe should be the original queue name");
        }
    }

    // === HELPER METHODS ===

    /**
     * Creates a DocumentWorkerTask job with optional batching prefix.
     * @param jobId Job ID
     * @param subdocCount Number of subdocuments
     * @param queueName Target queue name
     * @param withBatchingPrefix If true, adds DocumentWorkerSubdocumentBatcher() prefix to task pipe
     */
    private NewJob createDocumentWorkerJob(final String jobId, final int subdocCount, final String queueName,
                                           final boolean withBatchingPrefix) throws Exception
    {
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
        // Add prefix if batching is opted-in
        task.setTaskPipe(withBatchingPrefix ? SUBDOCUMENT_BATCHER_PREFIX + queueName : queueName);
        task.setTargetPipe(queueName + "-out");

        final NewJob newJob = new NewJob();
        newJob.setName("BatchingTest_" + jobId);
        newJob.setDescription("Payload batching integration test");
        newJob.setTask(task);

        return newJob;
    }

    private NewJob createNonBatchedJob(final String jobId, final String queueName) throws Exception
    {
        declareQueue(queueName);

        final WorkerAction task = new WorkerAction();
        task.setTaskClassifier("SomeOtherWorker");
        task.setTaskApiVersion(1);
        task.setTaskData("{\"data\": \"test\"}");
        task.setTaskDataEncoding(WorkerAction.TaskDataEncodingEnum.UTF8);
        task.setTaskPipe(queueName);
        task.setTargetPipe(queueName + "-out");

        final NewJob newJob = new NewJob();
        newJob.setName("NonBatchingTest_" + jobId);
        newJob.setDescription("Non-batched job test");
        newJob.setTask(task);

        return newJob;
    }

    private void declareQueue(final String queueName) throws Exception
    {
        final Channel channel = rabbitConn.createChannel();
        final Map<String, Object> args = new HashMap<>();
        args.put(JobServiceConnectionUtil.RABBIT_PROP_QUEUE_TYPE,
            JobServiceConnectionUtil.RABBIT_PROP_QUEUE_TYPE_QUORUM);
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
     *
     * @param queueManager  the queue manager to consume messages from
     * @param expectedCount the number of messages to wait for
     * @return Supplier that when called, blocks until messages are received and returns them
     * @throws Exception if setup fails
     */
    private Supplier<List<TaskMessage>> startCollectingMessages(final QueueManager queueManager, final int expectedCount)
        throws Exception
    {
        final ExecutionContext context = new ExecutionContext(false);
        context.initializeContext();

        final List<TaskMessage> result = new ArrayList<>();
        final int[] receivedCount = {0};

        // Set up timeout timer (uses settings or default)
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
}



