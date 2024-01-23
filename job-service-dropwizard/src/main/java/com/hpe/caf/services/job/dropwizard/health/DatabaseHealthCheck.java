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
import com.hpe.caf.services.configuration.AppConfigProvider;
import com.hpe.caf.services.db.client.DatabaseConnectionProvider;
import java.sql.Connection;
import java.sql.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DatabaseHealthCheck extends HealthCheck
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseHealthCheck.class);

    @Override
    protected Result check() throws Exception
    {
        LOGGER.debug("Database Health Check: Starting...");
        try (final Connection conn = DatabaseConnectionProvider.getConnection(
                AppConfigProvider.getAppConfigProperties())) {

            LOGGER.debug("Database Health Check: Attempting to Contact Database");
            final Statement stmt = conn.createStatement();
            stmt.execute("SELECT 1");

            LOGGER.debug("Database Health Check: Healthy");
            return Result.healthy();
        } catch (final Exception e) {
            LOGGER.error("Database Health Check: Unhealthy : " + e.toString());
            return Result.unhealthy(e);
        }
    }
}
