package com.hpe.caf.services.job.queue;

import com.hpe.caf.api.Codec;
import com.hpe.caf.services.job.configuration.AppConfig;
import com.hpe.caf.util.rabbitmq.RabbitUtil;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * This class is responsible for creating the RabbitMQ connection and channel.
 */
public final class QueueServicesFactory {

    public static QueueServices create(final AppConfig configuration, final String targetQueue, final Codec codec) throws IOException, TimeoutException {
        //  Create connection and channel for publishing messages.
        Connection connection = createConnection(configuration);
        Channel publishChannel = connection.createChannel();

        //  Declare target worker queue.
        RabbitUtil.declareWorkerQueue(publishChannel, targetQueue);

        return new QueueServices(connection, publishChannel, targetQueue, codec);
    }

    private static Connection createConnection(AppConfig configuration)
            throws IOException, TimeoutException
    {
        Connection connection = RabbitUtil.createRabbitConnection(configuration.getRabbitMQHost(), configuration.getRabbitMQPort(), configuration.getRabbitMQUsername(), configuration.getRabbitMQPassword());
        return connection;
    }
}
