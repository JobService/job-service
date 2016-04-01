package com.hpe.caf.worker.jobtracking;


public class JobDatabaseProperties {

    public static String getDatabaseUrl() {
        return System.getenv("JOB_DATABASE_URL");
    }

    public static String getDatabaseUsername() {
        return System.getenv("JOB_DATABASE_USERNAME");
    }

    public static String getDatabasePassword() {
        return System.getenv("JOB_DATABASE_PASSWORD");
    }
}
