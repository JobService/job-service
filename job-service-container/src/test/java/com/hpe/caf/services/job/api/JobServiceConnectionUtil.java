/*
 * Copyright 2016-2023 Open Text.
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
package com.hpe.caf.services.job.api;

import java.sql.SQLException;
import java.util.Objects;

import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JobServiceConnectionUtil
{
    private JobServiceConnectionUtil()
    {
    }
    
    private static final Logger LOG = LoggerFactory.getLogger(JobServiceConnectionUtil.class);
    public static java.sql.Connection getDbConnection() throws SQLException
    {
        final String dbHost = Objects.requireNonNull(getPropertyOrEnvVar("JOB_SERVICE_DATABASE_HOST"));
        final String dbPortString = Objects.requireNonNull(getPropertyOrEnvVar("JOB_SERVICE_DATABASE_PORT"));
        final String dbName = Objects.requireNonNull(getPropertyOrEnvVar("JOB_SERVICE_DATABASE_NAME"));
        final String dbUser = Objects.requireNonNull(getPropertyOrEnvVar("JOB_SERVICE_DATABASE_USERNAME"));
        final String dbPass = Objects.requireNonNull(getPropertyOrEnvVar("JOB_SERVICE_DATABASE_PASSWORD"));
        final String appName = getPropertyOrEnvVar("JOB_SERVICE_DATABASE_APPNAME") != null ? getPropertyOrEnvVar(
                "JOB_SERVICE_DATABASE_APPNAME") : "Job Service IT";
        try {
            final int dbPort = Integer.parseInt(dbPortString);
            final java.sql.Connection conn;
            final PGSimpleDataSource dbSource = new PGSimpleDataSource();
            dbSource.setServerNames(new String[]{dbHost});
            dbSource.setPortNumbers(new int[]{dbPort});
            dbSource.setDatabaseName(dbName);
            dbSource.setUser(dbUser);
            dbSource.setPassword(dbPass);
            dbSource.setApplicationName(appName);
            LOG.info("Connecting to database {} with host {}, port {}, username {} and password {}", dbName, dbHost, dbPort,
                    dbUser, dbPass);
            conn = dbSource.getConnection();
            LOG.info("Connected to database");
            return conn;
        } catch (final Exception e) {
            LOG.error("ERROR connecting to database {} with host {}, port {}, username {} and password {}", dbName, dbHost, dbPortString,
                    dbUser, dbPass);
            throw e;
        }
    }
    
    private static String getPropertyOrEnvVar(final String key)
    {
        final String propertyValue = System.getProperty(key);
        return (propertyValue != null) ? propertyValue : System.getenv(key);
    }
}
