package com.hpe.caf.worker.jobtracking;

/**
 * Holds details of a task being proxied by the Job Tracking Worker
 */
public class ProxiedTaskInfo {
    /**
     * The classifier indicating the type of task message being proxied, i.e. the type of worker matching the proxied task.
     */
    private String proxiedTaskClassifier;

    /**
     * The data payload of the task message being proxied.
     */
    private byte[] proxiedTaskData;


    public ProxiedTaskInfo() {
    }


    public ProxiedTaskInfo(String proxiedTaskClassifier, byte[] proxiedTaskData) {
        this.proxiedTaskClassifier = proxiedTaskClassifier;
        this.proxiedTaskData = proxiedTaskData;
    }


    public String getProxiedTaskClassifier() {
        return proxiedTaskClassifier;
    }


    public void setProxiedTaskClassifier(String proxiedTaskClassifier) {
        this.proxiedTaskClassifier = proxiedTaskClassifier;
    }


    public byte[] getProxiedTaskData() {
        return proxiedTaskData;
    }


    public void setProxiedTaskData(byte[] proxiedTaskData) {
        this.proxiedTaskData = proxiedTaskData;
    }
}
