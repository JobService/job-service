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
