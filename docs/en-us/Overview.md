---
layout: default
title: CAF Job Service Overview
last_updated: Created and last modified by Conal Smith on June 14, 2016
---

# CAF Job Service Overview

## Introduction

The CAF Job Service Web API is a RESTful Web Service.  Its central aim is to make it easier to use and receive feedback from the CAF Workers which have been built.  There are different types of CAF Workers, but they all perform tasks asynchronously in the background.  The Job Service can be used to send tasks to these Workers, and then to check on the progress of the tasks, and even to cancel them if they have not yet been executed.

The CAF Job Service complements the CAF Workers by making their functionality more readily available, and by providing a standard mechanism for interacting with them.  For example, by using the Job Service users do not need to concern themselves with which messaging framework the CAF Workers use for communication.

> **Future:** In the future it is likely that we will enhance the Job Service to provide further, more fine-grained control, over the tasks that it has been used to send to the Workers.  For example, we will likely add operations to allow tasks to be Paused and Resumed.

The Job service’s extensible design allows you to define a batch of work and provide a batch processor plugin that interprets a batch, splitting it into smaller batches or individual items of work upon which the service can act. 

The process of batch splitting is scaled elastically using the autoscaler, allowing sub-batches of a larger batch to be processed in parallel. The individual items are also processed elastically by workers which scale up and down depending on the type of work to be performed on the item. The individual items of work emerge and go into the RabbitMQ queue, which is defined in the job and processed by workers. Throughout all of this feedback is collected to allow a user to inspect the progress of the batch by the job service.


## Job Service Web API

The Job Service Web API is the RESTful web service acting as the main entry point for users wishing to send operations to workers, check on the progress of these operations, and even allow for these operations to be cancelled. It adds Job entries to the Job Service database table which are then updated by the Job Tracking Worker.

To see the web methods made available by the Job Service Web API see [API](https://github.hpe.com/caf/job-service-container/blob/develop/docs/en-us/API.md).

For more details on the architecture behind the Job Service Web API see [Architecture](https://github.hpe.com/caf/job-service-container/blob/develop/docs/en-us/Architecture.md).

For instructions on deploying and using the Job Service Web API see [Getting Started](https://github.hpe.com/caf/job-service-container/blob/develop/docs/en-us/Getting-Started.md).


## Batch Worker

Whilst the Job Service can be used in conjunction with any CAF Worker, a key worker that the Job Service is often used with is the Batch Worker.

The Batch Worker is designed to take batches of work, and to recursively break up these batches into ever smaller batches of work, and eventually into individual tasks which it then sends to the other CAF Workers.

The Job Service is particularly beneficial when using the Batch Worker because it makes it possible to monitor the progress of the batch as a whole.  Even if the size of the batch is not known in advance, using the Job Service makes it possible to monitor the overall progress of the entire batch, as well as making other operations available such as the capability to cancel the batch.


## Job Tracking Worker

Another underlying CAF Worker with a role in the Job Service is the The Job Tracking Worker. It receives messages from the tracking pipe and performs the following functions:

1. Checks for task cancellation (as all workers do)
2. Reports the progress of the task to the Job Database, Either marking task complete in
the job database or else marking the task in progress.
3. If the job is paused, forwards the message to a Job-specific paused pipe
4. If the job is active, forwards messages to the correct destination pipe
5. Allows for more effective caching of job status
6. Can also accept Progress Update messages.

For more intricate details visit the [Job Tracking Worker](https://github.hpe.com/caf/worker-jobtracking) repository.

For more information on its architecture and role with the Job Service see [Architecture](https://github.hpe.com/caf/job-service-container/blob/develop/docs/en-us/Architecture.md).


## Example Usages
The combination of the Job Service and the Batch Worker is used in a number of document processing scenarios.  Here are some examples of this:

- **Document Reprocessing:** Where a set of documents which have already been ingested are put through the ingestion process again (perhaps because of some updates made to the process).
- **Document Tagging:** Where a specified tag is associated with a set of documents.
- **Document Export:** Where a set of documents are exported (into zip files for example).
- **Document Production:** Where renditions of a set of documents are produced (such as TIFF renditions for example).

In all of these scenarios using the Job Service means that the UI can provide feedback with regard to the progress of the operations, and can also allow the operations to be cancelled before they have been completed.  Additionally, using the Job Service allows the UI to report if the operation fails, and if it does then it allows the UI to report the failure details.
