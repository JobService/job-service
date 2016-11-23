package com.hpe.caf.services.job.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

/**
 * Configuration class for the job service api. Includes connection properties to both database and RabbitMQ.
 */
@Configuration
@PropertySource(value = "file:${JOB_SERVICE_API_CONFIG_PATH}/config.properties", ignoreResourceNotFound = true)
public class AppConfig {

    @Autowired
    private Environment environment;

    public String getDatabaseURL(){
        return environment.getProperty("CAF_DATABASE_URL");
    }

    public String getDatabaseUsername(){
        return environment.getProperty("CAF_DATABASE_USERNAME");
    }

    public String getDatabasePassword(){
        return environment.getProperty("CAF_DATABASE_PASSWORD");
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

    public String getStatusCheckTime() {
        return environment.getProperty("CAF_STATUS_CHECK_TIME");
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

}
