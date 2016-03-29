package com.hpe.caf.worker.jobtracking;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Configuration for the JobTrackingWorker, read in from test-configs/cfg_test_worker-jobtracking-JobTrackingWorkerConfiguration.
 */
public class JobTrackingWorkerConfiguration {
    /**
     * The URL of the Job Database (PostgreSQL).
     */
    @NotNull
    @Size(min = 1)
    private String jobDatabaseURL;

    /**
     * The username to use when connecting to the Job Database.
     */
    @NotNull
    @Size(min = 1)
    private String jobDatabaseUsername;

    /**
     * The password to use with the configured username when connecting to the Job Database.
     */
    @NotNull
    @Size(min = 1)
    private String jobDatabasePassword;

    /**
     * Output queue to return results to RabbitMQ.
     */
    @NotNull
    @Size(min = 1)
    private String outputQueue;

    /**
     * Number of threads to use in the worker.
     */
    @Min(1)
    @Max(20)
    private int threads;


    public JobTrackingWorkerConfiguration() { }


    public String getJobDatabaseURL() {
        return jobDatabaseURL;
    }


    public void setJobDatabaseURL(String jobDatabaseURL) {
        this.jobDatabaseURL = jobDatabaseURL;
    }


    public String getJobDatabaseUsername() {
        return jobDatabaseUsername;
    }


    public void setJobDatabaseUsername(String jobDatabaseUsername) {
        this.jobDatabaseUsername = jobDatabaseUsername;
    }


    public String getJobDatabasePassword() {
        return jobDatabasePassword;
    }


    public void setJobDatabasePassword(String jobDatabasePassword) {
        this.jobDatabasePassword = jobDatabasePassword;
    }


    public String getOutputQueue() {
        return outputQueue;
    }


    public void setOutputQueue(String outputQueue) {
        this.outputQueue = outputQueue;
    }


    public int getThreads() {
        return threads;
    }


    public void setThreads(int threads) {
        this.threads = threads;
    }
}
