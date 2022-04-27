/*
 * Copyright 2016-2022 Micro Focus or one of its affiliates.
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
package com.hpe.caf.jobservice.acceptance;

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TrackingInfo;
import com.hpe.caf.services.job.client.ApiException;
import com.hpe.caf.services.job.client.api.JobsApi;
import com.hpe.caf.services.job.client.model.Failure;
import com.hpe.caf.services.job.client.model.Job;
import com.hpe.caf.worker.example.ExampleWorkerResult;
import com.hpe.caf.worker.testing.ExecutionContext;
import com.hpe.caf.worker.testing.ResultHandler;
import com.hpe.caf.worker.testing.TestItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Verifies result messages issued at the end of the end-to-end test.
 */
public class FinalOutputDeliveryHandler implements ResultHandler<TaskMessage> {

    private final Codec codec;
    private final JobsApi jobsApi;
    private final ExecutionContext context;
    private final JobServiceEndToEndITExpectation expectation;
    private final List<String> expectedWorkerItems;
    private int currentWorkerItemNumber;
    private Timer cancellationVerifier;

    private static final Logger LOG = LoggerFactory.getLogger(FinalOutputDeliveryHandler.class);


    public FinalOutputDeliveryHandler(Codec codec, JobsApi jobsApi, ExecutionContext context, JobServiceEndToEndITExpectation expectation) {
        this.codec = codec;
        this.jobsApi = jobsApi;
        this.context = context;
        this.expectation = expectation;
        this.expectedWorkerItems = expectation.getWorkerItemAssetIds();
        this.currentWorkerItemNumber = 0;
        if (expectation.isExpectJobCancellation()) {
            LOG.debug("Expecting cancellation of Job {}", expectation.getJobId());
            cancellationVerifier = getCancellationVerifier(context);
        }
    }


    @Override
    public void handleResult(final TaskMessage resultMessage) {
        try {
            currentWorkerItemNumber++;
            LOG.info("Handling result message {}", resultMessage.getTaskId());
            verifyWorkerResult(resultMessage);
            verifyTrackingInfoPresence(resultMessage);
            if (!expectation.isExpectJobCancellation()) {
                verifyJob(resultMessage);
            }
        } catch (Exception e) {
            LOG.error("Error while handling result message {}. ", resultMessage.getTaskId(), e);
            context.failed(new TestItem(resultMessage.getTaskId(), null, null), e.getMessage());
        }

        if (currentWorkerItemNumber == expectedWorkerItems.size()) {
            if (expectation.isExpectJobCancellation()) {
                String errorMessage = "Job cancellation expected so we did not expect to receive all result messages for the job";
                LOG.error(errorMessage);
                context.failed(new TestItem(resultMessage.getTaskId(), null, null), errorMessage);
            }
            context.finishedSuccessfully();
        }
    }


    private void verifyWorkerResult(final TaskMessage resultMessage) throws CodecException {
        assertEqual("worker result task status", expectation.getTaskStatus().toString(), resultMessage.getTaskStatus().toString(), resultMessage);
        assertEqual("worker task classifier", expectation.getTaskClassifier(), resultMessage.getTaskClassifier(), resultMessage);
        assertEqual("worker task API version", String.valueOf(expectation.getTaskApiVersion()), String.valueOf(resultMessage.getTaskApiVersion()), resultMessage);

        ExampleWorkerResult workerResult = codec.deserialise(resultMessage.getTaskData(), ExampleWorkerResult.class);
        assertEqual("worker result status", expectation.getWorkerResultStatus().toString(), workerResult.workerStatus.toString(), resultMessage);
    }


    private void verifyTrackingInfoPresence(final TaskMessage resultMessage) {
        TrackingInfo tracking = resultMessage.getTracking();
        if (tracking != null && tracking.getTrackTo().equals(expectation.getTrackTo())) {
            String errorMessage = "Unexpected tracking info found on a result message from the end-to-end test for job " + expectation.getJobId() + ". This message has arrived at its trackTo queue " + expectation.getTrackTo() + " so it should have had its tracking info removed.";
            LOG.error(errorMessage);
            context.failed(new TestItem(resultMessage.getTaskId(), null, null), errorMessage);
        }
    }


