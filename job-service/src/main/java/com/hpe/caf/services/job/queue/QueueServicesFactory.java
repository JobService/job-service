/*
 * Copyright 2015-2017 Hewlett Packard Enterprise Development LP.
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

    /**
     * Create a new QueueServices object.
     *
     * @param   configuration       the app configuration
     * @param   targetQueue         the target queue
     * @param   codec               the serialization codec
     * @return  QueueServices       new QueueServices object
     * @throws  IOException         thrown if the connection cannot be created
     * @throws  TimeoutException    thrown if the connection cannot be created
     */
    public static QueueServices create(final AppConfig configuration, final String targetQueue, final Codec codec) throws IOException, TimeoutException {
        //  Create connection and channel for publishing messages.
        Connection connection = createConnection(configuration);
        Channel publishChannel = connection.createChannel();

        //  Declare target worker queue.
        RabbitUtil.declareWorkerQueue(publishChannel, targetQueue);

        return new QueueServices(connection, publishChannel, targetQueue, codec);
    }

    /**
     * Creates a connection to rabbit messaging server.
     */
    private static Connection createConnection(AppConfig configuration)
            throws IOException, TimeoutException
    {
        Connection connection = RabbitUtil.createRabbitConnection(configuration.getRabbitMQHost(), configuration.getRabbitMQPort(), configuration.getRabbitMQUsername(), configuration.getRabbitMQPassword());
        return connection;
    }
}
