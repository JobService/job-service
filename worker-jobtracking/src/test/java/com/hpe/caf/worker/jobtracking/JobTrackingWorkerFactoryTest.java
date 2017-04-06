/*
 * Copyright 2015-2017 Hewlett Packard Enterprise Development LP.
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

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.worker.*;
import com.hpe.caf.codec.JsonCodec;
import com.hpe.caf.util.rabbitmq.RabbitHeaders;
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
public class JobTrackingWorkerFactoryTest {

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
        JobTrackingReporter reporter = Mockito.mock(JobTrackingReporter.class);

        TaskMessage tm = new TaskMessage(taskId, anotherWorkerClassifier, 1, new byte[0], TaskStatus.RESULT_SUCCESS, Collections.EMPTY_MAP, toQueue);
        TrackingInfo tracking = new TrackingInfo(jobTaskId, new Date(), statusCheckUrl, trackingPipe, toQueue); //trackToPipe==toQueue
        tm.setTracking(tracking);

        Map<String, Object> headers = Collections.emptyMap();

        WorkerCallback mockCallback = Mockito.mock(WorkerCallback.class);

        //Create the worker factory subject to testing
        JobTrackingWorkerFactory workerFactory = createJobTrackingWorkerFactory(codec, reporter);

        //Test
        workerFactory.determineForwardingAction(tm, queueMsgId, headers, mockCallback);

        //verify results
        Mockito.verify(mockCallback, Mockito.times(1)).forward(Mockito.eq(queueMsgId), Mockito.eq(toQueue), Mockito.eq(tm),  Mockito.anyMap());
        Mockito.verify(reporter, Mockito.times(1)).reportJobTaskComplete(Mockito.eq(jobTaskId));
    }


    @Test
    public void testProxiedInProgressTask() throws Exception {
        //Setup
        Codec codec = new JsonCodec();
        JobTrackingReporter reporter = Mockito.mock(JobTrackingReporter.class);

        TaskMessage tm = new TaskMessage(taskId, anotherWorkerClassifier, 1, new byte[0], TaskStatus.RESULT_SUCCESS, Collections.EMPTY_MAP, toQueue);
        TrackingInfo tracking = new TrackingInfo(jobTaskId, new Date(), statusCheckUrl, trackingPipe, trackToPipe);
        tm.setTracking(tracking);

        Map<String, Object> headers = Collections.emptyMap();

        WorkerCallback mockCallback = Mockito.mock(WorkerCallback.class);

        //Create the worker factory subject to testing
        JobTrackingWorkerFactory workerFactory = createJobTrackingWorkerFactory(codec, reporter);

        //Test
        workerFactory.determineForwardingAction(tm, queueMsgId, headers, mockCallback);

        //verify results
        Mockito.verify(mockCallback, Mockito.times(1)).forward(Mockito.eq(queueMsgId), Mockito.eq(toQueue), Mockito.eq(tm),  Mockito.anyMap());
        Mockito.verify(reporter, Mockito.times(1)).reportJobTaskProgress(Mockito.eq(jobTaskId), Mockito.anyInt());
    }


    @Test
    public void testProxiedRejectedTask() throws Exception {
        //Setup
        Codec codec = new JsonCodec();
        JobTrackingReporter reporter = Mockito.mock(JobTrackingReporter.class);

        TaskMessage tm = new TaskMessage(taskId, anotherWorkerClassifier, 1, new byte[0], TaskStatus.RESULT_EXCEPTION, Collections.EMPTY_MAP, toQueue);
        TrackingInfo tracking = new TrackingInfo(jobTaskId, new Date(), statusCheckUrl, trackingPipe, trackToPipe);
        tm.setTracking(tracking);

        Map<String, Object> headers = new HashMap<String, Object>();
        headers.put(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_REJECTED, "EXCEEDED_RETRIES");

        WorkerCallback mockCallback = Mockito.mock(WorkerCallback.class);

        //Create the worker factory subject to testing
        JobTrackingWorkerFactory workerFactory = createJobTrackingWorkerFactory(codec, reporter);

        //Test
        workerFactory.determineForwardingAction(tm, queueMsgId, headers, mockCallback);

        //verify results
        Mockito.verify(mockCallback, Mockito.times(1)).forward(Mockito.eq(queueMsgId), Mockito.eq(toQueue), Mockito.eq(tm),  Mockito.anyMap());
        Mockito.verify(reporter, Mockito.times(1)).reportJobTaskRejected(Mockito.eq(jobTaskId), Mockito.any());
    }


    @Test
    public void testProxiedFailedTask() throws Exception {
        //Setup
        Codec codec = new JsonCodec();
        JobTrackingReporter reporter = Mockito.mock(JobTrackingReporter.class);

        TaskMessage tm = new TaskMessage(taskId, anotherWorkerClassifier, 1, new byte[0], TaskStatus.RESULT_FAILURE, Collections.EMPTY_MAP, toQueue);
        TrackingInfo tracking = new TrackingInfo(jobTaskId, new Date(), statusCheckUrl, trackingPipe, trackToPipe);
        tm.setTracking(tracking);

        Map<String, Object> headers = new HashMap<String, Object>();
        headers.put(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_RETRY, 1);

        WorkerCallback mockCallback = Mockito.mock(WorkerCallback.class);

        //Create the worker factory subject to testing
        JobTrackingWorkerFactory workerFactory = createJobTrackingWorkerFactory(codec, reporter);

        //Test
        workerFactory.determineForwardingAction(tm, queueMsgId, headers, mockCallback);

        //verify results
        Mockito.verify(mockCallback, Mockito.times(1)).forward(Mockito.eq(queueMsgId), Mockito.eq(toQueue), Mockito.eq(tm),  Mockito.anyMap());
        Mockito.verify(reporter, Mockito.times(1)).reportJobTaskProgress(Mockito.eq(jobTaskId),Mockito.anyInt());
    }


    @Test
    public void testTrackingEventTask() throws Exception {
        //Setup
        Codec codec = new JsonCodec();
        JobTrackingReporter reporter = Mockito.mock(JobTrackingReporter.class);

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


    private JobTrackingWorkerFactory createJobTrackingWorkerFactory(Codec codec, JobTrackingReporter reporter) throws WorkerException {
        ConfigurationSource configSource = Mockito.mock(ConfigurationSource.class);
        DataStore store = Mockito.mock(DataStore.class);
        return new JobTrackingWorkerFactory(configSource, store, codec, reporter);
    }


    private JobTrackingWorkerTask createTrackedTask(final String jobTaskId) {
        JobTrackingWorkerTask task = new JobTrackingWorkerTask();
        task.setJobTaskId(jobTaskId);
        return task;
    }
}
