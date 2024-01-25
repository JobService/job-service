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
import com.hpe.caf.services.job.client.ApiClient;
import com.hpe.caf.services.job.client.api.JobsApi;
import java.text.SimpleDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PingHealthCheck extends HealthCheck
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PingHealthCheck.class);

    @Override
    protected Result check() throws Exception
    {
        LOGGER.debug("Ping Health Check: Starting...");

        final String connectionString;
        final String pingUrl = System.getenv("JOB_SERVICE_INTERNAL_PING_URL");
        if (pingUrl == null) {
            connectionString = "http://localhost:8080/job-service/v1";
        } else {
            connectionString = pingUrl;
        }
        final ApiClient client = new ApiClient();
        client.setBasePath(connectionString);
        final SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        client.setDateFormat(f);
        final JobsApi jobsApi = new JobsApi(client);
        try {
            LOGGER.debug("Ping Health Check: Attempting to Ping Web Service");
            jobsApi.ping();
            LOGGER.debug("Ping Health Check: Healthy");
            return Result.healthy();
        } catch (final Exception e) {
            LOGGER.error("Ping Health Check: Unhealthy : " + e.toString());
            return Result.unhealthy(e);
        }
    }
}
