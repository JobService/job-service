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
import com.hpe.caf.services.job.util.JobTaskId;
import com.hpe.caf.util.ref.ReferencedData;
import com.hpe.caf.worker.example.ExampleWorkerConstants;
import com.hpe.caf.worker.example.ExampleWorkerResult;
import com.hpe.caf.worker.example.ExampleWorkerStatus;
import com.hpe.caf.worker.example.ExampleWorkerTask;
import com.hpe.caf.worker.queue.rabbit.RabbitWorkerQueueConfiguration;
import com.hpe.caf.worker.testing.*;
import com.hpe.caf.worker.tracking.report.TrackingReport;
import com.hpe.caf.worker.tracking.report.TrackingReportStatus;
import com.hpe.caf.worker.tracking.report.TrackingReportTask;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertThrows;

/**
 * Integration tests for Job Tracking Worker.
 */
public class JobTrackingWorkerIT {

    private static final String CONTEXT_KEY = "context";
    private static final byte[] CONTEXT_DATA = "testData".getBytes(StandardCharsets.UTF_8);
    private static final byte[] MOCK_EXAMPLE_DOC_DATA = "some text content".getBytes(StandardCharsets.UTF_8);
    private static final String STATUS_CHECK_URL = "http://fictional-host:1234/blah";
    private static final long defaultTimeOutMs = 600000; // 10 minutes

    private static ServicePath servicePath;
    private static WorkerServices workerServices;
    private static ConfigurationSource configurationSource;
    private static RabbitWorkerQueueConfiguration rabbitConfiguration;
    private static String jobTrackingWorkerInputQueue;
    private static JobDatabase jobDatabase;

    private String defaultPartitionId;


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

    @BeforeTest
    public void setupTest() {
        defaultPartitionId = UUID.randomUUID().toString();
    }


    /**
     * Tests reporting of an in-progress task. This test creates a task suitable for input to the Job Tracking worker. The Job Tracking
     * Worker should report the progress of this task to the Job Database, reporting it as active; the test verifies this by querying the
     * database directly.
     */
    @Test
    public void testTrackingReportTasks() throws Exception
    {
        final String jobTaskId = jobDatabase.createJobId();
        jobDatabase.createJobTask(defaultPartitionId, jobTaskId, "testProxiedActiveMessage");
        for (int i = 0; i <= 4; i++) {
            final JobStatus status;
            final int percentageCompleted;
            final TrackingReportStatus trackingReportStatus;
            if (i < 4) {
                status = JobStatus.Waiting;
                percentageCompleted = 0;
                trackingReportStatus = TrackingReportStatus.Progress;
            } else {
                status = JobStatus.Completed;
                percentageCompleted = 100;
                trackingReportStatus = TrackingReportStatus.Complete;
            }
            final TaskMessage taskMessage = getExampleTrackingReportMessage(defaultPartitionId, jobTaskId, trackingReportStatus,
                    percentageCompleted);
            final JobReportingExpectation expectation = getExpectation(jobTaskId, status, percentageCompleted);
            publishAndVerify(taskMessage, jobTaskId, expectation);
        }
    }

    /**
     * Tests reporting of an in-progress task. This test creates a task suitable for input to the Job Tracking worker. The Job Tracking
     * Worker should report the progress of this task to the Job Database, reporting it as active; the test verifies this by querying the
     * database directly.
     * The task is being sent containing a percentageCompleted value. If that value reaches 100 with a TrackingReportStatus as "Progress",
     * the percentageCompleted is rounded down to 99 as only a TrackingReportStatus should report 100% completion
     */
    @Test
    public void testTrackingReportProgressTasks() throws Exception
    {
        final String jobTaskId = jobDatabase.createJobId();
        jobDatabase.createJobTask(defaultPartitionId, jobTaskId, "testTrackingReportProgressTasks");
        for (int i = 0; i <= 4; i++) {
            final JobStatus status = (i == 0) ? JobStatus.Waiting : JobStatus.Active;
            final int percentageCompleted = ((25*i) == 100) ? 99 : (25*i);
            final TaskMessage taskMessage = getExampleTrackingReportMessage(defaultPartitionId, jobTaskId,
                    TrackingReportStatus.Progress, percentageCompleted);
            final JobReportingExpectation expectation = getExpectation(jobTaskId, status, percentageCompleted);
            publishAndVerify(taskMessage, jobTaskId, expectation);
        }
    }

