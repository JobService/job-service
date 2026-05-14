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
package com.github.jobservice.scheduledexecutor;

import com.github.cafapi.common.api.Codec;
import com.github.cafapi.common.api.CodecException;
import com.github.jobservice.util.JobTaskId;
import com.github.workerframework.api.TaskMessage;
import com.github.workerframework.api.TaskStatus;
import com.github.workerframework.api.TrackingInfo;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import com.github.jobservice.scheduledexecutor.batching.PayloadBatchingService;
import com.github.jobservice.scheduledexecutor.batching.SubdocumentBatchSplitter;
import com.github.jobservice.scheduledexecutor.batching.SubtaskIdGenerator;
import com.github.jobservice.util.TaskPipeUtil;

/**
 * This class is responsible for sending task data to the target queue.
 */
public final class QueueServices implements AutoCloseable
{
    private static final Logger LOG = LoggerFactory.getLogger(QueueServices.class);

    private static final int RABBIT_MQ_PUBLISH_TIMEOUT_MILLIS =
            ScheduledExecutorConfig.getRabbitMQPublishTimeoutSeconds() * 1000;

    private final Connection connection;
    private final Channel publisherChannel;
    private final String targetQueue;               // Queue that should be set in the 'to' field of the task message

    private final Codec codec;

    public QueueServices(
            final Connection connection,
            final Channel publisherChannel,
            final String targetQueue,
            final Codec codec) {

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
    ) throws IOException, URISyntaxException, InterruptedException, TimeoutException
    {
        // Check if payload batching is required
        if (PayloadBatchingService.shouldBatchPayload(workerAction)) {
            sendBatchedMessages(partitionId, jobId, workerAction);
        } else {
            sendSingleMessage(partitionId, jobId, workerAction);
        }
    }

    /**
     * Sends task data as a single message to the target queue.
     */
    private void sendSingleMessage(
            final String partitionId, final String jobId, final WorkerAction workerAction
    ) throws IOException, URISyntaxException, InterruptedException, TimeoutException
    {
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

        // Strip the batcher prefix from task pipe if present
        final WorkerAction NonBatchingWorkerAction;
        if (TaskPipeUtil.hasSubdocumentBatcherPrefix(workerAction.getTaskPipe())) {
            final String strippedTaskPipe = TaskPipeUtil.stripBatcherPrefix(workerAction.getTaskPipe());
            NonBatchingWorkerAction = new WorkerAction();
            NonBatchingWorkerAction.setTaskClassifier(workerAction.getTaskClassifier());
            NonBatchingWorkerAction.setTaskApiVersion(workerAction.getTaskApiVersion());
            NonBatchingWorkerAction.setTaskData(workerAction.getTaskData());
            NonBatchingWorkerAction.setTaskDataEncoding(workerAction.getTaskDataEncoding());
            NonBatchingWorkerAction.setTaskPipe(strippedTaskPipe);
            NonBatchingWorkerAction.setTargetPipe(workerAction.getTargetPipe());
            NonBatchingWorkerAction.setCorrelationId(workerAction.getCorrelationId());
        } else {
            NonBatchingWorkerAction = workerAction;
        }

        final Object taskMessage = getTaskMessage(trackingInfo, NonBatchingWorkerAction, taskId);

        //  Serialise and publish
        publishTaskMessage(taskMessage);
    }

