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
staging queue name.
`default`: ^tenant-(.+)$  
`example`: If the pattern is `^tenant-(.+)$` and the partition ID is `tenant-acmecorp`, the tenant ID extracted from this partition
ID will be `acmecorp`.

- `CAF_WMP_TARGET_QUEUE_NAMES_PATTERN`   
`description`: Only applies when `CAF_WMP_ENABLED` is true. Used to specify the target queue names pattern. This pattern is used
by the Job Service Scheduled Executor to check whether it should reroute a message to a staging queue or not. Only messages destined for 
target queues that match this pattern will be rerouted to staging queues.  
`default`: ^(?>dataprocessing-.*-in|worker-grammar-in)$  
`example`: If the pattern is `^(?>dataprocessing-.*-in|worker-grammar-in)$` and a message has a target queue name of
`dataprocessing-langdetect-in`, then the message will be rerouted to a staging queue. If however, the target queue name is 
`production-batch-in`, then the message will not be rerouted to a staging queue, and instead will be sent to the target queue.

- `CAF_WMP_USE_TARGET_QUEUE_CAPACITY_TO_REROUTE`  
`description`: Only applies when `CAF_WMP_ENABLED` is true. Determines whether the Job Service Scheduled Executor should use the target
queue's capacity when making a decision on whether to reroute a message. If true, a message will only be rerouted to a staging 
queue if the target queue does not have capacity for it. If false, a message will **always** be rerouted to a staging queue,
irregardless of the target queue's capacity.  
`default`: false

- `CAF_WMP_KUBERNETES_NAMESPACES`  
`description`: Used to specify the Kubernetes namespaces, comma separated, in which to search for a worker's labels. These labels
contain information about each worker's target queue, such as its name and maximum length. A non-null and non-empty value must be
provided for this environment variable if `CAF_WMP_USE_TARGET_QUEUE_CAPACITY_TO_REROUTE` is true. If
`CAF_WMP_USE_TARGET_QUEUE_CAPACITY_TO_REROUTE` is false, this environment variable is not used.  
`default`: None

- `CAF_WMP_KUBERNETES_LABEL_CACHE_EXPIRY_MINUTES`   
`description`: Used to specify the 'expire after write' minutes after which a Kubernetes label that has been added to the cache
should be removed. Set this to 0 to disable caching. Only used when `CAF_WMP_USE_TARGET_QUEUE_CAPACITY_TO_REROUTE` is true. If
`CAF_WMP_USE_TARGET_QUEUE_CAPACITY_TO_REROUTE` is false, this environment variable is not used.  
`default`: 60


