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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.MissedHeartbeatException;
import com.rabbitmq.client.PossibleAuthenticationFailureException;
import com.rabbitmq.client.ShutdownSignalException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for handling job failures consistently across the scheduled executor.
 * This class provides common functionality for analyzing failures and reporting them.
 */
class JobFailureHandler {
    private static final Logger LOG = LoggerFactory.getLogger(JobFailureHandler.class);

    private static final String FAILURE_ID = "ADD_TO_QUEUE_FAILURE";
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final ObjectMapper MAPPER = new ObjectMapper();
    static {
        MAPPER.setDateFormat(DATE_FORMAT);
    }

    /**
     * Analyzes an exception to determine if it represents a transient or non-transient failure.
     *
     * @param exception The exception to analyze
     * @return The failure type classification
     */
    public static RabbitMqAsyncListener.FailureType analyzeException(final Exception exception) {
        // Check for specific exception types that indicate non-transient failures
        if (exception instanceof IllegalArgumentException ||
                exception instanceof SecurityException ||
                exception instanceof ClassNotFoundException ||
                exception instanceof NoSuchMethodException ||
                exception instanceof InstantiationException ||
                exception instanceof PossibleAuthenticationFailureException ||
                exception instanceof AlreadyClosedException ||
                exception instanceof MissedHeartbeatException) {
            // Configuration, code-related, or definitive RabbitMQ state errors are non-transient.
            return RabbitMqAsyncListener.FailureType.NON_TRANSIENT;
        }

        if (exception instanceof ShutdownSignalException shutdownException) {
            // For shutdown signals, analyze the cause
            return analyzeShutdownException(shutdownException);
        }

        // Check for network-related exceptions (typically transient)
        if (exception instanceof IOException || exception instanceof TimeoutException) {
            // Network issues are typically transient.
            return RabbitMqAsyncListener.FailureType.TRANSIENT;
        }

        // Default to transient for unknown exceptions to allow retries
        LOG.warn("Unknown exception type encountered: {}. Defaulting to TRANSIENT classification to allow for retry.",
                exception.getClass().getName());
        return RabbitMqAsyncListener.FailureType.TRANSIENT;
    }

    /**
     * Handles non-transient failures by marking the job as permanently failed in the database
     * and then deleting it from the job_task_data table.
     *
     * @param partitionId The partition identifier
     * @param jobId The job identifier
     * @param reason A descriptive string of the failure
     */
    public static void handleNonTransientFailure(final String partitionId, final String jobId, final String reason) {
        final QueueFailure failure = createFailureRecord(jobId, reason);

        final String failureJson;
        try {
            failureJson = serializeFailure(failure);
        } catch (final JsonProcessingException e) {
            LOG.error("Failed to serialize failure record. Cannot mark as failed, so job will be retried. " +
                            "[partitionId={}, jobId={}, error={}]",
                    partitionId, jobId, e.getMessage(), e);
            return;
        }

        try {
            DatabasePoller.reportFailure(partitionId, jobId, failureJson);
        } catch (final ScheduledExecutorException e) {
            LOG.error("Failed to mark job as failed in the database. Job will be retried. " +
                            "[partitionId={}, jobId={}, error={}]",
                    partitionId, jobId, e.getMessage(), e);
            return;
        }

        try {
            DatabasePoller.deleteDependentJob(partitionId, jobId);
        } catch (final ScheduledExecutorException e) {
            LOG.error("Failed to delete non-transient job from job_task_data. Job may be retried despite being " +
                            "marked as failed. [partitionId={}, jobId={}, error={}]",
                    partitionId, jobId, e.getMessage(), e);
        }
    }

    /**
     * Creates a standard failure record with metadata for database storage.
     *
     * @param jobId The job identifier
     * @param reason The descriptive failure reason
     * @return A populated QueueFailure object
     */
    public static QueueFailure createFailureRecord(final String jobId, final String reason) {
        final QueueFailure failure = new QueueFailure();
        failure.setFailureId(FAILURE_ID);
        failure.setFailureTime(new Date());
        failure.failureSource(MessageFormat.format("Job Service Scheduled Executor for job id {0}", jobId));
        failure.failureMessage(reason);
        return failure;
    }

