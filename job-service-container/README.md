# Job Service Container

This docker image contains the [Job Service](../job-service) hosted in Apache Tomcat 8.

[Job-service-deploy](https://github.com/JobService/job-service-deploy) can be used to deploy this container on Docker.

## Job Service Links

[Overview](https://jobservice.github.io/job-service/pages/en-us/Overview)

[Getting Started](https://jobservice.github.io/job-service/pages/en-us/Getting-Started)

[API](https://jobservice.github.io/job-service/pages/en-us/API)

[Features](https://jobservice.github.io/job-service/pages/en-us/Features)

[Architecture](https://jobservice.github.io/job-service/pages/en-us/Architecture)

## Feature Testing
The testing for Job Service is defined [here](../testcases)  

### Configuration  

- `CAF_JOB_SERVICE_PROPAGATE_FAILURES`  
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
