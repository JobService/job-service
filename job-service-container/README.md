# Job Service Container

This docker image contains the [Job Service](../job-service) hosted using the Dropwizard framework.

[Job-service-deploy](https://github.com/JobService/job-service-deploy) can be used to deploy this container on Docker.

## Job Service Links

[Overview](https://jobservice.github.io/job-service/pages/en-us/Overview)

[Getting Started](https://jobservice.github.io/job-service/pages/en-us/Getting-Started)

[API](https://jobservice.github.io/job-service/pages/en-us/API)

[Features](https://jobservice.github.io/job-service/pages/en-us/Features)

[Architecture](https://jobservice.github.io/job-service/pages/en-us/Architecture)

## Feature Testing
The testing for Job Service is defined [here](../testcases)

### Environment Variables

##### JOB\_SERVICE\_LIVENESS\_INITIAL\_DELAY\_DURATION
The initial delay to use when first scheduling the liveness check. Default value: 15s

##### JOB\_SERVICE\_LIVENESS\_CHECK\_INTERVAL\_DURATION
The interval on which to perform a liveness check for while in a healthy state. Default value: 60s

##### JOB\_SERVICE\_LIVENESS\_DOWNTIME\_INTERVAL\_DURATION
The interval on which to perform a liveness check for while in an unhealthy state. Default value: 60s

##### JOB\_SERVICE\_LIVENESS\_FAILURE\_ATTEMPTS
The threshold of consecutive failed attempts needed to mark the liveness check as unhealthy (from a healthy state). Default value: 3

##### JOB\_SERVICE\_LIVENESS\_SUCCESS\_ATTEMPTS
The threshold of consecutive successful attempts needed to mark the liveness check as healthy (from an unhealthy state). Default value: 1

##### JOB\_SERVICE\_READINESS\_INITIAL\_DELAY\_DURATION
The initial delay to use when first scheduling the readiness check. Default value: 15s

##### JOB\_SERVICE\_READINESS\_CHECK\_INTERVAL\_DURATION
The interval on which to perform a readiness check for while in a healthy state. Default value: 60s

##### JOB\_SERVICE\_READINESS\_DOWNTIME\_INTERVAL\_DURATION
The interval on which to perform a readiness check for while in an unhealthy state. Default value: 60s

##### JOB\_SERVICE\_READINESS\_FAILURE\_ATTEMPTS
The threshold of consecutive failed attempts needed to mark the readiness check as unhealthy (from a healthy state). Default value: 3

##### JOB\_SERVICE\_READINESS\_SUCCESS\_ATTEMPTS
The threshold of consecutive successful attempts needed to mark the readiness check as healthy (from an unhealthy state). Default value: 1

