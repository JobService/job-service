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
package com.hpe.caf.services.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Configuration class for the job service api. Includes connection properties to both database and RabbitMQ.
 */
@Configuration
@PropertySource(value = "classpath:${JOB_SERVICE_API_CONFIG_PATH:config.properties}", ignoreResourceNotFound = true)
public class AppConfig {

    @Autowired
    private Environment environment;

    public String getDatabaseHost(){
        return environment.getProperty("JOB_SERVICE_DATABASE_HOST");
    }

    public String getDatabasePort(){
        return environment.getProperty("JOB_SERVICE_DATABASE_PORT");
    }

    public String getDatabaseName(){
        return environment.getProperty("JOB_SERVICE_DATABASE_NAME");
    }

    public String getDatabaseUsername(){
        return environment.getProperty("JOB_SERVICE_DATABASE_USERNAME");
    }

    public String getDatabasePassword(){
        return environment.getProperty("JOB_SERVICE_DATABASE_PASSWORD");
    }
    
    public String getApplicationName(){
        return environment.getProperty("JOB_SERVICE_DATABASE_APPNAME");
    }

    public String getRabbitMQHost(){
        return environment.getProperty("CAF_RABBITMQ_HOST");
    }

    public int getRabbitMQPort(){
        return Integer.parseInt(environment.getProperty("CAF_RABBITMQ_PORT"));
    }

    public String getRabbitMQUsername(){
        return environment.getProperty("CAF_RABBITMQ_USERNAME");
    }

    public String getRabbitMQPassword(){
        return environment.getProperty("CAF_RABBITMQ_PASSWORD");
    }

    public String getTrackingPipe() {
        return environment.getProperty("CAF_TRACKING_PIPE");
    }

    public String getStatusCheckIntervalSeconds() {
        return environment.getProperty("CAF_STATUS_CHECK_INTERVAL_SECONDS");
    }

    public String getWebserviceUrl() {
        return environment.getProperty("CAF_WEBSERVICE_URL");
    }

    public int getDefaultPageSize(){
        final String defaultPageSize = environment.getProperty("CAF_JOB_SERVICE_PAGE_SIZE");
        if (defaultPageSize == null) {
            return 0;
        }
        else {
            return Integer.parseInt(defaultPageSize);
        }
    }

    /**
     * @return Directory containing job type definition files, possibly `null`
     */
    public Path getJobTypeDefinitionsDir() {
        final String path = environment.getProperty("CAF_JOB_SERVICE_JOB_TYPE_DEFINITIONS_DIR");
        return path == null ? null : Paths.get(path);
    }

    /**
     * Retrieve a configuration property.
     *
     * @param propertyName
     * @return Property value, possibly `null`
     */
    public String getJobProperty(final String propertyName) {
        return environment.getProperty(propertyName.toUpperCase(Locale.ENGLISH));
    }

    public Pattern getSuspendedPartitionsPattern() {
        final String suspendedPartitionsRegex = environment.getProperty("CAF_JOB_SERVICE_SUSPENDED_PARTITIONS_REGEX");
        return suspendedPartitionsRegex == null ? null : Pattern.compile(suspendedPartitionsRegex);
    }

    public String getResumeJobQueue()
    {
        return environment.getProperty("CAF_JOB_SERVICE_RESUME_JOB_QUEUE");
    }

    public String getSchedulerQueue()
    {
        final String defaultName = "jobservicescheduler-in";
        final String schedulerInputQueue = environment.getProperty("CAF_SCHEDULER_INPUT_QUEUE");
        if (null != schedulerInputQueue) {
            return schedulerInputQueue;
        }
        return defaultName;
    }
}