    /**
     * Analyzes a ShutdownSignalException to determine if it represents a transient failure.
     * This uses the same logic as PublisherConfirmationAnalyzer.
     *
     * @param shutdownException The shutdown exception to analyze
     * @return The failure type classification
     */
    public static RabbitMqAsyncListener.FailureType analyzeShutdownException(
            final ShutdownSignalException shutdownException) {

        if (shutdownException.isInitiatedByApplication()) {
            return RabbitMqAsyncListener.FailureType.TRANSIENT;
        }

        final Object reason = shutdownException.getReason();
        final int replyCode = extractReplyCode(reason);

        if (replyCode == -1) {
            boolean isIoException = shutdownException.getCause() instanceof IOException;
            return isIoException ? RabbitMqAsyncListener.FailureType.TRANSIENT :
                    RabbitMqAsyncListener.FailureType.NON_TRANSIENT;
        }

        return isReplyCodeTransient(replyCode) ? RabbitMqAsyncListener.FailureType.TRANSIENT :
                RabbitMqAsyncListener.FailureType.NON_TRANSIENT;
    }

    /**
     * Extracts the AMQP reply code from a connection or channel close reason.
     *
     * @param reason The AMQP close reason object
     * @return The reply code, or -1 if no code is available
     */
    private static int extractReplyCode(final Object reason) {
        if (reason instanceof AMQP.Connection.Close closeConnection) {
            return closeConnection.getReplyCode();
        }
        if (reason instanceof AMQP.Channel.Close closeChannel) {
            return closeChannel.getReplyCode();
        }
        return -1;
    }

    /**
     * Determines if an AMQP reply code indicates a transient failure condition.
     *
     * @param replyCode The AMQP reply code to evaluate
     * @return true if the reply code suggests a transient issue
     */
    private static boolean isReplyCodeTransient(final int replyCode) {
        return switch (replyCode) {
            // Transient conditions that may resolve with a retry
            case AMQP.REPLY_SUCCESS,            // 200 - graceful shutdown, may come back online
                 AMQP.NO_CONSUMERS,             // 313 - consumers may reconnect
                 AMQP.CONNECTION_FORCED,        // 320 - administrative action, could be temporary
                 AMQP.RESOURCE_LOCKED,          // 405 - exclusive access conflict, may resolve
                 AMQP.COMMAND_INVALID,          // 503 - broker maintenance, temporary
                 AMQP.RESOURCE_ERROR,           // 506 - memory/disk issues, may be temporary
                 AMQP.INTERNAL_ERROR            // 541 - often recoverable
                    -> true;

            // Non-transient conditions that will not resolve with a retry
            case AMQP.CONTENT_TOO_LARGE,        // 311 - message size exceeds broker limits
                 AMQP.NO_ROUTE,                 // 312 - no matching route for message
                 AMQP.ACCESS_REFUSED,           // 403 - authentication/authorization failure
                 AMQP.NOT_FOUND,                // 404 - requested resource does not exist
                 AMQP.PRECONDITION_FAILED,      // 406 - method preconditions not met
                 AMQP.INVALID_PATH,             // 402 - malformed path or routing key
                 AMQP.FRAME_ERROR,              // 501 - protocol frame format error
                 AMQP.SYNTAX_ERROR,             // 502 - command syntax incorrect
                 AMQP.CHANNEL_ERROR,            // 504 - channel-specific protocol error
                 AMQP.UNEXPECTED_FRAME,         // 505 - frame received in wrong state
                 AMQP.NOT_ALLOWED,              // 530 - operation not permitted by policy
                 AMQP.NOT_IMPLEMENTED           // 540 - requested feature not supported
                    -> false;

            // Default to non-transient for unknown codes to prevent infinite retries
            default -> {
                LOG.warn("Unknown AMQP reply code: {}. Assuming NON-TRANSIENT to prevent retry loop.", replyCode);
                yield false;
            }
        };
    }

    /**
     * Serializes a failure record object into a JSON string.
     *
     * @param failure The failure record to serialize
     * @return The JSON representation of the failure
     * @throws JsonProcessingException if the serialization process fails
     */
    public static String serializeFailure(final QueueFailure failure) throws JsonProcessingException {
        return MAPPER.writeValueAsString(failure);
    }
}
