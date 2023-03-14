# Job Service Scheduled Executor

The Job Service Scheduled Executor is a polling service that identifies jobs in the system that depend on other jobs which are now complete.

### Dependent Jobs
The Job Service has the ability to execute jobs in an order defined by a job having a dependency on another job.  The Job Service will not execute a job until all the jobs it is dependent upon have completed.  The job can be configured to wait for a specified length of time after all the jobs it is dependent upon have completed before it is executed. 

### ExecutorService
The Job Service Scheduled Executor is an ExecutorService which schedules a task to execute repeatedly identifying jobs which are now ready to run. For each job identified, a message is published on to RabbitMQ to start the job.  

### Configuration  

- `CAF_JOB_SCHEDULER_PROPAGATE_FAILURES`  
`description`: This environment variable will indicate if the service should propagate job failures through subtasks. This means that if a task fails and as a result other tasks are not able to be run due to prerequisite commitments, those other tasks will be marked as failures also.
For instance, in the example below all tasks will be marked as failures because T1 failed. Possible values are `true` and `false`.  
`default`: false  
`example`: T1 has two dependent tasks, T2 and T3, T3 also has a dependent task T4. If this environment variable is set to true then the failure of T1 will cause the marking of tasks T2, T3 and T4 as failed as none of them will ever be eligible to run.  
````
T1
 |->T2
 |->T3
     |->T4
````
- `JOB_SERVICE_MESSAGE_OUTPUT_FORMAT`  
  `description`: This environment variable controls the format of the message being published by the scheduled-executor to the queue messaging system . It takes a String value which can be `V3` or `V4`.  
  The difference between those 2 formats is that **taskData** is _base64 encoded_ for `V3` while it's in plain json for `V4`.  
  Its default format is `V3`

- `CAF_WMP_ENABLED`  
`description`: Determines whether the Job Service Scheduled Executor should reroute a message to a worker's staging queue or not. If 
true, a message will attempt to be rerouted. If false, a message will not be rerouted and will be sent to the target queue rather than
to a staging queue.  
`default`: false

- `CAF_WMP_PARTITION_ID_PATTERN`   
`description`: Only applies when `CAF_WMP_ENABLED` is true. Used to specify the partition ID pattern. This pattern is used
by the Job Service Scheduled Executor to extract the tenant ID from the partition ID. The tenant ID is then used to construct the
staging queue name. The pattern must contain a named group called `tenantId`, which is what is used to extract the tenant ID.  
`default`: None  
`example`: If the pattern is `^tenant-(?<tenantId>.+)$` and the partition ID is `tenant-acmecorp`, the tenant ID extracted from this 
partition ID will be `acmecorp`.

- `CAF_WMP_TARGET_QUEUE_NAMES_PATTERN`   
`description`: Only applies when `CAF_WMP_ENABLED` is true. Used to specify the target queue names pattern. This pattern is used
by the Job Service Scheduled Executor to check whether it should reroute a message to a staging queue or not. Only messages destined for 
target queues that match this pattern will be rerouted to staging queues.  
`default`: None
