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
import com.github.workerframework.configs.RabbitConfiguration;
import com.github.workerframework.util.rabbitmq.RabbitUtil;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This class is responsible for creating the RabbitMQ connection and channels.
 * It maintains a single shared connection across all jobs for efficiency.
 */
public final class QueueServicesFactory
{
    private static final Logger LOG = LoggerFactory.getLogger(QueueServicesFactory.class);

    private static final String RABBIT_PROP_KEY_MAX_PRIORITY = "x-max-priority";
    private static final String RABBIT_PROP_QUEUE_TYPE = "x-queue-type";
    private static final String RABBIT_PROP_QUEUE_TYPE_CLASSIC = "classic";

    private static final Map<String, Object> QUEUE_ARGUMENTS = new HashMap<>();

    // Shared connection instance - thread-safe as RabbitMQ connections are thread-safe
    private static final AtomicReference<Connection> SHARED_CONNECTION = new AtomicReference<>();

    static {
        final int queueMaxPriority = ScheduledExecutorConfig.getQueueMaxPriority();
        final String queueType = ScheduledExecutorConfig.getQueueType();
        if (queueMaxPriority > 0 && queueType.equals(RABBIT_PROP_QUEUE_TYPE_CLASSIC)) {
            QUEUE_ARGUMENTS.put(RABBIT_PROP_KEY_MAX_PRIORITY, queueMaxPriority);
        }
        QUEUE_ARGUMENTS.put(RABBIT_PROP_QUEUE_TYPE, queueType);

        // Register shutdown hook to clean up connection on JVM shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            final Connection connection = SHARED_CONNECTION.get();
            if (connection != null && connection.isOpen()) {
                try {
                    LOG.info("Shutting down shared RabbitMQ connection...");
                    connection.close();
                } catch (final IOException e) {
                    LOG.warn("Error closing shared connection during shutdown", e);
                }
            }
        }));
    }

    /**
     * Create a new QueueServices object using the shared connection.
     *
     * @param   partitionId                     the partition ID
     * @param   jobId                           the job ID
     * @param   targetQueue                     the target queue
     * @param   codec                           the serialization codec
     * @return  QueueServices                   new QueueServices object
     * @throws  IOException                     thrown if the connection/channel cannot be created
     * @throws  TimeoutException                thrown if the connection cannot be created
     */
    public static QueueServices create(
            final String partitionId, final String jobId, final String targetQueue, final Codec codec)
            throws IOException, TimeoutException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException
    {
        // Get or create the shared connection
        final Connection connection = getOrCreateSharedConnection();

        // Create a new channel for this job (channels are not thread-safe, so each job needs its own)
        LOG.info("Creating channel for job [partitionId={}, jobId={}, queue={}]...",
                partitionId, jobId, targetQueue);
        final Channel publishChannel = connection.createChannel();
        if (publishChannel == null) {
            throw new IOException(
                    MessageFormat.format(
                            "Failed to create a new channel for job [partitionId={0}, jobId={1}, queue={2}]. " +
                            "Job will be retried.", partitionId, jobId, targetQueue));
        }

        publishChannel.confirmSelect();
        new PublisherConfirmationAnalyzer(publishChannel, partitionId, jobId, targetQueue);

        //  Declare worker queue.
        LOG.debug("Declaring worker queue {}...", targetQueue);
        //setting queue properties: durable - true, exclusive - false, autoDelete - false
        publishChannel.queueDeclare(targetQueue, true, false, false, QUEUE_ARGUMENTS);

        return new QueueServices(partitionId, jobId, publishChannel, targetQueue, codec);
    }

    /**
     * Gets the existing shared connection or creates a new one if needed.
     * This method is thread-safe and ensures only one connection is created.
     */
    private static Connection getOrCreateSharedConnection()
            throws IOException, TimeoutException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException {

        Connection connection = SHARED_CONNECTION.get();

        // Check if we need to create or recreate the connection
        if (connection == null || !connection.isOpen()) {
            synchronized (QueueServicesFactory.class) {
                // Double-check pattern to avoid race conditions
                connection = SHARED_CONNECTION.get();
                if (connection == null || !connection.isOpen()) {
                    if (connection != null && !connection.isOpen()) {
                        LOG.warn("Shared connection was closed, creating new connection...");
                    } else {
                        LOG.info("Creating shared RabbitMQ connection...");
                    }

                    connection = createConnection();
                    SHARED_CONNECTION.set(connection);

                    LOG.info("Shared RabbitMQ connection established successfully");
                }
            }
        }

        return connection;
    }

    /**
     * Creates a connection to rabbit messaging server.
     */
    private static Connection createConnection()
            throws IOException, TimeoutException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException
    {
        final RabbitConfiguration rabbitConfiguration = new RabbitConfiguration();
        rabbitConfiguration.setRabbitProtocol(ScheduledExecutorConfig.getRabbitMQProtocol());
        rabbitConfiguration.setRabbitTlsProtocolVersion(ScheduledExecutorConfig.getRabbitMQTlsProtocolVersion());
        rabbitConfiguration.setRabbitHost(ScheduledExecutorConfig.getRabbitMQHost());
        rabbitConfiguration.setRabbitPort(ScheduledExecutorConfig.getRabbitMQPort());
        rabbitConfiguration.setRabbitUser(ScheduledExecutorConfig.getRabbitMQUsername());
        rabbitConfiguration.setRabbitPassword(ScheduledExecutorConfig.getRabbitMQPassword());
        rabbitConfiguration.setMaxBackoffInterval(30);
        rabbitConfiguration.setBackoffInterval(1);
        rabbitConfiguration.setMaxAttempts(20);

        return RabbitUtil.createRabbitConnection(rabbitConfiguration);
    }

    /**
     * Returns whether the shared connection is currently open and available.
     */
    public static boolean isSharedConnectionOpen() {
        final Connection connection = SHARED_CONNECTION.get();
        return connection != null && connection.isOpen();
    }
}
