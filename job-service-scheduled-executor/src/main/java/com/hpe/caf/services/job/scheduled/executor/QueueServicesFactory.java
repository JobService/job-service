/*
 * Copyright 2016-2022 Micro Focus or one of its affiliates.
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
import com.hpe.caf.util.rabbitmq.RabbitUtil;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is responsible for creating the RabbitMQ connection and channel.
 */
public final class QueueServicesFactory
{
    private static final Logger LOG = LoggerFactory.getLogger(QueueServicesFactory.class);

    private static final boolean CAF_WMP_ENABLED = ScheduledExecutorConfig.isCafWmpEnabled();

    private static final Pattern CAF_WMP_PARTITION_ID_PATTERN
            = CAF_WMP_ENABLED
            ? Pattern.compile(
            Objects.requireNonNull(
                    ScheduledExecutorConfig.getCafWmpPartitionIdPattern(),
                    "CAF_WMP_PARTITION_ID_PATTERN must be set if CAF_WMP_ENABLED is true"))
            : null;

    private static final Pattern CAF_WMP_TARGET_QUEUE_NAMES_PATTERN
            = CAF_WMP_ENABLED
            ? Pattern.compile(
            Objects.requireNonNull(
                    ScheduledExecutorConfig.getCafWmpTargetQueueNamesPattern(),
                    "CAF_WMP_TARGET_QUEUE_NAMES_PATTERN must be set if CAF_WMP_ENABLED is true"))
            : null;

    /**
     * Create a new QueueServices object.
     *
     * @param   targetQueue                     the target queue
     * @param   partitionId                     the partition ID
     * @param   codec                           the serialization codec
     * @return  ScheduledExecutorQueueServices  new ScheduledExecutorQueueServices object
     * @throws  IOException                     thrown if the connection cannot be created
     * @throws  TimeoutException                thrown if the connection cannot be created
     */
    public static QueueServices create(final String targetQueue, final String partitionId, final Codec codec)
            throws IOException, TimeoutException {
        //  Create connection and channel for publishing messages.
        LOG.debug("Creating connection ...");
        final Connection connection = createConnection();

        LOG.debug("Creating channel ...");
        final Channel publishChannel = connection.createChannel();

        // Enable publisher confirms on the channel
        publishChannel.confirmSelect();

        // Register a listener in order to be notified of failed deliveries when basicPublish is called with the "mandatory" flag set
        publishChannel.addReturnListener(new ExceptionThrowingReturnListener());

        // Get the staging queue name if the message should be rerouted to a staging queue
        final String stagingQueueOrTargetQueue;

        if (CAF_WMP_ENABLED && CAF_WMP_TARGET_QUEUE_NAMES_PATTERN.matcher(targetQueue).matches()) {

            final Matcher matcher = CAF_WMP_PARTITION_ID_PATTERN.matcher(partitionId);

            final String tenantId = matcher.matches() ? matcher.group("tenantId") : partitionId;

            MessageRouterSingleton.init();

            stagingQueueOrTargetQueue = MessageRouterSingleton.route(targetQueue, tenantId);

            LOG.debug("MessageRouterSingleton.route({}, {}) returned the following queue name: {}. " +
                            "Messages will be routed to this queue.",
                    targetQueue,
                    tenantId,
                    stagingQueueOrTargetQueue);
        } else {
            stagingQueueOrTargetQueue = targetQueue;
        }

        //  Declare worker queue.
        LOG.debug("Declaring worker queue {}...", stagingQueueOrTargetQueue);
        publishChannel.queueDeclarePassive(stagingQueueOrTargetQueue);

        final Optional<String> optionalStagingQueue = stagingQueueOrTargetQueue.equals(targetQueue)
                ? Optional.empty()
                : Optional.of(stagingQueueOrTargetQueue);

        return new QueueServices(connection, publishChannel, optionalStagingQueue, stagingQueueOrTargetQueue, codec);
    }

    /**
     * Creates a connection to rabbit messaging server.
     */
    private static Connection createConnection()
            throws IOException, TimeoutException
    {
        return RabbitUtil.createRabbitConnection(
                ScheduledExecutorConfig.getRabbitMQHost(),
                ScheduledExecutorConfig.getRabbitMQPort(),
                ScheduledExecutorConfig.getRabbitMQUsername(),
                ScheduledExecutorConfig.getRabbitMQPassword(),
                new RethrowingReturnListenerExceptionHandler());
    }

}
