package com.hpe.caf.worker.jobtracking;

import com.hpe.caf.api.*;
import com.hpe.caf.api.worker.*;
import com.hpe.caf.util.rabbitmq.RabbitHeaders;
import com.hpe.caf.worker.AbstractWorkerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Date;
import java.util.Map;

/**
 * Factory class for creating a JobTrackingWorker.
 * The Job Tracking Worker application is special in that it is both a normal Worker application that receives
 * messages that were intended for it (although they are Event Messages rather than Document Messages), and it also
 * acts as a Proxy, routing messages that were not ultimately intended for it to the correct Worker (although the
 * actual message forwarding will be done by Worker Framework code). Messages will typically arrive at the
 * Job Tracking Worker because the pipe that it is consuming messages from is specified as the trackingPipe
 * (which will trigger the Worker Framework to re-route output messages).
 * The Job Tracking Worker reports the progress of the task to the Job Database and, if the job is active,
 * it will be forwarded to the correct destination pipe.
 */
public class JobTrackingWorkerFactory extends AbstractWorkerFactory<JobTrackingWorkerConfiguration, JobTrackingWorkerTask> implements TaskMessageForwardingEvaluator {

    private static final Logger LOG = LoggerFactory.getLogger(JobTrackingWorkerFactory.class);

    @NotNull
    private JobTrackingReporter reporter;


    public JobTrackingWorkerFactory(ConfigurationSource configSource, DataStore store, Codec codec) throws WorkerException {
        super(configSource, store, codec, JobTrackingWorkerConfiguration.class, JobTrackingWorkerTask.class);
        this.reporter = createReporter();
    }


    public JobTrackingWorkerFactory(ConfigurationSource configSource, DataStore store, Codec codec, JobTrackingReporter reporter) throws WorkerException {
        super(configSource, store, codec, JobTrackingWorkerConfiguration.class, JobTrackingWorkerTask.class);
        this.reporter = reporter;
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
        return new JobTrackingWorker(task, getConfiguration().getOutputQueue(), getCodec(), reporter);
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
            JobTrackingWorkerHealthCheck healthCheck = new JobTrackingWorkerHealthCheck(reporter);
            return healthCheck.healthCheck();
        } catch (Exception e) {
            return new HealthResult(HealthStatus.UNHEALTHY, "Failed to perform Job Tracking Worker health check. " + e.getMessage());
        }
    }


    /**
     * Reports the progress of the proxied task message, then forwards it to its destination.
     * @param proxiedTaskMessage the proxied task message being diverted via the Job Tracking Worker
     * @param queueMessageId the reference to the message this task arrived on
     * @param headers the map of key/value paired headers to be stamped on the message
     * @param callback worker callback to enact the forwarding action determined by the worker
     */
    @Override
    public void determineForwardingAction(TaskMessage proxiedTaskMessage, String queueMessageId, Map<String, Object> headers, WorkerCallback callback) {
        reportProxiedTask(proxiedTaskMessage, headers);

        LOG.debug("Forwarding task {}", proxiedTaskMessage.getTaskId());
        callback.forward(queueMessageId, proxiedTaskMessage.getTo(), proxiedTaskMessage, headers);
    }


    private void reportProxiedTask(final TaskMessage proxiedTaskMessage, Map<String, Object> headers) {
        try {
            TrackingInfo tracking = proxiedTaskMessage.getTracking();
            if (tracking == null) {
                LOG.warn("Cannot report job task progress for task {} - the task message has no tracking info", proxiedTaskMessage.getTaskId());
                return;
            }

            String jobTaskId = tracking.getJobTaskId();
            if (jobTaskId == null) {
                LOG.warn("Cannot report job task progress for task {} - the tracking info has no jobTaskId", proxiedTaskMessage.getTaskId());
                return;
            }

            String trackToPipe = tracking.getTrackTo();
            if (trackToPipe == null) {
                LOG.warn("Cannot evaluate job task progress for job task {} in worker task {} - the tracking info has no trackTo pipe", jobTaskId, proxiedTaskMessage.getTaskId());
                return;
            }

            TaskStatus taskStatus = proxiedTaskMessage.getTaskStatus();

            if (taskStatus == TaskStatus.NEW_TASK || taskStatus == TaskStatus.RESULT_SUCCESS || taskStatus == TaskStatus.RESULT_FAILURE) {
                String toPipe = proxiedTaskMessage.getTo();
                if (trackToPipe.equalsIgnoreCase(toPipe)) {
                    reporter.reportJobTaskComplete(jobTaskId);
                } else {
                    //TODO - FUTURE: supply an accurate estimatedPercentageCompleted
                    reporter.reportJobTaskProgress(jobTaskId, 0);
                }
                return;
            }

            if (taskStatus == TaskStatus.RESULT_EXCEPTION || taskStatus == TaskStatus.INVALID_TASK) {

                //  Failed to execute job task.
                JobTrackingWorkerFailure f = new JobTrackingWorkerFailure();
                f.setFailureId(taskStatus.toString());
                f.setFailureTime(new Date());
                f.setFailureSource(getWorkerName(proxiedTaskMessage));

                final byte[] taskData = proxiedTaskMessage.getTaskData();
                if (taskData != null) {
                    f.setFailureMessage(new String(taskData, StandardCharsets.UTF_8));
                }

                reporter.reportJobTaskRejected(jobTaskId, f);

                return;
            }

            boolean rejected = headers.getOrDefault(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_REJECTED, null) != null;
            int retries = Integer.parseInt(String.valueOf(headers.getOrDefault(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_RETRY, "0")));
            if (rejected) {
                String rejectedHeader = String.valueOf(headers.get(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_REJECTED));
                String rejectionDetails = MessageFormat.format("{0}. Execution of this job task was retried {1} times.", rejectedHeader, retries);

                //  Failed to execute job task.
                JobTrackingWorkerFailure f = new JobTrackingWorkerFailure();
                f.setFailureId(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_REJECTED);
                f.setFailureTime(new Date());
                f.setFailureSource(getWorkerName(proxiedTaskMessage));
                f.setFailureMessage(rejectionDetails);

                reporter.reportJobTaskRejected(jobTaskId, f);

            } else {
                String retryDetails = MessageFormat.format("This job task encountered a problem and will be retried. This will be retry attempt number {0} for this job task.", retries);
                reporter.reportJobTaskRetry(jobTaskId, retryDetails);
            }
        } catch (JobReportingException e) {
            LOG.warn("Error reporting task {} progress to the Job Database: ", proxiedTaskMessage.getTaskId(), e);
            //TODO - should this ex be rethrown?
        }
    }


    private JobTrackingReporter createReporter() throws TaskRejectedException {
        try {
            return new JobTrackingWorkerReporter();
        } catch (JobReportingException e) {
            throw new TaskRejectedException("Failed to create Job Database reporter for Job Tracking Worker. ", e);
        }
    }

    /**
     * Returns the worker name from the source information in the task message or an "Unknown" string if it is not present.
     *
     * @param taskMessage the task message to be examined
     * @return the name of the worker that created the task message
     */
    private static String getWorkerName(final TaskMessage taskMessage)
    {
        final TaskSourceInfo sourceInfo = taskMessage.getSourceInfo();
        if (sourceInfo == null) {
            return "Unknown - no source info";
        }

        final String workerName = sourceInfo.getName();
        if (workerName == null) {
            return "Unknown - worker name not set";
        }

        return workerName;
    }
}