    /**
     * Throws an exception if taskId is empty or null
     */
    @Test
    public void testProgressReportingMessagesEmptyTaskId() {
        final Exception exception1 = assertThrows(SQLException.class,
                ()->jobDatabase.reportTaskProgress(defaultPartitionId, "", 18));
        final Exception exception2 = assertThrows(SQLException.class,
                ()->jobDatabase.reportTaskProgress(defaultPartitionId, null, 18));

        final String expectedMessage = "Task identifier has not been specified";

        Assert.assertTrue(exception1.getMessage().contains(expectedMessage));
        Assert.assertTrue(exception2.getMessage().contains(expectedMessage));
    }

    /**
     * Throws an exception if percentage_complete is invalid
     */
    @Test
    public void testProgressReportingMessagesInvalidProgressionValue() {
        final Exception exception1 = assertThrows(SQLException.class,
                ()->jobDatabase.reportTaskProgress(defaultPartitionId, "fakeJob", -18));
        final Exception exception2 = assertThrows(SQLException.class,
                ()->jobDatabase.reportTaskProgress(defaultPartitionId, "fakeJob", 118));

        final String expectedMessage = "Invalid in_percentage_complete";

        Assert.assertTrue(exception1.getMessage().contains(expectedMessage));
        Assert.assertTrue(exception2.getMessage().contains(expectedMessage));
    }


    private JobReportingExpectation getExpectation(final String jobTaskId, final JobStatus status, final int percentageCompleted){
        return new JobReportingExpectation(jobTaskId, status, percentageCompleted, false, false, false, false, false);
    }

    private synchronized void publishAndVerify(final TaskMessage taskMessage, final String jobTaskId,
                                  final JobReportingExpectation expectation) throws Exception
    {
        try (final QueueManager queueManager = getQueueManager(jobTrackingWorkerInputQueue)) {
            final ExecutionContext context = new ExecutionContext(false);
            context.initializeContext();
            Timer timer = getTimer(context);
            Thread thread = queueManager.start(
                new JobTaskWorkerOutputDeliveryHandler(jobDatabase, context,
                    new JobTrackingWorkerITExpectation(
                        defaultPartitionId, jobTaskId, jobTaskId, true, expectation)));
            queueManager.publish(taskMessage);
            final JobDatabase database = new JobDatabase();
            // Increased the waiting time to match the 10s to wait induced by the batch
            // (see in JobtrackingWorkerFactory.processTasks())
            wait(2000);
            database.verifyJobStatus(defaultPartitionId, jobTaskId, expectation);
        }
    }


    /**
     * Tests reporting of a completed task.
     * This test creates a task suitable for input to the Example worker complete with tracking info which should divert
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
        String jobTaskId = jobDatabase.createJobId();
        jobDatabase.createJobTask(defaultPartitionId, jobTaskId, "testProxiedCompletedMessage");
        String to = "jobtrackingworker-test-example-input-2";
        String trackTo = to;
        TaskMessage taskMessage = getExampleTaskMessage(defaultPartitionId, jobTaskId, to, trackTo);
        JobTrackingWorkerITExpectation expectation =
            new JobTrackingWorkerITExpectation(defaultPartitionId, jobTaskId, to, false,
                new JobReportingExpectation(jobTaskId, JobStatus.Completed, 100, false, false, false, false, false));
        testProxiedMessageReporting(taskMessage, expectation);

        final DBJob jobFromDb = jobDatabase.getJob(defaultPartitionId, jobTaskId);
        Assert.assertTrue(jobFromDb.getLastUpdateDate().isAfter(jobFromDb.getCreateDate()),
            "last-update-time should be updated on complete");
    }


    /**
     * Tests reporting of a failed task.
     * This test creates a task suitable for input to the Example worker complete with tracking info, then generates
     * a failure as though the Example worker had failed this task. The failure result message should be diverted
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
        String jobTaskId = jobDatabase.createJobId();
        jobDatabase.createJobTask(defaultPartitionId, jobTaskId, "testProxiedFailureMessage");
        String to = "jobtrackingworker-test-example-output-3";
        String trackTo = to;
        TaskMessage taskMessage = getExampleTaskMessage(defaultPartitionId, jobTaskId, to, trackTo);
        TaskMessage failureMessage = failTask(to, taskMessage, TaskStatus.RESULT_FAILURE);
        JobTrackingWorkerITExpectation expectation =
            new JobTrackingWorkerITExpectation(defaultPartitionId, jobTaskId, to, false,
                new JobReportingExpectation(jobTaskId, JobStatus.Completed, 100, false, true, true, true, true));
        testProxiedMessageReporting(failureMessage, expectation);

        final DBJob jobFromDb = jobDatabase.getJob(defaultPartitionId, jobTaskId);
        Assert.assertTrue(jobFromDb.getLastUpdateDate().isAfter(jobFromDb.getCreateDate()),
            "last-update-time should be updated on fail");
    }

    /**
     * When a worker responds with INVALID_TASK, the tracking worker should fail the job in the
     * database.
     */
    @Test
    public void testProxiedInvalidMessage() throws Exception {
        final String jobId = jobDatabase.createJobId();
        jobDatabase.createJobTask(defaultPartitionId, jobId, "testProxiedInvalidMessage");

        final String queue = "jobtrackingworker-test-example-output";
        final TaskMessage basicMessage =
            getExampleTaskMessage(defaultPartitionId, jobId, queue, queue);
        final TaskMessage invalidMessage = failTask(queue, basicMessage, TaskStatus.INVALID_TASK);
        final JobTrackingWorkerITExpectation expectation =
            new JobTrackingWorkerITExpectation(defaultPartitionId, jobId, queue, false,
                new JobReportingExpectation(
                    jobId, JobStatus.Failed, 0, true, true, true, true, true));
        testProxiedMessageReporting(invalidMessage, expectation);

        final DBJob jobFromDb = jobDatabase.getJob(defaultPartitionId, jobId);
        Assert.assertTrue(jobFromDb.getLastUpdateDate().isAfter(jobFromDb.getCreateDate()),
            "last-update-time should be updated on fail");
    }

