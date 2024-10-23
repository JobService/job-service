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
