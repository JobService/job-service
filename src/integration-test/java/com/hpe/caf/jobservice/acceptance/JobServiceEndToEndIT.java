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
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

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
    private static final String exampleWorkerMessageOutQueue = "exampleworker-test-output-1";
    private static final String datastoreContainerId = "datastore.container.id";
    private static final String jobCorrelationId = "1";
    private static final long defaultTimeOutMs = 600000; // 10 minutes
    private static ServicePath servicePath;
    private static WorkerServices workerServices;
    private static ConfigurationSource configurationSource;
    private static RabbitWorkerQueueConfiguration rabbitConfiguration;
    private static JobsApi jobsApi;
    private static final int numTestItemsToGenerate = 50;

    private List<String> testItemAssetIds;


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
    }


    private static JobsApi createJobsApi() {
        String connectionString = System.getenv("CAF_WEBSERVICE_URL");
        ApiClient client = new ApiClient();
        client.setBasePath(connectionString);
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        client.setDateFormat(f);
        return new JobsApi(client);
    }


    @Before
    public void testSetup() throws Exception {
        testItemAssetIds = generateWorkerBatch();
    }


    @Test
    public void testJobCompletion() throws Exception {
        String jobId = "J" + System.currentTimeMillis();

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

            createJob(jobId);

            // Waits for the final result message to appear on the Example worker's output queue.
            // When we read it from this queue it should have been processed fully and its status reported to the Job Database as Completed.
            TestResult result = context.getTestResult();
            Assert.assertTrue(result.isSuccess());
        }
    }


    @Test
    public void testJobCancellation() throws Exception {
        String jobId = "J" + System.currentTimeMillis();

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

            createJob(jobId);

            cancelJob(jobId);

            TestResult result = context.getTestResult();
            Assert.assertTrue(result.isSuccess());
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
     */
    private void createJob(final String jobId) throws Exception {
        NewJob newJob = constructNewJob(jobId);
        jobsApi.createOrUpdateJob(jobId, newJob, jobCorrelationId);
    }


    private void cancelJob(final String jobId) throws Exception {
        jobsApi.cancelJob(jobId, jobCorrelationId);
    }


    private NewJob constructNewJob(String jobId) throws Exception {
        WorkerAction batchWorkerAction = constructBatchWorkerAction();
        String jobName = "Job_" + jobId;
        NewJob newJob = new NewJob();
        newJob.setName(jobName);
        newJob.setDescription(jobName + " description");
        newJob.setExternalData(jobName + " external data");
        newJob.setTask(batchWorkerAction);
        return newJob;
    }


    private WorkerAction constructBatchWorkerAction() throws Exception {
        WorkerAction batchWorkerAction = new WorkerAction();
        batchWorkerAction.setTaskClassifier(BatchWorkerConstants.WORKER_NAME);
        batchWorkerAction.setTaskApiVersion(BatchWorkerConstants.WORKER_API_VERSION);
        batchWorkerAction.setTaskData(constructSerialisedBatchWorkerTask());
        batchWorkerAction.setTaskDataEncoding(WorkerAction.TaskDataEncodingEnum.UTF8);
        batchWorkerAction.setTaskPipe(batchWorkerMessageInQueue);
        batchWorkerAction.setTargetPipe(exampleWorkerMessageOutQueue);
        return batchWorkerAction;
    }


    private String constructSerialisedBatchWorkerTask() throws Exception {
        Map<String, String> exampleWorkerTaskMessageParams = new HashMap<>();
        exampleWorkerTaskMessageParams.put("datastorePartialReference", getContainerId());
        exampleWorkerTaskMessageParams.put("action", "CAPITALISE");

        BatchWorkerTask task = new BatchWorkerTask();
        task.batchDefinition = toBatchDefinition(testItemAssetIds);
        task.batchType = "AssetIdBatchPlugin";
        task.taskMessageType = "ExampleWorkerTaskBuilder";
        task.taskMessageParams = exampleWorkerTaskMessageParams;
        task.targetPipe = exampleWorkerMessageInQueue;
        return new String(workerServices.getCodec().serialise(task), StandardCharsets.UTF_8);
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
