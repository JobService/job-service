# Job Service Deployment

## Introduction
The Job Service is designed to provide additional tracking and control operations for applications that are built using the [Worker Framework](https://workerframework.github.io/worker-framework/).

It makes it possible to use the Worker Framework for complex operations, especially batch operations, which are expected to take a significant length of time to complete.

The Job Service is a RESTful Web Service and provides a simple API.  It tracks tasks through the Worker Framework and can be used to report on progress or on failure, as well as allowing for the cancellation of tasks if that is required.

## Deployment Repository
This repository provides the necessary files to easily get started using the Job Service.

The pre-requisites required to get started is that [Docker](https://www.docker.com/) must be available on the system and either Marathon/Mesos or Kubernetes.

As well as the Job Service and the Job Service Database the deployment files reference several other services.  These are just simple workers, built using the Worker Framework, that are included to provide a simple but complete demonstration of the Job Service.

## Demonstration
The deployment files contain the following services:

![](images/job-service-deploy.png)

1. Job Service  
    This is the Job Service itself.  As discussed it is a RESTful Web Service and is the primary service being demonstrated here.

    By default port 9411 is used to communicate with the Job Service but if that port is not available then the `JOB_SERVICE_PORT` environment variable can be set to have a different port used.

2. Job Service Database  
    Internally the Job Service uses a PostgreSQL Database to store the Job Status information.  When the stack is started a `job-service-db` volume will be created to store the database files.

3. RabbitMQ  
    The Worker Framework is a pluggable infrastructure and technically it can use different messaging systems.  However it is most common for RabbitMQ to be used for messaging, and that is what is used here.

4. Job Tracking Worker  
    For simplicity the Job Tracking Worker is not shown on the diagram above.  The diagram shows messages passing directly between the workers, but in reality the messages are passed through the Job Tracking Worker, which acts as a proxy for them.  It routes them to their intended destination but it also updates the Job Service Database with the progress.  This means that the Job Service is able to provide accurate progress reports when they are requested.

5. Job Service Scheduled Executor  
    This is a polling service that identifies jobs in the system that depend on other jobs which are now complete. It is an ExecutorService which schedules a task to execute repeatedly identifying jobs which are ready to run. For simplicity, this service is not shown in the diagram but for each job identified, a message is then published on RabbitMQ in order to start the job.

6. GlobFilter Worker  
    This is a simple worker developed just for this demonstration.  It is a Batch Worker which takes in a glob-pattern as the Batch Definition.  Glob-patterns are generally fairly simple.  For example, `*.txt` means "all text files in the input folder".  Even more complex patterns like `**/t*.txt`, which means "all text files which start with the letter 't' and are in the input folder or in any subfolders of the input folder", are fairly easy to understand.  The worker produces a separate task for each file which matches the glob-pattern.

    By default the input folder is `./input-files`, which is a directory in this repository which contains a few sample text files in different languages.  A different input folder can be used by setting the `JOB_SERVICE_DEMO_INPUT_DIR` environment variable.

7. Language Detection Worker  
    This worker reads text files and determines what language or languages they are written in.  Typically it would return the result to another worker but for this demonstration it is configured to output the results to a folder.

    By default the output folder used is `./output-files`, but a different folder can be used by setting the `JOB_SERVICE_DEMO_OUTPUT_DIR` environment variable.

## Production Deployment

### Production-Marathon

The [production-marathon](production-marathon) folder contains a set of template files for the configuration and deployment of the Job Service on Mesos/Marathon. This folder contains the marathon environment and template files that are required to deploy the Job Service, Job Service Scheduled Executor and Job Tracking Worker.

### Production-Marathon-Prerequisites

The [production-marathon-prerequisites](production-marathon-prerequisites) folder is used for testing the production templates in a non-production environment. It contains Marathon templates that are required to deploy the Job Service Database and RabbitMQ. **Note:** templates are provided to run a PostgreSQL database in Marathon, whereas in a real production environment the PostgreSQL database should be set up independently, following its own production standards.

### Production-Marathon-Testing

The [production-marathon-testing](production-marathon-testing) deployment supports the deployment of the components required to smoke test a Job Service deployment on Mesos/Marathon. This folder contains the marathon environment and template files that are required to deploy the Glob Filter and Language Detection Workers.

### Production-Kubernetes

The [production-kubernetes](production-kubernetes) folder contains a set of template files for the configuration and deployment of the Job Service on Kubernetes. This folder contains the marathon environment and template files that are required to deploy the Job Service, Job Service Scheduled Executor and Job Tracking Worker.

### Production-Kubernetes-Prerequisites

The [production-kubernetes-prerequisites](production-kubernetes-prerequisites) folder is used for testing the production templates in a non-production environment. It contains Kubernetes templates that are required to deploy the Job Service Database and RabbitMQ. **Note:** templates are provided to run a PostgreSQL database in Kubernetes, whereas in a real production environment the PostgreSQL database should be set up independently, following its own production standards.

### Production-Kubernetes-Testing

The [production-kubernetes-testing](production-kubernetes-testing) deployment supports the deployment of the components required to smoke test a Job Service deployment on Kubernetes. This folder contains the marathon environment and template files that are required to deploy the Glob Filter and Language Detection Workers.
