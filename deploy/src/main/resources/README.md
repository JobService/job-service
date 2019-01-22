# Job Service Deployment

## Introduction
The Job Service is designed to provide additional tracking and control operations for applications that are built using the [Worker Framework](https://workerframework.github.io/worker-framework/).

It makes it possible to use the Worker Framework for complex operations, especially batch operations, which are expected to take a significant length of time to complete.

The Job Service is a RESTful Web Service and provides a simple API.  It tracks tasks through the Worker Framework and can be used to report on progress or on failure, as well as allowing for the cancellation of tasks if that is required.

## Deployment Repository
This repository provides the necessary files to easily get started using the Job Service.

The deployment files are in [Kubernetes](https://kubernetes.io/) format. If you are new to Kubernetes then a quick way to get started is to use [Minikube](https://kubernetes.io/docs/setup/minikube/).

As well as the Job Service the deployment files reference several other services.  These are just simple workers, built using the Worker Framework, that are included to provide a simple but complete demonstration of the Job Service.

## Demonstration
The deployment files contain the following services:

![](images/job-service-deploy.png)

1. Job Service  
    This is the Job Service itself.  As discussed it is a RESTful Web Service and is the primary service being demonstrated here.

2. RabbitMQ  
    The Worker Framework is a pluggable infrastructure and technically it can use different messaging systems.  However it is most common for RabbitMQ to be used for messaging, and that is what is used here.

3. Job Tracking Worker  
    For simplicity the Job Tracking Worker is not shown on the diagram above.  The diagram shows messages passing directly between the workers, but in reality the messages are passed through the Job Tracking Worker, which acts as a proxy for them.  It routes them to their intended destination but it also updates the Job Service Database with the progress.  This means that the Job Service is able to provide accurate progress reports when they are requested.

4. Job Service Scheduled Executor  
    This is a polling service that identifies jobs in the system that depend on other jobs which are now complete. It is an ExecutorService which schedules a task to execute repeatedly identifying jobs which are ready to run. For simplicity, this service is not shown in the diagram but for each job identified, a message is then published on RabbitMQ in order to start the job.

5. GlobFilter Worker  
    This is a simple worker developed just for this demonstration.  It is a Batch Worker which takes in a glob-pattern as the Batch Definition.  Glob-patterns are generally fairly simple.  For example, `*.txt` means "all text files in the input folder".  Even more complex patterns like `**/t*.txt`, which means "all text files which start with the letter 't' and are in the input folder or in any subfolders of the input folder", are fairly easy to understand.  The worker produces a separate task for each file which matches the glob-pattern.

    The input folder can be defined by setting the `JOB_SERVICE_DEMO_INPUT_DIR` environment variable. This should be a directory in which contains a few sample text files in different languages. A few example files are contained in this repository `./input-files` directory.

6. Language Detection Worker  
    This worker reads text files and determines what language or languages they are written in.  Typically it would return the result to another worker but for this demonstration it is configured to output the results to a folder.

    The output folder can be defined by setting the `JOB_SERVICE_DEMO_OUTPUT_DIR` environment variable.

## Usage
1. Download the files from this repository  
    You can clone this repository using Git or else you can simply download the files as a Zip using the following link:  
    [https://github.com/JobService/job-service-deploy/archive/develop.zip](https://github.com/JobService/job-service-deploy/archive/develop.zip)

2. Edit the `kubernetes.env` file adding relevant values for the following Environment Variables:

    <table>
      <tr>
        <th>Environment Variable</th>
        <th>Description</th>
      </tr>
      <tr>
        <td>JOB_SERVICE_DB_HOSTNAME</td>
        <td>This configures the host name for the PostgreSQL database.</td>
      </tr>
      <tr>
        <td>JOB_SERVICE_DB_PORT</td>
        <td>This configures the port for the PostgreSQL database.</td>
      </tr>
      <tr>
        <td>JOB_SERVICE_DB_USER</td>
        <td>The username for the PostgreSQL database.</td>
      </tr>
      <tr>
        <td>JOB_SERVICE_DB_PASSWORD</td>
        <td>The password for the PostgreSQL database.</td>
      </tr>
      <tr>
        <td>CAF_RABBITMQ_HOST</td>
        <td>This configures the host address for RabbitMQ.</td>
      </tr>
      <tr>
        <td>CAF_RABBITMQ_PORT</td>
        <td>This configures the port where RabbitMQ is accepting messages.</td>
      </tr>
      <tr>
        <td>CAF_RABBITMQ_MANAGEMENT_PORT</td>
        <td>This configures the management port for the RabbitMQ UI.</td>
      </tr>
      <tr>
        <td>CAF_RABBITMQ_USERNAME</td>
        <td>This configures the username for RabbitMQ.</td>
      </tr>
      <tr>
        <td>CAF_RABBITMQ_PASSWORD</td>
        <td>This configures the password for RabbitMQ.</td>
      </tr>
      <tr>
        <td>JOB_SERVICE_HOST</td>
        <td>This configures the host name for the `CAF_WEBSERVICE_URL`.</td>
      </tr>
      <tr>
        <td>JOB_SERVICE_8080_SERVICE_PORT</td>
        <td>This configures the external port number on the host machine that will be forwarded to the Job Service containers internal 8080 port. This port is used to call the Job Service web service.</td>
      </tr>
      <tr>
        <td>JOB_TRACKING_8080_SERVICE_PORT</td>
        <td>This configures the external port number on the host machine that will be forwarded to the Job Tracking workers internal 8080 port. This port is used to call the workers health check.</td>
      </tr>
      <tr>
        <td>JOB_TRACKING_8081_SERVICE_PORT</td>
        <td>This configures the external port number on the host machine that will be forwarded to the Job Tracking workers internal 8081 port. This port is used to retrieve metrics from the worker.</td>
      </tr>
      <tr>
        <td>JOB_SCHEDULED_EXECUTOR_8081_SERVICE_PORT</td>
        <td>This configures the external port number on the host machine that will be forwarded to the Job Scheduled Executors internal 8081 port.</td>
      </tr>
      <tr>
        <td>CAF_WORKER_GLOBFILTER_INPUT_QUEUE</td>
        <td>The RabbitMQ queue on which the Glob Filter worker listens.</td>
      </tr>
      <tr>
        <td>CAF_BATCH_WORKER_ERROR_QUEUE</td>
        <td>The RabbitMQ queue where failed Glob Filter worker messages are sent.</td>
      </tr>
      <tr>
        <td>CAF_GLOB_WORKER_BINARY_DATA_INPUT_FOLDER</td>
        <td>The location of the mounted directory inside the container where the test files are located.</td>
      </tr>
      <tr>
        <td>CAF_WORKER_LANGDETECT_INPUT_QUEUE</td>
        <td>The RabbitMQ queue on which the Language Detection worker listens.</td>
      </tr>
      <tr>
        <td>CAF_WORKER_LANGDETECT_OUTPUT_QUEUE</td>
        <td>The RabbitMQ queue on which the Language Detection worker outputs messages.</td>
      </tr>
      <tr>
        <td>CAF_LANG_DETECT_WORKER_OUTPUT_FOLDER</td>
        <td>The folder in which the Language Detection worker places result files.</td>
      </tr>
      <tr>
        <td>JOB_SERVICE_DEMO_INPUT_DIR</td>
        <td>The directory where the test files are located on the host.</td>
      </tr>
      <tr>
        <td>JOB_SERVICE_DEMO_OUTPUT_DIR</td>
        <td>The output directory for test results on the host.</td>
      </tr>
    </table>

4. Deploy the services  
	Deploy the persistent volume by issuing the following command from the directory where you have downloaded the files to:

    `kubectl create -f jobservice-persistentvolumeclaim.yaml`

	Deploy the database and RabbitMQ by issuing the following command from the directory where you have downloaded the files to:

    ```
	source ./kubernetes.env \
            ; cat jobservice-deployment.yaml \
            | perl -pe 's/\$\{(\w+)\}/(exists $ENV{$1} && length $ENV{$1} > 0 ? $ENV{$1} : "NOT_SET_$1")/eg' \
            | kubectl create -f -
	```

5. Navigate to the Job Service UI  
    The Job Service is a RESTful Web Service and is primarily intended for programmatic access, however it also ships with a Swagger-generated user-interface.

    Using a browser, navigate to the `/job-service-ui` endpoint on the Job Service:  

        http://<docker-host>:<JOB_SERVICE_8080_SERVICE_PORT>/job-service-ui

    Adjust `docker-host` to be the name of your own Docker Host and adjust the `JOB_SERVICE_8080_SERVICE_PORT` to match what you set it to in the `kubernetes.env` file.

6. Try the `GET /jobStats/count` operation  
    Click on this operation and then click on the 'Try it out!' button.

    You should see the response is zero as you have not yet created any jobs.

7. Create a Job  
    Go to the `PUT /jobs/{jobId}` operation.

    - Choose a Job Id, for example, `DemoJob`, and set it in the `jobId` parameter.
    - Enter the following Job Definition into the `newJob` parameter:

        <pre><code>{
          "name": "Some job name",
          "description": "The description of the job",
          "task": {
            "taskClassifier": "BatchWorker",
            "taskApiVersion": 1,
            "taskData": {
              "batchType": "GlobPattern",
              "batchDefinition": "*.txt",
              "taskMessageType": "DocumentMessage",
              "taskMessageParams": {
                "field:binaryFile": "CONTENT",
                "field:fileName": "FILE_NAME",
                "cd:outputSubfolder": "subDir",
                "cd:resultFormat": "COMPLEX"
              },
              "targetPipe": "languageidentification-in"
            },
            "taskPipe": "globfilter-in",
            "targetPipe": "languageidentification-out"
          }
        }</code></pre>

8. Check on the Job's progress  
    Go to the `GET /jobs/{jobId}` operation.

    - Enter the Job Id that you chose when creating the job.
    - Click on the 'Try it out!' button.

    You should see a response returned from the Job Service.
    - If the job is still in progress then the `status` field will be `Active` and the `percentageComplete` field will indicate the progress of the job.
    - If the job has finished then the `status` field will be `Completed`.

    Given that the Language Detection Worker is configured to output the results to files in a folder you should see that these files have been created in the output folder.  If you examine the output files you should see that they contain the details of what languages were detected in the corresponding input files.

## Production Deployment

### Production-Marathon

The [production-marathon](production-marathon) folder contains a set of template files for the configuration and deployment of the Job Service on Mesos/Marathon. This folder contains the marathon environment and template files that are required to deploy the Job Service, Job Service Scheduled Executor and Job Tracking Worker.

### Production-Marathon-Prerequisites

The [production-marathon-prerequisites](production-marathon-prerequisites) folder is used for testing the production templates in a non-production environment. It contains Marathon templates that are required to deploy the Job Service Database and RabbitMQ. **Note:** templates are provided to run a PostgreSQL database in Marathon, whereas in a real production environment the PostgreSQL database should be set up independently, following its own production standards.

### Production-Marathon-Testing

The [production-marathon-testing](production-marathon-testing) deployment supports the deployment of the components required to smoke test a Job Service deployment on Mesos/Marathon. This folder contains the marathon environment and template files that are required to deploy the Glob Filter and Language Detection Workers.

### Production Docker Swarm Deployment

The [Production Docker Stack](production-swarm) Deployment supports the deployment of the Job Service on Docker Swarm.
