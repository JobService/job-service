/*
 * Copyright 2016-2020 Micro Focus or one of its affiliates.
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

import javax.validation.constraints.NotNull;

class WorkerTaskBulkItem {
    @NotNull
    final String jobId;
    @NotNull
    final private WorkerTask workerTask;
    @NotNull
    final private String partitionId;


    public WorkerTaskBulkItem(@NotNull final WorkerTask workerTask, @NotNull final String partitionId,
                              @NotNull final String jobId) {
        this.workerTask = workerTask;
        this.partitionId = partitionId;
        this.jobId = jobId;
    }


    public WorkerTask getWorkerTask() {
        return workerTask;
    }

    public String getPartitionId() {
        return partitionId;
    }

    public String getJobId() {
        return jobId;
    }


}
