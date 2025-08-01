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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Manages and analyzes publisher confirmations, returns, and shutdowns to determine
 * if a message failure is transient.
 * <p>
 * It implements all necessary listeners and should be added to the channel.
 */
public class PublisherConfirmationAnalyzer implements ConfirmListener, ReturnListener, ShutdownListener {

    private static final Logger LOG = LoggerFactory.getLogger(PublisherConfirmationAnalyzer.class);

    private final String partitionId;
    private final String jobId;
    private final String targetQueue;

    public enum FailureType {
        /** The error may be resolved by retrying later (e.g., queue full, network issue). */
        TRANSIENT,
        /** The error will not be resolved by retrying (e.g., unroutable message, invalid permissions). */
        NON_TRANSIENT;
    }

    public PublisherConfirmationAnalyzer(
            final Channel channel,
            final String partitionId,
            final String jobId,
            final String targetQueue) {
        if (channel != null) {
            channel.addConfirmListener(this);
            channel.addReturnListener(this);
            channel.addShutdownListener(this);
        } else {
            throw new IllegalArgumentException("Channel cannot be null");
        }

        this.partitionId = partitionId;
        this.jobId = jobId;
        this.targetQueue = targetQueue;
    }

    /**
     * Handles a successful ACK from the broker.
     */
    @Override
    public void handleAck(final long deliveryTag, final boolean multiple) throws IOException {
        LOG.info("Received ACK for deliveryTag: {} (multiple: {}) [partitionId={}, jobId={}, queue={}]",
                deliveryTag, multiple, partitionId, jobId, targetQueue);

        // Delete the job from the job_task_data table as it has been successfully processed
        DatabasePoller.deleteDependentJob(partitionId, jobId);

        // Remove the QueueServices instance from the cache (will also call QueueServices.close())
        QueueServicesCache.invalidate(new QueueServicesCache.Key(partitionId, jobId));
    }

    /**
     * Handles a broker NACK. This is typically due to a transient issue like a queue
     * reaching its max-length or max-bytes limit. Since the protocol doesn't provide
     * a specific reason here, we assume it's transient.
     *
     * @param deliveryTag the delivery tag of the NACKed message.
     * @param multiple if true, all outstanding messages up to this delivery tag are NACKed.
     */
    @Override
    public void handleNack(final long deliveryTag, final boolean multiple) throws IOException {
        // This is a soft failure. Assume TRANSIENT because the most common reason is a full queue.
        final String reason = String.format(
                "Received NACK for deliveryTag: %d (multiple: %b) [partitionId=%s, jobId=%s, queue=%s]",
                deliveryTag, multiple, partitionId, jobId, targetQueue);
        LOG.warn(reason);

        handleFailure(FailureType.TRANSIENT);
    }

    /**
     * Handles a message that was published as 'mandatory' but could not be routed.
     * This is a definitive NON-TRANSIENT failure.
     */
    @Override
    public void handleReturn(final int replyCode, final String replyText, final String exchange, final String routingKey,
                             final AMQP.BasicProperties properties, final byte[] body) throws IOException {
        // This is a hard failure. The message is unroutable.
        final String reason = String.format("Unroutable message. Reply code: %d, Text: %s, Exchange: '%s', " +
                        "RoutingKey: '%s' [partitionId=%s, jobId=%s, queue=%s]",
                replyCode, replyText, exchange, routingKey, partitionId, jobId, targetQueue);
        LOG.error("NON-TRANSIENT failure: {}", reason);

        handleFailure(FailureType.NON_TRANSIENT);
    }

    /**
     * Handles an unexpected channel or connection shutdown. The reason for the shutdown
     * determines if the failure is transient or not.
     *
     * @param cause the ShutdownSignalException containing the reason.
     */
    @Override
    public void shutdownCompleted(final ShutdownSignalException cause) {
        // This is a hard failure. We need to analyze the cause.
        if (cause.isInitiatedByApplication()) {
            LOG.info("Shutdown initiated by application. No action needed. [partitionId={}, jobId={}, queue={}]",
                    partitionId, jobId, targetQueue);
            return;
        }

        final FailureType failureType = isShutdownTransient(cause) ? FailureType.TRANSIENT : FailureType.NON_TRANSIENT;
        final String reason = String.format("Shutdown signal received: %s", cause.getMessage());
        LOG.error("{} failure: {} [partitionId={}, jobId={}, queue={}]",
                failureType, reason, partitionId, jobId, targetQueue, cause);

        handleFailure(failureType);
    }

    /**
     * Analyzes the shutdown reason to determine if it is transient.
     * This logic is adapted from your original NackReasonAnalyzer.
     *
     * @param cause The exception that caused the shutdown.
     * @return true if the shutdown reason suggests a transient problem.
     */
    private boolean isShutdownTransient(final ShutdownSignalException cause) {
        final Object reason = cause.getReason();
        int replyCode = -1;

        if (reason instanceof AMQP.Connection.Close) {
            replyCode = ((AMQP.Connection.Close) reason).getReplyCode();
        } else if (reason instanceof AMQP.Channel.Close) {
            replyCode = ((AMQP.Channel.Close) reason).getReplyCode();
        }

        if (replyCode == -1) {
            // Could be a network I/O exception, which is transient.
            return cause.getCause() instanceof IOException;
        }

        LOG.info("Analyzing shutdown reply code: {} [partitionId={}, jobId={}, queue={}]",
                replyCode, partitionId, jobId, targetQueue);

        return switch (replyCode) {
            // Transient reply codes
            case AMQP.REPLY_SUCCESS, // e.g. Connection shutdown requested by client
                 313, // NO_CONSUMERS (can be transient if consumers come online)
                 320, // CONNECTION_FORCED (e.g. by management UI, could be for transient reasons)
                 405, // RESOURCE_LOCKED (e.g., exclusive queue access conflict)
                 503, // SERVICE_UNAVAILABLE (e.g., RabbitMQ is shutting down gracefully)
                 506, // RESOURCE_ERROR (e.g. out of memory)
                 541, // INTERNAL_ERROR (often transient)
                 542 -> true; // BROKER_OVERLOADED

            // Non-transient reply codes
            case 311, // CONTENT_TOO_LARGE
                 312, // NO_ROUTE
                 403, // ACCESS_REFUSED
                 404, // NOT_FOUND
                 406, // PRECONDITION_FAILED
                 501, // NOT_IMPLEMENTED
                 502, // COMMAND_INVALID
                 504, // CHANNEL_ERROR
                 505 -> false; // UNEXPECTED_FRAME

            default -> {
                // Default to non-transient if uncertain.
                LOG.warn("Unknown reply code: {}. Assuming NON-TRANSIENT.", replyCode);
                yield false;
            }
        };
    }

    /**
     * Central point for handling the analyzed failure.
     *
     * @param type The type of failure (TRANSIENT or NON_TRANSIENT).
     */
    private void handleFailure(final FailureType type) {
        switch (type) {
            case TRANSIENT:
                // Remove the QueueServices instance from the cache (will also call QueueServices.close())
                QueueServicesCache.invalidate(new QueueServicesCache.Key(partitionId, jobId));

                break;
            case NON_TRANSIENT:
                // Delete the job from the job_task_data table to prevent the job from being retried
                DatabasePoller.deleteDependentJob(partitionId, jobId);

                // Remove the QueueServices instance from the cache (will also call QueueServices.close())
                QueueServicesCache.invalidate(new QueueServicesCache.Key(partitionId, jobId));

                // Mark the job as failed in the database
                LOG.error("TODO MARK JOB AS FAILED IN DB");

                break;
        }
    }
}