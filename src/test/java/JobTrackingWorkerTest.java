import com.hpe.caf.api.Codec;
import com.hpe.caf.api.worker.*;
import com.hpe.caf.codec.JsonCodec;
import com.hpe.caf.util.rabbitmq.RabbitHeaders;
import com.hpe.caf.worker.jobtracking.*;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JUnit test to verify the worker correctly performs the desired action.
 */
@RunWith(MockitoJUnitRunner.class)
public class JobTrackingWorkerTest {

    private static final String taskId = "task-id";
    private static final String anotherWorkerClassifier = "AnotherWorker";
    private static final String toQueue = "to-queue";
    private static final String trackingPipe = "tracking-pipe";
    private static final String trackToPipe = "track-to-pipe";
    private static final String queueMsgId = "queue-msg-id";
    private static final String outputQueue = "output-queue";
    private static final String jobTaskId = "J123.1.2";
    private static final String statusCheckUrl = "http://status-check-host:1234/blah";


    @Test
    public void testProxiedCompleteTask() throws Exception {
        //Setup
        Codec codec = new JsonCodec();
        JobTrackingWorkerReporter reporter = Mockito.mock(JobTrackingWorkerReporter.class);

        TaskMessage tm = new TaskMessage(taskId, anotherWorkerClassifier, 1, new byte[0], TaskStatus.RESULT_SUCCESS, Collections.EMPTY_MAP, toQueue);
        TrackingInfo tracking = new TrackingInfo(jobTaskId, new Date(), statusCheckUrl, trackingPipe, toQueue); //trackToPipe==toQueue
        tm.setTracking(tracking);

        Map<String, Object> headers = Collections.emptyMap();

        WorkerCallback mockCallback = Mockito.mock(WorkerCallback.class);

        //Create the worker subject to testing
        JobTrackingWorker worker = new JobTrackingWorker(createProxiedTask(anotherWorkerClassifier, codec.serialise(tm)), outputQueue, codec, reporter);

        //Test
        worker.determineForwardingAction(tm, queueMsgId, headers, mockCallback);

        //verify results
        Mockito.verify(mockCallback, Mockito.times(1)).forward(Mockito.eq(queueMsgId), Mockito.eq(toQueue), Mockito.eq(tm),  Mockito.anyMap());
        Mockito.verify(reporter, Mockito.times(1)).reportJobTaskComplete(Mockito.eq(jobTaskId));
    }


    @Test
    public void testProxiedInProgressTask() throws Exception {
        //Setup
        Codec codec = new JsonCodec();
        JobTrackingWorkerReporter reporter = Mockito.mock(JobTrackingWorkerReporter.class);

        TaskMessage tm = new TaskMessage(taskId, anotherWorkerClassifier, 1, new byte[0], TaskStatus.RESULT_SUCCESS, Collections.EMPTY_MAP, toQueue);
        TrackingInfo tracking = new TrackingInfo(jobTaskId, new Date(), statusCheckUrl, trackingPipe, trackToPipe);
        tm.setTracking(tracking);

        Map<String, Object> headers = Collections.emptyMap();

        WorkerCallback mockCallback = Mockito.mock(WorkerCallback.class);

        //Create the worker subject to testing
        JobTrackingWorker worker = new JobTrackingWorker(createProxiedTask(anotherWorkerClassifier, codec.serialise(tm)), outputQueue, codec, reporter);

        //Test
        worker.determineForwardingAction(tm, queueMsgId, headers, mockCallback);

        //verify results
        Mockito.verify(mockCallback, Mockito.times(1)).forward(Mockito.eq(queueMsgId), Mockito.eq(toQueue), Mockito.eq(tm),  Mockito.anyMap());
        Mockito.verify(reporter, Mockito.times(1)).reportJobTaskProgress(Mockito.eq(jobTaskId), Mockito.anyInt());
    }


