---
layout: default
title: Getting Started
last_updated: Created and last modified by Conal Smith on June 15, 2016
---

# Getting Started

## Deploying the CAF Job Service with Chateau

**[Chateau](https://github.hpe.com/caf/chateau)** can be used to launch workers and services such as the Job Service.

To download and set up Chateau, follow the instructions in the [README.md](https://github.hpe.com/caf/chateau/blob/develop/README.md). 

Installation instructions for the Job Service Database can be found [here](https://github.hpe.com/caf/chateau/blob/develop/services/job-service/README.md).

To deploy the Job Service follow the [Service Deployment](https://github.hpe.com/caf/chateau/blob/develop/deployment.md) guide and use the following option with the deployment shell script.

`./deploy-service.sh job-service`

## Using the Job Service Web API

A handy user interface is provided and accessible on the same host and port as the Web service. The Swagger UI page will be accessible at the following address:

`http://<docker-host-address>:<service-port>/job-service-ui`

### Add a job

Expand the PUT /jobs/{jobId} method. Enter value for jobId. Click on the example value box on the right to fill in the
newJob body. Edit these fields with your own details:

`name`: name of the job
`description`: description of the job
`externalData`: external data
`taskClassifier`: classifier of the task
`taskApiVersion`: API version of the task
`taskData`: data of the task (include a batch definition if sending to the batch worker)
`taskDataEncoding`: encoding of the task data e.g. `utf8`
`taskPipe`: name of rabbit queue feeding messages to the first worker
`targetPipe`: name of the final worker's output queue where tracking will stop

Press `Try it out!`, the result code will show whether the addition of the job is a success or not (201 if job is successfully added or 204 if job is successfully updated).

![Add Job](images/JobServiceUIAddJob.PNG)

### Get jobs

Expand the GET /jobs method. Press `Try it out!`. The list of jobs in the system will appear in the response body and you will be able to see the job you just created.

![Add Job](images/JobServiceUIGet.PNG)

## Deploying End-To-End System

In order to run an end-to-end Job Service system we need to run the Batch Worker and another service to send the tasks to, in this case the Example Worker.

### Batch Worker

The Batch Worker can be run using Chateau.

Prerequisites for running the Batch Worker can be found [here](https://github.hpe.com/caf/chateau/blob/develop/services/batch-worker/README.md).

The Batch Worker will be deployed by the deployment script with the following command:

`./deploy-service.sh batch-worker`

### Example Worker

Currently the Example Worker can be run using Marathon Loader.

Download the marathon configuration files [here](https://github.hpe.com/caf/worker-example-container/tree/develop/configuration/marathon-template-config). 

Download the marathon json file for example worker [here](https://github.hpe.com/caf/worker-example-container/blob/develop/configuration/marathon-template-json/marathon-example-worker.json).

Download [marathon-properties.json](https://github.hpe.com/caf/worker-example-container/blob/develop/configuration/marathon-properties.json).

Download the marathon loader jar from Nexus repository: [http://cmbg-maven.autonomy.com/nexus/content/repositories/releases/](http://cmbg-maven.autonomy.com/nexus/content/repositories/releases/)

Run marathon loader with the command:

```
java -jar marathon-loader-2.1-jar-with-dependencies.jar -m "./marathon-template-json" -v "./marathon-properties.json" -e http://localhost:8080 -mo "./marathon-config"
```

* -m specifies the location of the marathon-template-json folder
* -v specifies the location of the marathon-properties.json file
* -e is used to specify the Marathon endpoint
* -mo specifies the location where the generated marathon configs will be output

This will launch the container for Example Worker.

You will also need dummy data in datastore and a datastore reference to this data. Dummy data can be uploaded via the document-generator.

The status of the services can be viewed on Marathon at the following URL:

`http://<docker-host-address>:8080/ui/#`

Here you will be able to see the health of the workers and services.

![Marathon Health](images/MarathonAllHealthy.png)

### Send a Job

Open the Swagger UI as explained above.

Add a job with the newJob body following the template:

```
{
  "name": "Job_1",
  "description": "end-to-end",
  "externalData": "string",
  "task": {
    "taskClassifier": "BatchWorker",
    "taskApiVersion": 1,
    "taskData": "{\"batchDefinition\":\"[\\\"2f0e1a924d954ed09966f91d726e4960/fda3cf959a1d456b8d54800ba9e9b2f5\\\",\\\"02f0e1a924d954ed09966f91d726e4960/fda3cf959a1d456b8d54800ba9e9b2f5\\\"]\",\"batchType\":\"AssetIdBatchPlugin\",\"taskMessageType\":\"ExampleWorkerTaskBuilder\",\"taskMessageParams\":{\"datastorePartialReference\":\"2f0e1a924d954ed09966f91d726e4960\",\"action\":\"REVERSE\"},\"targetPipe\":\"demo-example-in\"}",
    "taskDataEncoding": "utf8",
    "taskPipe": "demo-batch-in",
    "targetPipe": "demo-example-out"
  }
}
```

* Task classifier must be set to `BatchWorker` as we are sending the job to the Batch Worker.

* Set the task Api version i.e. 1.

* Set the taskData, in this case we are adding a batch definition with a storage reference and the datastorePartialReference is the container ID.

* Set taskPipe to the name of the queue consumed by the first worker you want to sent the work to i.e. Batch Worker `demo-batch-in` - so that the batch can be broken down into task items.

* Set targetPipe to the name of the final worker where tracking will stop i.e. `demo-example-out`.

### Verification of correct setup

Observe that the message output to the Example Worker output queue demo-example-out won't contain any tracking info. The payload for the messages sent to RabbitMQ will look like this. Notice `tracking` is `null`.

```
{"version":3,"taskId":"j_demo_1.1","taskClassifier":"ExampleWorker","taskApiVersion":1,"taskData":"eyJ3b3JrZXJTdGF0dXMiOiJDT01QTEVURUQiLCJ0ZXh0RGF0YSI6eyJyZWZlcmVuY2UiOm51bGwsImRhdGEiOiJBQUFBQUFEdnY3MEFBQUR2djcwQUF3QURBQUFBQUFZRlMxQjBlSFF1TTJOdlpIUnpaWFFBQUFCa0FBQUFJQUFCQUFBQUFBQUFBQXdBQUFBSUFBQUFDQlB2djczdnY3MGFTQ2hhVkFBQUFBQUFGQUFVQWdGTFVIUjRkQzR5WTI5a2RITmxkQUFBQURJQUFBQWdBQUVBQUFBQUFBQUFEQUFBQUFnQUFBQUlaTysvdmUrL3ZlKy92VWdvV2swQUFBQUFBQlFBRkFJQlMxQjBlSFF1TVdOdlpIUnpaWFFBQUFBQUFBQUFJQUFCQUFBQUFBQUFBQXdBQUFBSUFBQUFDTysvdmM2VE5rZ29Xa1VBQUFBQUFCUUFGQUlCUzFBelkyOWtkSE5sZEhSNGRDNHpZMjlrZEhObGRBQUFBQXdBQUFBSUFBQUFDQlB2djczdnY3MGFTQ2hhVkFBQUFBQUFGQVFEUzFBeVkyOWtkSE5sZEhSNGRDNHlZMjlrZEhObGRBQUFBQXdBQUFBSUFBQUFDR1R2djczdnY3M3Z2NzFJS0ZwTkFBQUFBQUFVQkFOTFVERmpiMlIwYzJWMGRIaDBMakZqYjJSMGMyVjBBQUFBREFBQUFBZ0FBQUFJNzcrOXpwTTJTQ2hhUlFBQUFBQUFGQVFEUzFBPSJ9fQ==","taskStatus":"RESULT_SUCCESS","context":{},"to":"demo-example-out","tracking":null,"sourceInfo":{"name":"ExampleWorker","version":"1.0-SNAPSHOT"}}
```

The image below shows how to locate the stdout output for the Job Tracking Worker, after clicking on the Jobtracking application in Marathon .

![Jobtracking Stdout](images/Jobtracking_stdout.png)

Open the stdout log file for the Job Tracking Worker and verify the following:

* Message is registered and split into separate tasks by the Batch Worker

* Separate messages are forwarded to the Example Worker input queue

* Job Status check returns Active for separated messages

* Single message forwarded to the Batch Worker output queue

* Job Status check returns Completed for separated messages

* Separate messages forwarded to the Example Worker output queue

* Tracking information is removed from separate messages

The output log will look something like this:

```
DEBUG [2016-06-29 16:21:44,765] com.hpe.caf.worker.queue.rabbit.WorkerQueueConsumerImpl: Registering new message 22
DEBUG [2016-06-29 16:21:44,766] com.hpe.caf.worker.core.WorkerCore: Received task j_demo_1.1 (message id: 22)
DEBUG [2016-06-29 16:21:44,766] com.hpe.caf.worker.core.WorkerCore: Task j_demo_1.1 active status is not being checked - it is not yet time for the status check to be performed: status check due at Wed Jun 29 16:21:49 UTC 2016
DEBUG [2016-06-29 16:21:44,766] com.hpe.caf.worker.core.WorkerCore: Task j_demo_1.1 (message id: 22) is not intended for this worker: input queue demo-jobtracking-in does not match message destination queue demo-example-in
DEBUG [2016-06-29 16:21:44,766] com.hpe.caf.worker.jobtracking.JobTrackingWorkerReporter: Connecting to database jdbc:postgresql://16.49.191.34:5432/jobservice ...
INFO  [2016-06-29 16:21:44,793] com.hpe.caf.worker.jobtracking.JobTrackingWorkerReporter: Reporting progress of job task j_demo_1.1 with status Active ...
DEBUG [2016-06-29 16:21:44,999] com.hpe.caf.worker.jobtracking.JobTrackingWorkerFactory: Forwarding task j_demo_1.1
DEBUG [2016-06-29 16:21:44,999] com.hpe.caf.worker.core.WorkerCore: Task j_demo_1.1 (message id: 22) being forwarded to queue demo-example-in
DEBUG [2016-06-29 16:21:45,001] com.hpe.caf.worker.queue.rabbit.WorkerPublisherImpl: Publishing message with ack id 22
DEBUG [2016-06-29 16:21:45,001] com.hpe.caf.worker.queue.rabbit.WorkerConfirmListener: Listening for confirmation of publish sequence 22 (ack message: 22)
DEBUG [2016-06-29 16:21:45,002] com.hpe.caf.worker.queue.rabbit.WorkerQueueConsumerImpl: Registering new message 23
DEBUG [2016-06-29 16:21:45,002] com.hpe.caf.worker.core.WorkerCore: Received task j_demo_1.2 (message id: 23)
DEBUG [2016-06-29 16:21:45,002] com.hpe.caf.worker.core.WorkerCore: Task j_demo_1.2 active status is not being checked - it is not yet time for the status check to be performed: status check due at Wed Jun 29 16:21:49 UTC 2016
DEBUG [2016-06-29 16:21:45,003] com.hpe.caf.worker.core.WorkerCore: Task j_demo_1.2 (message id: 23) is not intended for this worker: input queue demo-jobtracking-in does not match message destination queue demo-example-in
DEBUG [2016-06-29 16:21:45,003] com.hpe.caf.worker.jobtracking.JobTrackingWorkerReporter: Connecting to database jdbc:postgresql://16.49.191.34:5432/jobservice ...
DEBUG [2016-06-29 16:21:45,006] com.hpe.caf.worker.queue.rabbit.WorkerConfirmListener: RabbitMQ broker ACKed published sequence id 22 (multiple: false)
INFO  [2016-06-29 16:21:45,029] com.hpe.caf.worker.jobtracking.JobTrackingWorkerReporter: Reporting progress of job task j_demo_1.2* with status Active ...
DEBUG [2016-06-29 16:21:45,069] com.hpe.caf.worker.jobtracking.JobTrackingWorkerFactory: Forwarding task j_demo_1.2
DEBUG [2016-06-29 16:21:45,069] com.hpe.caf.worker.core.WorkerCore: Task j_demo_1.2 (message id: 23) being forwarded to queue demo-example-in
DEBUG [2016-06-29 16:21:45,071] com.hpe.caf.worker.queue.rabbit.WorkerQueueConsumerImpl: Acknowledging message 22
DEBUG [2016-06-29 16:21:45,072] com.hpe.caf.worker.queue.rabbit.WorkerPublisherImpl: Publishing message with ack id 23
DEBUG [2016-06-29 16:21:45,072] com.hpe.caf.worker.queue.rabbit.WorkerConfirmListener: Listening for confirmation of publish sequence 23 (ack message: 23)
DEBUG [2016-06-29 16:21:45,076] com.hpe.caf.worker.queue.rabbit.WorkerQueueConsumerImpl: Registering new message 24
DEBUG [2016-06-29 16:21:45,077] com.hpe.caf.worker.core.WorkerCore: Received task ec9c4556-4753-478b-a714-bd57fde837b5 (message id: 24)
DEBUG [2016-06-29 16:21:45,077] com.hpe.caf.worker.core.WorkerCore: Task ec9c4556-4753-478b-a714-bd57fde837b5 active status is not being checked - it is not yet time for the status check to be performed: status check due at Wed Jun 29 16:21:49 UTC 2016
DEBUG [2016-06-29 16:21:45,077] com.hpe.caf.worker.core.WorkerCore: Task ec9c4556-4753-478b-a714-bd57fde837b5 (message id: 24) is not intended for this worker: input queue demo-jobtracking-in does not match message destination queue demo-batch-out
DEBUG [2016-06-29 16:21:45,077] com.hpe.caf.worker.jobtracking.JobTrackingWorkerReporter: Connecting to database jdbc:postgresql://16.49.191.34:5432/jobservice ...
DEBUG [2016-06-29 16:21:45,078] com.hpe.caf.worker.queue.rabbit.WorkerConfirmListener: RabbitMQ broker ACKed published sequence id 23 (multiple: false)
INFO  [2016-06-29 16:21:45,108] com.hpe.caf.worker.jobtracking.JobTrackingWorkerReporter: Reporting progress of job task j_demo_1 with status Active ...
DEBUG [2016-06-29 16:21:45,124] com.hpe.caf.worker.jobtracking.JobTrackingWorkerFactory: Forwarding task ec9c4556-4753-478b-a714-bd57fde837b5
DEBUG [2016-06-29 16:21:45,124] com.hpe.caf.worker.core.WorkerCore: Task ec9c4556-4753-478b-a714-bd57fde837b5 (message id: 24) being forwarded to queue demo-batch-out
DEBUG [2016-06-29 16:21:45,130] com.hpe.caf.worker.queue.rabbit.WorkerPublisherImpl: Publishing message with ack id 24
DEBUG [2016-06-29 16:21:45,130] com.hpe.caf.worker.queue.rabbit.WorkerConfirmListener: Listening for confirmation of publish sequence 24 (ack message: 24)
DEBUG [2016-06-29 16:21:45,130] com.hpe.caf.worker.queue.rabbit.WorkerQueueConsumerImpl: Acknowledging message 23
DEBUG [2016-06-29 16:21:45,132] com.hpe.caf.worker.queue.rabbit.WorkerConfirmListener: RabbitMQ broker ACKed published sequence id 24 (multiple: false)
DEBUG [2016-06-29 16:21:45,133] com.hpe.caf.worker.queue.rabbit.WorkerQueueConsumerImpl: Acknowledging message 24
DEBUG [2016-06-29 16:21:47,955] com.hpe.caf.worker.queue.rabbit.WorkerQueueConsumerImpl: Registering new message 25
DEBUG [2016-06-29 16:21:47,957] com.hpe.caf.worker.core.WorkerCore: Received task j_demo_1.1 (message id: 25)
DEBUG [2016-06-29 16:21:47,957] com.hpe.caf.worker.core.WorkerCore: Task j_demo_1.1 active status is not being checked - it is not yet time for the status check to be performed: status check due at Wed Jun 29 16:21:49 UTC 2016
DEBUG [2016-06-29 16:21:47,958] com.hpe.caf.worker.core.WorkerCore: Task j_demo_1.1 (message id: 25) is not intended for this worker: input queue demo-jobtracking-in does not match message destination queue demo-example-out
DEBUG [2016-06-29 16:21:47,959] com.hpe.caf.worker.jobtracking.JobTrackingWorkerReporter: Connecting to database jdbc:postgresql://16.49.191.34:5432/jobservice ...
INFO  [2016-06-29 16:21:47,989] com.hpe.caf.worker.jobtracking.JobTrackingWorkerReporter: Reporting progress of job task j_demo_1.1 with status Completed ...
DEBUG [2016-06-29 16:21:48,020] com.hpe.caf.worker.jobtracking.JobTrackingWorkerFactory: Forwarding task j_demo_1.1
DEBUG [2016-06-29 16:21:48,020] com.hpe.caf.worker.core.WorkerCore: Task j_demo_1.1 (message id: 25) being forwarded to queue demo-example-out
DEBUG [2016-06-29 16:21:48,020] com.hpe.caf.worker.core.WorkerCore: Task j_demo_1.1 (message id: 25): removing tracking info from this message as tracking ends on publishing to the queue demo-example-out.
DEBUG [2016-06-29 16:21:48,024] com.hpe.caf.worker.queue.rabbit.WorkerPublisherImpl: Publishing message with ack id 25
DEBUG [2016-06-29 16:21:48,024] com.hpe.caf.worker.queue.rabbit.WorkerConfirmListener: Listening for confirmation of publish sequence 25 (ack message: 25)
DEBUG [2016-06-29 16:21:48,026] com.hpe.caf.worker.queue.rabbit.WorkerConfirmListener: RabbitMQ broker ACKed published sequence id 25 (multiple: false)
DEBUG [2016-06-29 16:21:48,027] com.hpe.caf.worker.queue.rabbit.WorkerQueueConsumerImpl: Acknowledging message 25
DEBUG [2016-06-29 16:21:49,246] com.hpe.caf.worker.queue.rabbit.WorkerQueueConsumerImpl: Registering new message 26
DEBUG [2016-06-29 16:21:49,247] com.hpe.caf.worker.core.WorkerCore: Received task j_demo_1.2 (message id: 26)
DEBUG [2016-06-29 16:21:49,247] com.hpe.caf.worker.core.WorkerCore: Task j_demo_1.2 active status is not being checked - it is not yet time for the status check to be performed: status check due at Wed Jun 29 16:21:49 UTC 2016
DEBUG [2016-06-29 16:21:49,247] com.hpe.caf.worker.core.WorkerCore: Task j_demo_1.2 (message id: 26) is not intended for this worker: input queue demo-jobtracking-in does not match message destination queue demo-example-out
DEBUG [2016-06-29 16:21:49,247] com.hpe.caf.worker.jobtracking.JobTrackingWorkerReporter: Connecting to database jdbc:postgresql://16.49.191.34:5432/jobservice ...
INFO  [2016-06-29 16:21:49,274] com.hpe.caf.worker.jobtracking.JobTrackingWorkerReporter: Reporting progress of job task j_demo_1.2* with status Completed ...
DEBUG [2016-06-29 16:21:49,298] com.hpe.caf.worker.jobtracking.JobTrackingWorkerFactory: Forwarding task j_demo_1.2
DEBUG [2016-06-29 16:21:49,298] com.hpe.caf.worker.core.WorkerCore: Task j_demo_1.2 (message id: 26) being forwarded to queue demo-example-out
DEBUG [2016-06-29 16:21:49,298] com.hpe.caf.worker.core.WorkerCore: Task j_demo_1.2 (message id: 26): removing tracking info from this message as tracking ends on publishing to the queue demo-example-out.
DEBUG [2016-06-29 16:21:49,301] com.hpe.caf.worker.queue.rabbit.WorkerPublisherImpl: Publishing message with ack id 26
DEBUG [2016-06-29 16:21:49,301] com.hpe.caf.worker.queue.rabbit.WorkerConfirmListener: Listening for confirmation of publish sequence 26 (ack message: 26)
DEBUG [2016-06-29 16:21:49,306] com.hpe.caf.worker.queue.rabbit.WorkerConfirmListener: RabbitMQ broker ACKed published sequence id 26 (multiple: false)
DEBUG [2016-06-29 16:21:49,306] com.hpe.caf.worker.queue.rabbit.WorkerQueueConsumerImpl: Acknowledging message 26
```

## Links

For more information on Chateau see [here](https://github.hpe.com/caf/chateau).

For more information on Job Service templates, configuration and property files see [here](https://github.hpe.com/caf/chateau/blob/develop/services/job-service/README.md).

For more information on Batch Worker templates, configuration and property files see [here](https://github.hpe.com/caf/chateau/blob/develop/services/batch-worker/README.md).

