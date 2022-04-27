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
package com.hpe.caf.worker.jobtracking;

import com.hpe.caf.api.worker.WorkerTask;
import com.hpe.caf.services.job.util.JobTaskId;

import java.util.List;
import java.util.Objects;

/**
 * A sorted map of these objects is used in the {@link JobTrackingWorkerFactory} class when processing messages in bulk. This object holds
 * the worker task object (i.e. the message), and the completed task ids that it contains, and also whether or not this is the final entry
 * in the sorted map which contains a reference to this worker task.
 */
final class CompletedWorkerTaskEntity
{
    private final WorkerTask workerTask;
    private final List<JobTaskId> completedTaskIds;
    private final boolean finalJob;

    public CompletedWorkerTaskEntity(final WorkerTask workerTask, final List<JobTaskId> completedTaskIds, final boolean finalJob)
    {
        this.workerTask = Objects.requireNonNull(workerTask);
        this.completedTaskIds = Objects.requireNonNull(completedTaskIds);
        this.finalJob = finalJob;
    }

    public WorkerTask getWorkerTask()
    {
        return workerTask;
    }

    public List<JobTaskId> getCompletedTaskIds()
    {
        return completedTaskIds;
    }

    public boolean isFinalJob()
    {
        return finalJob;
    }
}
