/*
 * Copyright 2016-2023 Open Text.
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
package com.hpe.caf.services.job.scheduled.executor;

import com.github.workerframework.workermessageprioritization.rerouting.MessageRouterSingleton;
import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.QueueTaskMessage;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.api.worker.TrackingInfo;
import com.hpe.caf.services.job.util.JobTaskId;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.MessageProperties;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is responsible for sending task data to the target queue.
 */
public final class QueueServices implements AutoCloseable
{
    private static final Logger LOG = LoggerFactory.getLogger(QueueServices.class);

    private static final boolean CAF_WMP_ENABLED = ScheduledExecutorConfig.isCafWmpEnabled();

    private static final Pattern CAF_WMP_PARTITION_ID_PATTERN
            = CAF_WMP_ENABLED
            ? Pattern.compile(ScheduledExecutorConfig.getCafWmpPartitionIdPattern())
            : null;

    private static final Pattern CAF_WMP_TARGET_QUEUE_NAMES_PATTERN
            = CAF_WMP_ENABLED
            ? Pattern.compile(ScheduledExecutorConfig.getCafWmpTargetQueueNamesPattern())
            : null;

    private final Connection connection;
    private final Channel publisherChannel;
    private final String targetQueue;
    private final Codec codec;

    public QueueServices(final Connection connection, final Channel publisherChannel, final String targetQueue, final Codec codec) {

        this.connection = connection;
        this.publisherChannel = publisherChannel;
        this.targetQueue = targetQueue;
        this.codec = codec;
    }

    /**
     * Send task data message to the target queue.
     *
     * @param   jobId               the job identifier
     * @param   workerAction        the worker task details
     * @throws IOException          thrown if message cannot be sent
     */
    public void sendMessage(
        final String partitionId, final String jobId, final WorkerAction workerAction
    ) throws IOException, URISyntaxException {
        //  Generate a random task id.
        LOG.debug("Generating task id ...");
        final String taskId = UUID.randomUUID().toString();
        final Object taskMessage;

        //  Set up string for statusCheckUrl
        final String statusCheckUrl = UriBuilder.fromUri(ScheduledExecutorConfig.getWebserviceUrl())
            .path("partitions").path(partitionId)
            .path("jobs").path(jobId)
            .path("status").build().toString();

        //  Construct the task message.
        LOG.debug("Constructing the task message ...");
        final TrackingInfo trackingInfo = new TrackingInfo(
                new JobTaskId(partitionId, jobId).getMessageId(),
                new Date(),
                getStatusCheckIntervalMillis(ScheduledExecutorConfig.getStatusCheckIntervalSeconds()),
                statusCheckUrl, ScheduledExecutorConfig.getTrackingPipe(), workerAction.getTargetPipe());

        final String messageFormatVersion = ScheduledExecutorConfig.useNewQueueMessageFormat();
        if ("v3".equalsIgnoreCase(messageFormatVersion)) {
            taskMessage = getTaskMessage(trackingInfo, workerAction, taskId);
        } else {
            taskMessage = getQueueTaskMessage(trackingInfo, workerAction, taskId);
        }

        //  Serialise the task message.
        //  Wrap any CodecException as a RuntimeException as it shouldn't happen
        final byte[] taskMessageBytes;
        try {
            LOG.debug("Serialise the task message ...");
            taskMessageBytes = codec.serialise(taskMessage);
        } catch (final CodecException e) {
            LOG.error(e.getMessage());
            throw new RuntimeException(e);
        }

        // Reroute the message to a staging queue if applicable
        final String stagingQueueName;

        if (CAF_WMP_ENABLED && CAF_WMP_TARGET_QUEUE_NAMES_PATTERN.matcher(targetQueue).matches()) {

            final Matcher matcher = CAF_WMP_PARTITION_ID_PATTERN.matcher(partitionId);

            final String tenantId = matcher.matches() ? matcher.group(1) : partitionId;

            MessageRouterSingleton.init();

            stagingQueueName = MessageRouterSingleton.route(targetQueue, tenantId);

            LOG.debug("MessageRouterSingleton.route({}, {}) returned the following queue name: {}. " +
                            "The following task data will be routed to this queue: {}",
                    targetQueue,
                    tenantId,
                    stagingQueueName,
                    workerAction);
        } else {
            stagingQueueName = targetQueue;
        }

        //  Send the message.
        LOG.debug("Publishing the message ...");
        publisherChannel.basicPublish(
                "", stagingQueueName, MessageProperties.PERSISTENT_TEXT_PLAIN, taskMessageBytes);
    }