    /**
     * Create a job with 2 subtasks and send a complete message for 1 of the subtasks.  This should
     * update the last-update-time and completion percentage.
     */
    @Test
    public void testCompleteWithParent() throws Exception {
        final String parentId = jobDatabase.createJobId();
        final String child1Id = parentId + ".1";
        final String child2Id = parentId + ".2";
        jobDatabase.createJobTask(defaultPartitionId, parentId, "testCompleteWithParent-parent");
        jobDatabase.createJobTask(defaultPartitionId, child1Id, "testCompleteWithParent-child1");
        jobDatabase.createJobTask(defaultPartitionId, child2Id, "testCompleteWithParent-child2");

        // send a message for the completion of 1 of the 2 child tasks
        final String queue = "jobtrackingworker-test-example-input";
        final TaskMessage child1CompleteMessage =
            getExampleTaskMessage(defaultPartitionId, child1Id, queue, queue);
        final JobTrackingWorkerITExpectation expectation =
            new JobTrackingWorkerITExpectation(defaultPartitionId, parentId, queue, false,
                new JobReportingExpectation(
                    parentId, JobStatus.Active, 50, false, false, false, false, false));
        testProxiedMessageReporting(child1CompleteMessage, expectation);

        final DBJob jobFromDb = jobDatabase.getJob(defaultPartitionId, parentId);
        Assert.assertTrue(jobFromDb.getLastUpdateDate().isAfter(jobFromDb.getCreateDate()),
            "last-update-time should be updated on subtask complete");
    }

    /**
     * Verify that there's no interference between jobs with the same ID in different partitions
     * when tracking subtasks.
     */
    @Test
    public void testSubtasksAcrossDifferentPartitions() throws Exception {
        final String queue = "jobtrackingworker-test-example-input";
        final String partitionId1 = UUID.randomUUID().toString();
        final String partitionId2 = UUID.randomUUID().toString();
        final String parentId = jobDatabase.createJobId();
        final String childId = parentId + ".1";
        jobDatabase.createJobTask(partitionId1, parentId, "testSubtasksAcrossPartitions-parent1");
        jobDatabase.createJobTask(partitionId1, childId, "testSubtasksAcrossPartitions-child1");
        jobDatabase.createJobTask(partitionId2, parentId, "testSubtasksAcrossPartitions-parent2");
        jobDatabase.createJobTask(partitionId2, childId, "testSubtasksAcrossPartitions-child2");

        final TaskMessage child1CompleteMessage =
            getExampleTaskMessage(partitionId1, childId, queue, queue);
        final JobTrackingWorkerITExpectation partition1Expectation =
            new JobTrackingWorkerITExpectation(partitionId1, parentId, queue, false,
                new JobReportingExpectation(
                    parentId, JobStatus.Active, 50, false, false, false, false, false));
        testProxiedMessageReporting(child1CompleteMessage, partition1Expectation);

        final DBJob job2FromDb = jobDatabase.getJob(partitionId2, parentId);
        Assert.assertEquals(job2FromDb.getStatus(), JobStatus.Waiting,
            "partition 2 job status should be unaffected by progress of partition 1 job");

        final TaskMessage child2CompleteMessage =
            getExampleTaskMessage(partitionId2, childId, queue, queue);
        final JobTrackingWorkerITExpectation partition2Expectation =
            new JobTrackingWorkerITExpectation(partitionId2, parentId, queue, false,
                new JobReportingExpectation(
                    parentId, JobStatus.Active, 50, false, false, false, false, false));
        testProxiedMessageReporting(child2CompleteMessage, partition2Expectation);
    }


