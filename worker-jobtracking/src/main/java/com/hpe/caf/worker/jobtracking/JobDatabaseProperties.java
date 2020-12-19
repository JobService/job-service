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
package com.hpe.caf.worker.jobtracking;


public class JobDatabaseProperties {

    /**
     * Gets the database URL from the environment variable.
     *
     * @return database url
     */
    public static String getDatabaseUrl() {
        return getPropertyOrEnvVar("JOB_SERVICE_DATABASE_URL") != null ? getPropertyOrEnvVar("JOB_SERVICE_DATABASE_URL")
            : getPropertyOrEnvVar("JOB_DATABASE_URL");
    }

    /**
     * Gets the database username from the environment variable.
     *
     * @return database username
     */
    public static String getDatabaseUsername() {
        return getPropertyOrEnvVar("JOB_SERVICE_DATABASE_USERNAME") != null ? getPropertyOrEnvVar("JOB_SERVICE_DATABASE_USERNAME")
            : getPropertyOrEnvVar("JOB_DATABASE_USERNAME");
    }

    /**
     * Gets the database password from the environment variable.
     *
     * @return database password
     */
    public static String getDatabasePassword() {
        return getPropertyOrEnvVar("JOB_SERVICE_DATABASE_PASSWORD") != null ? getPropertyOrEnvVar("JOB_SERVICE_DATABASE_PASSWORD")
            : getPropertyOrEnvVar("JOB_DATABASE_PASSWORD");
    }
    
    /**
     * Gets the application name from the environment variable.
     *
     * @return application name
     */
    public static String getApplicationName() {
        return getPropertyOrEnvVar("JOB_SERVICE_DATABASE_APPNAME") != null ? getPropertyOrEnvVar("JOB_SERVICE_DATABASE_APPNAME")
            : getPropertyOrEnvVar("JOB_DATABASE_APPNAME");
    }
    
    public static boolean getShouldPropagateFailures() {
        final String propFailures = getPropertyOrEnvVar("CAF_JOB_TRACKING_PROPAGATE_FAILURES");
        return Boolean.parseBoolean(propFailures);
    }
    
    private static String getPropertyOrEnvVar(final String key)
    {
        final String propertyValue = System.getProperty(key);
        return (propertyValue != null) ? propertyValue : System.getenv(key);
    }
}
