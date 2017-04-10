---
layout: default
title: Scheduling Jobs
---

# Scheduling Jobs

[Chronos](https://mesos.github.io/chronos/), a distributed job scheduler, can be used to schedule jobs with the CAF Job Service and to monitor them through to completion.

## Chronos
Chronos is a framework for [Apache Mesos](http://mesos.apache.org/) that was originally developed as a replacement for cron. It is a fully-featured, distributed, and fault-tolerant job scheduler. It includes a [REST API](https://mesos.github.io/chronos/docs/api.html) that allows for scripting of scheduled jobs and a Web UI for ease of use.

At the heart of Chronos job scheduling is a JSON POST request. The JSON hash you send to Chronos will include a job name, the command to be executed by Chronos and the scheduling for the job in [ISO 8601](https://en.wikipedia.org/wiki/ISO_8601) format. Chronos also supports the configuration of scheduled Docker jobs which can run in Docker containers.

## Job Service Caller Container

A Docker image is available which can be used to makes REST calls to the CAF Job Service API in order to create new jobs and wait on their completion. The latest image can be downloaded from Artifactory, for example:

    docker pull jobservice/job-service-caller:2.0.0

This Docker container has been configured to run as an executable and hosts a script, `createJob.py`, which calls the CAF Job Service API. The  script supports the following arguments:

- **-j** or **--job jobId** 
    - specifies the job identifier. (required)
- **-u** or **--url jobWebServiceURL** 
    - specifies the CAF Job Service URL. (required)
- **-h** or **--help** 
    - used to print a short description of all command line options (optional) 
- **-c** or **--correlation correlationId** 
    - specifies the CAF correlation identifier. (optional) 
- **-f** or **--filename jobDefinitionFilename**  
    - specifies the file name comprising the definition of the job to create. If the file name is not specified, the job definition will be read from stdin. (optional)
- **-p** or **--polling pollingInterval**
    - specifies the polling interval in seconds used to wait on job completion. (optional) 

## Getting Started
This section describes how Chronos can be used to schedule jobs within an end-to-end Job Service system. It assumes you have a microservices environment running Mesos, Marathon, Chronos and Docker. The expected end-to-end Job Service system should comprise the Job Tracking Worker, Batch Worker and Example Worker. The sample job definition used in this Getting Started section will send a message initially to the Batch Worker. This worker will then split the batch into individual items and forward each item on to the Example Worker.

### Boot Up Chronos Environment
In your microservices environment running Mesos, Marathon, Chronos and Docker, ensure the Chronos service is started. Run the following to check if Chronos is already started:

    sudo service chronos status

If Chronos is not started, run:

    sudo service chronos start

Chronos provides a Web UI which is available via port 4400. This can be used to prove that Chronos is running:

    http://<microservices-environment>:4400

### Deploying Job Service
You should deploy an end-to-end Job Service system comprising the Job Service, Job Tracking Worker, Batch Worker and Example Worker. See the Job Service's [Getting Started](Getting-Started) guide for deployment instructions.

### Download Job Service Caller Container
Download the latest Docker image for the Job Service Caller from Artifactory, for example:

    docker pull jobservice/job-service-caller:2.0.0

### Create Job Service Definition JSON
In order to add a new job using the Job Service, you should create a JSON file, e.g. `MyJobDefinition.json`, comprising a sample job definition:

    {
        "name": "MyScheduledJob",
        "description": "Description for my end-to-end scheduled job.",
        "task": {
            "taskClassifier": "BatchWorker",
            "taskApiVersion": 1,
            "taskData": "{\"batchDefinition\":\"[\\\"b591d8c6615c4af99d7915719b01259c/3a44156891e645c6828cfe47667f159f\\\"]\",\"batchType\":\"AssetIdBatchPlugin\",\"taskMessageType\":\"ExampleWorkerTaskBuilder\",\"taskMessageParams\":{\"datastorePartialReference\":\"b591d8c6615c4af99d7915719b01259c\",\"action\":\"REVERSE\"},\"targetPipe\":\"dataprocessing-example-in\"}",
            "taskDataEncoding": "utf8",
            "taskPipe": "dataprocessing-batch-in",
            "targetPipe": "dataprocessing-example-out"
        }
    }

where:

- TaskClassifier should be set to `BatchWorker` as you are sending the job to the batch worker.
- Set the taskApiVersion to 1.
- For the taskData, we are adding a batch definition with a storage reference and the datastorePartialReference is the container ID. This storage reference is the reference to the dummy data stored using the document generator.
- Set taskPipe to the queue consumed by the first worker to which you want to send the work, in this case, the batch worker `dataprocessing-batch-in`. The batch can then be broken down into task items.
- Set targetPipe to the name of the final worker where tracking will stop, in this case, the example worker `dataprocessing-example-out`.

### Adding a Chronos Docker Job
Next, add a Docker job using the Chronos REST API to run on the Job Service Caller container. 

#### Create Job Hash
A job hash needs to be created to specify the job details including the name, the command to execute and the name of the Docker container to run on. A sample JSON hash is provided next:

    {
        "schedule": "R2/2016-12-05T07:58:22Z/PT1M",
        "name": "my-docker-job",
        "container": {
            "type": "DOCKER",
            "image": "jobservice/job-service-caller:2.0.0",
            "forcePullImage": true,
            "network": "BRIDGE"
        },
        "cpus": "0.01",
        "mem": "64",
        "uris": ["/vagrant/testing-package/MyJobDefinition.json"],
        "command": "python2 createJob.py -j BatchWorker:$mesos_task_id -u http://192.168.56.10:9410 -f $MESOS_SANDBOX/MyJobDefinition.json"
    }

where:

- schedule: the scheduling for the job, in ISO 8601 format. This consists of 3 parts separated by /:
    - The number of times to repeat the job: Rn to repeat n times, or R to repeat forever
    - The start time of the job. Our format is ISO 8601 - YYYY-MM-DDThh:mm:ss.sTZD  where:
        - YYYY = four-digit year 
        - MM = two-digit month (01 = January, etc.) 
        - DD = two-digit day of month (01 through 31) 
        - hh = two-digit hour in 24-hour time (00 through 23) 
        - mm = two-digit minute (00 through 59) 
        - ss = two-digit second (00 through 59) 
        - s = one or more digits representing a decimal fraction of a second 
        - TZD = time zone designator (Z for UTC or +hh:mm or -hh:mm for UTC offset) * 
    - The run interval, defined following the "Duration" component of the ISO 8601 standard. P is required. T is for distinguishing M(inute) and M(onth)––it is required when specifying Hour/Minute/Second. For example:
        - P10M = 10 months 
        - PT10M = 10 minutes 
        - P1Y12M12D = 1 year, 12 months, and 12 days 
        - P12DT12M = 12 days and 12 minutes * P1Y2M3DT4H5M6S = 1 year, 2 months, 3 days, 4 hours, and 5 minutes
- name: the name of the scheduled Docker job.
- container: the Job Service Caller container to run on and includes the type (i.e. DOCKER), the image and whether or not the Docker image should always be pulled rather than relying on a locally cached version.
- cpus: the amount of CPU of each Job Service Caller container.
- mem: the amount of RAM of each Job Service Caller container.
- uris: the output location where the Job Service definition JSON file is located.
- command: the actual command that will be executed by Chronos. In this instance, the `createJob.py` script will be executed with a set of arguments specifying the job identifier, Job Web Service URL and the job definition JSON. The job identifier is derived from a prefix (i.e. `BatchWorker:`) and the task id auto-generated by Chronos. With the job definition JSON specified in the uris section of the Docker job hash, Docker is then able to automatically pick the file up using the special environment variable, `$MESOS_SANDBOX`, that the Mesos Docker executor provides to the running task. 

#### Send Job Hash to Chronos
Once you've generated the job hash, send it to Chronos like so:

- Endpoint: /scheduler/iso8601
- Method: POST
- Example: `curl -L -H 'Content-Type: application/json' -X POST -d '{job hash}' "http://<microservices-environment>:4400/scheduler/iso8601"`

After you have sent the job hash to Chronos using the example REST API call above, you can verify that a scheduled job has been created successfully using the Web UI. 

### Verification of Chronos Docker Job

#### Chronos Web UI
The Chronos Web UI can be used to track the status (i.e. SUCCESS or FAILURE) of the scheduled Docker job. This includes date and time information for the last completed and/or failed run. The Web UI also displays the number of successful and failed job runs to date.

![DockerJobSuccess]({{site.baseurl}}/pages/en-us/images/ChronosScheduledJobSuccess.png)

#### Mesos Logs
Additional logging from the `createJob.py` script will be available in the sandbox standard output log for the completed Chronos task. See under the Completed Tasks section of the Chronos Framework in the Mesos UI.

#### Job Service End-to-End Output
The Job Service's [Getting Started](Getting-Started) guide provides additional verification instructions for the expected end-to-end set up if the Chronos Docker job has completed successfully.
