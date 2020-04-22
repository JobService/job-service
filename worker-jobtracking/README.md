# worker-jobtracking
The job tracking worker reports the progress of jobs to the Job Database. It does this by acting as a proxy for task messages that are
intended for ordinary workers, reporting the message before forwarding it to the intended worker in each case. It can also receive
progress event messages sent explicitly to it.

Messages will typically arrive at the Job Tracking Worker because the queue that it is consuming messages from is specified as the
trackingPipe in a task message, which will trigger the Worker Framework to re-route the message to the Job Tracking Worker. The worker
checks for task cancellation, reports the progress of the task to the Job Database, and forwards the message to the correct destination
queue, e.g. the input queue of another worker.

The Job Service has the ability to execute jobs in an order defined by a job having a dependency on another job.  The Job Service will not execute a job until all the jobs it is dependent upon have completed.  The job tracking worker monitors the progress of jobs, once all dependent jobs have been completed the job tracking worker will automatically forward on the jobs for execution which were previously waiting upon dependent jobs.

## Configuration
This section describes environment variables that can be used to change the behaviour of the worker-jobtracking.  

- `CAF_JOB_TRACKING_PROPAGATE_FAILURES`  
`description`: This environment variable will indicate if the worker should propagate job failures through subtasks. This means that if a task fails and as a results other tasks are not able to be run due to prerequisite commitments, those other tasks will be marked as failures also.
For instance, in the example below all tasks will be marked as failures because T1 failed. Possible values are `true` and `false`.  
`default`: false  
`example`: T1 has two dependent tasks, T2 and T3, T3 also has a dependent task T4. If this environment variable is set to true then the failure of T1 will cause the marking of tasks T2, T3 and T4 as failed as none of them will ever be eligible to run.  
````
T1
 |->T2
 |->T3
     |->T4
````
