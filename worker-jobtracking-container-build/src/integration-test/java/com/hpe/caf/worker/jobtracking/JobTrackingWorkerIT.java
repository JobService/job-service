package com.hpe.caf.worker.jobtracking;

import com.hpe.caf.api.BootstrapConfiguration;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.api.worker.TrackingInfo;
import com.hpe.caf.api.worker.WorkerResponse;
import com.hpe.caf.config.system.SystemBootstrapConfiguration;
import com.hpe.caf.naming.ServicePath;
import com.hpe.caf.util.ref.ReferencedData;
import com.hpe.caf.worker.ocr.OCRWorkerConstants;
import com.hpe.caf.worker.ocr.OCRWorkerResult;
import com.hpe.caf.worker.ocr.OCRWorkerStatus;
import com.hpe.caf.worker.ocr.OCRWorkerTask;
import com.hpe.caf.worker.queue.rabbit.RabbitWorkerQueueConfiguration;
import com.hpe.caf.worker.testing.*;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeoutException;


/**
 * Integration tests for Job Tracking Worker.
 */
public class JobTrackingWorkerIT {

    private static final String CONTEXT_KEY = "context";
    private static final byte[] CONTEXT_DATA = "testData".getBytes(StandardCharsets.UTF_8);
    private static final byte[] MOCK_OCR_DOC_DATA = "some text content".getBytes(StandardCharsets.UTF_8);
    private static final String STATUS_CHECK_URL = "http://fictional-host:1234/blah";
    private static final long defaultTimeOutMs = 600000; // 10 minutes

    private static ServicePath servicePath;
    private static WorkerServices workerServices;
    private static ConfigurationSource configurationSource;
    private static RabbitWorkerQueueConfiguration rabbitConfiguration;
    private static String jobTrackingWorkerInputQueue;
    private static JobDatabase jobDatabase;


    @BeforeClass
    public static void setup() throws Exception {
        BootstrapConfiguration bootstrap = new SystemBootstrapConfiguration();
        servicePath = bootstrap.getServicePath();
        workerServices = WorkerServices.getDefault();
        configurationSource = workerServices.getConfigurationSource();
        rabbitConfiguration = configurationSource.getConfiguration(RabbitWorkerQueueConfiguration.class);
        rabbitConfiguration.getRabbitConfiguration().setRabbitHost(SettingsProvider.defaultProvider.getSetting(SettingNames.dockerHostAddress));
        rabbitConfiguration.getRabbitConfiguration().setRabbitPort(Integer.parseInt(SettingsProvider.defaultProvider.getSetting(SettingNames.rabbitmqNodePort)));
        jobTrackingWorkerInputQueue = rabbitConfiguration.getInputQueue();
        jobDatabase = new JobDatabase();
    }


    /**
     * Tests reporting of an in-progress task.
     * This test creates a task suitable for input to the OCR worker complete with tracking info which should divert
     * the message to the Job Tracking Worker - i.e. the TrackingInfo.trackingPipe is the input queue of the
     * Job Tracking Worker under test.
     * The Job Tracking Worker should report the progress of this task to the Job Database, reporting it as active;
     * the test verifies this by querying the database directly.
     * The Job Tracking Worker should then forward the task to the queue specified as the 'to' field in the message.
     *
     * trackingPipe=jobTrackingWorkerInputQueue, as specified in rabbitConfiguration.
     */
    @Test
    public void testProxiedActiveMessage() throws Exception {
        String jobTaskId = jobDatabase.createJobTask("testProxiedActiveMessage");
        String to = "jobtrackingworker-test-ocr-input-1";
        String trackTo = "jobtrackingworker-test-ocr-output-1";
        TaskMessage taskMessage = getOcrTaskMessage(jobTaskId, to, trackTo);
        JobTrackingWorkerITExpectation expectation =
                new JobTrackingWorkerITExpectation(
                        jobTaskId,
                        to,
                        true,
                        new JobReportingExpectation(jobTaskId, JobStatus.Active, 0, false, false, false, false, false));
        testProxiedMessageReporting(taskMessage, expectation);
    }


    /**
     * Tests reporting of a completed task.
     * This test creates a task suitable for input to the OCR worker complete with tracking info which should divert
     * the message to the Job Tracking Worker - i.e. the TrackingInfo.trackingPipe is the input queue of the
     * Job Tracking Worker under test.
     * The Job Tracking Worker should report to the Job Database that this task is completed because the trackTo
     * destination matches the to destination; the test verifies this by querying the database directly.
     * The Job Tracking Worker should then forward the task to the queue specified as the 'to' field in the message.
     * The worker framework should also strip the tracking info from the message before the Job Tracking Worker
     * forwards it, as the message is being published to its trackTo destination, meaning that tracking of this message
     * has ended.
     *
     * trackingPipe=jobTrackingWorkerInputQueue, as specified in rabbitConfiguration.
     */
    @Test
    public void testProxiedCompletedMessage() throws Exception {
        String jobTaskId = jobDatabase.createJobTask("testProxiedCompletedMessage");
        String to = "jobtrackingworker-test-ocr-input-2";
        String trackTo = to;
        TaskMessage taskMessage = getOcrTaskMessage(jobTaskId, to, trackTo);
        JobTrackingWorkerITExpectation expectation =
                new JobTrackingWorkerITExpectation(
                        jobTaskId,
                        to,
                        false,
                        new JobReportingExpectation(jobTaskId, JobStatus.Completed, 100, false, false, false, false, false));
        testProxiedMessageReporting(taskMessage, expectation);
    }