    /**
     * Sends task data as multiple batched messages for large DocumentWorkerTask payloads.
     */
    private void sendBatchedMessages(
        final String partitionId,
        final String jobId,
        final WorkerAction workerAction
    ) throws IOException, URISyntaxException, InterruptedException, TimeoutException
    {
        // Deserialize taskData JSON to Map
        final Map<String, Object> taskDataMap = PayloadBatchingService.deserializeTaskData(workerAction);
        if (taskDataMap == null) {
            throw new RuntimeException("Failed to deserialize taskData for batching");
        }

        // Extract subdocuments reference
        final List<Object> subdocuments = SubdocumentBatchSplitter.extractSubdocuments(taskDataMap);
        if (subdocuments == null) {
            throw new RuntimeException("No subdocuments found for batching");
        }

        final int batchSize = PayloadBatchingService.getBatchSize();
        final int totalBatches = SubdocumentBatchSplitter.calculateBatchCount(subdocuments.size(), batchSize);

        // Generate ONE base task UUID for all batches of this job
        final String baseTaskId = UUID.randomUUID().toString();
        final String baseJobTaskId = new JobTaskId(partitionId, jobId).getMessageId();

        // Status check URL is same for all batches (points to parent job)
        final String statusCheckUrl = UriBuilder.fromUri(ScheduledExecutorConfig.getWebserviceUrl())
            .path("partitions").path(partitionId)
            .path("jobs").path(jobId)
            .path("status").build().toString();

        // Strip the DocumentWorkerSubdocumentBatcher() prefix from task pipe
        final String strippedTaskPipe = TaskPipeUtil.stripBatcherPrefix(workerAction.getTaskPipe());

        LOG.info("Sending {} batched messages for job {} ({} subdocuments, batch size {}, task pipe: {})",
                 totalBatches, jobId, subdocuments.size(), batchSize, strippedTaskPipe);

        // Process ONE batch at a time
        for (int batchIndex = 1; batchIndex <= totalBatches; batchIndex++) {

            // 1. Get subdocuments subList VIEW for this batch
            final List<Object> subdocBatch = SubdocumentBatchSplitter.getSubdocumentsBatchView(
                subdocuments, batchIndex, batchSize);

            // 2. Create batched taskData Map with this batch's subdocuments
            final Map<String, Object> batchedTaskDataMap = SubdocumentBatchSplitter.createBatchedTaskData(
                taskDataMap, subdocBatch);

            // 3. Serialize batched taskData to JSON String for WorkerAction
            final byte[] batchedTaskDataBytes;
            try {
                batchedTaskDataBytes = codec.serialise(batchedTaskDataMap);
            } catch (final CodecException e) {
                throw new RuntimeException("Failed to serialize batched taskData", e);
            }
            final String batchedTaskDataString = new String(batchedTaskDataBytes, StandardCharsets.UTF_8);

            // 4. Generate subtask IDs for this batch
            final String taskSubtaskId = SubtaskIdGenerator.generateTaskSubtaskId(
                baseTaskId, batchIndex, totalBatches);
            final String jobTaskSubtaskId = SubtaskIdGenerator.generateJobTaskSubtaskId(
                baseJobTaskId, batchIndex, totalBatches);

            // 5. Create TrackingInfo with subtask jobTaskId
            final TrackingInfo trackingInfo = new TrackingInfo(
                jobTaskSubtaskId,
                new Date(),
                getStatusCheckIntervalMillis(ScheduledExecutorConfig.getStatusCheckIntervalSeconds()),
                statusCheckUrl,
                ScheduledExecutorConfig.getTrackingPipe(),
                workerAction.getTargetPipe());

            // 6. Create a modified WorkerAction with batched taskData and stripped task pipe
            final WorkerAction batchWorkerAction = new WorkerAction();
            batchWorkerAction.setTaskClassifier(workerAction.getTaskClassifier());
            batchWorkerAction.setTaskApiVersion(workerAction.getTaskApiVersion());
            batchWorkerAction.setTaskData(batchedTaskDataString);
            batchWorkerAction.setTaskDataEncoding(WorkerAction.TaskDataEncodingEnum.UTF8);
            batchWorkerAction.setTaskPipe(strippedTaskPipe);
            batchWorkerAction.setTargetPipe(workerAction.getTargetPipe());
            batchWorkerAction.setCorrelationId(workerAction.getCorrelationId());

            // 7. Reuse existing getTaskMessage() method
            final TaskMessage taskMessage = getTaskMessage(trackingInfo, batchWorkerAction, taskSubtaskId);

            // 8. Publish this batch
            LOG.debug("Sending batch {}/{} with {} subdocuments (taskId={}, jobTaskId={})",
                      batchIndex, totalBatches, subdocBatch.size(), taskSubtaskId, jobTaskSubtaskId);

            publishTaskMessage(taskMessage);
        }

        LOG.info("Successfully sent all {} batches for job {}", totalBatches, jobId);
    }

    /**
     * Publishes a task message to RabbitMQ.
     */
    private void publishTaskMessage(final Object taskMessage)
        throws IOException, InterruptedException, TimeoutException
    {
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
        publisherChannel.waitForConfirmsOrDie(RABBIT_MQ_PUBLISH_TIMEOUT_MILLIS);
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
