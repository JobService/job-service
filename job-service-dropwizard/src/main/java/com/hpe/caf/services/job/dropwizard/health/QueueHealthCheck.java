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
package com.hpe.caf.services.job.dropwizard.health;

import com.codahale.metrics.health.HealthCheck;
import com.hpe.caf.util.rabbitmq.RabbitUtil;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class QueueHealthCheck extends HealthCheck
{
    private static final Logger LOGGER = LoggerFactory.getLogger(QueueHealthCheck.class);

    @Override
    protected Result check() throws Exception
    {
        LOGGER.debug("RabbitMQ Health Check: Starting...");

        final Connection conn;
        final Channel channel;

        // Attempt to create a connection and channel to RabbitMQ. If an error occurs update the statueResponseMap and
        // return.
        try {
            conn = createConnection();
            channel = conn.createChannel();
        } catch (final IOException | TimeoutException e) {
            LOGGER.error("RabbitMQ Health Check: Unhealthy", e);
            return Result.unhealthy(e);
        }

        // Attempt to create a open a connection and channel to RabbitMQ. If an error occurs update the
        // statueResponseMap and return.
        try {
            if (!conn.isOpen()) {
                LOGGER.error("RabbitMQ Health Check: Unhealthy, unable to open connection");
                return Result.unhealthy("Attempt to open connection to RabbitMQ failed");
            } else if (!channel.isOpen()) {
                LOGGER.error("RabbitMQ Health Check: Unhealthy, unable to open channel");
                return Result.unhealthy("Attempt to open channel to RabbitMQ failed");
            }
        } catch (final Exception e) {
            LOGGER.error("RabbitMQ Health Check: Unhealthy", e);
            return Result.unhealthy(e);
        } finally {
            conn.close();
        }

        // There where no issues in attempting to create and open a connection and channel to RabbitMQ.
        return Result.healthy();
    }

    private static Connection createConnection() throws IOException, TimeoutException
    {
        return RabbitUtil.createRabbitConnection(
                System.getenv("CAF_RABBITMQ_HOST"),
                Integer.parseInt(System.getenv("CAF_RABBITMQ_PORT")),
                System.getenv("CAF_RABBITMQ_USERNAME"),
                System.getenv("CAF_RABBITMQ_PASSWORD"));
    }
}
