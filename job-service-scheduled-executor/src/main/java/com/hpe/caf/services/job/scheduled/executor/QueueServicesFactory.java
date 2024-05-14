/*
 * Copyright 2016-2024 Open Text.
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

import com.hpe.caf.api.Codec;
import com.hpe.caf.util.rabbitmq.RabbitUtil;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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

    private static final String RABBIT_PROP_KEY_MAX_PRIORITY = "x-max-priority";

    private static final String RABBIT_PROP_QUEUE_TYPE = "x-queue-type";

    private static final String RABBIT_PROP_QUEUE_TYPE_CLASSIC = "classic";

    private static final Map<String, Object> queueArguments = new HashMap<>();

    static {
        final int queueMaxPriority = ScheduledExecutorConfig.getQueueMaxPriority();
        final String queueType = ScheduledExecutorConfig.getQueueType();
        if (queueMaxPriority > 0 && queueType.equals(RABBIT_PROP_QUEUE_TYPE_CLASSIC)) {
            queueArguments.put(RABBIT_PROP_KEY_MAX_PRIORITY, queueMaxPriority);
        }
        queueArguments.put(RABBIT_PROP_QUEUE_TYPE, queueType);
    }

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
            throws IOException, TimeoutException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException
    {
        //  Create connection and channel for publishing messages.
        LOG.debug("Creating connection ...");
        final Connection connection = createConnection();

        LOG.debug("Creating channel ...");
        final Channel publishChannel = connection.createChannel();

        // Get the staging queue name if the message should be rerouted to a staging queue
        final String stagingQueueOrTargetQueue;

        if (CAF_WMP_ENABLED && CAF_WMP_TARGET_QUEUE_NAMES_PATTERN.matcher(targetQueue).matches()) {

            final Matcher matcher = CAF_WMP_PARTITION_ID_PATTERN.matcher(partitionId);

            final String tenantId = matcher.matches() ? matcher.group("tenantId") : partitionId;

            stagingQueueOrTargetQueue = StagingQueueRerouter.route(targetQueue, tenantId);

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

        //setting queue properties: durable - true, exclusive - false, autoDelete - false
        publishChannel.queueDeclare(stagingQueueOrTargetQueue, true, false, false, queueArguments);

        // stagingQueueOrTargetQueue = Queue where message should be published to
        // targetQueue               = Queue that should be set in the 'to' field of the task message

        return new QueueServices(connection, publishChannel, stagingQueueOrTargetQueue, targetQueue, codec);
    }

    /**
     * Creates a connection to rabbit messaging server.
     */
    private static Connection createConnection()
            throws IOException, TimeoutException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException
    {
        return RabbitUtil.createRabbitConnection(ScheduledExecutorConfig.getRabbitMQProtocol(), ScheduledExecutorConfig.getRabbitMQHost(), ScheduledExecutorConfig.getRabbitMQPort(), ScheduledExecutorConfig.getRabbitMQUsername(), ScheduledExecutorConfig.getRabbitMQPassword());
    }
}
