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

import static java.text.MessageFormat.*;

import java.sql.Connection;
import java.sql.SQLException;

import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DBConnection
{
    private DBConnection()
    {
    }
    
    private static final Logger LOG = LoggerFactory.getLogger(DBConnection.class);
    
    public static Connection get() throws ScheduledExecutorException
    {
        final String dbHost = ScheduledExecutorConfig.getDatabaseHost();
        final int dbPort = ScheduledExecutorConfig.getDatabasePort();
        final String dbName = ScheduledExecutorConfig.getDatabaseName();
        final String dbUser = ScheduledExecutorConfig.getDatabaseUsername();
        final String dbPass = ScheduledExecutorConfig.getDatabasePassword();
        final String appName =
                ScheduledExecutorConfig.getApplicationName() != null ? ScheduledExecutorConfig
                        .getApplicationName()
                        : "Job Service Scheduled Executor";
        
        final Connection conn;
        try
        {
            final PGSimpleDataSource dbSource = new PGSimpleDataSource();
            dbSource.setServerNames(new String[]{dbHost});
            dbSource.setPortNumbers(new int[]{dbPort});
            dbSource.setDatabaseName(dbName);
            dbSource.setUser(dbUser);
            dbSource.setPassword(dbPass);
            dbSource.setApplicationName(appName);
            LOG.debug("Connecting to database {} with host {}, port {}, username {} and password {} ...",
                            dbName, dbHost, dbPort, dbUser, dbPass);
            conn = dbSource.getConnection();
            LOG.debug("Connected to database.");
        }
        catch(final SQLException se)
        {
            final String errorMessage = format("Failed to connect to database {} with host {}, port {}, username {} and password {} ...",
                    dbName, dbHost, dbPort, dbUser, dbPass);
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
