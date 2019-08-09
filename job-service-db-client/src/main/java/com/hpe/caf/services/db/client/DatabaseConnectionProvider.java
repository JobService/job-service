/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
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
import java.sql.DriverManager;
import java.util.Locale;
import java.util.Properties;

/**
 * The DatabaseHelper class is responsible for database operations.
 */
public final class DatabaseConnectionProvider
{
    private static final String JDBC_POSTGRESQL_PREFIX = "jdbc:postgresql:";
    private static final String JDBC_DRIVER = "org.postgresql.Driver";
    private static final String ERR_MSG_DB_URL_FORMAT_INVALID
            = "Invalid database url string format - must start with jdbc:postgresql:";

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
        final String dbURL = appConfig.getDatabaseURL().toLowerCase(Locale.ENGLISH);
        if ( !dbURL.startsWith(JDBC_POSTGRESQL_PREFIX) )
        {
            throw new Exception(ERR_MSG_DB_URL_FORMAT_INVALID);
        }

        try{
            // Register JDBC driver.
            LOG.debug("Registering JDBC driver...");
            Class.forName(JDBC_DRIVER);

            // Open a connection.
            Properties myProp = new Properties();
            myProp.put("user", appConfig.getDatabaseUsername());
            myProp.put("password", appConfig.getDatabasePassword());

            LOG.debug("Connecting to database...");
            conn = DriverManager.getConnection(dbURL, myProp);
        } catch (final Exception ex) {
            LOG.error("Cannot get connection");
            throw ex;
        }

        return conn;
    }
}

