package com.hpe.caf.worker.jobtracking;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Configuration for the JobTrackingWorker.
 */
public class JobTrackingWorkerConfiguration {

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