    private TaskMessage getTaskMessage(
        final TrackingInfo trackingInfo,
        final WorkerAction workerAction,
        final String taskId
    ) {
        //  Serialise the data payload. Encoding type is provided in the WorkerAction.
        final byte[] taskData;

        //  Check whether taskData is in the form of a string or object, and serialise/decode as appropriate.
        LOG.debug("Validating the task data ...");
        final Object taskDataObj = workerAction.getTaskData();
        final String taskDataStr = (String) taskDataObj;
        final WorkerAction.TaskDataEncodingEnum encoding = workerAction.getTaskDataEncoding();

        if (taskDataObj instanceof String) {
            if (encoding == null || encoding == WorkerAction.TaskDataEncodingEnum.UTF8) {
                taskData = taskDataStr.getBytes(StandardCharsets.UTF_8);
            } else if (encoding == WorkerAction.TaskDataEncodingEnum.BASE64) {
                taskData = Base64.decodeBase64(taskDataStr);
            } else {
                final String errorMessage = "Unknown taskDataEncoding";
                LOG.error(errorMessage);
                throw new RuntimeException(errorMessage);
            }
        } else if (taskDataObj instanceof Map<?, ?>) {
            try {
                taskData = codec.serialise(taskDataObj);
            } catch (final CodecException e) {
                final String errorMessage = "Failed to serialise TaskData";
                LOG.error(errorMessage);
                throw new RuntimeException(errorMessage, e);
            }
        } else {
            final String errorMessage = "The taskData is an unexpected type";
            LOG.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }

        return new TaskMessage(
            taskId,
            workerAction.getTaskClassifier(),
            workerAction.getTaskApiVersion(),
            taskData,
            TaskStatus.NEW_TASK,
            Collections.emptyMap(),
            targetQueue,
            trackingInfo);
    }

    private QueueTaskMessage getQueueTaskMessage(
        final TrackingInfo trackingInfo,
        final WorkerAction workerAction,
        final String taskId
    ) {
        //  Serialise the data payload. Encoding type is provided in the WorkerAction.
        final Object taskData;

        //  Check whether taskData is in the form of a string or object, and serialise/decode as appropriate.
        LOG.debug("Validating the task data ...");
        final Object taskDataObj = workerAction.getTaskData();

        if (taskDataObj instanceof String) {
            final String taskDataStr = (String) taskDataObj;
            final WorkerAction.TaskDataEncodingEnum encoding = workerAction.getTaskDataEncoding();

            taskData = tryToDeserialiseToMap(toTaskDataByteArray(taskDataStr, encoding));
        } else if (taskDataObj instanceof Map<?, ?>) {
            taskData = taskDataObj;
        } else {
            final String errorMessage = "The taskData is an unexpected type";
            LOG.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }

        return new QueueTaskMessage(
            taskId,
            workerAction.getTaskClassifier(),
            workerAction.getTaskApiVersion(),
            taskData,
            TaskStatus.NEW_TASK,
            Collections.emptyMap(),
            targetQueue,
            trackingInfo);
    }

    private static byte[] toTaskDataByteArray(
        final String taskDataStr,
        final WorkerAction.TaskDataEncodingEnum encoding)
    {
        if (encoding == null || encoding == WorkerAction.TaskDataEncodingEnum.UTF8) {
            return taskDataStr.getBytes(StandardCharsets.UTF_8);
        } else if (encoding == WorkerAction.TaskDataEncodingEnum.BASE64) {
            return Base64.decodeBase64(taskDataStr);
        } else {
            final String errorMessage = "Unknown taskDataEncoding";
            LOG.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }
    }

    private Object tryToDeserialiseToMap(final byte[] taskData)
    {
        try {
            return codec.deserialise(taskData, Map.class);
        } catch (final CodecException ex) {
            LOG.info("Issue encountered while deserialising {}", taskData, ex);
            return taskData;
        }
    }

    private static long getStatusCheckIntervalMillis(final String statusCheckIntervalSeconds)
    {
        try {
            return Long.parseLong(statusCheckIntervalSeconds) * 1000;
        } catch (NumberFormatException e) {
            throw new RuntimeException("Please provide a valid integer for statusCheckIntervalSeconds. " + e);
        }
    }

    /**
     * Closes the queue connection.
     * @throws RuntimeException thrown if the queue connection cannot be closed.
     */
    @Override
    public void close() throws RuntimeException{
        try {
            //  Close channel.
            if (publisherChannel != null) {
                LOG.debug("Closing channel ...");
                publisherChannel.close();
            }

            //  Close connection.
            if (connection != null) {
                LOG.debug("Closing connection ...");
                connection.close();
            }

        } catch (IOException | TimeoutException e) {
            final String errorMessage = "Failed to close the queuing connection.";
            LOG.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }
    }

}
