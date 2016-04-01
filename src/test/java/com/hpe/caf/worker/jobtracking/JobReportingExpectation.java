package com.hpe.caf.worker.jobtracking;

/**
 * Holds the values expected from an integration test, to be compared against the reported state of a job in the Job Database.
 */
public class JobReportingExpectation {
    private String jobId;
    private JobStatus status;
    private float percentageComplete;
    private boolean failureDetailsPresent;


    public JobReportingExpectation(String jobId, JobStatus status, float percentageComplete, boolean failureDetailsPresent) {
        this.jobId = jobId;
        this.status = status;
        this.percentageComplete = percentageComplete;
        this.failureDetailsPresent = failureDetailsPresent;
    }


    public String getJobId() {
        return jobId;
    }


    public void setJobId(String jobId) {
        this.jobId = jobId;
    }


    public JobStatus getStatus() {
        return status;
    }


    public void setStatus(JobStatus status) {
        this.status = status;
    }


    public float getPercentageComplete() {
        return percentageComplete;
    }


    public void setPercentageComplete(float percentageComplete) {
        this.percentageComplete = percentageComplete;
    }


    public boolean getFailureDetailsPresent() {
        return failureDetailsPresent;
    }


    public void setFailureDetailsPresent(boolean failureDetailsPresent) {
        this.failureDetailsPresent = failureDetailsPresent;
    }
}
