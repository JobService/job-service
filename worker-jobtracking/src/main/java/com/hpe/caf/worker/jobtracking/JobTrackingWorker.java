package com.hpe.caf.worker.jobtracking;

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.worker.*;
import com.hpe.caf.worker.AbstractWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.util.Objects;


public class JobTrackingWorker extends AbstractWorker<JobTrackingWorkerTask, JobTrackingWorkerResult> {

    private static final Logger LOG = LoggerFactory.getLogger(JobTrackingWorker.class);

    @NotNull
    private JobTrackingReporter reporter;


    public JobTrackingWorker(final JobTrackingWorkerTask task, final String outputQueue, final Codec codec, final JobTrackingReporter reporter) throws InvalidTaskException {
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
     * Reports the progress of the tracked task specified in the JobTrackingWorkerTask for which this Job Tracking Worker instance was created.
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
