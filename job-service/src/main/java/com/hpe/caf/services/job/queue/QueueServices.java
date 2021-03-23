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
package com.hpe.caf.services.job.queue;

import static com.github.cafapi.correlation.constants.CorrelationIdConfigurationConstants.MDC_KEY;

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.api.worker.TrackingInfo;
import com.hpe.caf.services.job.api.generated.model.WorkerAction;
import com.hpe.caf.services.configuration.AppConfig;
import com.hpe.caf.services.job.util.JobTaskId;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.MessageProperties;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * This class is responsible sending task data to the target queue.
 */
public final class QueueServices {

    private static final Logger LOG = LoggerFactory.getLogger(QueueServices.class);
    private final Connection connection;
    private final Channel publisherChannel;
    private final String targetQueue;
    private final Codec codec;

    public QueueServices(Connection connection, Channel publisherChannel, String targetQueue, Codec codec) {

        this.connection = connection;
        this.publisherChannel = publisherChannel;
        this.targetQueue = targetQueue;
        this.codec = codec;
    }

    /**
     * Send task data message to the target queue.
     */
    public void sendMessage(
        final String partitionId, String jobId, WorkerAction workerAction, AppConfig config
    ) throws IOException, InterruptedException, TimeoutException
    {
        //  Generate a random task id.
        String taskId = UUID.randomUUID().toString();

        //  Serialise the data payload. Encoding type is provided in the WorkerAction.
        byte[] taskData = null;

        //Check whether taskData is in the form of a string or object, and serialise/decode as appropriate.
        final Object taskDataObj = workerAction.getTaskData();
        
        if (taskDataObj instanceof String) {
            final String taskDataStr = (String) taskDataObj;
            final WorkerAction.TaskDataEncodingEnum encoding = workerAction.getTaskDataEncoding();

            if (encoding == null || encoding == WorkerAction.TaskDataEncodingEnum.UTF8) {
                taskData = taskDataStr.getBytes(StandardCharsets.UTF_8);
            } else if (encoding == WorkerAction.TaskDataEncodingEnum.BASE64) {
                taskData = Base64.decodeBase64(taskDataStr);
            } else {
                throw new RuntimeException("Unknown taskDataEncoding");
            }
        } else if (taskDataObj instanceof Map<?, ?>) {
            try {
                taskData = codec.serialise(taskDataObj);
            } catch (CodecException e) {
                throw new RuntimeException("Failed to serialise TaskData", e);
            }
        } else {
            throw new RuntimeException("The taskData is an unexpected type");
        }

        //set up string for statusCheckUrl
        String statusCheckUrl = UriBuilder.fromUri(config.getWebserviceUrl())
            .path("partitions").path(partitionId)
            .path("jobs").path(jobId)
            .path("status").build().toString();

        //  Construct the task message.
        final TrackingInfo trackingInfo = new TrackingInfo(
                new JobTaskId(partitionId, jobId).getMessageId(),
                null,
                getStatusCheckIntervalMillis(config.getStatusCheckIntervalSeconds()),
                statusCheckUrl, config.getTrackingPipe(), workerAction.getTargetPipe());

        final TaskMessage taskMessage = new TaskMessage(
                taskId,
                workerAction.getTaskClassifier(),
                workerAction.getTaskApiVersion(),
                taskData,
                TaskStatus.NEW_TASK,
                Collections.<String, byte[]>emptyMap(),
                targetQueue,
                trackingInfo,
                null,
                MDC.get(MDC_KEY));

        //  Serialise the task message.
        //  Wrap any CodecException as a RuntimeException as it shouldn't happen
        final byte[] taskMessageBytes;
        try {
            taskMessageBytes = codec.serialise(taskMessage);
        } catch (CodecException e) {
            throw new RuntimeException(e);
        }

        //  Send the message.
        try {
            publishMessage(taskMessageBytes);
        } catch (IOException | TimeoutException e) {
            LOG.warn("Failed to publish message. Will retry", e);
            publishMessage(taskMessageBytes);
        }
    }

    private void publishMessage(final byte[] taskMessageBytes)
            throws IOException, InterruptedException, TimeoutException
    {
        publisherChannel.basicPublish(
                "", targetQueue, MessageProperties.PERSISTENT_TEXT_PLAIN, taskMessageBytes);
        publisherChannel.waitForConfirmsOrDie(10000);
    }

    private static long getStatusCheckIntervalMillis(final String statusCheckIntervalSeconds){
        try{
            return Long.parseLong(statusCheckIntervalSeconds) * 1000;
        } catch (NumberFormatException e) {
            throw new RuntimeException("Please provide a valid integer for statusCheckIntervalSeconds. " + e);
        }
    }

    /**
     * Closes the queue connection.
     */
    public void close() throws Exception {
        try {
            //  Close channel.
            if (publisherChannel != null) {
                    publisherChannel.close();
            }

            //  Close connection.
            if (connection != null) {
                    connection.close();
            }

        } catch (IOException | TimeoutException e) {
            throw new Exception("Failed to close the queuing connection.");
        }
    }

}
