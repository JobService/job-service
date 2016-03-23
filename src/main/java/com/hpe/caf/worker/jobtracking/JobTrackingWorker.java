package com.hpe.caf.worker.jobtracking;

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.worker.*;
import com.hpe.caf.util.rabbitmq.RabbitHeaders;
import com.hpe.caf.worker.AbstractWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.util.Map;
import java.util.Objects;

/**
 * The Job Tracking Worker is special in that it is both a normal Worker that receives messages that were
 * intended for it (although they are Event Messages rather than Document Messages), and it also acts as
 * a Proxy, routing messages that were not ultimately intended for it to the correct Worker (although the
 * actual message forwarding will be done by Worker Framework code). Messages will typically arrive at the
 * Job Tracking Worker because the pipe that it is consuming messages from is specified as the trackingPipe
 * (which will trigger the Worker Framework to re-route output messages).
 * The Job Tracking Worker reports the progress of the task to the Job Database and, if the job is active,
 * it will be forwarded to the correct destination pipe.
 */
public class JobTrackingWorker extends AbstractWorker<JobTrackingWorkerTask, JobTrackingWorkerResult> implements TaskMessageForwardingEvaluator {

    private static final Logger LOG = LoggerFactory.getLogger(JobTrackingWorker.class);

    @NotNull
    private JobTrackingWorkerReporter reporter;


    public JobTrackingWorker(final JobTrackingWorkerTask task, final String outputQueue, final Codec codec, final JobTrackingWorkerReporter reporter) throws InvalidTaskException {
        super(task, outputQueue, codec);
        this.reporter = Objects.requireNonNull(reporter);
    }


    @Override
    public String getWorkerIdentifier() {
        return JobTrackingWorkerConstants.WORKER_NAME;
    }


    @Override
    public int getWorkerApiVersion() {
        return JobTrackingWorkerConstants.WORKER_API_VER;
    }


    @Override
    public WorkerResponse doWork() throws InterruptedException, TaskRejectedException {
        JobTrackingWorkerResult result = processTrackingEvent();
        if (result.getStatus() == JobTrackingWorkerStatus.COMPLETED){
            return createSuccessResult(result);
        } else {
            return createFailureResult(result);
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

        LOG.warn("Forwarding task {}", proxiedTaskMessage.getTaskId());
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

            String toPipe = proxiedTaskMessage.getTo();
            if (toPipe == null) {
                LOG.warn("Cannot evaluate job task progress for job task {} in worker task {} - the task message has no 'to' pipe", jobTaskId, proxiedTaskMessage.getTaskId());
                return;
            }

            TaskStatus taskStatus = proxiedTaskMessage.getTaskStatus();

            if (taskStatus == TaskStatus.NEW_TASK || taskStatus == TaskStatus.RESULT_SUCCESS) {
                if (trackToPipe.equalsIgnoreCase(toPipe)) {
                    reporter.reportJobTaskComplete(jobTaskId);
                } else {
                    //TODO - FUTURE: supply an accurate estimatedPercentageCompleted
                    reporter.reportJobTaskProgress(jobTaskId, 0);
                }
                return;
            }

            boolean rejected = headers.getOrDefault(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_REJECTED, null) != null;
            int retries = Integer.parseInt(String.valueOf(headers.getOrDefault(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_RETRY, "0")));
            if (rejected) {
                reporter.reportJobTaskRejected(jobTaskId, retries);
            } else {
                reporter.reportJobTaskFailure(jobTaskId, retries);
            }
        } catch (JobReportingException e) {
            LOG.warn("Error reporting task {} progress to the Job Database: ", proxiedTaskMessage.getTaskId(), e);
        }
    }


    /**
     * Reports the progress of the tracked task specified in the JobTrackingWorkerTask for which this Job Tracking Worker instance was created.
     * If this method is being called then this worker's JobTrackingWorkerTask must be a "tracking event" - a message created solely for the Job Tracking Worker to report the progress of another task.
     * It is not a proxied message being diverted via the Job Tracking Worker.
     * @return indicator of successful reporting or otherwise
     * @throws InterruptedException
     */
    private JobTrackingWorkerResult processTrackingEvent() throws InterruptedException {
        LOG.info("Starting work");
        checkIfInterrupted();

        try {
            JobTrackingWorkerTask jtwTask = this.getTask();
            reporter.reportJobTaskProgress(jtwTask.getJobTaskId(), jtwTask.getEstimatedPercentageCompleted());

            JobTrackingWorkerResult workerResult = new JobTrackingWorkerResult();
            workerResult.setStatus(JobTrackingWorkerStatus.COMPLETED);
            return workerResult;
        } catch (JobReportingException e) {
            LOG.warn("Error reporting task progress to the Job Database: ", e);
            return createErrorResult(JobTrackingWorkerStatus.PROGRESS_UPDATE_FAILED);
        }
    }


    /**
     * If an error in the worker occurs, create a new JobTrackingWorkerResult with the corresponding worker failure status.
     */
    private JobTrackingWorkerResult createErrorResult(JobTrackingWorkerStatus status){
        JobTrackingWorkerResult workerResult = new JobTrackingWorkerResult();
        workerResult.setStatus(status);
        return workerResult;
    }
}
