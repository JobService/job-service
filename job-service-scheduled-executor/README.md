# Job Service Scheduled Executor

The Job Service Scheduled Executor is a polling service that identifies jobs in the system that depend on other jobs which are now complete.

### Dependent Jobs
The Job Service has the ability to execute jobs in an order defined by a job having a dependency on another job.  The Job Service will not execute a job until all the jobs it is dependent upon have completed.  The job can be configured to wait for a specified length of time after all the jobs it is dependent upon have completed before it is executed. 

### ExecutorService
The Job Service Scheduled Executor is an ExecutorService which schedules a task to execute repeatedly identifying jobs which are now ready to run. For each job identified, a message is published on to RabbitMQ to start the job.