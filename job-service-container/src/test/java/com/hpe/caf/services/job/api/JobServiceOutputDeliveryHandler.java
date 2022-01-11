/*
 * Copyright 2016-2021 Micro Focus or one of its affiliates.
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
package com.hpe.caf.services.job.api;

import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TrackingInfo;
import com.hpe.caf.services.job.util.JobTaskId;
import com.hpe.caf.worker.testing.ExecutionContext;
import com.hpe.caf.worker.testing.ResultHandler;
import com.hpe.caf.worker.testing.TestItem;

import java.text.MessageFormat;
import java.util.Date;

/**
 * JobServiceOutputDeliveryHandler will obtain the result from the SimpleQueueConsumerImpl and this can be used for
 * assertions.
 */
public class JobServiceOutputDeliveryHandler implements ResultHandler {

    private ExecutionContext context;
    private JobServiceTrackingInfoExpectation expectation;

    public JobServiceOutputDeliveryHandler(ExecutionContext context, JobServiceTrackingInfoExpectation expectation) {
        this.context = context;
        this.expectation = expectation;
    }

    @Override
    public void handleResult(TaskMessage taskMessage) {
        TrackingInfo tracking = taskMessage.getTracking();

        if (expectation.isTrackingInfoPresent() != (tracking != null)) {
            context.failed(new TestItem(taskMessage.getTaskId(), null, null),
                    MessageFormat.format(
                            "In the forwarded task message for job task {0}, expected tracking info to be {1} but it was {2}.",
                            expectation.getJobTaskId(),
                            expectation.isTrackingInfoPresent() ? "present" : "absent",
                            expectation.isTrackingInfoPresent() ? "absent" : "present"));
        }

        if (tracking != null) {
            String trackingJobTaskId = tracking.getJobTaskId();
            final String expectMessageId =
                new JobTaskId(expectation.getPartitionId(), expectation.getJobTaskId()).getMessageId();
            if (!expectMessageId.equals(trackingJobTaskId)) {
                context.failed(new TestItem(taskMessage.getTaskId(), null, null),
                        MessageFormat.format(
                                "In the forwarded task message, expected job task ID {0} but found {1} in the tracking info.",
                                expectMessageId,
                                trackingJobTaskId));
            }

            String statusCheckUrl = tracking.getStatusCheckUrl();
            if (!expectation.getStatusCheckUrl().equals(statusCheckUrl)) {
                context.failed(new TestItem(taskMessage.getTaskId(), null, null),
                        MessageFormat.format(
                                "In the forwarded task message, expected status check URL address {0} but found {1} in the tracking info.",
                                expectation.getStatusCheckUrl(),
                                statusCheckUrl));
            }

            String trackingPipe = tracking.getTrackingPipe();
            if (!expectation.getTrackingPipe().equals(trackingPipe)) {
                context.failed(new TestItem(taskMessage.getTaskId(), null, null),
                        MessageFormat.format(
                                "In the forwarded task message, expected tracking pipe {0} but found {1} in the tracking info.",
                                expectation.getTrackingPipe(),
                                trackingPipe));
            }

            String trackTo = tracking.getTrackTo();
            if (!expectation.getTrackingTo().equals(trackTo)) {
                context.failed(new TestItem(taskMessage.getTaskId(), null, null),
                        MessageFormat.format(
                                "In the forwarded task message, expected trackTo field as {0} but found {1} in the tracking info.",
                                expectation.getTrackingTo(),
                                trackTo));
            }

            // Can't compare lastStatusCheckTime as we don't know what time it would have been set to.

            final long statusCheckIntervalMillis = tracking.getStatusCheckIntervalMillis();
            if (expectation.getStatusCheckIntervalMillis() != statusCheckIntervalMillis) {
                context.failed(new TestItem(taskMessage.getTaskId(), null, null),
                               MessageFormat.format(
                                   "In the forwarded task message, expected statusCheckIntervalMillis field as {0} but found {1} in the"
                                   + "tracking info.",
                                   expectation.getStatusCheckIntervalMillis(),
                                   statusCheckIntervalMillis));
            }
        }

        context.finishedSuccessfully();
    }
}
