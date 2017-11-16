---
layout: default
title: CAF Job Service Overview

banner:
    icon: 'assets/img/fork-lift.png'
    title: Job Service
    subtitle: Orchestration, Management and Monitoring of Data Processing
    links:
        - title: GitHub
          url: https://github.com/JobService/job-service
---

# Overview

The Job Service is a RESTful web service that makes it easy for you to use and receive feedback from Worker Framework microservices (workers).

## Introduction
The Worker Framework provides many different types of workers, but they all perform tasks asynchronously in the background. The Job Service can send tasks to these workers, check the progress on those tasks, and cancel them, if they have not executed yet. The Job Service complements the Worker Framework by making its functionality more readily available, and providing a standard mechanism for interacting with workers. For example, using the Job Service relieves you from having to concern yourself with which messaging framework the workers use for communication.

**_Future: We might enhance the Job Service to provide further, more fine-grained control over the tasks that it sends to the workers. For example, the Job Service could add operations to pause or resume tasks._**

## Job Service Web API
The Job Service web API is the RESTful web service acting as the main entry point for sending operations to workers, checking on the progress of these operations, and even allowing cancellation of these operations. The service adds job entries to the Job Service database table, which is then updated by the job tracking worker.

To see the web methods in the Job Service web API, see [API](API).

For more details on the architecture of the Job Service web API, see [Architecture](Architecture).

For instructions on deploying and using the Job Service web API, see [Getting Started](Getting-Started).

### Batch Worker
The Job Service works in conjunction with any worker from the Worker Framework, but the batch worker is the key worker most often controlled by the Job Service.

The batch worker takes batches of work, recursively breaks them down into ever smaller batches of work and ultimately individual tasks, which are then sent to other workers.

The Job Service is particularly beneficial to the batch worker because it monitors the progress of the batch as a whole. Even if the size of the batch is not known in advance, using the Job Service makes it possible to monitor the overall progress of the entire batch, as well as making other operations available, such as the capability to cancel the batch.

### Dependent Jobs
The Job Service has the ability to execute jobs in an order defined by a job having a dependency on another job.  The Job Service will not execute a job until all the jobs it is dependent upon have completed.  The job can be configured to wait for a specified length of time after all the jobs it is dependent upon have completed before itself is executed. By default, once all dependent jobs have completed the Job Service will automatically execute the job.

### Example Usages
The combination of the Job Service and the batch worker is used in a number of document processing scenarios. Here are some examples of this:

- Document Reprocessing: A set of documents that have already been ingested is put through the ingestion process again. For example, you might re-ingest because of some updates to the process).
- Document Tagging: A specified tag is associated with a set of documents.
- Document Export: A set of documents is exported, for example, to a zip file.
- Document Production: A set of documents is rendered in some format, for example, TIFF.

In all of these scenarios, the Job Service enables the user interface to provide feedback on the progress of the operations. It also allows the cancellation of operations before they have completed. Additionally, using the Job Service lets the user interface report operation failures, including the failure details.

