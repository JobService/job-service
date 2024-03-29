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

import java.util.Collections;
import java.util.List;

/**
 * Implements the JobTrackingReporter interface. For most methods this is just a pass-through to an underlying JobTrackingReporter
 * implementation. But for the {@link #reportJobTaskComplete} method it instead adds the task identifier to the supplied list. This is to
 * allow these task ids to be reported in bulk at a later stage.
 */
final class JobTrackingReporterPartialProxy implements JobTrackingReporter
{
    private final JobTrackingReporter reporter;
    private final List<String> completedTaskIds;

    public JobTrackingReporterPartialProxy(final JobTrackingReporter reporter, final List<String> completedTaskIds)
    {
        this.reporter = reporter;
        this.completedTaskIds = completedTaskIds;
    }

    @Override
    public void reportJobTaskProgress(final String jobTaskId, final int estimatedPercentageCompleted) throws JobReportingException
    {
        reporter.reportJobTaskProgress(jobTaskId, estimatedPercentageCompleted);
    }

    @Override
    public List<JobTrackingWorkerDependency> reportJobTaskComplete(final String jobTaskId) throws JobReportingException
    {
        completedTaskIds.add(jobTaskId);
        return Collections.emptyList();
    }

    @Override
    public List<JobTrackingWorkerDependency> reportJobTasksComplete(
        final String partitionId,
        final String jobId,
        final List<String> jobTaskIds
    )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reportJobTaskRetry(final String jobTaskId, final String retryDetails) throws JobReportingException
    {
        reporter.reportJobTaskRetry(jobTaskId, retryDetails);
    }

    @Override
    public void reportJobTaskRejected(final String jobTaskId, final JobTrackingWorkerFailure rejectionDetails)
        throws JobReportingException
    {
        reporter.reportJobTaskRejected(jobTaskId, rejectionDetails);
    }

    @Override
    public boolean verifyJobDatabase()
    {
        return reporter.verifyJobDatabase();
    }
}
