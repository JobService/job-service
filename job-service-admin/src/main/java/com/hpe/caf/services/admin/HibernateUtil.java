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
package com.hpe.caf.services.admin;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class HibernateUtil
{

    private static Logger logger = LoggerFactory.getLogger(HibernateUtil.class);

    private static final String JDBC_PREFIX = "jdbc:";

    private static SessionFactory sessionFactory;

    private static Lock lock = new ReentrantLock();

    /**
     * Get the single instance of the SessionFactory object for the application's properties.
     *
     * Usually an application has a single SessionFactory instance and threads
     * servicing client requests obtain Session instances from this factory.
     *
     * There will be one session factory per application.
     *
     * @return SessionFactory
     */
    public static SessionFactory getSessionFactory()
    {
        if(sessionFactory == null) {
            lock.lock();
            try {
                if(sessionFactory == null){
                    final Configuration cfg = new Configuration();
                    // Load DB details from app config
                    final String connectionString = System.getenv("CAF_DATABASE_URL");
                    cfg.setProperty("hibernate.connection.url", connectionString);
                    cfg.setProperty("hibernate.connection.username", System.getenv("CAF_DATABASE_USERNAME"));
                    cfg.setProperty("hibernate.connection.password", System.getenv("CAF_DATABASE_PASSWORD"));
                    cfg.setProperty("hibernate.connection.driver_class", getDriverClass(connectionString));

                    // Setup ServiceRegistry from the hibernate configuration settings
                    final ServiceRegistry serviceRegistry =
                            new StandardServiceRegistryBuilder().applySettings(cfg.getProperties()).build();
                    sessionFactory = cfg.buildSessionFactory(serviceRegistry);  // Build session factory
                }
            } catch (final Throwable ex) {
                // Make sure you log the exception, as it might be swallowed
                logger.error("Could not get session", ex);
                throw new ExceptionInInitializerError(ex);
            } finally {
                lock.unlock();
            }
        }
        return sessionFactory;
    }

    private static String getDriverClass(final String connectionString)
    {

        // look at connection string, and decide what to use.
        // e.g. hibernate.connectionstring=jdbc:postgresql://localhost:5432/<dbname>
        // should use postgres driver
        if ( !connectionString.startsWith(JDBC_PREFIX) )
        {
            throw new RuntimeException("Invalid hibernate connection string format. Must start with jdbc:");
        }

        // Get the next : after the jdbc prefix.
        final int index = connectionString.indexOf( ":", JDBC_PREFIX.length());

        if ( index == -1 )
        {
            throw new RuntimeException("Invalid hibernate connection string format. Must be of the format jdbc:xxx:");
        }

        //take string from jdbc: to the next : as our connection type.
        final String connectionType = connectionString.substring(JDBC_PREFIX.length(), index);

        switch( connectionType.toLowerCase() ) {

            case "postgresql":
                logger.info("Detected driver required as postgresql");
                return "org.postgresql.Driver";
            case "h2":
                logger.info("Detected driver required as h2");
                return "org.h2.Driver";
            case "mysql":
                logger.info("Detected driver required as mysql");
                return "com.mysql.jdbc.Driver";
            default:
                // use my sql and log.
                logger.warn("Unknown jdbc driver type - defaulting to mysql.");
                // before we error, check incase someone has specified the property to use externally.
                final String driverClass = System.getProperty("hibernate.connection.driver_class");

                if (driverClass.isEmpty() || Objects.isNull(driverClass))
                    throw new RuntimeException("Unknown hibernate driver type - to add support please specify " +
                            "environment option hibernate.connection.driver_class.");

                // someone has already passed it in, just use it.
                logger.info("Using externally supplied driver class: " + driverClass);
                return driverClass;
        }
    }
}
