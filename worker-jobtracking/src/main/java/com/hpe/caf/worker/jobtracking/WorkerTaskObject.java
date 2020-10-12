/*
 * Copyright 2020 EntIT Software LLC, a Micro Focus company.
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
import java.util.Objects;

final class WorkerTaskObject
{
    private final WorkerTask workerTask;
    private final String taskId;

    public WorkerTaskObject(final WorkerTask workerTask, final String taskId)
    {
        this.workerTask = Objects.requireNonNull(workerTask);
        this.taskId = Objects.requireNonNull(taskId);
    }

    public WorkerTask getWorkerTask()
    {
        return workerTask;
    }

    public String getTaskId()
    {
        return taskId;
    }
}
