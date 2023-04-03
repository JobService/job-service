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
package com.hpe.caf.services.admin;

import com.google.gson.Gson;
import org.junit.Assert;
import org.junit.Test;

import org.junit.Rule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

/**
 * JUnit test to verify health check negative testing. Positive test is held within container module's JobServiceIT.
 */
public class HealthCheckTest {

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Test
    public void testHealthCheckWithUnavailableDatabaseAndRabbitMQ() throws Exception
    {
        // Setup dud DB environment variables
        environmentVariables.set("JOB_SERVICE_DATABASE_HOST", "UNKNOWNHOST1234567890");
        environmentVariables.set("JOB_SERVICE_DATABASE_PORT", "9999");
        environmentVariables.set("CAF_WEBSERVICE_URL", "http://jobservice:8080/job-service/v1");
        environmentVariables.set("JOB_SERVICE_DATABASE_NAME", "jobservicedb");
        environmentVariables.set("JOB_SERVICE_DATABASE_APPNAME", "unknownapplicationname");
        environmentVariables.set("JOB_SERVICE_DATABASE_USERNAME", "unknownuser");
        environmentVariables.set("JOB_SERVICE_DATABASE_PASSWORD", "unknownpass");
        // Setup dud RabbitMQ environment variables
        environmentVariables.set("CAF_RABBITMQ_HOST", "unknown-rabbitmq-host");
        environmentVariables.set("CAF_RABBITMQ_PORT", "9999");
        environmentVariables.set("CAF_RABBITMQ_USERNAME", "unknownuser");
        environmentVariables.set("CAF_RABBITMQ_PASSWORD", "unknownpass");

        final HealthCheck healthCheck = new HealthCheck();

        final HttpServletResponseForTesting httpServletResponse = new HttpServletResponseForTesting();

        healthCheck.doGet(null, httpServletResponse);

        final HealthCheckResponse healthCheckResponse =
            new Gson().fromJson(new String(httpServletResponse.getContent()), HealthCheckResponse.class);

        Assert.assertFalse("database.healthy should be false in health check response",
                           healthCheckResponse.database.healthy);
        Assert.assertEquals("database.message has unexpected value in health check response",
                            "org.postgresql.util.PSQLException: The connection attempt failed.", healthCheckResponse.database.message);
        Assert.assertFalse("queue.healthy should be false in health check response",
                           healthCheckResponse.queue.healthy);

        // The content of queue.message in the health check response will be different depending on what Java version and/or env is being
        // used:
        //
        // AdoptOpenJDK jdk-8.0.222.10-hotspot on Windows: java.net.UnknownHostException: unknown-rabbitmq-host
        // AdoptOpenJDK jdk-11.0.7.10-hotspot on Windows:  java.net.UnknownHostException: No such host is known (unknown-rabbitmq-host)
        // IcedTea 1.8.0_252 on Linux:                     java.net.UnknownHostException: unknown-rabbitmq-host: Name or service not known
        //
        // so we're just asserting that it contains the host here, rather than an exact match.
        Assert.assertTrue("queue.message should contain the failed host: unknown-rabbitmq-host",
                          healthCheckResponse.queue.message.contains("unknown-rabbitmq-host"));

        Assert.assertEquals("Status code set should be 503", 503, httpServletResponse.getStatus());
    }

    private final class HealthCheckResponse
    {
        private final HealthCheckItem database;
        private final HealthCheckItem queue;

        private HealthCheckResponse(final HealthCheckItem database, final HealthCheckItem queue)
        {
            this.database = database;
            this.queue = queue;
        }

        private final class HealthCheckItem
        {
            private final boolean healthy;
            private final String message;

            private HealthCheckItem(final boolean healthy, final String message)
            {
                this.healthy = healthy;
                this.message = message;
            }
        }
    }
}
