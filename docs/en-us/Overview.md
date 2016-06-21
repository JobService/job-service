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
and for deployment information see [Getting Started](https://github.hpe.com/caf/job-service-container/blob/develop/docs/en-us/getting-started.md).

### Job Tracking Worker

The Job Tracking Worker is a component of the bulk document processing system. It is a CAF Worker which receives messages from the tracking pipe
and performs the following functions:

1. Checks for task cancellation (as all workers do)
2. Reports the progress of the task to the Job Database, Either marking task complete in
the job database or else marking the task in progress.
3. If the job is paused, forwards the message to a Job-specific paused pipe
4. If the job is active, forwards messages to the correct destination pipe
5. Allows for more effective caching of job status
6. Can also accept Progress Update messages.

For more intricate details visit the [Job Tracking Worker Repository](https://github.hpe.com/caf/worker-jobtracking) or for more information of its role within the bulk document processing system see the [CAF Job Service Architecture](https://github.hpe.com/caf/job-service-container/blob/develop/docs/en-us/Architecture.md) page.

### Job Database

You can install the job database on your own system using the job-service-db-installer.jar, instructions are described in [Getting Started](https://github.hpe.com/caf/job-service-container/blob/develop/docs/en-us/Getting-Started.md)

The Job table stores information on the jobs that are requested. Entries will be added by the Job Service Web API and updated by the Job Tracking Worker. 

| **Column**     | **Data Type** | **Nullable?** | **Primary Key?** |
|----------------|---------------|---------------|------------------|
| CreateDate     | DateTime      |               |                  |
| FailureDetails | ---           | Yes           |                  |
| IsComplete     | Boolean       |               |                  |
| JobId          | String        |               | Yes              |

Task tables have the same structure as the job table. There is one task table per job which is created when the first subtask is reported, and deleted when the job has completed successfully.

For more information on the Job Service database see [Job Service DB](https://github.hpe.com/caf/job-service-db).

### Batch Worker

The Batch Worker is used to break up large batches of documents into smaller items which can be ingested into the job system. 

For more information on the Batch Worker go to [Batch Worker](https://github.hpe.com/caf/worker-batch) or see the [CAF Job Service Architecture](https://github.hpe.com/caf/job-service-container/blob/develop/docs/en-us/Architecture.md) page for more information on its role within the bulk document processing system.

### Use Case Scenarios

#### Document Reprocessing

Suppose a user has a set of documents they want to re-ingest into the system. They can select a batch of documents that have already been through the ingestion process and have them re-sent through the ingestion process again (perhaps some updates were made to the process). From the Job Service UI the documents can be selected to be re-ingested and the user can receive feedback with regard to the process of the re-ingestion. The user can also cancel the operation.

#### Document Tagging

A user can append a tag to a batch of documents and receive progress reports on the operation.

#### Document Export

A user can export a set of documents into zip files.

#### Document Production

A user can produce TIFF renditions of a set of documents.