    @Test
    public void testProxiedRejectedTask() throws Exception {
        //Setup
        Codec codec = new JsonCodec();
        JobTrackingWorkerReporter reporter = Mockito.mock(JobTrackingWorkerReporter.class);

        TaskMessage tm = new TaskMessage(taskId, anotherWorkerClassifier, 1, new byte[0], TaskStatus.RESULT_FAILURE, Collections.EMPTY_MAP, toQueue);
        TrackingInfo tracking = new TrackingInfo(jobTaskId, new Date(), statusCheckUrl, trackingPipe, trackToPipe);
        tm.setTracking(tracking);

        Map<String, Object> headers = new HashMap<String, Object>();
        headers.put(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_REJECTED, "EXCEEDED_RETRIES");

        WorkerCallback mockCallback = Mockito.mock(WorkerCallback.class);

        //Create the worker subject to testing
        JobTrackingWorker worker = new JobTrackingWorker(createProxiedTask(anotherWorkerClassifier, codec.serialise(tm)), outputQueue, codec, reporter);

        //Test
        worker.determineForwardingAction(tm, queueMsgId, headers, mockCallback);

        //verify results
        Mockito.verify(mockCallback, Mockito.times(1)).forward(Mockito.eq(queueMsgId), Mockito.eq(toQueue), Mockito.eq(tm),  Mockito.anyMap());
        Mockito.verify(reporter, Mockito.times(1)).reportJobTaskRejected(Mockito.eq(jobTaskId), Mockito.anyInt());
    }


    @Test
    public void testProxiedRetriedTask() throws Exception {
        //Setup
        Codec codec = new JsonCodec();
        JobTrackingWorkerReporter reporter = Mockito.mock(JobTrackingWorkerReporter.class);

        TaskMessage tm = new TaskMessage(taskId, anotherWorkerClassifier, 1, new byte[0], TaskStatus.RESULT_FAILURE, Collections.EMPTY_MAP, toQueue);
        TrackingInfo tracking = new TrackingInfo(jobTaskId, new Date(), statusCheckUrl, trackingPipe, trackToPipe);
        tm.setTracking(tracking);

        Map<String, Object> headers = new HashMap<String, Object>();
        int numRetries = 42;
        headers.put(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_RETRY, numRetries);

        WorkerCallback mockCallback = Mockito.mock(WorkerCallback.class);

        //Create the worker subject to testing
        JobTrackingWorker worker = new JobTrackingWorker(createProxiedTask(anotherWorkerClassifier, codec.serialise(tm)), outputQueue, codec, reporter);

        //Test
        worker.determineForwardingAction(tm, queueMsgId, headers, mockCallback);

        //verify results
        Mockito.verify(mockCallback, Mockito.times(1)).forward(Mockito.eq(queueMsgId), Mockito.eq(toQueue), Mockito.eq(tm),  Mockito.anyMap());
        Mockito.verify(reporter, Mockito.times(1)).reportJobTaskFailure(Mockito.eq(jobTaskId), Mockito.eq(numRetries));
    }


    @Test
    public void testTrackingEventTask() throws Exception {
        //Setup
        Codec codec = new JsonCodec();
        JobTrackingWorkerReporter reporter = Mockito.mock(JobTrackingWorkerReporter.class);

        //Create the worker subject to testing
        JobTrackingWorker worker = new JobTrackingWorker(createTrackedTask(jobTaskId), outputQueue, codec, reporter);

        //Test
        WorkerResponse response = worker.doWork();

        //verify results
        Mockito.verify(reporter, Mockito.times(1)).reportJobTaskProgress(Mockito.eq(jobTaskId), Mockito.anyInt());
        Assert.assertEquals(TaskStatus.RESULT_SUCCESS, response.getTaskStatus());
        JobTrackingWorkerResult workerResult = codec.deserialise(response.getData(), JobTrackingWorkerResult.class);
        Assert.assertNotNull(workerResult);
        Assert.assertEquals(JobTrackingWorkerStatus.COMPLETED, workerResult.getStatus());
    }


    private JobTrackingWorkerTask createProxiedTask(final String proxiedTaskClassifier, final byte[] proxiedTaskData) {
        JobTrackingWorkerTask task = new JobTrackingWorkerTask();
        task.setProxiedTaskInfo(new ProxiedTaskInfo(proxiedTaskClassifier, proxiedTaskData));
        return task;
    }


    private JobTrackingWorkerTask createTrackedTask(final String jobTaskId) {
        JobTrackingWorkerTask task = new JobTrackingWorkerTask();
        task.setJobTaskId(jobTaskId);
        return task;
    }
}
