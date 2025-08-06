/*
 * Copyright 2016-2025 Open Text.
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
package com.github.jobservice.scheduledexecutor;

import com.github.cafapi.common.api.Codec;
import com.github.cafapi.common.api.CodecException;
import com.github.jobservice.util.JobTaskId;
import com.github.workerframework.api.TaskMessage;
import com.github.workerframework.api.TaskStatus;
import com.github.workerframework.api.TrackingInfo;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * This class is responsible for sending task data to the target queue.
 * It uses a shared connection but maintains its own channel per job.
 */
public final class QueueServices
{
    private static final Logger LOG = LoggerFactory.getLogger(QueueServices.class);

    private final String partitionId;
    private final String jobId;
    private final Channel publisherChannel;         // This instance owns and manages this channel
    private final String targetQueue;               // Queue that should be set in the 'to' field of the task message
    private final Codec codec;

    public QueueServices(
            final String partitionId,
            final String jobId,
            final Channel publisherChannel,
            final String targetQueue,
            final Codec codec) {

        this.partitionId = partitionId;
        this.jobId = jobId;
        this.publisherChannel = publisherChannel;
        this.targetQueue = targetQueue;
        this.codec = codec;
    }

    /**
     * Send task data message to the target queue.
     *
     * @param   partitionId         the partition identifier
     * @param   jobId               the job identifier
     * @param   workerAction        the worker task details
     * @throws IOException          thrown if message cannot be sent
     */
    public void sendMessage(
        final String partitionId, final String jobId, final WorkerAction workerAction
    ) throws IOException, URISyntaxException, InterruptedException, TimeoutException {
        //  Generate a random task id.
        LOG.debug("Generating task id ...");
        final String taskId = UUID.randomUUID().toString();

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

        final Object taskMessage = getTaskMessage(trackingInfo, workerAction, taskId);

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

        if (LOG.isDebugEnabled()) {
            LOG.debug("Publishing the following message to the {} queue: {}",
                    targetQueue, new String(taskMessageBytes, StandardCharsets.UTF_8));
        }

        publisherChannel.basicPublish("", targetQueue, true, MessageProperties.PERSISTENT_TEXT_PLAIN, taskMessageBytes);
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
            trackingInfo,
            null,
            workerAction.getCorrelationId());
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
     * Closes only the channel owned by this instance.
     * The shared connection is NOT closed as it may be used by other jobs.
     */
    public void close() {
        try {
            // Close only the channel - the shared connection should remain open for other jobs
            if (publisherChannel != null && publisherChannel.isOpen()) {
                LOG.debug("Closing channel [partitionId={}, jobId={}, queue={}]...",
                        partitionId, jobId, targetQueue);
                publisherChannel.close();
            } else {
                LOG.debug("Publisher channel is already closed or was never opened [partitionId={}, jobId={}, queue={}]",
                        partitionId, jobId, targetQueue);
            }
        } catch (final IOException | TimeoutException e) {
            LOG.error("Failed to close the channel [partitionId={}, jobId={}, queue={}]",
                    partitionId, jobId, targetQueue, e);
        }
    }
}
