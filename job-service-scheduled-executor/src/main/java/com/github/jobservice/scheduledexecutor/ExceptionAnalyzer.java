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

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.MissedHeartbeatException;
import com.rabbitmq.client.PossibleAuthenticationFailureException;
import com.rabbitmq.client.ShutdownSignalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

final class ExceptionAnalyzer
{
    private static final Logger LOG = LoggerFactory.getLogger(ExceptionAnalyzer.class);

    public static ExceptionType analyzeException(final Exception exception)
    {
        // Configuration, code-related errors are permanent
        if (exception instanceof IllegalArgumentException ||
                exception instanceof SecurityException ||
                exception instanceof ClassNotFoundException ||
                exception instanceof NoSuchMethodException ||
                exception instanceof InstantiationException ||
                exception instanceof PossibleAuthenticationFailureException) {

            return ExceptionType.PERMANENT;
        }

        // Connection/channel state issues are transient as we create new connections/channels when retrying
        if (exception instanceof AlreadyClosedException || exception instanceof MissedHeartbeatException) {
            return ExceptionType.TRANSIENT;
        }

        // Handle ShutdownSignalException with AMQP reply code analysis
        if (exception instanceof ShutdownSignalException shutdownException) {
            return analyzeShutdownException(shutdownException);
        }

        // Check for network-related exceptions (typically transient)
        if (exception instanceof IOException || exception instanceof TimeoutException) {
            return ExceptionType.TRANSIENT;
        }

        // Default to permanent for unknown exceptions to prevent infinite retry loops
        LOG.error("Unknown exception type encountered: {}. Assuming PERMANENT to prevent retry loop.",
                exception.getClass().getName());
        return ExceptionType.PERMANENT;
    }

    private static ExceptionType analyzeShutdownException(
            final ShutdownSignalException shutdownException)
    {
        if (shutdownException.isInitiatedByApplication()) {
            return ExceptionType.TRANSIENT;
        }

        final Object reason = shutdownException.getReason();
        final int replyCode = extractReplyCode(reason);

        if (replyCode == -1) {
            boolean isIoException = shutdownException.getCause() instanceof IOException;
            return isIoException ? ExceptionType.TRANSIENT : ExceptionType.PERMANENT;
        }

        return isReplyCodeTransient(replyCode) ? ExceptionType.TRANSIENT : ExceptionType.PERMANENT;
    }

    private static int extractReplyCode(final Object reason)
    {
        if (reason instanceof AMQP.Connection.Close closeConnection) {
            return closeConnection.getReplyCode();
        }
        if (reason instanceof AMQP.Channel.Close closeChannel) {
            return closeChannel.getReplyCode();
        }
        return -1;
    }

    private static boolean isReplyCodeTransient(final int replyCode)
    {
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

            // Permanent conditions that will not resolve with a retry
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

            // Default to permanent for unknown codes to prevent infinite retries
            default -> {
                LOG.warn("Unknown AMQP reply code: {}. Assuming PERMANENT to prevent retry loop.", replyCode);
                yield false;
            }
        };
    }
}
