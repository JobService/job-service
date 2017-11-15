/*
 * Copyright 2015-2017 EntIT Software LLC, a Micro Focus company.
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
        return System.getProperty("CAF_DATABASE_URL", System.getenv("CAF_DATABASE_URL"));
    }

    public static String getDatabaseUsername(){
        return System.getProperty("CAF_DATABASE_USERNAME", System.getenv("CAF_DATABASE_USERNAME"));
    }

    public static String getDatabasePassword(){
        return System.getProperty("CAF_DATABASE_PASSWORD", System.getenv("CAF_DATABASE_PASSWORD"));
    }

    public static String getRabbitMQHost(){
        return System.getProperty("CAF_RABBITMQ_HOST", System.getenv("CAF_RABBITMQ_HOST"));
    }

    public static int getRabbitMQPort(){
        return Integer.parseInt(System.getProperty("CAF_RABBITMQ_PORT", System.getenv("CAF_RABBITMQ_PORT")));
    }

    public static String getRabbitMQUsername(){
        return System.getProperty("CAF_RABBITMQ_USERNAME", System.getenv("CAF_RABBITMQ_USERNAME"));
    }

    public static String getRabbitMQPassword(){
        return System.getProperty("CAF_RABBITMQ_PASSWORD", System.getenv("CAF_RABBITMQ_PASSWORD"));
    }

    public static String getTrackingPipe() {
        return System.getProperty("CAF_TRACKING_PIPE", System.getenv("CAF_TRACKING_PIPE"));
    }

    public static String getStatusCheckTime() {
        return System.getProperty("CAF_STATUS_CHECK_TIME", System.getenv("CAF_STATUS_CHECK_TIME"));
    }

    public static String getWebserviceUrl() {
        return System.getProperty("CAF_WEBSERVICE_URL", System.getenv("CAF_WEBSERVICE_URL"));
    }
}
