/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hpe.caf.worker.jobtracking;

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.worker.InvalidTaskException;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TaskRejectedException;
import com.hpe.caf.api.worker.WorkerResponse;
import com.hpe.caf.api.worker.WorkerTaskData;
import com.hpe.caf.worker.AbstractWorker;
import com.hpe.caf.worker.tracking.report.TrackingReport;
import com.hpe.caf.worker.tracking.report.TrackingReportStatus;
import com.hpe.caf.worker.tracking.report.TrackingReportTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class JobTrackingReportUpdateWorker extends AbstractWorker<TrackingReportTask, JobTrackingWorkerResult> {

    private static final Logger LOG = LoggerFactory.getLogger(JobTrackingReportUpdateWorker.class);

    @NotNull
    private JobTrackingReporter reporter;

    private WorkerTaskData workerTask;

    /**
     * Constructor called by the JobTrackingWorkerFactory.
     */
    public JobTrackingReportUpdateWorker(final TrackingReportTask trackingReportTask, final WorkerTaskData workerTask,
                                         final String outputQueue, final Codec codec,
                                         final JobTrackingReporter reporter) throws InvalidTaskException
    {
        super(trackingReportTask, outputQueue, codec, workerTask);
        this.reporter = Objects.requireNonNull(reporter);
        this.workerTask = Objects.requireNonNull(workerTask);
    }

    /**
     * Gets the worker name.
     *
     * @return worker name
     */
    @Override
    public String getWorkerIdentifier() {
        return JobTrackingWorkerConstants.WORKER_NAME;
    }

    /**
     * Gets the worker API version.
     *
     * @return worker api version
     */
    @Override
    public int getWorkerApiVersion() {
        return JobTrackingWorkerConstants.WORKER_API_VER;
    }

    /**
     * Main work method of the JobTrackingWorker.
     *
     * @return  worker response
     * @throws  InterruptedException    if the work was interrupted by some cause
     * @throws  TaskRejectedException   if the task is rejected
     */
    @Override
    public WorkerResponse doWork() throws InterruptedException, TaskRejectedException {
        final JobTrackingWorkerResult result = processTrackingEvent();
        if (result.getStatus() == JobTrackingWorkerStatus.COMPLETED){
            //  Completion should not result in a message.
            return createSuccessNoOutputToQueue();
        } else {
            return createFailureResult(result);
        }
    }

    /**
     * Reports the progress of the tracked task specified in the JobTrackingWorkerTask for which this Job Tracking
     * Worker instance was created.
     * @return indicator of successful reporting or otherwise
     * @throws InterruptedException
     */
    private JobTrackingWorkerResult processTrackingEvent() throws InterruptedException, TaskRejectedException
    {
        LOG.info("Starting report update work");
        checkIfInterrupted();

        // Report progress on the report updates specified in task.
        final List<JobTrackingWorkerDependency> jobDependencyList;
        try {
            jobDependencyList = reportJobTasksProgress(getTask());
        } catch (final JobReportingTransientException jrte) {
            LOG.warn("Transient error detected reporting progress on the list of job tasks specified to the Job Database: ", jrte);
            throw new TaskRejectedException("Failed to report progress on the list of job tasks specified.");
        } catch (final JobReportingException e) {
            LOG.warn("Error reporting task progress to the Job Database: ", e);
            return JobTrackingWorkerUtil.createErrorResult(JobTrackingWorkerStatus.PROGRESS_UPDATE_FAILED);
        }

        //  As a result of reporting completion of job tasks, a number of dependent jobs
        //  may be ready for execution. If so, forward these to the tracking pipe.
        if (jobDependencyList != null && !jobDependencyList.isEmpty()) {
            //  For each dependent job, create a TaskMessage object and publish to the
            //  messaging queue.
            for (final JobTrackingWorkerDependency dependency : jobDependencyList) {
                final TaskMessage dependentJobTaskMessage =
                        JobTrackingWorkerUtil.createDependentJobTaskMessage(dependency, workerTask.getTo());
                workerTask.sendMessage(dependentJobTaskMessage);
            }
        }

        final JobTrackingWorkerResult workerResult = new JobTrackingWorkerResult();
        workerResult.setStatus(JobTrackingWorkerStatus.COMPLETED);
        return workerResult;
    }

    private List<JobTrackingWorkerDependency> reportJobTasksProgress(final TrackingReportTask trackingReportTask)
            throws JobReportingException
    {
        final List<JobTrackingWorkerDependency> jobDependencyList = new ArrayList<>();
        if (trackingReportTask != null){
            //  Identify report updates to process.
            final List<TrackingReport> trackingReportList = trackingReportTask.trackingReports;

            //  Iterate through each report update and record progress in the DB.
            for (final TrackingReport trackingReport : trackingReportList) {
                final String jobTaskId = trackingReport.jobTaskId;
                final TrackingReportStatus trackingReportStatus = trackingReport.status;

                final List<JobTrackingWorkerDependency> jobTaskCompletionDependencyList;

                //  Check report update status.
                if (trackingReportStatus == TrackingReportStatus.Complete) {
                    //  Job task is complete so flag as complete in the database and identify any dependent
                    //  jobs that can now run.
                    jobTaskCompletionDependencyList = reportJobTaskAsComplete(jobTaskId);

                    //  Check if there are any dependent jobs now ready to run as a result of job completion.
                    if (jobTaskCompletionDependencyList != null && !jobTaskCompletionDependencyList.isEmpty()) {
                        //  Append dependency job id to overall list of dependents jobs now
                        //  available to run..
                        jobDependencyList.addAll(jobTaskCompletionDependencyList);
                    }
                } else if (trackingReportStatus == TrackingReportStatus.Progress) {
                    //  Job task is still in progress so flag as still active in the database.
                    reportJobTaskAsInProgress(jobTaskId);
                } else if (trackingReportStatus == TrackingReportStatus.Failed) {
                    //  Job task has failed so flag as much in the database.
                    reportJobTaskAsRejected(trackingReport);
                } else if (trackingReportStatus == TrackingReportStatus.Retry) {
                    //  Job task is to be retried so flag as a retry and update retry count.
                    reportJobTaskAsRetry(jobTaskId, trackingReport.retries);
                } else {
                    final String errorMessage = "Unexpected report update status received.";
                    LOG.debug(errorMessage);
                    throw new JobReportingException(errorMessage);
                }
            }
        }

        return jobDependencyList;
    }

    private List<JobTrackingWorkerDependency> reportJobTaskAsComplete(final String jobTaskId)
            throws JobReportingException
    {
        return reporter.reportJobTaskComplete(jobTaskId);
    }

    private void reportJobTaskAsInProgress(final String jobTaskId)
            throws JobReportingException
    {
        reporter.reportJobTaskProgress(jobTaskId, 0);
    }

    private void reportJobTaskAsRejected(final TrackingReport trackingReport)
            throws JobReportingException
    {
        final JobTrackingWorkerFailure f = new JobTrackingWorkerFailure();
        f.setFailureId(trackingReport.failure.failureId);
        f.setFailureTime(trackingReport.failure.failureTime);
        f.setFailureSource(trackingReport.failure.failureSource);
        f.setFailureMessage(trackingReport.failure.failureMessage);
        reporter.reportJobTaskRejected(trackingReport.jobTaskId, f);
    }

    private void reportJobTaskAsRetry(final String jobTaskId, final int retries)
            throws JobReportingException
    {
        final String retryDetails =
                MessageFormat.format("This job task encountered a problem and will be retried. This will be retry " +
                        "attempt number {0} for this job task.", retries);
        reporter.reportJobTaskRetry(jobTaskId, retryDetails);
    }
}
