/*
 * Copyright 2016-2020 Micro Focus or one of its affiliates.
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

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * JUnit test to verify health check negative testing. Positive test is held within container module's JobServiceIT.
 */
public class HealthCheckTest {

    /**
     * Class that enables overriding of environment variables without effecting the environment variables set on the host
     */
    static class TestEnvironmentVariablesOverrider
    {
        @SuppressWarnings("unchecked")
        public static void configureEnvironmentVariable(String name, String value) throws Exception
        {
            Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
            Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
            theEnvironmentField.setAccessible(true);
            Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
            env.put(name, value);
            Field theCaseInsensitiveEnvironmentField = processEnvironmentClass
                    .getDeclaredField("theCaseInsensitiveEnvironment");
            theCaseInsensitiveEnvironmentField.setAccessible(true);
            Map<String, String> cienv = (Map<String, String>) theCaseInsensitiveEnvironmentField.get(null);
            cienv.put(name, value);
        }
    }

    @Test
    public void testHealthCheckWithUnavailableDatabaseAndRabbitMQ() throws Exception
    {
        // Setup dud DB environment variables
        TestEnvironmentVariablesOverrider.configureEnvironmentVariable("JOB_SERVICE_DATABASE_URL",
                "jdbc:postgresql://UNKNOWNHOST1234567890:9999/jobservicedb");
        TestEnvironmentVariablesOverrider.configureEnvironmentVariable("JOB_SERVICE_DATABASE_APPNAME", "unknownapplicationname");
        TestEnvironmentVariablesOverrider.configureEnvironmentVariable("JOB_SERVICE_DATABASE_USERNAME", "unknownuser");
        TestEnvironmentVariablesOverrider.configureEnvironmentVariable("JOB_SERVICE_DATABASE_PASSWORD", "unknownpass");
        // Setup dud RabbitMQ environment variables
        TestEnvironmentVariablesOverrider.configureEnvironmentVariable("CAF_RABBITMQ_HOST", "unknown-rabbitmq-host");
        TestEnvironmentVariablesOverrider.configureEnvironmentVariable("CAF_RABBITMQ_PORT", "9999");
        TestEnvironmentVariablesOverrider.configureEnvironmentVariable("CAF_RABBITMQ_USERNAME", "unknownuser");
        TestEnvironmentVariablesOverrider.configureEnvironmentVariable("CAF_RABBITMQ_PASSWORD", "unknownpass");

        final HealthCheck healthCheck = new HealthCheck();

        final HttpServletResponseForTesting httpServletResponse = new HttpServletResponseForTesting();

        healthCheck.doGet(null, httpServletResponse);

        final String expectedResponse = "{\"database\":{\"healthy\":\"false\"," +
                "\"message\":\"org.postgresql.util.PSQLException: The connection attempt failed.\"}," +
                "\"queue\":{\"healthy\":\"false\"," +
                "\"message\":\"java.net.UnknownHostException: unknown-rabbitmq-host\"}}";

        Assert.assertEquals("Expected Health Check response should match actual response", expectedResponse,
                new String(httpServletResponse.getContent()));

        Assert.assertEquals("Status code set should be 500", 500, httpServletResponse.getStatus());
    }
}
