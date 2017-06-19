---
layout: default
title: Batch Worker API
---
# Batch Worker API

## Batch Worker Task
The Batch Worker will expect a `BatchWorkerTask` object, which contains the following fields:

- `batchDefinition` - definition of the batch.
- `batchType` - identifies the Batch Worker Plugin that is to be used to interpret the batchDefinition field.
- `taskMessageType` - identifies the type of messages to be produced by the Batch Worker.
- `taskMessageParams` - contains extra information that is required to produce the target messages.
- `targetPipe` - identifies the queue where the Batch Worker is to send the messages produced.

## Batch Worker Plugin
The Batch Worker requires a plugin to interpret and process batch definitions. The plugin is responsible for splitting the batch into smaller batches OR individual worker items. The plugin should implement the `BatchWorkerPlugin` interface which is defined in the [worker-batch-extensibility](https://github.com/JobService/worker-batch/tree/develop/worker-batch-extensibility) project as follows:

    interface BatchWorkerPlugin
    {
        void processBatch
        (
            BatchWorkerServices batchWorkerServices,
            String batchDefinition,
            String taskMessageType,
            Map<String, String> taskMessageParams
        )
        throws BatchDefinitionException;
    }

This interface only has a single method, `processBatch()`, which is called by the Batch Worker when it consumes a message sent to it. The Batch Worker constructs an instance of a `BatchWorkerServices` object that provides necessary services to the plugin and passes through the `batchDefinition`, `taskMessageType` and `taskMessageParams` fields of a `BatchWorkerTask`. It is expected that `processBatch()` can throw a `BatchDefinitionException` which will be handled appropriately by the Batch Worker.

### Task Message Builder 
For individual worker items, the Batch Worker Plugin will require a message builder object to construct worker specific task messages for individual worker items. 

An example task message builder, `ExampleWorkerTaskBuilder`, already exists which demonstrates how a task message can be built for the Example Worker, which is a trivial worker implementation. It can be found [here](https://github.com/WorkerFramework/worker-framework/tree/develop/worker-example/worker-example-message-builder). This example implementation introduces an additional layer of abstraction via the [DocumentMessageBuilder](https://github.com/JobService/document-message-builder) interface to aid plugin re-usability.

## Batch Worker Services
The Batch Worker constructs an object that provides necessary services to the Batch Worker Plugin. This services object implements the `BatchWorkerServices` interface, which is defined in the [worker-batch-extensibility](https://github.com/JobService/worker-batch/tree/develop/worker-batch-extensibility) project as follows:

    interface BatchWorkerServices 
    {
        // Creates a new sub-task which represents a smaller batch
        void registerBatchSubtask
        (
            String batchDefinition
        );

        // Creates a new sub-task for a single item
        void registerItemSubtask
        (
            String taskClassifier, 
            int taskApiVersion, 
            Object taskData
        );
    }

This interface has two methods defined, namely `registerBatchSubtask` and `registerItemSubtask`.

### Sub-Batches (registerBatchSubtask)
The Batch Worker Plugin will make calls to the service's `registerBatchSubtask` method to register smaller sub-batches for further batch definition refinement. The sub-batches are published to the Batch Worker's input queue.

### Individual Worker Items (registerItemSubtask)
The Batch Worker Plugin will make calls to the service's `registerItemSubtask` method to register task messages for individual worker items. The task messages are published to the target worker's input queue. The sub-task is composed of three parts: 

- `taskClassifier`: Specifies the type of message being sent. It should be set to the name of the classifier or target worker the messages are intended for. <br>
- `taskApiVersion`: Specifies the contract version. It should be set to the value of the worker Api version. For example, "1". <br>
- `taskData`: Contains the actual message that is to be sent to the target worker.
