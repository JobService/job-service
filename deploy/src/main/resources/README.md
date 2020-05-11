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

2. PostgreSQL  
    PostgreSQL 11.7 container used to store the Job Service database.

3. RabbitMQ  
    The Worker Framework is a pluggable infrastructure and technically it can use different messaging systems.  However it is most common for RabbitMQ to be used for messaging, and that is what is used here.

4. Job Tracking Worker  
    For simplicity the Job Tracking Worker is not shown on the diagram above.  The diagram shows messages passing directly between the workers.  A second message is passed by the workers to the Job Tracking Worker with progress information, which updates the Job Service Database. This means that the Job Service is able to provide accurate progress reports when they are requested.

5. Job Service Scheduled Executor  
    This is a polling service that identifies jobs in the system that depend on other jobs which are now complete. It is an ExecutorService which schedules a task to execute repeatedly identifying jobs which are ready to run. For simplicity, this service is not shown in the diagram but for each job identified, a message is then published on RabbitMQ in order to start the job.

6. GlobFilter Worker  
    This is a simple worker developed just for this demonstration.  It is a Batch Worker which takes in a glob-pattern as the Batch Definition.  Glob-patterns are generally fairly simple.  For example, `*.txt` means "all text files in the input folder".  Even more complex patterns like `**/t*.txt`, which means "all text files which start with the letter 't' and are in the input folder or in any subfolders of the input folder", are fairly easy to understand.  The worker produces a separate task for each file which matches the glob-pattern.

7. Language Detection Worker  
    This worker reads text files and determines what language or languages they are written in.  Typically it would return the result to another worker but for this demonstration it is configured to output the results to a folder.

8. FileBrowser  
    This is a web-based file browser provided here to store the input and output test files for demonstration purposes. 

## Usage
1. Download the files from this repository  
    You can clone this repository using Git or else you can simply download the files as a Zip using the following link:  
    [https://github.com/JobService/job-service-deploy/archive/develop.zip](https://github.com/JobService/job-service-deploy/archive/develop.zip)

2. Create the Config Map by issuing the following command from the directory where you have downloaded the files to:
 
	`kubectl create -f jobservice-config.yaml`

3. Create the Persistent Volumes by issuing the following command from the directory where you have downloaded the files to:
 
	`kubectl create -f jobservice-pv.yaml`

4. Create the Services by issuing the following command from the directory where you have downloaded the files to:
 
	`kubectl create -f jobservice-service.yaml`

5. Deploy the Job Service and other required components by issuing the following command from the directory where you have downloaded the files to:

    `kubectl create -f jobservice-deployment.yaml`

    **Note:** By default the database is configured to run on port 5432, the Rabbit UI is configured to run on port 15672, the Job Service is configured to run on port 9411 and FileBrowser is configured to run on port 9415 on the Kubernetes cluster, if these are in use you can edit the `jobservice-deployment.yaml` and change the `hostPort` values before deploying.

6. Navigate to the FileBrowser UI  
    Using a browser, navigate to the following URL to access FileBrowser:

        http://<KUBERNETES_CLUSTER>:9415

    Replace `<KUBERNETES_CLUSTER>` with the IP address of your own Kubernetes cluster. If you changed the `hostPort` values in step 5 then you should replace `9415` with the port you configured.

7. Upload the test files for the demonstration to FileBrowser  
    When prompted login to FileBrowser using:

        USERNAME: admin
        PASSWORD: admin
    
    Click into the `input-files` directory and click the `Upload` button at the top-right of the screen. Then browse to the `input-files` directory of the repository files on your machine and upload all `txt` files.

8. Navigate to the Job Service UI  
    The Job Service is a RESTful Web Service and is primarily intended for programmatic access, however it also ships with a Swagger-generated user-interface.

    Using a browser, navigate to the `/job-service-ui` endpoint on the Job Service:  

        http://<KUBERNETES_CLUSTER>:9411/job-service-ui

    Replace `<KUBERNETES_CLUSTER>` with the IP address of your own Kubernetes cluster. If you changed the `hostPort` values in step 5 then you should replace `9411` with the port you configured.

9. Try the `GET /partitions/{partitionId}/jobStats/count` operation  
    Click on this operation, choose a partition Id, and then click on the 'Try it out!' button.

    You should see the response is zero as you have not yet created any jobs in this partition.

10. Create a Job  
    Go to the `PUT /partitions/{partitionId}/jobs/{jobId}` operation.

    - Choose a partition Id and set it in the `partitionId` parameter.
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

11. Check on the Job's progress  
    Go to the `GET /partitions/{partitionId}/jobs/{jobId}` operation.

    - Enter the partition Id and Job Id that you chose when creating the job.
    - Click on the 'Try it out!' button.

    You should see a response returned from the Job Service.
    - If the job is still in progress then the `status` field will be `Active` and the `percentageComplete` field will indicate the progress of the job.
    - If the job has finished then the `status` field will be `Completed`.

12. Check the results  
    The Language Detection Worker is configured to output the results to files and you should see that these files have been created in the `output-files` directory in FileBrowser. If you examine the files via FileBrowser you should see that they contain the details of what languages were detected in the corresponding input files.

## Production Deployment

### Production-Marathon

The [production-marathon](production-marathon) folder contains a set of template files for the configuration and deployment of the Job Service on Mesos/Marathon. This folder contains the marathon environment and template files that are required to deploy the Job Service, Job Service Scheduled Executor and Job Tracking Worker.

### Production-Marathon-Prerequisites

The [production-marathon-prerequisites](production-marathon-prerequisites) folder is used for testing the production templates in a non-production environment. It contains Marathon templates that are required to deploy the Job Service Database and RabbitMQ. **Note:** templates are provided to run a PostgreSQL database in Marathon, whereas in a real production environment the PostgreSQL database should be set up independently, following its own production standards.

### Production-Marathon-Testing

The [production-marathon-testing](production-marathon-testing) deployment supports the deployment of the components required to smoke test a Job Service deployment on Mesos/Marathon. This folder contains the marathon environment and template files that are required to deploy the Glob Filter and Language Detection Workers.

### Production Docker Swarm Deployment

The [Production Docker Stack](production-swarm) Deployment supports the deployment of the Job Service on Docker Swarm.
