# worker-jobtracking
The job tracking worker reports the progress of jobs to the Job Database. It does this by acting as a proxy for task messages that are intended for ordinary workers, reporting the message before forwarding it to the intended worker in each case. It can also receive progress event messages sent explicitly to it.

Messages will typically arrive at the Job Tracking Worker because the queue that it is consuming messages from is specified as the trackingPipe in a task message, which will trigger the Worker Framework to re-route the message to the Job Tracking Worker. The worker checks for task cancellation, reports the progress of the task to the Job Database, and forwards the message to the correct destination queue, e.g. the input queue of another worker.
