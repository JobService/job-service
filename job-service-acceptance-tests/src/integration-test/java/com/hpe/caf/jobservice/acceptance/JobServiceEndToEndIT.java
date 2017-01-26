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
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

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

    //  Scripted Job Service testing.
    private static final String CREATE_JOB_DEFN_CONTAINER_JSON_FILENAME = "create_job_definition_container.json";
    private static final String CREATE_JOB_SERVICE_CALLER_CONTAINER_JSON_FILENAME = "create_job_service_caller_container.json";
    private static String jobDefinitionContainerJSON;
    private static String jobServiceCallerContainerJSON;
    private static final String pollingInterval = "10";
    private static final String jobDefinitionFile = "test_job_definition.json";
    private static String dockerContainersURL;
    private static String jobServiceURL;
    private static String jobServiceCallerImage;

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
        jobServiceURL = System.getenv("CAF_WEBSERVICE_URL").replace("/job-service/v1","");
        jobDefinitionContainerJSON = JobServiceCallerTestsHelper.getJSONFromFile(CREATE_JOB_DEFN_CONTAINER_JSON_FILENAME);
        jobServiceCallerContainerJSON = JobServiceCallerTestsHelper.getJSONFromFile(CREATE_JOB_SERVICE_CALLER_CONTAINER_JSON_FILENAME);
        jobServiceCallerImage = System.getenv("CAF_JOB_SERVICE_CALLER_IMAGE");
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

    @Test
    @SuppressWarnings("unchecked")
    public void testJobServiceCaller_Success() throws ParseException, IOException, TimeoutException {
        //  Generate job identifier.
        String jobId = "J" + System.currentTimeMillis();

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

            //  Before job service caller container can be started, a number of changes to the container JSON needs to be made including Cmd, HostConfig and Image.
            //  Configure Cmd (i.e. modify job identifier, web service url, polling interval and job definition file that will be passed to the containerized script).
            JSONArray cmd = new JSONArray();
            cmd.add(0, "-j");
            cmd.add(1, jobId);
            cmd.add(2, "-u");
            cmd.add(3, jobServiceURL);
            cmd.add(4, "-p");
            cmd.add(5, pollingInterval);
            cmd.add(6, "-f");
            cmd.add(7, "/jobDefinition/" + jobDefinitionFile);
            createContainerObject.put("Cmd", cmd);

            //  Configure HostConfig
            JSONObject hostConfig = (JSONObject) createContainerObject.get("HostConfig");
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
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testJobServiceCaller_Failure() throws ParseException, IOException, TimeoutException {
        //  Generate job identifier.
        String jobId = "J" + System.currentTimeMillis();

        //  Identify name of test data container as  we need to set up VolumesFrom to access the test data file.
        String jobDefinitionContainerName = JobServiceCallerTestsHelper.getJobDefinitionContainerName(jobDefinitionContainerJSON, dockerContainersURL);

        //  Parse the job service caller container json string.
        JSONObject createContainerObject = JobServiceCallerTestsHelper.parseJson(jobServiceCallerContainerJSON);

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

        //  Expecting StatusCode > 0 for failure.
        Assert.assertNotNull(statusCode);
        Assert.assertTrue(statusCode > 0);
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
