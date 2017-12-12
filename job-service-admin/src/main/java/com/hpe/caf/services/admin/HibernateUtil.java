package com.hpe.caf.services.admin;

import com.hpe.caf.services.job.configuration.AppConfig;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.service.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class HibernateUtil {

    private static Logger logger = LoggerFactory.getLogger(HibernateUtil.class);

    private final String JDBC_PREFIX = "jdbc:";

    private static SessionFactory sessionFactory;

//    private static Logger logger = LoggerFactory.getLogger(HibernateUtil.class);

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
    public static SessionFactory getSessionFactory(final AppConfig appConfig) {
        if(sessionFactory == null) {
            lock.lock();
            try {
                if(sessionFactory == null){
                    Configuration cfg = new Configuration();
                    // Create the SessionFactory from hibernate.cfg.xml
//                    cfg.configure("uniqueIdGenerator.cfg.xml"); // Load hibernate configuration from xml
                    // Load DB details from app config
                    final String connectionString = appConfig.getDatabaseURL();
                    cfg.setProperty("hibernate.connection.url", connectionString);
                    cfg.setProperty("hibernate.connection.username", appConfig.getDatabaseUsername());
                    cfg.setProperty("hibernate.connection.password", appConfig.getDatabasePassword());
                    cfg.setProperty("hibernate.connection.driver_class", getDriverClass(connectionString));
//                    cfg.setProperty("hibernate.c3p0.acquireRetryAttempts", appConfig);
//                    cfg.setProperty("hibernate.c3p0.acquireRetryDelay", properties.getAcquireDbConnectionRetryDelay());
//                    cfg.setProperty("hibernate.c3p0.breakAfterAcquireFailure", properties.getAcquireDbBreakAfterAcquireFailure());

                    final ServiceRegistry serviceRegistry =
                            new StandardServiceRegistryBuilder().applySettings(cfg.getProperties()).build(); // Setup ServiceReg
                    sessionFactory = cfg.buildSessionFactory(serviceRegistry);  // Build session factory
                }
            } catch (Throwable ex) {
                // Make sure you log the exception, as it might be swallowed
                logger.error("Could not get session", ex);
                throw new ExceptionInInitializerError(ex);
            } finally {
                lock.unlock();
            }
        }
        return sessionFactory;
    }

    private static String getDriverClass(final String connectionString) {

        // look at connection string, and decide what to use.
        // e.g. hibernate.connectionstring=jdbc:postgresql://localhost:5432/<dbname>
        // should use postgres driver

        if ( !connectionString.startsWith(JDBC_PREFIX) )
        {
            throw new RuntimeException("Invalid hibernate connection string format. Must start with jdbc:");
        }

        // Get the next : after the jdbc prefix.
        int index = connectionString.indexOf( ":", JDBC_PREFIX.length());

        if ( index == -1 )
        {
            throw new RuntimeException("Invalid hibernate connection string format. Must be of the format jdbc:xxx:");
        }

        //take string from jdbc: to the next : as our connection type.
        String connectionType = connectionString.substring(JDBC_PREFIX.length(), index);

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
                String driverClass = System.getProperty("hibernate.connection.driver_class");

                if (driverClass.isEmpty() || Objects.isNull(driverClass))
                    throw new RuntimeException("Unknown hibernate driver type - to add support please specify environment option hibernate.connection.driver_class.");

                // someone has already passed it in, just use it.
                logger.info("Using externally supplied driver class: " + driverClass);
                return driverClass;
        }
    }

    /**
     * Method to destroy the session factory and release all its resources.
     *
     * This is not responsible for closing individual sessions.
     */
    public static void closeSessionFactory(){
        if(sessionFactory == null){
            return;
        }

        try {
            lock.lock();
            if(sessionFactory == null)
                return;

            sessionFactory.close();
            sessionFactory = null;
        } finally {
            lock.unlock();
        }
    }

}
