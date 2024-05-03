---
layout: default
title: Batch Worker Getting Started
---

# Getting Started

This guide provides instructions on creating a Batch Worker from the Batch Worker Maven Archetype and testing it using the Job Service.

## Introduction

A Batch Worker can take batches of work, recursively break them down into smaller batches of work and then ultimately individual tasks, which are sent to other workers.

The Job Service works in conjunction with any worker from the Worker Framework, but Batch Workers are workers that are most often controlled by the Job Service.

The Job Service is particularly beneficial to the Batch Worker because it monitors the progress of the batch as a whole. Even if the size of the batch is not known in advance, using the Job Service makes it possible to monitor the overall progress of the entire batch, as well as making other operations available, such as the capability to cancel the batch.

This guide covers:

- [Components of a Batch Worker Project](#components-of-a-batch-worker-project)
	- An overview of the main components that make up a Batch Worker project.
- [Creating a Batch Worker from Archetype](#creating-a-batch-worker-from-archetype)
	- How to create a Batch Worker, containing the main components, from the Batch Worker Maven Archetype.
- [Testing the Batch Worker](#testing-the-batch-worker)
	- How to test a Batch Worker with the use of the Job Service.

## Components of a Batch Worker Project

The Batch Worker Plugin and it's corresponding container are the two main components that make up a Batch Worker project.

### Batch Worker Plugin module

The Batch Worker Plugin creates worker-specific task messages. The implementation depends on the structure of the batch definition and the targeted worker the messages are intended for. For example, the batch definition could be a list of case files. For the purposes of this Getting Started guide we will assume that a batch definition is comprised of a list of strings to be language detected and that each string should be split into tasks for a downstream language detection worker.

#### Maven Project Dependencies

The Batch Worker Plugin and Batch Worker Services interfaces ([worker-batch-extensibility](https://github.com/JobService/worker-batch/tree/develop/worker-batch-extensibility)) should be included in the dependencies section of the project POM:

    <dependency>
        <groupId>com.hpe.caf.worker.batch</groupId>
        <artifactId>worker-batch-extensibility</artifactId>
        <version>1.1.0</version>
        <scope>provided</scope>
    </dependency>

#### Batch Worker Plugin

The Batch Worker Plugin class is responsible for interpreting and recursively splitting the batch definition into individual worker items that can be sent to a queue that a target worker is listening on. The primary requirement of the Batch Worker Plugin class is that it should implement the `BatchWorkerPlugin` interface defined in ([worker-batch-extensibility](https://github.com/JobService/worker-batch/tree/develop/worker-batch-extensibility)):

    package com.hpe.caf.worker.batch.plugins;
    
    import ...;
    
    /**
     * Example Batch Worker Plugin
     */
    public class ExampleBatchPlugin implements BatchWorkerPlugin
    {
        @Override
        public void processBatch(BatchWorkerServices batchWorkerServices,
                                 String batchDefinition,
                                 String taskMessageType,
                                 Map<String, String> taskMessageParams)
            throws BatchDefinitionException
        {
            // Start processing the batch definition.
            ...
            
        }
    }

The author of the Batch Worker Plugin class controls the structure of the batch definition supplied to the `processBatch()` method.

#### Batch Worker Services

It is the responsibility of the Batch Worker Plugin to repeatedly call the Batch Worker Services object in order to register sub-batches or individual work items. This can be done in the `processBatch()` method by making the appropriate `registerBatchSubtask()` and `registerItemSubtask()` services calls:

    @Override
    public void processBatch(BatchWorkerServices batchWorkerServices,
                             String batchDefinition,
                             String taskMessageType,
                             Map<String, String> taskMessageParams)
        throws BatchDefinitionException
    {
        // Start processing the batch definition
        ...
        
        // Determine if batch requires further splitting
        if (isSubBatchTrue) {
            // Register sub-batch
            batchWorkerServices.registerBatchSubtask(theSubbatchDefinition);
        } else {
            ...
            
            // Register individual worker item sub-task
            batchWorkerServices.registerItemSubtask(
                taskMessageToSend.getTaskClassifier(),
                taskMessageToSend.getTaskApiVersion(),
                taskMessageToSend.getTaskData());
        }
    }

### Batch Worker Container module

The Batch Worker Container module of the project consumes the Batch Worker Plugin implementation as a dependency, produces a container from with it and runs testcase integration testing against the container with the `BatchWorkerAcceptanceIT.java` class.

The next section, [Creating a Batch Worker from Archetype](#Creating-a-Batch-Worker-from-Archetype), creates a basic Batch Worker project for you that contains both Batch Worker Plugin and Batch Worker Container modules. It is recommended that you use this as the basis of your Batch Worker.

## Creating a Batch Worker from Archetype

The Batch Worker project offers a Batch Worker Archetype that provides a quick and easy way to generate a Batch Worker project containing Batch Worker Plugin and Container modules.

To read more about the Batch Worker Archetype and its usage click [here](https://github.com/JobService/worker-batch/blob/develop/worker-batch-archetype/documentation/WorkerArchetypeUsage.md).

## Testing the Batch Worker

For the purposes of getting started, this section assumes a new example Batch Worker Plugin has been created using the archetype. It assumes a `batchDefinition` that is comprised of a list of strings is to be used. The `processBatch()` method of the plugin is expected to recursively split the batch definition until individual worker items for a single string can be sent to a target worker. The target worker in this case is expected to be the Language Detection Document Worker.

The Job Service can be employed to test the Batch Worker Plugin created. The Job Service can send and monitor the progress of a batch of work sent to a Batch Worker. The message to be sent to the Batch Worker should include a batch definition comprising a list of strings as well as the target batch processor plugin (this is the name of the class that implements the Batch Worker Plugin interface). The Job Tracking Worker will also be deployed alongside the Job Service and the logs belonging to this worker can be utilised to verify the Batch Worker Plugin implementation.

### Job Service and Batch Worker Deployment

Follow the deployment instructions for the Job Service, Job Tracking Worker, Batch Worker (within the Docker Compose file, switch the Glob Filter Worker out for your newly created Batch Worker container and configuration) and Language Detection Worker in the [Job Service Ease of Deployment Guide](https://github.com/JobService/job-service-deploy/blob/develop/README.md).

#### Sending a Job to the Batch Worker

A Swagger user interface (UI) is provided on the same host and port as the Job Service. Usage instructions are provided in the [Getting Started guide for the Job Service](https://jobservice.github.io/job-service/pages/en-us/Getting-Started).

You can use the Swagger UI to send the Batch Worker a batch of work. Add a job with the new job body following this template:

    {
      "name": "ExampleBatchPlugin_Job_1",
      "description": "batch-split-worker-plugin-test",
      "externalData": "",
      "task": {
          "taskClassifier": "BatchWorker",
          "taskApiVersion": 1,
          "taskData": {
              "batchType": "BatchSplitWorkerBatchPlugin",
              "batchDefinition": "[\"EnglishLanguageString1\",\"EnglishLanguageString2\",\"EnglishLanguageString3\",\"EnglishLanguageString4\"]",
              "taskMessageType": "DocumentMessage",
              "taskMessageParams": {
                  "contentFieldName": "CONTENT"
              },
              "targetPipe": "languageidentification-in"
          },
          "taskPipe": "batchsplit-in",
          "targetPipe": "languageidentification-out"
        }
    }

Note the following:

* The `name` and `description` fields are just informational.
* The `externalData` field can be used to store information that you want associated with the job but that has no effect on it.
* The `taskClassifier` field specifies the type of message being sent. This must be set to `BatchWorker` as you are sending the job to a Batch Worker.
* The `taskApiVersion` field specifies the contract version. Set this to 1.
* The `taskData` field should contain the actual message that is to be sent to the Batch Worker and should be JSON-encoded.
  * In the template above, we are adding a batch definition comprising a list of strings for language detection.
  * The `batchType` field specifies the plugin to be used so set this to the name of your Batch Worker Plugin.
  * The `taskMessageType` field indicates to the Batch Worker the type of task message that it should construct for the downstream worker. In this case `DocumentMessage` is used as the plugin, that the archetype produces, constructs and sends messages intended for Document Workers.
* The `taskPipe` field specifies the queue where the message is to be forwarded to. Set this to the queue consumed by the Batch Worker, `batchsplit-in`.
* The `targetPipe` field specifies the ultimate pipe where messages should arrive after processing. Set this to the name of the final worker where tracking will stop, in this case, `languageidentification-out`.

#### Job Verification

After the job has been created, the Swagger UI can be used to track the progress and status of the job using the GET /partitions/{partitionId}/jobs method.

On completion, the payload sent to the Language Detection Document Worker output queue, `languageidentification-out`, will look similar to the following:

    {
       "version":3,
       "taskId":"AssetIDJob4.1.1.1",
       "taskClassifier":"DocumentWorker",
       "taskApiVersion":1,
       "taskData":"eyJmaWVsZENoYW5nZXMiOnsiRGV0ZWN0ZWRMYW5ndWFnZTJfQ29uZmlkZW5jZVBlcmNlbnRhZ2UiOnsiYWN0aW9uIjoicmVwbGFjZSIsInZhbHVlcyI6W3siZGF0YSI6IjAifV19LCJEZXRlY3RlZExhbmd1YWdlMV9OYW1lIjp7ImFjdGlvbiI6InJlcGxhY2UiLCJ2YWx1ZXMiOlt7ImRhdGEiOiJFTkdMSVNIIn1dfSwiRGV0ZWN0ZWRMYW5ndWFnZTJfTmFtZSI6eyJhY3Rpb24iOiJyZXBsYWNlIiwidmFsdWVzIjpbeyJkYXRhIjoiVW5rbm93biJ9XX0sIkRldGVjdGVkTGFuZ3VhZ2UzX0NvZGUiOnsiYWN0aW9uIjoicmVwbGFjZSIsInZhbHVlcyI6W3siZGF0YSI6InVuIn1dfSwiRGV0ZWN0ZWRMYW5ndWFnZTJfQ29kZSI6eyJhY3Rpb24iOiJyZXBsYWNlIiwidmFsdWVzIjpbeyJkYXRhIjoidW4ifV19LCJEZXRlY3RlZExhbmd1YWdlMV9Db2RlIjp7ImFjdGlvbiI6InJlcGxhY2UiLCJ2YWx1ZXMiOlt7ImRhdGEiOiJlbiJ9XX0sIkRldGVjdGVkTGFuZ3VhZ2UxX0NvbmZpZGVuY2VQZXJjZW50YWdlIjp7ImFjdGlvbiI6InJlcGxhY2UiLCJ2YWx1ZXMiOlt7ImRhdGEiOiI5NSJ9XX0sIkRldGVjdGVkTGFuZ3VhZ2UzX05hbWUiOnsiYWN0aW9uIjoicmVwbGFjZSIsInZhbHVlcyI6W3siZGF0YSI6IlVua25vd24ifV19LCJEZXRlY3RlZExhbmd1YWdlM19Db25maWRlbmNlUGVyY2VudGFnZSI6eyJhY3Rpb24iOiJyZXBsYWNlIiwidmFsdWVzIjpbeyJkYXRhIjoiMCJ9XX0sIkRldGVjdGVkTGFuZ3VhZ2VzX1N0YXR1cyI6eyJhY3Rpb24iOiJyZXBsYWNlIiwidmFsdWVzIjpbeyJkYXRhIjoiQ09NUExFVEVEIn1dfSwiRGV0ZWN0ZWRMYW5ndWFnZXNfUmVsaWFibGVSZXN1bHQiOnsiYWN0aW9uIjoicmVwbGFjZSIsInZhbHVlcyI6W3siZGF0YSI6InRydWUifV19fX0=",
       "taskStatus":"RESULT_SUCCESS",
       "context":{

       },
       "to":"languageidentification-out",
       "tracking":null,
       "sourceInfo":{
          "name":"worker-languagedetection",
          "version":"1.0.0-23"
       }
    }

The logs of the Job Tracking Worker can also be inspected to verify the processing of the original message or batch of work sent to the Batch Worker. The [Getting Started guide for the Job Service](https://jobservice.github.io/job-service/pages/en-us/Getting-Started) explains how to locate the stdout output for the job tracking worker. You can use this to verify the following:

* Message is registered and split into separate tasks by the batch worker
* Separate messages directed back to the batch worker output queue for recursive splitting
* Separate messages are forwarded to the language-detection worker input queue
* Job status check returns Active for separated messages
* Job status check returns Completed for separated messages
* Separate messages forwarded to the language-detection worker output queue
