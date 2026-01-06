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
package com.github.jobservice.dropwizard.health;

import com.codahale.metrics.health.HealthCheck;
import com.github.cafapi.common.util.secret.SecretUtil;
import com.github.workerframework.configs.RabbitConfiguration;
import com.github.workerframework.util.rabbitmq.RabbitUtil;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class QueueHealthCheck extends HealthCheck
{
    private static final Logger LOGGER = LoggerFactory.getLogger(QueueHealthCheck.class);

    private final String rabbitPassword;

    public QueueHealthCheck()
    {
        try {
            this.rabbitPassword = SecretUtil.getSecret("CAF_RABBITMQ_PASSWORD");
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

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

    private Connection createConnection()
            throws IOException, TimeoutException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException
    {
        final RabbitConfiguration rabbitConfiguration = new RabbitConfiguration();
        rabbitConfiguration.setRabbitProtocol(getRabbitProtocol());
        rabbitConfiguration.setRabbitTlsProtocolVersion(getRabbitTlsProtocolVersion());
        rabbitConfiguration.setRabbitHost(System.getenv("CAF_RABBITMQ_HOST"));
        rabbitConfiguration.setRabbitPort(Integer.parseInt(System.getenv("CAF_RABBITMQ_PORT")));
        rabbitConfiguration.setRabbitUser(System.getenv("CAF_RABBITMQ_USERNAME"));
        rabbitConfiguration.setRabbitPassword(rabbitPassword);
        rabbitConfiguration.setMaxBackoffInterval(30);
        rabbitConfiguration.setBackoffInterval(1);
        rabbitConfiguration.setMaxAttempts(20);

        return RabbitUtil.createRabbitConnection(rabbitConfiguration);
    }

    private static String getRabbitProtocol()
    {
        // Default to 'amqp' if CAF_RABBITMQ_PROTOCOL is not specified
        final String rabbitProtocol = System.getenv("CAF_RABBITMQ_PROTOCOL");
        if (null == rabbitProtocol || rabbitProtocol.isEmpty()) {
            return "amqp";
        }
        return rabbitProtocol;
    }

    private static String getRabbitTlsProtocolVersion()
    {
        final String rabbitTlsProtocolVersion = System.getenv("CAF_RABBITMQ_TLS_PROTOCOL_VERSION");
        if (null == rabbitTlsProtocolVersion || rabbitTlsProtocolVersion.isEmpty()) {
            return "TLSv1.2";
        }
        return rabbitTlsProtocolVersion;
    }
}
