# worker-jobtracking
The job tracking worker reports the progress of jobs to the Job Database. It does this by acting as a proxy for task messages that are
intended for ordinary workers, reporting the message before forwarding it to the intended worker in each case. It can also receive
progress event messages sent explicitly to it.

Messages will typically arrive at the Job Tracking Worker because the queue that it is consuming messages from is specified as the
trackingPipe in a task message, which will trigger the Worker Framework to re-route the message to the Job Tracking Worker. The worker
checks for task cancellation, reports the progress of the task to the Job Database, and forwards the message to the correct destination
queue, e.g. the input queue of another worker.

The Job Service has the ability to execute jobs in an order defined by a job having a dependency on another job.  The Job Service will not execute a job until all the jobs it is dependent upon have completed.  The job tracking worker monitors the progress of jobs, once all dependent jobs have been completed the job tracking worker will automatically forward on the jobs for execution which were previously waiting upon dependent jobs.
