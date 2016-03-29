package com.hpe.caf.worker.jobtracking;

import com.hpe.caf.api.*;
import com.hpe.caf.api.worker.*;
import com.hpe.caf.worker.AbstractWorkerFactory;

/**
 * Factory class for creating a JobTrackingWorker.
 */
public class JobTrackingWorkerFactory extends AbstractWorkerFactory<JobTrackingWorkerConfiguration, JobTrackingWorkerTask> {

    public JobTrackingWorkerFactory(ConfigurationSource configSource, DataStore store, Codec codec) throws WorkerException {
        super(configSource, store, codec, JobTrackingWorkerConfiguration.class, JobTrackingWorkerTask.class);
    }


    /**
     * {@inheritDoc}
     * Overridden so that a Job Tracking Worker can be created with the task data of a different worker type.
     * Job Tracking Worker has to be able to proxy messages for other workers.
     */
    @Override
    public Worker getMismatchedWorker(final String classifier, final int version, final TaskStatus status, final byte[] data, final byte[] context) throws TaskRejectedException, InvalidTaskException {
        JobTrackingWorkerTask jtwTask = new JobTrackingWorkerTask();
        jtwTask.setProxiedTaskInfo(new ProxiedTaskInfo(classifier, data));
        return new JobTrackingWorker(jtwTask, getConfiguration().getOutputQueue(), getCodec(), createReporter());
    }


    @Override
    protected String getWorkerName() {
        return JobTrackingWorkerConstants.WORKER_NAME;
    }


    @Override
    protected int getWorkerApiVersion() {
        return JobTrackingWorkerConstants.WORKER_API_VER;
    }


    @Override
    public Worker createWorker(JobTrackingWorkerTask task) throws TaskRejectedException, InvalidTaskException {
        return new JobTrackingWorker(task, getConfiguration().getOutputQueue(), getCodec(), createReporter());
    }


    @Override
    public String getInvalidTaskQueue() {
        return getConfiguration().getOutputQueue();
    }


    @Override
    public int getWorkerThreads() {
        return getConfiguration().getThreads();
    }


    @Override
    public HealthResult healthCheck() {
        try {
            JobTrackingWorkerHealthCheck healthCheck = new JobTrackingWorkerHealthCheck(createReporter());
            return healthCheck.healthCheck();
        } catch (TaskRejectedException e) {
            return new HealthResult(HealthStatus.UNHEALTHY, "Failed to perform Job Tracking Worker health check. " + e.getMessage());
        }
    }


    private JobTrackingReporter createReporter() throws TaskRejectedException {
        try {
            return new JobTrackingWorkerReporter(getConfiguration().getJobDatabaseURL(), getConfiguration().getJobDatabaseUsername(), getConfiguration().getJobDatabasePassword());
        } catch (JobReportingException e) {
            throw new TaskRejectedException("Failed to create Job Database reporter for Job Tracking Worker. ", e);
        }
    }
}