    public void testProxiedMessageReporting(final TaskMessage testMessage, final JobTrackingWorkerITExpectation expectation) throws Exception {
        try (QueueManager queueManager = getQueueManager(expectation.getForwardingQueue())) {
            ExecutionContext context = new ExecutionContext(false);
            context.initializeContext();
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


    private TaskMessage getExampleTaskMessage(
        final String partitionId, final String jobTaskId, final String to, final String trackTo
    ) throws CodecException {
        ExampleWorkerTask exampleTask = getExampleWorkerTask();

        // Wrap the Example task in a TaskMessage with tracking info
        String taskId = UUID.randomUUID().toString();
        Map<String, byte[]> context = Collections.singletonMap(CONTEXT_KEY, CONTEXT_DATA);
        TrackingInfo tracking =
                new TrackingInfo(
                        new JobTaskId(partitionId, jobTaskId).getMessageId(),
                        null,
                        0,
                        STATUS_CHECK_URL,
                        jobTrackingWorkerInputQueue, //trackingPipe is Job Tracking Worker's input queue
                        trackTo);
        return new TaskMessage(
                taskId,
                ExampleWorkerConstants.WORKER_NAME,
                ExampleWorkerConstants.WORKER_API_VER,
                workerServices.getCodec().serialise(exampleTask),
                TaskStatus.NEW_TASK,
                context,
                to,
                tracking);
    }

    private TaskMessage getExampleTrackingReportMessage(
        final String partitionId, final String jobTaskId, final TrackingReportStatus status, final int percentageCompleted
    ) throws CodecException {
        final TrackingReportTask exampleTask = new TrackingReportTask();
        exampleTask.trackingReports = new ArrayList<>();
        final TrackingReport report = new TrackingReport();
        report.jobTaskId = new JobTaskId(partitionId, jobTaskId).getMessageId();
        report.estimatedPercentageCompleted = percentageCompleted;
        report.failure = null;
        report.retries = null;
        report.status = status;
        exampleTask.trackingReports.add(report);
        // Wrap the Example task in a TaskMessage with tracking info
        final String taskId = UUID.randomUUID().toString();
        return new TaskMessage(
            taskId,
            "TrackingReportTask",
            ExampleWorkerConstants.WORKER_API_VER,
            workerServices.getCodec().serialise(exampleTask),
            TaskStatus.NEW_TASK,
            new HashMap<>(),
            jobTrackingWorkerInputQueue);
    }


    private ExampleWorkerTask getExampleWorkerTask() {
        ExampleWorkerTask task = new ExampleWorkerTask();
        ReferencedData sourceDataRef = ReferencedData.getWrappedData(MOCK_EXAMPLE_DOC_DATA);
        task.sourceData = sourceDataRef;
        return task;
    }


    private TaskMessage failTask(
        final String responseQueue, final TaskMessage taskMessage, final TaskStatus taskStatus
    ) throws CodecException {
        ExampleWorkerResult exampleFailureResult = new ExampleWorkerResult();
        exampleFailureResult.workerStatus = ExampleWorkerStatus.WORKER_EXAMPLE_FAILED;
        WorkerResponse exampleWorkerResponse =
                new WorkerResponse(
                        responseQueue,
                        taskStatus,
                        workerServices.getCodec().serialise(exampleFailureResult),
                        ExampleWorkerConstants.WORKER_NAME,
                        ExampleWorkerConstants.WORKER_API_VER,
                        (byte[])null);

        Map<String, byte[]> contextMap = taskMessage.getContext();
        if ( exampleWorkerResponse.getContext() != null ) {
            contextMap.put(servicePath.toString(), exampleWorkerResponse.getContext());
        }

        return new TaskMessage(
                taskMessage.getTaskId(),
                exampleWorkerResponse.getMessageType(),
                exampleWorkerResponse.getApiVersion(),
                exampleWorkerResponse.getData(),
                exampleWorkerResponse.getTaskStatus(),
                contextMap,
                exampleWorkerResponse.getQueueReference(),
                taskMessage.getTracking());
    }
}
