---
layout: default
title: Overview
last_updated: Created and last modified by Conal Smith on June 14, 2016
---

# Overview

The job service provides a pluggable batch processing mechanism that allows users of the service to
devise their own batch definition and provide logic to translate a batch into smaller and smaller batches
until ultimately single items are sent for processing by worker nodes.

The batch translation process scales elastically allowing a large batch to be broken down in parallel by multiple
batch processors. The individual items are also processed elastically by workers which scale up and down depending
on the type of work to be performed on the item. Throughout all of this feedback is collected to allow a user to
inspect the progress of the batch by the job service.

### Job Service Web API

![Alt text](images/job_service_database_overview.png)

The [Job Service Web API](https://github.hpe.com/caf/job-service) is a REST web service which has methods to create,
retrieve, update, delete, cancel and check the status of jobs. It is the main entry point for users wishing to send
operations to workers, check on the progress of these operations, and even allow for these operations to be cancelled,
paused or resumed.

The job service runs with a base Tomcat image and provides a friendly user interface for making requests which can be
accessed at `http://<host>:<port>/job-service-ui`. For more details on this visit [Job Service UI](https://github.hpe.com/caf/job-service-ui)
and for deployment information see [Getting Started](https://github.hpe.com/caf/job-service-container/blob/develop/documentation/getting-started.md).

### Job Tracking Worker

The Job Tracking Worker is a CAF Worker which receives messages from the tracking pipe
and performs the following functions:

1. Checks for task cancellation (as all workers do)
2. Reports the progress of the task to the Job Database, Either marking task complete in
the job database or else marking the task in progress.
3. If the job is paused, forwards the message to a Job-specific paused pipe
4. If the job is active, forwards messages to the correct destination pipe
5. Allows for more effective caching of job status
6. Can also accept Progress Update messages.

For more intricate details visit the [Job Tracking Worker Repository](https://github.hpe.com/caf/worker-jobtracking).