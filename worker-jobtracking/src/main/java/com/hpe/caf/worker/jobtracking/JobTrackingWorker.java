/*
 * Copyright 2016-2024 Open Text.
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
import com.hpe.caf.api.worker.*;
import com.hpe.caf.worker.AbstractWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.constraints.NotNull;
import java.util.Objects;


public class JobTrackingWorker extends AbstractWorker<JobTrackingWorkerTask, JobTrackingWorkerResult> {

    private static final Logger LOG = LoggerFactory.getLogger(JobTrackingWorker.class);

    @NotNull
    private JobTrackingReporter reporter;

    /**
     * Constructor called by the JobTrackingWorkerFactory.
     */
    public JobTrackingWorker(final JobTrackingWorkerTask task, final String outputQueue, final Codec codec,
                             final JobTrackingReporter reporter, final WorkerTaskData workerTaskData) throws InvalidTaskException
    {
        super(task, outputQueue, codec, workerTaskData);
        this.reporter = Objects.requireNonNull(reporter);
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
        JobTrackingWorkerResult result = processTrackingEvent();
        if (result.getStatus() == JobTrackingWorkerStatus.COMPLETED){
            return createSuccessNoOutputToQueue();
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
            return JobTrackingWorkerUtil.createErrorResult(JobTrackingWorkerStatus.PROGRESS_UPDATE_FAILED, e.getMessage());
        }
    }

}
