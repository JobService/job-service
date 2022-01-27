/*
 * Copyright 2016-2021 Micro Focus or one of its affiliates.
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
package com.hpe.caf.services.db.client;

import com.hpe.caf.services.configuration.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import org.postgresql.ds.PGSimpleDataSource;

/**
 * The DatabaseHelper class is responsible for database operations.
 */
public final class DatabaseConnectionProvider
{
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseConnectionProvider.class);

    private DatabaseConnectionProvider()
    {
    }

    /**
     * Creates a connection to the PostgreSQL database.
     */
    public static Connection getConnection(final AppConfig appConfig) throws Exception
    {
        final Connection conn;

        // Only JDBC/PostgreSQL connections supported.
        final String appname = appConfig.getApplicationName() != null ? appConfig.getApplicationName() : "Job Service";

        try{
            // Open a connection.
            final PGSimpleDataSource dbSource = new PGSimpleDataSource();
            dbSource.setServerNames(new String[]{appConfig.getDatabaseHost()});
            dbSource.setPortNumbers(new int[]{appConfig.getDatabasePort()});
            dbSource.setDatabaseName(appConfig.getDatabaseName());
            dbSource.setUser(appConfig.getDatabaseUsername());
            dbSource.setPassword(appConfig.getDatabasePassword());
            dbSource.setApplicationName(appname);

            LOG.debug("Connecting to database...");
            conn = dbSource.getConnection();
        } catch (final Exception ex) {
            LOG.error("Cannot get connection");
            throw ex;
        }

        return conn;
    }
}

