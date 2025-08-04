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

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConfirmListener;
import com.rabbitmq.client.ReturnListener;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;
import java.io.IOException;
import java.text.MessageFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monitors RabbitMQ publisher confirmations, message returns, and channel shutdowns
 * to determine if message publication failures are transient or permanent.
 * <p>
 * This analyzer listens for three event types to provide comprehensive failure analysis:
 * <ul>
 * <li><b>Publisher confirmations (ACK/NACK):</b> Indicates the broker's acceptance or rejection of a message.</li>
 * <li><b>Message returns:</b> Signifies that a 'mandatory' message could not be routed to any queue.</li>
 * <li><b>Channel shutdowns:</b> Reports unexpected connection or channel-level failures.</li>
 * </ul>
 * <p>
 * The analyzer is automatically registered as a listener on the provided channel.
 * Based on its analysis, it either leaves the job to be retried (transient failure)
 * or marks it as permanently failed and removes it from the job_task_data table (non-transient failure).
 */
public final class PublisherConfirmationAnalyzer implements ConfirmListener, ReturnListener, ShutdownListener {

    private static final Logger LOG = LoggerFactory.getLogger(PublisherConfirmationAnalyzer.class);

    private final String partitionId;
    private final String jobId;
    private final String targetQueue;

    /**
     * Enumerates the types of message publication failures.
     */
    public enum FailureType {
        /**
         * A temporary failure that may be resolved by retrying later.
         * Examples include network issues, full queues, or a broker under heavy load.
         */
        TRANSIENT,

        /**
         * A permanent failure that will not be resolved by retrying.
         * Examples include unroutable messages, invalid permissions, or protocol errors.
         */
        NON_TRANSIENT
    }

    /**
     * Constructs a new analyzer and registers it with the specified channel.
     *
     * @param channel The RabbitMQ channel to monitor (must not be null).
     * @param partitionId The partition identifier for this job.
     * @param jobId The unique job identifier.
     * @param targetQueue The name of the queue where messages are being sent.
     * @throws IllegalArgumentException if the provided channel is null.
     */
    public PublisherConfirmationAnalyzer(
            final Channel channel,
            final String partitionId,
            final String jobId,
            final String targetQueue) {

        if (channel == null) {
            throw new IllegalArgumentException("Channel cannot be null");
        }

        // Register this analyzer to receive all relevant events.
        channel.addConfirmListener(this);
        channel.addReturnListener(this);
        channel.addShutdownListener(this);

        this.partitionId = partitionId;
        this.jobId = jobId;
        this.targetQueue = targetQueue;
    }

    /**
     * Handles a successful message acknowledgment from the broker.
     * This indicates that the message has been accepted and safely handled by the broker.
     * The job is considered successfully processed and is removed from the system.
     *
     * @param deliveryTag The unique identifier for the acknowledged message.
     * @param multiple If {@code true}, all messages up to this tag are acknowledged.
     * @throws IOException if a communication-related issue occurs during cleanup.
     */
    @Override
    public void handleAck(final long deliveryTag, final boolean multiple) throws IOException {
        LOG.info("Message acknowledged by broker (deliveryTag: {}, multiple: {}). [partitionId={}, jobId={}, queue={}]",
                deliveryTag, multiple, partitionId, jobId, targetQueue);

        // A successful ACK means we can safely remove the job from the system.
        cleanupSuccessfulJob();
    }

    /**
     * Handles a message rejection (NACK) from the broker.
     * NACKs are typically caused by transient issues, such as a queue reaching its capacity,
     * so they are treated as retryable failures.
     *
     * @param deliveryTag The unique identifier for the rejected message.
     * @param multiple If {@code true}, all messages up to this tag are rejected.
     */
    @Override
    public void handleNack(final long deliveryTag, final boolean multiple) {
        final String reason = MessageFormat.format(
                "Message rejected by broker (deliveryTag: {0}, multiple: {1}). " +
                        "Likely a transient issue like a full queue.",
                deliveryTag, multiple);

        LOG.warn("TRANSIENT failure detected. {} [partitionId={}, jobId={}, queue={}]",
                reason, partitionId, jobId, targetQueue);
        handleFailure(FailureType.TRANSIENT, reason);
    }

