package com.hpe.caf.worker.jobtracking;

import java.util.Date;

public class JobTrackingWorkerFailure   {

    private String failureId = null;
    private Date failureTime = null;
    private String failureSource = null;
    private String failureMessage = null;

    public JobTrackingWorkerFailure() {}

    public String getFailureId() {
        return failureId;
    }
    public void setFailureId(String failureId) {
        this.failureId = failureId;
    }

    public Date getFailureTime() {
        return failureTime;
    }
    public void setFailureTime(Date failureTime) {
        this.failureTime = failureTime;
    }

    public String getFailureSource() {
        return failureSource;
    }
    public void setFailureSource(String failureSource) {
        this.failureSource = failureSource;
    }

    public String getFailureMessage() {
        return failureMessage;
    }
    public void setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
    }

}

