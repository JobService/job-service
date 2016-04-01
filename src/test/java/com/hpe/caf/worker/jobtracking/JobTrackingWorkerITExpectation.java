package com.hpe.caf.worker.jobtracking;

/**
 * Represents the expected results of an integration test.
 */
public class JobTrackingWorkerITExpectation {
    /**
     * The job task identifier of the task under test.
     */
    private String jobTaskId;

    /**
     * The queue to which the Job Tracking Worker is expected to forward the result message from the test.
     */
    private String forwardingQueue;

    /**
     * Indicates whether we expect there to be TrackingInfo on the message forwarded by the Job Tracking Worker in the test.
     */
    private boolean trackingInfoPresent;

    /**
     * The specific values expected to be reported in the Job Database for the test.
     */
    private JobReportingExpectation jobReportingExpectation;


    public JobTrackingWorkerITExpectation(String jobTaskId, String forwardingQueue, boolean trackingInfoPresent, JobReportingExpectation jobReportingExpectation) {
        this.jobTaskId = jobTaskId;
        this.forwardingQueue = forwardingQueue;
        this.trackingInfoPresent = trackingInfoPresent;
        this.jobReportingExpectation = jobReportingExpectation;
    }


    public String getJobTaskId() {
        return jobTaskId;
    }


    public void setJobTaskId(String jobTaskId) {
        this.jobTaskId = jobTaskId;
    }

    public String getForwardingQueue() {
        return forwardingQueue;
    }


    public void setForwardingQueue(String forwardingQueue) {
        this.forwardingQueue = forwardingQueue;
    }


    public boolean isTrackingInfoPresent() {
        return trackingInfoPresent;
    }


    public void setTrackingInfoPresent(boolean trackingInfoPresent) {
        this.trackingInfoPresent = trackingInfoPresent;
    }

    public JobReportingExpectation getJobReportingExpectation() {
        return jobReportingExpectation;
    }


    public void setJobReportingExpectation(JobReportingExpectation jobReportingExpectation) {
        this.jobReportingExpectation = jobReportingExpectation;
    }
}