    /**
     * Tests reporting of a failed task.
     * This test creates a task suitable for input to the OCR worker complete with tracking info, then generates
     * a failure as though the OCR worker had failed this task. The failure result message should be diverted
     * to the Job Tracking Worker - i.e. the TrackingInfo.trackingPipe is the input queue of the
     * Job Tracking Worker under test.
     * The Job Tracking Worker should report the failure of this task to the Job Database; the test verifies this by
     * querying the database directly.
     * The Job Tracking Worker should then forward the failure result message to the queue specified as the 'to' field
     * in the message.
     * The worker framework should also strip the tracking info from the message before the Job Tracking Worker
     * forwards it, as the message is being published to its trackTo destination, meaning that tracking of this message
     * has ended.
     *
     * trackingPipe=jobTrackingWorkerInputQueue, as specified in rabbitConfiguration.
     */
    @Test
    public void testProxiedFailureMessage() throws Exception {
        String jobTaskId = jobDatabase.createJobTask("testProxiedFailureMessage");
        String to = "jobtrackingworker-test-ocr-output-3";
        String trackTo = to;
        TaskMessage taskMessage = getOcrTaskMessage(jobTaskId, to, trackTo);
        TaskMessage failureMessage = failTask(to, taskMessage);
        JobTrackingWorkerITExpectation expectation =
                new JobTrackingWorkerITExpectation(
                        jobTaskId,
                        to,
                        false,
                        new JobReportingExpectation(jobTaskId, JobStatus.Failed, 0, true, true, true, true, true));
        testProxiedMessageReporting(failureMessage, expectation);
    }


    public void testProxiedMessageReporting(final TaskMessage testMessage, final JobTrackingWorkerITExpectation expectation) throws Exception {
        try (QueueManager queueManager = getQueueManager(expectation.getForwardingQueue())) {
            ExecutionContext context = new ExecutionContext(false);
            Timer timer = getTimer(context);
            Thread thread = queueManager.start(new JobTaskWorkerOutputDeliveryHandler(jobDatabase, context, expectation));
            queueManager.publish(testMessage);
            TestResult result = context.getTestResult();
            Assert.assertTrue(result.isSuccess());
        }
    }


    private QueueManager getQueueManager(final String forwardingQueue) throws IOException, TimeoutException {
        //Test messages are published to the Job Tracking Worker input queue, as specified in rabbitConfiguration.
        //The Job Tracking Worker should forward these to forwardingQueue so we'll consume from there rather than the Job Tracking Worker's own output queue.
        QueueServices queueServices = QueueServicesFactory.create(rabbitConfiguration, forwardingQueue, workerServices.getCodec());
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


    private TaskMessage getOcrTaskMessage(final String jobTaskId, final String to, final String trackTo) throws CodecException {
        OCRWorkerTask ocrTask = getOcrWorkerTask();

        // Wrap the OCR task in a TaskMessage with tracking info
        String taskId = UUID.randomUUID().toString();
        Map<String, byte[]> context = Collections.singletonMap(CONTEXT_KEY, CONTEXT_DATA);
        TrackingInfo tracking =
                new TrackingInfo(
                        jobTaskId,
                        new Date(),
                        STATUS_CHECK_URL,
                        jobTrackingWorkerInputQueue, //trackingPipe is Job Tracking Worker's input queue
                        trackTo);
        return new TaskMessage(
                taskId,
                OCRWorkerConstants.WORKER_NAME,
                OCRWorkerConstants.WORKER_API_VER,
                workerServices.getCodec().serialise(ocrTask),
                TaskStatus.NEW_TASK,
                context,
                to,
                tracking);
    }


    private OCRWorkerTask getOcrWorkerTask() {
        OCRWorkerTask task = new OCRWorkerTask();
        ReferencedData sourceDataRef = ReferencedData.getWrappedData(MOCK_OCR_DOC_DATA);
        task.setSourceData(sourceDataRef);
        task.setIncludePageDetail(false);
        return task;
    }


    private TaskMessage failTask(final String responseQueue, final TaskMessage taskMessage) throws CodecException {
        OCRWorkerResult ocrFailureResult = new OCRWorkerResult(OCRWorkerStatus.OCR_FAILED);
        WorkerResponse ocrWorkerResponse =
                new WorkerResponse(
                        responseQueue,
                        TaskStatus.RESULT_FAILURE,
                        workerServices.getCodec().serialise(ocrFailureResult),
                        OCRWorkerConstants.WORKER_NAME,
                        OCRWorkerConstants.WORKER_API_VER,
                        (byte[])null);

        Map<String, byte[]> contextMap = taskMessage.getContext();
        if ( ocrWorkerResponse.getContext() != null ) {
            contextMap.put(servicePath.toString(), ocrWorkerResponse.getContext());
        }

        return new TaskMessage(
                taskMessage.getTaskId(),
                ocrWorkerResponse.getMessageType(),
                ocrWorkerResponse.getApiVersion(),
                ocrWorkerResponse.getData(),
                ocrWorkerResponse.getTaskStatus(),
                contextMap,
                ocrWorkerResponse.getQueueReference(),
                taskMessage.getTracking());
    }
}
