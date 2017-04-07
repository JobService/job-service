---
layout: default
title: Batch Worker Getting Started
---

# Getting Started
This guide provides instructions for creating, registering and testing an example Batch Worker plugin for use by the Batch Worker.


### ExampleWorkerTaskBuilder
An existing [ExampleWorkerTaskBuilder](https://github.com/WorkerFramework/worker-framework/tree/develop/worker-example/worker-example-message-builder) implementation is already supported which forwards items comprising a single document storage reference onto the Example Worker. This message builder implements an already defined [document-message-builder](https://github.com/JobService/job-service/tree/develop/document-message-builder) interface. The remainder of this document will refer to this message builder interface and implementation.

## Creating a Batch Worker Plugin

The Batch Worker Plugin requires a task message builder implementation in order to create worker-specific task messages. The implementation depends on the structure of the batch definition and the targeted worker the messages are to be intended for. For example, the batch definition could be a list of case files. For the purposes of this Getting Started guide we will assume that a batch definition comprising a list of document storage references is available.

### Create Maven Project

Create a new Maven project for the Batch Worker plugin. The Batch Worker Plugin and Batch Worker Services interfaces ([worker-batch-extensibility](https://github.com/JobService/worker-batch/tree/develop/worker-batch-extensibility)), and the relevant task message builder interfaces ([document-message-builder](https://github.com/JobService/job-service/tree/develop/document-message-builder)) should be included in the dependencies section of the project POM:

    <dependency>
        <groupId>com.hpe.caf.worker.batch</groupId>
        <artifactId>worker-batch-extensibility</artifactId>
        <version>1.1.0</version>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>com.hpe.caf.messagebuilder</groupId>
        <artifactId>document-message-builder</artifactId>
        <version>1.1.0</version>
    </dependency>

### Implement Batch Worker Plugin

Create a Batch Worker Plugin Java class. This class is responsible for interpreting and recursively splitting the batch definition into individual worker items that can be sent to a queue that a target worker is listening on. The primary requirement of the Batch Worker plugin class is that it should implement the BatchWorkerPlugin interface defined in [worker-batch-extensibility](https://github.com/JobService/worker-batch/tree/develop/worker-batch-extensibility):

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

The author of the Batch Worker plugin class controls the structure of the batch definition supplied to the `processBatch()` method.

#### Service File
Create a service file, `com.hpe.caf.worker.batch.BatchWorkerPlugin` under resources/META-INF/services and register the example Batch Worker plugin. This service file requires one line, the fully qualified name of the implementation class:

    com.hpe.caf.worker.batch.plugins.ExampleBatchPlugin
 
#### Batch Worker Services
It is the responsibility of the Batch Worker plugin to repeatedly call the Batch Worker services object in order to register sub-batches or individual work items. This can be done in the `processBatch()` method by making the appropriate `registerBatchSubtask()` and `registerItemSubtask()` services calls:

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

#### Task Message Builder
The `processBatch()` method of the Batch Worker Plugin can use the CAF ModuleProvider to load the appropriate task message builder implementation. The builder implementation can then be used to create a worker specific task message for each individual worker item using the `buildMessage()` method. In the sample code below, the `buildMessage()` method of the Document Message Builder requires a `DocumentServices` object that can be used to retrieve a `Document` object using the storage reference specified in the batch definition. It also requires additional message parameter details for the target worker.

    @Override
    public void processBatch(BatchWorkerServices batchWorkerServices,
                             String batchDefinition,
                             String taskMessageType,
                             Map<String, String> taskMessageParams)
        throws BatchDefinitionException
    {
        ...
        
        if (isSubBatchTrue) {
            ...
        } else {
            // Use the CAF ModuleProvider to load the appropriate task message
            // builder implementation.
            DocumentMessageBuilder builder = ModuleProvider.getInstance()
                .getModule(DocumentMessageBuilder.class, taskMessageType);
            ...
            
            // Construct the DocumentServices object
            Document document = new DocumentImpl(reference);
            DocumentServices documentServices = new DocumentServicesImpl(document);
            
            // Create worker specific task message for individual worker item
            TaskMessage taskMessageToSend =
                builder.buildMessage(documentServices, taskMessageParams);
            ...
            
            // Register individual worker item
            batchWorkerServices.registerItemSubtask(...);
        }
    }

In order to utilise the CAF ModuleProvider, you will need to reference the [util-moduleloader](https://github.com/CAFapi/caf-common/tree/develop/util-moduleloader) artifact in the project POM:

    <dependency>
        <groupId>com.github.cafapi.util</groupId>
        <artifactId>util-moduleloader</artifactId>
        <version>1.0.0</version>
    </dependency>

## Register the Batch Worker Plugin
To register the Batch Worker plugin, add a reference to it in the dependency management section of the [worker-batch-plugins-package](https://github.com/JobService/worker-batch/tree/develop/worker-batch-plugins-package) project POM:

    <dependencies>
        <!-- BatchWorkerPlugin implementations -->
        <dependency>
            <groupId>com.github.JobService</groupId>
            <artifactId>example-batch-plugin</artifactId>
            <version>1.0.0-SNAPSHOT</version>
        </dependency>
        
        <!-- DocumentMessageBuilder implementations -->
        <dependency>
            <groupId>com.hpe.caf.worker.messagebuilder</groupId>
            <artifactId>worker-example-message-builder</artifactId>
            <version>1.1.0-SNAPSHOT</version>
        </dependency>
    </dependencies>

The [worker-batch-plugins-package](https://github.com/JobService/worker-batch/tree/develop/worker-batch-plugins-package) project is a collection of plugins and message builder implementations for use by the Batch Worker that are packaged in a single aggregated tar.gz. This can be unpacked and placed on the Batch Worker classpath.

## Testing the Batch Worker Plugin

For the purposes of getting started, this section assumes a new example Batch Worker plugin has been created using the instructions provided in this document. It assumes a batch definition comprising a list of document storage references is to be used. The `processBatch()` method of the plugin is expected to recursively split the list of document storage references until individual worker items for a single document storage reference can be sent to a target worker. The target worker in this case is expected to be the Example Worker. The [ExampleWorkerTaskBuilder](https://github.com/WorkerFramework/worker-framework/tree/develop/worker-example/worker-example-message-builder) implementation is the assumed choice of task message builder.

The Job Service should be employed to test the example Batch Worker plugin created. The Job Service can send and monitor the progress of a batch of work sent to a Batch Worker. The message to be sent to the Batch Worker should include a batch definition comprising a list of document storage references as well as the target batch processor plugin. The Job Tracking Worker will also be deployed alongside the Job Service and the logs belonging to this worker can be utilised to verify the example Batch Worker plugin implementation.

### Job Service and Worker Deployment
Follow the deployment instructions for the Job Service, Job Tracking Worker, Batch Worker and Example Worker in the [Getting Started guide for the Job Service](https://jobservice.github.io/job-service/pages/en-us/Getting-Started).

####  Batch Worker Plugin Registration
Build the [worker-batch-plugins-package](https://github.com/JobService/worker-batch/tree/develop/worker-batch-plugins-package) project and copy the aggregated batch-plugins.tar.gz to the ${marathon-uris-root}/${batch-plugin-location} location specified in the [Chateau pre-requisite](https://github.hpe.com/caf/chateau/blob/develop/services/batch-worker/README.md)  instructions for running the Batch Worker. This step places the example batch processing plugin on the Batch Worker classpath.

#### Sending a Job to the Batch Worker

A Swagger user interface (UI) is provided on the same host and port as the Job Service. Usage instructions are provided in the [Getting Started guide for the Job Service](https://jobservice.github.io/job-service/pages/en-us/Getting-Started).

You can use the Swagger UI to send the Batch Worker a batch of work. Add a job with the new job body following this template:

    {
      "name": "ExampleBatchPlugin_Job_1",
      "description": "example-batch-worker-plugin-test",
      "externalData": "",
      "task": {
          "taskClassifier": "BatchWorker",
          "taskApiVersion": 1,
          "taskData": "{\"batchDefinition\":\"[\\\"66d1c33ec8c74fee99d207e7521a93ec/9f97451f75024be2a4017d2c17dca0b4\\\",\\\"66d1c33ec8c74fee99d207e7521a93ec/cfd59ecef76d4ae99581ac62409c3302\\\"]\",\"batchType\":\"ExampleBatchPlugin\",\"taskMessageType\":\"ExampleWorkerTaskBuilder\",\"taskMessageParams\":{\"datastorePartialReference\":\"66d1c33ec8c74fee99d207e7521a93ec\",\"action\":\"REVERSE\"},\"targetPipe\":\"dataprocessing-example-in\"}",
          "taskDataEncoding": "utf8",
          "taskPipe": "dataprocessing-batch-in",
          "targetPipe": "dataprocessing-example-out"
      }
    }

Note the following:

* The `name` and `description` fields are just informational.
* The `externalData` field can be used to store information that you want associated with the job but that has no effect on it.
* The `taskClassifier` field specifies the type of message being sent. This must be set to `BatchWorker` as you are sending the job to the Batch Worker.
* The `taskApiVersion` field specifies the contract version. Set this to 1.
* The `taskData` field should contain the actual message that is to be sent to the Batch Worker and should be JSON-encoded.
  * In the template above, we are adding a batch definition comprising a list of storage references and the `datastorePartialReference` is the container ID.
  * The `batchType` field specifies the plugin to be used so set this to the name of your Batch Worker plugin.
  * The `taskMessageType` field indicates the task message builder implementation to use, in this case the already existing `ExampleWorkerTaskBuilder` which is used to build and send messages onto the Example Worker for further processing.
* The `taskDataEncoding` field specifies how the value of the `taskData` field should be encoded. Set this to utf8.
* The `taskPipe` field specifies the queue where the message is to be forwarded to. Set this to the queue consumed by the Batch Worker, dataprocessing-batch-in.
* The `targetPipe` field specifies the ultimate pipe where messages should arrive after processing. Set this to the name of the final worker where tracking will stop, in this case, dataprocessing-example-out.

#### Job Verification

After the job has been created, the Swagger UI can be used to track the progress and status of the job using the GET /jobs method.

On completion, the payload sent to the Example Worker output queue, dataprocessing-example-out, will look similar to the following:

    {
        "version": 3,
        "taskId": "7.1.1",
        "taskClassifier": "ExampleWorker",
        "taskApiVersion": 1,
        "taskData": "{"workerStatus":"COMPLETED","textData":{"reference":null,"data":"MF9tZXRJdHNlVA=="}}",
        "taskStatus": "RESULT_SUCCESS",
        "context": {},
        "to": "dataprocessing-example-out",
        "tracking": null,
        "sourceInfo": {
            "name": "ExampleWorker",
            "version": "1.0.2"
        }
    }

The logs of the Job Tracking Worker can also be inspected to verify the processing of the original message or batch of work sent to the Batch Worker. The [Getting Started guide for the Job Service](https://jobservice.github.io/job-service/pages/en-us/Getting-Started) explains how to locate the stdout output for the job tracking worker. You can use this to verify the following:

* Message is registered and split into separate tasks by the batch worker
* Separate messages directed back to the batch worker output queue for recursive splitting
* Separate messages are forwarded to the example worker input queue
* Job status check returns Active for separated messages
* Job status check returns Completed for separated messages
* Separate messages forwarded to the example worker output queue
