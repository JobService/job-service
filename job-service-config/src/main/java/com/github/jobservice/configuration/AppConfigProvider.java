/*
 * Copyright 2016-2026 Open Text.
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
package com.github.jobservice.configuration;

public final class AppConfigProvider
{
    private static final String ERR_MSG_DB_CONNECTION_PROPS_MISSING = "One or more PostgreSQL database connection " +
            "properties have not been provided.";
    private static final String ERR_MSG_DB_CONNECTION_PORT_INVALID = "The PostgreSQL database connection port" +
            "provided is invalid. ";
    private static final String ERR_MSG_RESUME_JOB_QUEUE_PROP_MISSING = "CAF_JOB_SERVICE_RESUME_JOB_QUEUE property has not been " +
            "provided.";

    private AppConfigProvider()
    {
    }

    /**
     * Load required inputs from environment variables.
     *
     * @return  properties  an object containing the properties read from environment variables
     * @throws  AppConfigException thrown upon configuration issue
     */
    public static AppConfig getAppConfigProperties() throws AppConfigException {
        final AppConfig properties = new AppConfig();

        try {
            //  Make sure DB connection properties have been specified.
            if (
                null == properties.getDatabaseHost() ||
                null == properties.getDatabasePort() ||
                null == properties.getApplicationName() ||
                null == properties.getDatabaseUsername() ||
                null == properties.getDatabasePassword()
            ) {
                throw new AppConfigException(ERR_MSG_DB_CONNECTION_PROPS_MISSING);
            }
            Integer.parseInt(properties.getDatabasePort());
        } catch (final NullPointerException npe) {
            throw new AppConfigException(ERR_MSG_DB_CONNECTION_PROPS_MISSING);
        } catch (final NumberFormatException ex){
            throw new AppConfigException(ERR_MSG_DB_CONNECTION_PORT_INVALID + properties.getDatabasePort());
        }

        //  Make sure the resume job queue property has been specified.
        if (properties.getResumeJobQueue() == null) {
            throw new AppConfigException(ERR_MSG_RESUME_JOB_QUEUE_PROP_MISSING);
        }

        return properties;
    }
}
