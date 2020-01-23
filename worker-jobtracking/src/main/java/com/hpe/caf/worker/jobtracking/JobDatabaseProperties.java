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
        return System.getenv("JOB_SERVICE_DATABASE_URL");
    }

    /**
     * Gets the database username from the environment variable.
     *
     * @return database username
     */
    public static String getDatabaseUsername() {
        return System.getenv("JOB_SERVICE_DATABASE_USERNAME");
    }

    /**
     * Gets the database password from the environment variable.
     *
     * @return database password
     */
    public static String getDatabasePassword() {
        return System.getenv("JOB_SERVICE_DATABASE_PASSWORD");
    }
    
    /**
     * Gets the application name from the environment variable.
     *
     * @return application name
     */
    public static String getApplicationName() {
        return System.getenv("JOB_SERVICE_DATABASE_APPNAME");
    }
}
