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

/**
 * Configuration class for the Job Service Scheduled Executor. Includes connection properties to both database and RabbitMQ.
 */
public class ScheduledExecutorConfig {

    public static String getDatabaseURL(){
        return getPropertyOrEnvVar("JOB_SERVICE_DATABASE_URL") != null ? getPropertyOrEnvVar("JOB_SERVICE_DATABASE_URL")
            : getPropertyOrEnvVar("CAF_DATABASE_URL");
    }

    public static String getDatabaseUsername(){
        return getPropertyOrEnvVar("JOB_SERVICE_DATABASE_USERNAME") != null ? getPropertyOrEnvVar("JOB_SERVICE_DATABASE_USERNAME") 
            : getPropertyOrEnvVar("CAF_DATABASE_USERNAME");
    }

    public static String getDatabasePassword(){
        return getPropertyOrEnvVar("JOB_SERVICE_DATABASE_PASSWORD") != null ? getPropertyOrEnvVar("JOB_SERVICE_DATABASE_PASSWORD")
            : getPropertyOrEnvVar("CAF_DATABASE_PASSWORD");
    }
    
    public static String getApplicationName(){
        return getPropertyOrEnvVar("JOB_SERVICE_DATABASE_APPNAME") != null ? getPropertyOrEnvVar("JOB_SERVICE_DATABASE_APPNAME")
            : getPropertyOrEnvVar("CAF_DATABASE_APPNAME");
    }

    public static String getRabbitMQHost(){
        return getPropertyOrEnvVar("CAF_RABBITMQ_HOST");
    }

    public static int getRabbitMQPort(){
        return Integer.parseInt(getPropertyOrEnvVar("CAF_RABBITMQ_PORT"));
    }

    public static String getRabbitMQUsername(){
        return getPropertyOrEnvVar("CAF_RABBITMQ_USERNAME");
    }

    public static String getRabbitMQPassword(){
        return getPropertyOrEnvVar("CAF_RABBITMQ_PASSWORD");
    }

    public static String getTrackingPipe() {
        return getPropertyOrEnvVar("CAF_TRACKING_PIPE");
    }

    public static String getStatusCheckIntervalSeconds() {
        final String checkInterval = getPropertyOrEnvVar("CAF_STATUS_CHECK_INTERVAL_SECONDS");
        if (null == checkInterval || checkInterval.isEmpty()){
            return "5";
        }
        return checkInterval;
    }

    public static String getWebserviceUrl() {
        return getPropertyOrEnvVar("CAF_WEBSERVICE_URL");
    }

    public static int getScheduledExecutorPeriod() {
        //  Default to 10 seconds if CAF_SCHEDULED_EXECUTOR_PERIOD not specified.
        final String  scheduledExecutorPeriod = getPropertyOrEnvVar("CAF_SCHEDULED_EXECUTOR_PERIOD");
        if (null == scheduledExecutorPeriod || scheduledExecutorPeriod.isEmpty()) {
            return 10;
        }
        return Integer.parseInt(scheduledExecutorPeriod);
    }
    
    public static int getDropTablesSchedulerPeriod() {
        final String  period = getPropertyOrEnvVar("CAF_DROP_TABLES_SCHEDULER_PERIOD");
        if (null == period || period.isEmpty()) {
            return 60;
        }
        return Integer.parseInt(period);
    }

    private static String getPropertyOrEnvVar(final String key)
    {
        final String propertyValue = System.getProperty(key);
        return (propertyValue != null) ? propertyValue : System.getenv(key);
    }

}