    private void verifyJob(final TaskMessage resultMessage) throws ApiException {
        //verifyJobActive(resultMessage);   // CAF-2567: Uncomment this line after fix
        Job job = jobsApi.getJob(
            expectation.getPartitionId(), expectation.getJobId(), expectation.getCorrelationId());
        //verifyJobStatus(resultMessage, job);  // CAF-2567: Uncomment this line after fix
        verifyJobFailures(resultMessage, job);
    }


    private void verifyJobActive(final TaskMessage resultMessage) throws ApiException {
        boolean jobIsActive = jobsApi.getJobActive(
            expectation.getPartitionId(), expectation.getJobId(), expectation.getCorrelationId());
        boolean expectJobActive = "Active".equals(getCurrentMessageExpectedJobStatus());
        assertEqual("job active", String.valueOf(expectJobActive), String.valueOf(jobIsActive), resultMessage);
    }


    private void verifyJobStatus(final TaskMessage resultMessage, final Job job) throws ApiException {
        assertEqual("job status", getCurrentMessageExpectedJobStatus(), job.getStatus().toString(), resultMessage);
    }

    private void verifyJobFailures(final TaskMessage resultMessage, final Job job) throws ApiException {
        int numFailures = job.getFailures().size();
        boolean failuresFound = numFailures > 0;
        boolean expectedFailures = "Failed".equals(getCurrentMessageExpectedJobStatus());
        if (expectedFailures != failuresFound) {
            String errorMessage = "Expected job " + expectation.getJobId() + " to have " + (expectedFailures ? "" : "no ") + "failures." + (expectedFailures ? "" : " Found " + String.valueOf(numFailures) + " failures: " + String.valueOf(getFailureMessages(job)));
            LOG.error(errorMessage);
            context.failed(new TestItem(resultMessage.getTaskId(), null, null), errorMessage);
        }
    }


    private String getFailureMessages(final Job job) {
        StringBuilder concatResult = new StringBuilder();
        for (Failure failure : job.getFailures()) {
            concatResult.append(System.lineSeparator());
            concatResult.append(failure.getFailureMessage());
        }
        return concatResult.toString();
    }


    private String getCurrentMessageExpectedJobStatus() {
        return currentMessageIsLastExpected() ? "Completed" : "Active";
    }


    private boolean currentMessageIsLastExpected() {
        return currentWorkerItemNumber == expectedWorkerItems.size();
    }


    private void assertEqual(final String valueName, final String expected, final String actual, final TaskMessage resultMessage) {
        if (!expected.equals(actual)) {
            String errorMessage = "Expected job " + expectation.getJobId() + " to have " + valueName + " = " + expected + " but it has " + valueName + " = " + actual;
            LOG.error(errorMessage);
            context.failed(new TestItem(resultMessage.getTaskId(), null, null), errorMessage);
        }
    }


    private Timer getCancellationVerifier(ExecutionContext context) {
        int allowedSecondsPerWorkItem = 5;
        long timeoutSecs = allowedSecondsPerWorkItem * expectedWorkerItems.size();
        LOG.debug("Starting cancellation timer - after {} seconds (allowing {} seconds per work item) this will verify that the job was cancelled and therefore that we did not receive all result messages.", timeoutSecs, allowedSecondsPerWorkItem);
        long timeout = 1000 * timeoutSecs;
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                LOG.debug("Cancellation timer - {} seconds have elapsed so verifying cancellation.", timeoutSecs);
                if (currentWorkerItemNumber == expectedWorkerItems.size()) {
                    String errorMessage = "Job cancellation expected so we did not expect to receive all result messages for the job";
                    LOG.error(errorMessage);
                    context.failed(new TestItem("JobId=" + expectation.getJobId(), null, null), errorMessage);
                }
                verifyJobCancelled();
                context.finishedSuccessfully();
            }
        }, timeout);
        return timer;
    }


    private void verifyJobCancelled() {
        try {
            Job job = jobsApi.getJob(
                expectation.getPartitionId(), expectation.getJobId(), expectation.getCorrelationId());
            if (!"Cancelled".equals(job.getStatus().toString())) {
                String errorMessage = "Expected job " + expectation.getJobId() + " to have status = CANCELLED but it has STATUS = " + job.getStatus();
                LOG.error(errorMessage);
                context.failed(new TestItem("JobId=" + expectation.getJobId(), null, null), errorMessage);
            }
        } catch (ApiException e) {
            LOG.error("Error while verifying job cancellation for job {}. ", expectation.getJobId(), e);
            context.failed(new TestItem("JobId=" + expectation.getJobId(), null, null), e.getMessage());
        }
    }
}