    /**
     * Handles messages that were published as 'mandatory' but could not be routed to any queue.
     * This is considered a permanent, non-transient failure.
     *
     * @param replyCode The AMQP reply code indicating the routing failure reason.
     * @param replyText The human-readable description of the failure.
     * @param exchange The exchange the message was published to.
     * @param routingKey The routing key used.
     * @param properties The message properties.
     * @param body The message body.
     */
    @Override
    public void handleReturn(final int replyCode, final String replyText, final String exchange,
                             final String routingKey, final AMQP.BasicProperties properties, final byte[] body) {

        final String reason = MessageFormat.format(
                "Unroutable message. Reply code: {0}, Text: ''{1}'', Exchange: ''{2}'', RoutingKey: ''{3}''",
                replyCode, replyText, exchange, routingKey);

        LOG.error("NON-TRANSIENT failure detected. {} [partitionId={}, jobId={}, queue={}]",
                reason, partitionId, jobId, targetQueue);
        handleFailure(FailureType.NON_TRANSIENT, reason);
    }

    /**
     * Handles an unexpected channel or connection shutdown.
     * The shutdown cause is analyzed to determine if the failure is transient or permanent.
     * Application-initiated shutdowns are ignored.
     *
     * @param cause The {@link ShutdownSignalException} containing shutdown details.
     */
    @Override
    public void shutdownCompleted(final ShutdownSignalException cause) {
        // Ignore graceful shutdowns initiated by our application.
        if (cause.isInitiatedByApplication()) {
            LOG.info("Channel shutdown initiated by application. No action needed. [partitionId={}, jobId={}, queue={}]",
                    partitionId, jobId, targetQueue);
            return;
        }

        final FailureType failureType = JobFailureHandler.analyzeShutdownException(cause);
        final String reason = "Unexpected shutdown: " + cause.getMessage();

        LOG.error("{} failure detected. {} [partitionId={}, jobId={}, queue={}]",
                failureType, reason, partitionId, jobId, targetQueue, cause);

        handleFailure(failureType, reason);
    }

    /**
     * Performs cleanup operations for a successfully completed job.
     * This includes releasing cached resources and removing the job from the database.
     */
    private void cleanupSuccessfulJob() {
        // Release cached resources.
        QueueServicesCache.invalidate(new QueueServicesCache.Key(partitionId, jobId));

        // Remove the completed job from the job_task_data table.
        try {
            DatabasePoller.deleteDependentJob(partitionId, jobId);
        } catch (final ScheduledExecutorException e) {
            // If deletion fails, log a warning. The job will be retried later, which is not ideal but acceptable.
            LOG.error("Failed to delete completed job from job_task_data. Job may be retried. " +
                            "[partitionId={}, jobId={}, queue={}, error={}]",
                    partitionId, jobId, targetQueue, e.getMessage(), e);
        }
    }

    /**
     * A centralized handler for all failure scenarios.
     *
     * <p>For transient failures, it simply logs the event and cleans up resources, allowing
     * the job to be picked up again for a retry.
     *
     * <p>For non-transient failures, it marks the job as permanently failed in the database
     * and then removes it from the task data table to prevent any further retries.
     *
     * @param failureType The classification of the failure.
     * @param reason A descriptive string of the failure.
     */
    private void handleFailure(final FailureType failureType, final String reason) {
        LOG.info("Handling failure from async listener. [partitionId={}, jobId={}, type={}]",
                partitionId, jobId, failureType);

        QueueServicesCache.invalidate(new QueueServicesCache.Key(partitionId, jobId));

        if (failureType == FailureType.NON_TRANSIENT) {
            final String asyncReason = "Async listener: " + reason;
            JobFailureHandler.handleNonTransientFailure(partitionId, jobId, asyncReason);
        } else {
            LOG.warn("Transient failure handled by async listener. Job will be retried later. " +
                    "[partitionId={}, jobId={}, queue={}]", partitionId, jobId, targetQueue);
        }
    }
}
