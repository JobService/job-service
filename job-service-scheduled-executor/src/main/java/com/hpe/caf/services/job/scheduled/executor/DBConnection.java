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
package com.hpe.caf.services.job.scheduled.executor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBConnection
{
    private static final Logger LOG = LoggerFactory.getLogger(DBConnection.class);
    
    private static final String JDBC_POSTGRESQL_PREFIX = "jdbc:postgresql:";
    private static final String JDBC_DRIVER = "org.postgresql.Driver";
    
    public static Connection get() throws ScheduledExecutorException
    {
        final String databaseUrl = ScheduledExecutorConfig.getDatabaseURL();
        final String dbUser = ScheduledExecutorConfig.getDatabaseUsername();
        final String dbPass = ScheduledExecutorConfig.getDatabasePassword();
        final String appName =
                ScheduledExecutorConfig.getApplicationName() != null ? ScheduledExecutorConfig
                        .getApplicationName()
                        : "Job Service Scheduled Executor";
        
        // Only JDBC/PostgreSQL connections are supported.
        if(!databaseUrl.startsWith(JDBC_POSTGRESQL_PREFIX))
        {
            throw new ScheduledExecutorException(
                    "Invalid database url string format - must start with jdbc:postgresql:");
        }
        
        try
        {
            LOG.debug("Registering JDBC driver \"{}\" ...", JDBC_DRIVER);
            Class.forName(JDBC_DRIVER);
        }
        catch(final Exception e)
        {
            final String errorMessage = MessageFormat
                    .format("Failed to register JDBC driver \"{0}\". {1}.", JDBC_DRIVER,
                            e.getMessage());
            LOG.error(errorMessage);
            throw new ScheduledExecutorException(errorMessage, e);
        }
        
        final Connection conn;
        try
        {
            final Properties myProp = new Properties();
            myProp.put("user", dbUser);
            myProp.put("password", dbPass);
            myProp.put("ApplicationName", appName);
            LOG.debug(MessageFormat
                    .format("Connecting to database {0} with username {1} and password {2} ...", databaseUrl,
                            dbUser, dbPass));
            conn = DriverManager.getConnection(databaseUrl, myProp);
            LOG.debug("Connected to database.");
        }
        catch(final SQLException se)
        {
            final String errorMessage = MessageFormat
                    .format("Failed to connect to database {0} with username {1} and password {2}.",
                            databaseUrl, dbUser, dbPass);
            /*
            SCMOD-6525 - FALSE POSITIVE on FORTIFY SCAN for Log forging. The values of databaseUrl, dbUser, dbPass are all set using
            properties or env variables.
            */
            LOG.error(errorMessage);
            throw new ScheduledExecutorException(errorMessage);
        }
        
        return conn;
    }
}
