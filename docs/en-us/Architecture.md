---
layout: default
title: Architecture
last_updated: Created and last modified by Conal Smith on June 14, 2016
---

# Architecture

This document outlines the high-level architecture of facilities to be added to CAF and to the CAF Worker Framework to allow for greater control over how background operations are tracked and controlled.

These enhancements should mean that it is possible to use the Worker Framework more widely, for complex operations which are expected to take a significant length of time to complete.

Users will be able to use a new REST Web Service, the CAF Job Service, to have operations sent to the Workers, to check on the progress of these operations, and even to allow the operations to be cancelled, paused, or resumed._NB: Pause and Resume are future requirements. They are unlikely to be in the first release, though some thought has been given as to how they will be implemented._

### Epic

[https://jira.autonomy.com/browse/CAF-596](https://jira.autonomy.com/browse/CAF-596)

### Requirements

The complete set of requirements are formally listed on the CAF R&D Wiki: [https://rndwiki.corp.hpecorp.net/confluence/display/CAF/Job+Service+Requirements](https://rndwiki.corp.hpecorp.net/confluence/display/CAF/Job+Service+Requirements)

#### Main Requirements

The main use cases for this functionality centre around batch document processing, so in addition to the introduction of the Job Service and the enhancements to the Worker Framework we will also introduce a new **Batch Dispatch Worker** to break up batches of work so that they can be assigned to other workers. Here are some example use cases:

- **Document Reprocessing:** We should be able to select a batch of documents that have already been through the ingestion process, and have them re-sent through the ingestion process again (perhaps because of some updates made to the process). From the UI, not only should the facility be available to select the documents to be re-ingested, but the user should receive feedback with regard to the progress of the re-ingestion _(and potentially should be allowed to cancel the operation)_.
- **Document Tagging:** We want to be able to provide a facility to append a tag to a batch of documents. Again, the user should receive progress reports on the operation.
- **Document Export:** We want to export a set of documents (into zip files for example).
- **Document Production:** We want to produce TIFF renditions of a set of documents.

#### Other Requirements

Ultimately we will need be able to:

##### Scheduling

- schedule operations to start at a specified date and time
- schedule operations to happen repeatedly, after a specified length of time has elapsed

##### History

- retain information with regard to completed jobs for a specified length of time (e.g. start time, completion time, etc.)
- retain information with regard to failed jobs for a specified length of time (e.g. failure data)
- remove retained information when it has expired

### Batch Dispatch Worker

(or should we just call this Batch Worker?)

This will be an unusual worker in that, much like the Keyview Worker (when it is installed with the Policy Plugin), it may dispatch multiple messages for each message that it receives. (Most workers only dispatch a single Completion or Failure message for each work packet that is sent to them.)

#### Batch Worker Message Structure

In common with all of the other workers, we will define the structure of the messages to be sent in a **worker-batch-shared** package.

batchDefinition: String
batchType: String
taskMessageType: String
taskMessageParams: Map&lt;String, String&gt;
targetPipe: String

| **batchDefinition**   | This is the definition of the batch. For example, it might be a string like "workbook == 5". The definition string will be interpreted by the type specified to the 'batchType' field.  **Example:** workbook == 5                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
|-----------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **batchType**         | This is the type that is used to interpret the batch definition string. The Batch Worker will need to create an instance of this class to interpret the definition string, so the specified class must be made available on the Batch Worker's classpath. We should be able to make these types available as a mounted tar, in the same way that we make the Policy Handlers and Converters available to the Policy Worker.  **Note:** We should really use a simple type locator mechanism here, so that a string like "DocumentBatch" can be specified rather than a string like "com.hpe.caf.worker.batch.plugins.document".                                                                                                                                                                                                               |
| **taskMessageType**   | This is a factory type that is used to construct the TaskMessage for each item of the batch. This string will be passed to the instance of the 'batchType' object and it will need to be able to create an instance of this class, so only types which have been deployed may be specified.  Obviously this type is highly tied to the 'targetPipe' field, in that the messages produced by this object must be compatible with the workers they are being sent to.  The interface that this type must implement, and the services provided by the 'batchType' plugin to it, are not defined at the BatchWorker level - that is an internal detail between the two objects.  **Note:** I think we should change the type of this field from the simple String that it currently is to a structure which specifies both a type and parameters. |
| **taskMessageParams** | This is a set of named parameters to be passed to the specified TaskMessage builder (i.e. the factory type specified by the 'taskMessageType' parameter). Their meaning is dependant on the type specified.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| **targetPipe**        | A message is constructed for each item of the batch. This field specifies the pipe (channel or queue) where these per-item messages are to be forwarded to.  **Example:** workflow-in                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |

#### Interfaces

##### BatchWorkerPlugin

This interface is implemented by the type specified to the 

batchType field. Its 

processBatch() method is called by the Batch Worker and is expected to interpret the batch definition string, and to make multiple calls to the supplied

BatchWorkerServices object in line with how the batch needs to be broken down.

It must also return an identifying string that will be used in the batch type field of the BatchWorkerTask to load the plugin.

```
interface BatchWorkerPlugin
{
    void processBatch (
        BatchWorkerServices bwServices,
        String batchDefinition,
        String taskMessageType,
        Map&lt;String, String&gt; taskMessageParams
    );

    String getIdentifier();
}
```

##### BatchWorkerServices

An implementation of this type is contained in the Batch Worker code.
It is passed to the 

batchType object so that it can utilise services provided by the Batch Worker.

interface BatchWorkerServices
{
    // Creates a new sub-task which represents a smaller batch
    void registerBatchSubtask(String batchDefinition, String batchType, String taskMessageType, Map&lt;String, String&gt; taskMessageParams, String targetPipe);

    // Creates a new sub-task for a single item
    void registerItemSubtask(String taskClassifier, int taskApiVersion, Object taskData);

}

#### Batch Worker Walk-through

When the Batch Worker receives a batch to process, it first constructs an instance of the type that is specified by the 

batchType field. This type should implement the 

BatchWorkerPlugin interface.

It then constructs its own internal 

BatchWorkerServicesImpl object - an object which implements the 

BatchWorkerServices interface.

It calls 

processBatch(), passing in the services object and the other parameters.

The implementation of the 

processBatch() method will interpret the batch definition string and split it up into either:

1. a set of batch definitions representing smaller batches, OR
2. a set of items

If it determines to split the batch into a set of smaller batches, then it will make a series of calls to the 

registerBatchSubtask() method. The Batch Worker will construct messages which are to be directed back towards itself and dispatch them to the input pipe that the Batch Worker itself is listening on (not to the pipe specified by the 

targetPipe field).

If it instead determines to split the batch into a set of items, then it will first construct an instance of the type that is specified by the 

taskMessageFactory field, and then use it to generate task messages that are appropriate to be sent to the worker listening on the 

targetPipe. It will call the 

registerItemSubtask() method for each item, and the Batch Worker will dispatch the messages to the pipe specified by the 

targetPipe field.

**Note:** It strikes me that the 

taskMessageFactory object is performing a role very similar to that performed by the Policy Handlers. We may require one of these objects for each potential destination worker. Not sure if this is a valid observation, or whether there is any mileage in trying to rationalise the concepts.

**Note:** We might want to include some code in the Batch Worker to ensure that a faulty plugin can't turn this into an infinite loop, where a faulty Batch Plugin returns same size or larger batches. Possibly add a 'Nesting Level' field, and then abort messages that have been through the mill.

#### Subtask Identification

I haven't yet described the tracking fields that we will add, but note that if they are present, that the implementation of the registerSubtask() methods must indicate that the task is a subtask by appending a subtask identifier to the taskId. So if the taskId was J5.1 and this is the second subtask, it should be assigned the taskId J5.1.2. Also note that the subtask's 

trackTo pipe need not necessarily be the same as the parent task's 

trackTo pipe, if indeed it has one.

_**TODO:** Think about whether we should also introduce a concept of relative weights, or can we just assume that all subtasks have equal weight (when it comes to Percentage Complete reporting)._

### Worker Framework Enhancements

In order to facilitate the Progress Reporting and Job Control requirements we need to make some enhancements to the Worker Framework itself.

These are the fields which are currently in the 

TaskMessage structure:

taskId: String    -    What is this?
taskClassifier: String
taskApiVersion: int
taskData: byte[]
taskStatus: TaskStatus
context: Map&lt;String, byte[]&gt;

#### Tracking Fields

We will add additional 'tracking' fields to the 

TaskMessage structure.

Additional fields to be added:

tracking: TrackingInfo

TrackingInfo {
    taskId: String
    statusCheckTime: dateTime
    statusCheckUrl: String
    trackingPipe: String
    trackTo: String
}

| **taskId**          | This is an identifier assigned for tracking the task.Depending on what the other taskId is maybe we don't need this. I envisage this to be the letter J followed by the job id, and then possibly followed by multiple numeric subtask ids separated by periods.  **Example:** J5.1.2                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
|---------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **statusCheckTime** | This is the time after which it is appropriate to try to confirm that the task has not been cancelled or aborted.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| **statusCheckUrl**  | This is the url to use to check whether the job has been cancelled or aborted.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| **trackingPipe**    | This is the pipe output messages relating to this task should be sent, regardless of their nature (i.e. whether they are Reject messages, Retry messages, Response messages, or some other type of message). It is the responsibility of the Job Tracking Worker, which will be consuming messages sent to this pipe, to forward the message to the intended recipient, which is indicated by the 'to' field (mentioned later).  **Note:** One exception to this is where the tracking pipe specified is the same pipe that the worker itself is consuming messages from. If this is the case then the tracking pipe should be ignored. It likely means that this is the Job Tracking Worker. Not making an exception for this case would cause to an infinite loop. |
| **trackTo**         | This is the pipe where tracking is to stop. If the Worker Framework is publishing a message to this pipe then it should remove the 'tracking' fields, as we are not interested in tracking from this point.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |

> **Note to self:** It looks like we're inadvertently going to end up here being able to create Jobs by simply creating a TaskMessage with 'tracking' info set - i.e. if you just make up your own job id.

When a Worker receives a message to be processed then the Worker Framework will first compare the current time to the time specified in the `statusCheckTime` field. If the expiry time has passed then the `statusCheckUrl` is used to re-check the job status.

- If the job has been cancelled or aborted then the message is simply discarded and not processed.
- If the specified url could not be reached for some reason then this is logged as a warning, but the work package is still progressed on the assumption that the job is still active.
- If the job has been confirmed to still be active, then the work package is progressed, and when the Worker ultimately dispatches another message relating to this task, it will have the `statusCheckTime` field updated to a new value, so that there is a chance that a downstream worker will not also have to repeat this work and check the job status again.
    - **Qustions:** Where should we define the length of time to wait between status checks? Perhaps it should be another field in the message itself? Or it might be better if the service returned it with the status?
    - **Note:** We could and probably should cache a small number of recently retrieved Job Id statuses within the Worker Framework itself, as the same Worker could receive multiple work packages belonging to the same job and may already know the updated status without having to use the `statusCheckUrl`.

#### Message Forwarding

We will add an additional `to` field the structure:

`to: String`

This field will be automatically set by the Worker Framework and is the destination pipe where the sender intends the message to be sent. Note that whilst the `taskClassifier` field indicates the shape of the message, there could still be multiple pools of workers capable to processing a message of a given shape, so this new `to` field is simply being explicit about the target pipe.

After the Worker Framework has checked that the task is still active, but before it instantiates the actual Worker code, it will check the `to` field, to confirm that the message was actually intended for this worker.

- If the message was intended for it (i.e. if the `to` field is set to the pipe that the Worker is consuming) then the Worker Framework will continue to process the message as normal, using the existing interfaces.
- If the message was not intended for it (i.e. if the `to` field is not set to the pipe that the Worker is consuming), then the Worker Framework will check if the Worker code supports a new interface (to be defined) and will take instructions through this interface with regard to how the message should be processed. The worker code should be able to examine the message, and will be able to instruct the framework to:
    - discard the message
    - publish the message to the destination pipe (this should be the default)

### Job Tracking Worker

The Job Tracking Worker is special in that it is both a normal Worker that receives messages that were intended for it (although they are Event Messages rather than Document Messages), and it is also acts as a Proxy, routing messages that were not ultimately intended for it to the correct Worker (although the actual message forwarding will be done by Worker Framework code).

Messages will typically arrive at the Job Tracking Worker because the pipe that it is consuming messages from is specified as the `trackingPipe` (which will trigger the Worker Framework to re-route output messages).

This Worker performs the following functions:

1. Checks for task cancellation (as all workers do)
2. Reports the progress of the task to the Job Database
3. If the job is paused, forwards the message to a Job-specific paused pipe _[Ignore for v1 implementation]_
4. If the job is active, forwards messages to the correct destination pipe
5. Allows for more effective caching of job status (inc. potential bulk update) _[Ignore for v1 implementation]_
6. Can also accept Progress Update messages. _[Ignore for v1 implementation]_

#### Progress Reporting

When the Job Tracking Worker receives a success message which is to be proxied (i.e. one where the `taskStatus` is `RESULT_SUCCESS` or `NEW_TASK` and the `to` field is not the pipe that the worker itself is listening on), then it needs to check whether the `trackTo` pipe is the same as the `to` pipe.

- If it is, then the task is complete, and it should be marked complete in the Job Database.
- If it is not, then the Job Database should be updated to reflect that the task is still progressing but is not complete. Unfortunately we can only tell that the task is progressing; we cannot get an estimated percentage completion, as we don't know how many more workers it will have to go through before it gets to the `trackTo` pipe, or how long each worker will take.

The Job Tracking Worker should be able to recognise Failure messages and Retry messages which are being proxied, and update the Job Database appropriately.

_[The Partial Progress Updating functionality described in this paragraph is not to be included in the initial implementation]._ The Job Tracking Worker should also be able to receive Progress Update messages sent directly to it by workers which have been updated to do so. These are messages which simply contain the task identifier and an estimated Percentage Completion. The Job Tracking Worker should update the Job Database accordingly. It might make sense to send always send a 0% Complete message from the Worker Framework before a Worker starts processing a task, but apart from that it would of course only make sense to update those workers which tend to require a lot of time to progress tasks (such as the Speech Worker for example).

### Job Database

We will use a **PostgreSQL** database to store the Job information.

#### Job Table

This table stores information on the jobs that are requested. Entries will be added by the Job Service and updated by the Job Tracking Worker.

| **Column**     | **Data Type** | **Nullable?** | **Primary Key?** |
|----------------|---------------|---------------|------------------|
| CreateDate     | DateTime      |               |                  |
| FailureDetails | ---           | Yes           |                  |
| IsComplete     | Boolean       |               |                  |
| JobId          | String        |               | Yes              |

#### Task Tables

The task tables will have the same structure as the Job Table. There will be one task table per job. It will be created when the first subtask is reported, and deleted when the job has completed successfully. If the job fails we will retain it for a period of time for examination.

When a task is marked complete, we need to check whether it means that the parent task (or the job if it is the top level) can also be marked complete.

### Job Service

We will introduce a new REST Web Service known as the Job Service.

It will act as the entry-point to allow background operations such as the bulk document processing operations to be initiated, tracked, and cancelled.

The specification is defined in the swagger.yaml file in the job-service-contract project. _It was written as a wrapper around Chronos so it will need to be updated for some of the changes here._

The Job Service itself will be stateless, so that it can be auto-scaled in future.