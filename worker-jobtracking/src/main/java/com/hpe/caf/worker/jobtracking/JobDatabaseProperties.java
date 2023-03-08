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
package com.hpe.caf.worker.jobtracking;


import java.util.Objects;

public class JobDatabaseProperties {

    /**
     * Gets the database Host from the environment variable.
     *
     * @return database host
     */
    public static String getDatabaseHost() {
        return Objects.requireNonNull(getPropertyOrEnvVar("JOB_SERVICE_DATABASE_HOST"));
    }

    /**
     * Gets the database port from the environment variable.
     *
     * @return database port
     */
    public static String getDatabasePort() {
        return Objects.requireNonNull(getPropertyOrEnvVar("JOB_SERVICE_DATABASE_PORT"));
    }

    /**
     * Gets the database name from the environment variable.
     *
     * @return database name
     */
    public static String getDatabaseName() {
        return Objects.requireNonNull(getPropertyOrEnvVar("JOB_SERVICE_DATABASE_NAME"));
    }

    /**
     * Gets the database username from the environment variable.
     *
     * @return database username
     */
    public static String getDatabaseUsername() {
        return Objects.requireNonNull(getPropertyOrEnvVar("JOB_SERVICE_DATABASE_USERNAME"));
    }

    /**
     * Gets the database password from the environment variable.
     *
     * @return database password
     */
    public static String getDatabasePassword() {
        return Objects.requireNonNull(getPropertyOrEnvVar("JOB_SERVICE_DATABASE_PASSWORD"));
    }
    
    /**
     * Gets the application name from the environment variable.
     *
     * @return application name
     */
    public static String getApplicationName() {
        return Objects.requireNonNull(getPropertyOrEnvVar("JOB_SERVICE_DATABASE_APPNAME"));
    }
    
    private static String getPropertyOrEnvVar(final String key)
    {
        final String propertyValue = System.getProperty(key);
        return (propertyValue != null) ? propertyValue : System.getenv(key);
    }
}
