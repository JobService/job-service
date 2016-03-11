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
        return environment.getProperty("database.url");
    }

    public String getDatabaseUsername(){
        return environment.getProperty("database.username");
    }

    public String getDatabasePassword(){
        return environment.getProperty("database.password");
    }

    public String getRabbitMQHost(){
        return environment.getProperty("rabbitmq.host");
    }

    public int getRabbitMQPort(){
        return Integer.parseInt(environment.getProperty("rabbitmq.port"));
    }

    public String getRabbitMQUsername(){
        return environment.getProperty("rabbitmq.username");
    }

    public String getRabbitMQPassword(){
        return environment.getProperty("rabbitmq.password");
    }

}
