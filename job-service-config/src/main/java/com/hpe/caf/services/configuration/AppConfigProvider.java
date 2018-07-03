/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
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

import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

public final class AppConfigProvider
{
    private static final String ERR_MSG_DB_CONNECTION_PROPS_MISSING = "One or more PostgreSQL database connection " +
            "properties have not been provided.";

    private AppConfigProvider()
    {
    }

    /**
     * Load required inputs from config.properties or environment variables.
     *
     * @return  properties  an object containing the properties read from environment variables
     * @throws  AppConfigException thrown upon configuration issue
     */
    public static AppConfig getAppConfigProperties() throws AppConfigException {
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
                throw new AppConfigException(ERR_MSG_DB_CONNECTION_PROPS_MISSING);
            }
        } catch (NullPointerException npe) {
            throw new AppConfigException(ERR_MSG_DB_CONNECTION_PROPS_MISSING);
        }

        return properties;
    }
}
