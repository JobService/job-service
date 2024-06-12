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

import java.util.Arrays;
import java.util.List;

import org.eclipse.jetty.server.ServerConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.health.HealthCheck;

import io.dropwizard.core.setup.Environment;

public final class PortsHealthCheck extends HealthCheck
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PortsHealthCheck.class);

    private final Environment environment;
    private final Type type;

    public enum Type
    {
        ALIVE,
        READY;
    }

    public PortsHealthCheck(final Environment environment, final Type type)
    {
        this.environment = environment;
        this.type = type;
    }

    @Override
    protected Result check()
    {
        final List<ServerConnector> serverConnectors = Arrays
                .stream(environment.getApplicationContext().getServer().getConnectors())
                .filter(connector -> connector instanceof ServerConnector)
                .map(connector -> (ServerConnector)connector)
                .toList();

        if (serverConnectors.isEmpty()) {
            final String message = "No ServerConnectors found";
            LOGGER.error(message);
            return Result.unhealthy(message);
        }

        for (final ServerConnector serverConnector : serverConnectors) {
            final String serverConnectorName = serverConnector.getName();
            final int serverConnectorPort = serverConnector.getPort();

            switch (type) {
                case ALIVE:
                    if (!serverConnector.isStarted()) {
                        final String message = getMessage(serverConnectorName, serverConnectorPort, "not started");
                        LOGGER.error(message);
                        return Result.unhealthy(message);
                    }

                    if (!serverConnector.isOpen()) {
                        final String message = getMessage(serverConnectorName, serverConnectorPort, "not open");
                        LOGGER.error(message);
                        return Result.unhealthy(message);
                    }

                    LOGGER.error(getMessage(serverConnectorName, serverConnectorPort, "started and open")); // TODO debug

                    break;
                case READY:
                    if (!serverConnector.isAccepting()) {
                        final String message = getMessage(serverConnectorName, serverConnectorPort, "not accepting connections");
                        LOGGER.error(message);
                        return Result.unhealthy(message);
                    }

                    LOGGER.error(getMessage(serverConnectorName, serverConnectorPort, "accepting connections")); // TODO debug

                    break;
                default:
                    throw new IllegalArgumentException("Unknown health check type: " + type);
            }

        }

        return Result.healthy();
    }

    private static String getMessage(
            final String serverConnectorName,
            final int serverConnectorPort,
            final String serverConnectorStatus)
    {
        return String.format("%s connector on port %s is: %s",
                serverConnectorName, serverConnectorPort, serverConnectorStatus);
    }
}
