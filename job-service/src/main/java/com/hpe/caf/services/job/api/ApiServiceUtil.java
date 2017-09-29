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
package com.hpe.caf.services.job.api;

import com.hpe.caf.services.job.configuration.AppConfig;
import com.hpe.caf.services.job.exceptions.BadRequestException;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

/**
 * Utility class for shared functionality.
 */
public final class ApiServiceUtil {

    private static final String ERR_MSG_DB_CONNECTION_PROPS_MISSING = "One or more PostgreSQL database connection properties have not been provided.";
    private static final String API_SERVICE_RESERVED_CHARACTERS_REGEX = "[.,:;*?!|()]";

    public static final String ERR_MSG_JOB_ID_NOT_SPECIFIED = "The job identifier has not been specified.";
    public static final String ERR_MSG_JOB_ID_CONTAINS_INVALID_CHARS = "The job identifier contains one or more invalid characters.";

    /**
     * Load required inputs from config.properties or environment variables.
     *
     * @return  properties  an object containing the properties read from environment variables
     * @throws  BadRequestException thrown upon bad request
     */
    public static AppConfig getAppConfigProperties() throws BadRequestException {
        AppConfig properties;

        AnnotationConfigApplicationContext propertiesApplicationContext = new AnnotationConfigApplicationContext();
        propertiesApplicationContext.register(PropertySourcesPlaceholderConfigurer.class);
        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setBeanClass(AppConfig.class);
        propertiesApplicationContext.registerBeanDefinition("AppConfig", beanDefinition);
        propertiesApplicationContext.refresh();

        properties = propertiesApplicationContext.getBean(AppConfig.class);

        try {
            //  Make sure DB connection properties have been specified.
            if (properties.getDatabaseURL() == null ||
                    properties.getDatabaseUsername() == null ||
                    properties.getDatabasePassword() == null) {
                throw new BadRequestException(ERR_MSG_DB_CONNECTION_PROPS_MISSING);
            }
        } catch (NullPointerException npe) {
            throw new BadRequestException(ERR_MSG_DB_CONNECTION_PROPS_MISSING);
        }

        return properties;
    }

    /**
     * Returns TRUE if the specified string is empty or null, otherwise FALSE.
     *
     * @param   str string to validate
     * @return  boolean flag
     */
    public static boolean isNotNullOrEmpty(String str) {
        return str != null && !str.isEmpty();
    }

    /**
     * Returns TRUE if the specified string contains one or more pre-defined reserved characters, otherwise FALSE.
     *
     * @param   toExamine string to validate
     * @return  boolean flag
     */
    public static boolean containsInvalidCharacters(String toExamine) {
        String[] arr = toExamine.split(API_SERVICE_RESERVED_CHARACTERS_REGEX, 2);
        return arr.length > 1;
    }
}
