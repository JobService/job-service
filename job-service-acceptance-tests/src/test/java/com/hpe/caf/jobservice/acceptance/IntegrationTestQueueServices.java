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
package com.hpe.caf.jobservice.acceptance;

import com.hpe.caf.worker.testing.SettingNames;
import com.hpe.caf.worker.testing.SettingsProvider;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

final class IntegrationTestQueueServices implements AutoCloseable
{
    private static final Logger LOGGER = LoggerFactory.getLogger(IntegrationTestQueueServices.class);
    private static final String RABBIT_PROP_QUEUE_TYPE = "x-queue-type";
    private static final String RABBIT_PROP_QUEUE_TYPE_QUORUM = "quorum";
    private static final String DOCKER_HOST_ADDRESS = SettingsProvider.defaultProvider.getSetting(SettingNames.dockerHostAddress);
    private static final String RABBITMQ_NODE_PORT = SettingsProvider.defaultProvider.getSetting(SettingNames.rabbitmqNodePort);
    private static final String RABBITMQ_CTRL_PORT = SettingsProvider.defaultProvider.getSetting(SettingNames.rabbitmqCtrlPort);
    private static final Map<String, List<String>> QUEUE_MESSAGES = new ConcurrentHashMap<>();
    public static final String RESUME_JOB_QUEUE_NAME = "worker-taskunstowing-in";
    
    private final Connection connection;
    private final HttpHost rabbitHost;
    private final HttpClientContext httpContext;
    private final Channel resumeJobQueueChannel;

    public IntegrationTestQueueServices() throws IOException, TimeoutException
    {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(DOCKER_HOST_ADDRESS);
        factory.setPort(Integer.parseInt(RABBITMQ_NODE_PORT));
        factory.setUsername("guest");
        factory.setPassword("guest");

        QUEUE_MESSAGES.put(RESUME_JOB_QUEUE_NAME, new ArrayList<>());

        LOGGER.info("Getting Rabbit MQ connection...");
        this.connection = factory.newConnection();

        LOGGER.info("Creating Rabbit MQ channels...");
        LOGGER.info("Creating Rabbit MQ resume job queue channel...");
        this.resumeJobQueueChannel = connection.createChannel();

        LOGGER.info("Declaring Rabbit MQ queues...");
        LOGGER.info("Declaring Rabbit MQ resume job queue...");
        final Map<String, Object> args = new HashMap<>();
        args.put(RABBIT_PROP_QUEUE_TYPE, RABBIT_PROP_QUEUE_TYPE_QUORUM);
        resumeJobQueueChannel.queueDeclare(RESUME_JOB_QUEUE_NAME, true, false, false, args);

        rabbitHost = new HttpHost(DOCKER_HOST_ADDRESS, Integer.parseInt(RABBITMQ_CTRL_PORT), "http");
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("guest", "guest"));
        AuthCache authCache = new BasicAuthCache();
        authCache.put(rabbitHost, new BasicScheme());
        // Add AuthCache to the execution httpContext
        httpContext = HttpClientContext.create();
        httpContext.setCredentialsProvider(credsProvider);
        httpContext.setAuthCache(authCache);
    }

    public void startListening() throws IOException
    {
        getMessage(resumeJobQueueChannel, RESUME_JOB_QUEUE_NAME);
    }

    private void getMessage(final Channel channel, final String queue) throws IOException
    {
        final String watchedQueue = "[" + queue + " Queue]";

        LOGGER.info("************** Watching queue : " + watchedQueue);

        final Consumer consumer = new DefaultConsumer(channel)
        {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                                       byte[] body) throws IOException
            {
                final String message = new String(body, "UTF-8");
                LOGGER.info("-------------------------" + watchedQueue + " Received '" + message + "' {}", envelope.getDeliveryTag());
                if (queue.equalsIgnoreCase(RESUME_JOB_QUEUE_NAME)) {
                    QUEUE_MESSAGES.get(RESUME_JOB_QUEUE_NAME).add(message);
                } else {
                    throw new RuntimeException("Received message: " + message + " for an unexpected queue: " + queue);
                }
                channel.basicAck(envelope.getDeliveryTag(), true);
            }
        };
        final boolean autoAck = false; // acknowledgment is covered below
        channel.basicConsume(queue, autoAck, consumer);
    }

    public void waitForMessages(final int msgCount, final int timeoutMs, final String queueName) throws InterruptedException
    {
        int sleepTime = 0;
        do {
            if (getMessageCount(queueName) >= msgCount) {
                return;
            }
            sleepTime += 1000;
            Thread.sleep(1000);
        } while (sleepTime < timeoutMs);
    }

    public int getMessageCount(final String queueName)
    {
        return QUEUE_MESSAGES.get(queueName).size();
    }

    public List<String> getMessages(final String queueName)
    {
        return QUEUE_MESSAGES.get(queueName);
    }

    @Override
    public void close() throws Exception
    {
        try {
            // Close channel.
            if (resumeJobQueueChannel != null) {
                resumeJobQueueChannel.close();
            }

            // Close connection.
            if (connection != null) {
                connection.close();
            }

        } catch (final IOException | TimeoutException e) {
            throw new Exception("Failed to close the queuing connection.");
        }
    }
}
