/*
 * Copyright 2016-2025 Open Text.
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
package com.github.jobservice.scheduledexecutor;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import com.github.cafapi.common.util.secret.SecretUtil;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Configuration class for the Job Service Scheduled Executor. Includes connection properties to both database and RabbitMQ.
 */
public class ScheduledExecutorConfig {

    private enum SecretKey
    {
        CAF_RABBITMQ_PASSWORD,
        JOB_SERVICE_DATABASE_PASSWORD,
    }

    private static final LoadingCache<SecretKey, String> SECRETS_CACHE = CacheBuilder.newBuilder()
            .maximumSize(SecretKey.values().length)
            .build(new CacheLoader<>() {
                @Override
                public String load(final SecretKey key) throws IOException {
                    final String propertyValue = System.getProperty(key.name());
                    return (propertyValue != null) ? propertyValue : SecretUtil.getSecret(key.name());
                }
            });

    public static String getDatabaseHost(){
        return getPropertyOrEnvVar("JOB_SERVICE_DATABASE_HOST");
    }

    public static String getDatabasePort(){
        return getPropertyOrEnvVar("JOB_SERVICE_DATABASE_PORT");
    }

    public static String getDatabaseName(){
        return getPropertyOrEnvVar("JOB_SERVICE_DATABASE_NAME");
    }

    public static String getDatabaseUsername(){
        return getPropertyOrEnvVar("JOB_SERVICE_DATABASE_USERNAME");
    }

    public static String getDatabasePassword(){
        try {
            return SECRETS_CACHE.get(SecretKey.JOB_SERVICE_DATABASE_PASSWORD);
        } catch (final ExecutionException e) {
            throw new RuntimeException("Failed to get secret for 'JOB_SERVICE_DATABASE_PASSWORD'", e);
        }
    }

    public static String getApplicationName(){
        return getPropertyOrEnvVar("JOB_SERVICE_DATABASE_APPNAME");
    }

    public static String getRabbitMQProtocol()
    {
        // Default to 'amqp' if CAF_RABBITMQ_PROTOCOL is not specified
        final String rabbitMqProtocol = getPropertyOrEnvVar("CAF_RABBITMQ_PROTOCOL");
        if (null == rabbitMqProtocol || rabbitMqProtocol.isEmpty()) {
            return "amqp";
        }
        return rabbitMqProtocol;
    }

    public static String getRabbitMQTlsProtocolVersion()
    {
        final String rabbitMqTlsProtocolVersion = getPropertyOrEnvVar("CAF_RABBITMQ_TLS_PROTOCOL_VERSION");
        if (null == rabbitMqTlsProtocolVersion || rabbitMqTlsProtocolVersion.isEmpty()) {
            return "TLSv1.2";
        }
        return rabbitMqTlsProtocolVersion;
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
        try {
            return SECRETS_CACHE.get(SecretKey.CAF_RABBITMQ_PASSWORD);
        } catch (final ExecutionException e) {
            throw new RuntimeException("Failed to get secret for 'CAF_RABBITMQ_PASSWORD'", e);
        }
    }

    public static int getRabbitMQPublishTimeoutSeconds() {
        final String rabbitmqPublishTimeoutSeconds = getPropertyOrEnvVar("CAF_RABBITMQ_PUBLISH_TIMEOUT_SECONDS");
        if (null == rabbitmqPublishTimeoutSeconds || rabbitmqPublishTimeoutSeconds.isEmpty()) {
            return 10;
        }
        return Integer.parseInt(rabbitmqPublishTimeoutSeconds);
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

    public static String getQueueType() {
        final String queueType = getPropertyOrEnvVar("CAF_RABBITMQ_QUEUE_TYPE");
        if (null == queueType || queueType.isEmpty()) {
            return "quorum";
        }
        return queueType;
    }

    public static int getQueueMaxPriority() {
        final String queueMaxPriority = getPropertyOrEnvVar("CAF_RABBITMQ_MAX_PRIORITY");
        if (null == queueMaxPriority || queueMaxPriority.isEmpty()) {
            return 0;
        }
        return Integer.parseInt(queueMaxPriority);
    }

    private static String getPropertyOrEnvVar(final String key)
    {
        final String propertyValue = System.getProperty(key);
        return (propertyValue != null) ? propertyValue : System.getenv(key);
    }

    public static int getJobProgressTasksToUpdate() {
        final String  numOfTasks = getPropertyOrEnvVar("CAF_JOB_PROGRESS_NUM_TASKS_TO_UPDATE");
        if (null == numOfTasks || numOfTasks.isEmpty()) {
            return 100;
        }
        return Integer.parseInt(numOfTasks);
    }

    public static int getJobProgressUpdatePeriod() {
        final String  period = getPropertyOrEnvVar("CAF_JOB_PROGRESS_UPDATE_SCHEDULER_PERIOD");
        if (null == period || period.isEmpty()) {
            return 2;
        }
        return Integer.parseInt(period);
    }
}
