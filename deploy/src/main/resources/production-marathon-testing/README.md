# Production Marathon Testing

The Production Marathon Testing deployment supports the deployment of the components required to smoke test a Job Service deployment on Mesos/Marathon. This folder contains the marathon environment and template files that are required to deploy the Glob Filter and Language Detection Workers.

## Service Configuration

### Marathon Template
The `marathon.json.b` template file describes the marathon deployment information required for starting the Glob Filter and Language Detection Workers. The template file uses property substitution to get values for configurable properties **required** for service deployment. These properties are configured in the marathon environment file `marathon.env`.

### Marathon Environment
The `marathon.env` file supports configurable property settings necessary for service deployment. These include:

- `CAF_RABBITMQ_HOST`: The hostname for the RabbitMQ instance
- `CAF_RABBITMQ_PORT`: The port for the RabbitMQ instance
- `CAF_RABBITMQ_PASSWORD`: The password for the RabbitMQ instance
- `CAF_RABBITMQ_USERNAME`: The username for the RabbitMQ instance

- `CAF_WORKER_GLOBFILTER_INPUT_QUEUE`: The RabbitMQ queue on which the Glob Filter worker listens
- `CAF_BATCH_WORKER_ERROR_QUEUE`: The RabbitMQ queue where failed Glob Filter worker messages go
- `CAF_GLOB_WORKER_BINARY_DATA_INPUT_FOLDER`: The location of the mounted directory inside the container where the test files are located

- `CAF_WORKER_LANGDETECT_INPUT_QUEUE`: The RabbitMQ queue on which the Language Detection worker listens
- `CAF_WORKER_LANGDETECT_OUTPUT_QUEUE`: The RabbitMQ queue on which the Language Detection worker outputs messages
- `CAF_LANG_DETECT_WORKER_OUTPUT_FOLDER`: The folder in which the Language Detection worker places result files

- `JOB_SERVICE_DEMO_INPUT_DIR`: The directory where the test files are located on the host
- `JOB_SERVICE_DEMO_OUTPUT_DIR`: The output directory for test results on the host

- `CAF_WORKER_STORAGE_HOST_DATA_DIRECTORY`: The directory on the host that the Glob Filter and Language Detection workers can use as a datastore  

### Additional Marathon Configuration
The `marathon.json.b` deployment template file specifies default values for a number of additional settings which you may choose to modify directly for your custom deployment. These include:

##### Application CPU, Memory and Instances

- `cpus` : This setting can be used to configure the amount of CPU for the Glob Filter and Language Detection Worker containers. This does not have to be a whole number. **Default Value: 0.5**

- `mem`: This configures the amount of RAM for the Glob Filter and Language Detection Worker containers. Note that this property does not configure the amount of RAM available to the containers but is instead an upper limit. If the container's RAM exceeds this value it will cause docker to destroy and restart the container. **Default Value: 1024**

- `instances`: This setting specifies the number of instances of the Glob Filter and Language Detection Worker containers to start on launch. **Default value: 1.**

## Service Deployment

1. Deploy the Production Marathon Prerequisite services as described [here](../production-marathon-prerequisites/README.md)

2. Deploy the Production Marathon services as described [here](../production-marathon/README.md).

3. Deploy the testing Docker containers for Job Service by issuing the following command from the 'production-marathon-testing' directory:

		source ./marathon.env ; \
	     	cat marathon.json.b \
	     	| perl -pe 's/\$\{(\w+)\}/(exists $ENV{$1} && length $ENV{$1} > 0 ? $ENV{$1} : "NOT_SET_$1")/eg' \
	     	| curl -H "Content-Type: application/json" -d @- http://localhost:8080/v2/groups/

4. Navigate to the Job Service UI  
    The Job Service is a RESTful Web Service and is primarily intended for programmatic access, however it also ships with a Swagger-generated user-interface.

    Using a browser, navigate to the `/job-service-ui` endpoint on the Job Service:  

        http://<DOCKER-HOST>:<JOB-SERVICE-PORT>/job-service-ui

    Adjust '<DOCKER-HOST>` and `<JOB-SERVICE-PORT>' to be the name of your own environment.

5. Try the `GET /partitions/{partitionId}/jobStats/count` operation  
    Click on this operation, choose a partition Id, and then click on the 'Try it out!' button.

    You should see the response is zero as you have not yet created any jobs.

6. Create a Job  
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
                "cd:outputSubfolder": "subDir"
              },
              "targetPipe": "languageidentification-in"
            },
            "taskPipe": "globfilter-in",
            "targetPipe": "languageidentification-out"
          }
        }</code></pre>

7. Check on the Job's progress  
    Go to the `GET /partitions/{partitionId}/jobs/{jobId}` operation.

    - Enter the partition Id and Job Id that you chose when creating the job.
    - Click on the 'Try it out!' button.

    You should see a response returned from the Job Service.
    - If the job is still in progress then the `status` field will be `Active` and the `percentageComplete` field will indicate the progress of the job.
    - If the job has finished then the `status` field will be `Completed`.

    Given that the Language Detection Worker is configured to output the results to files in a folder you should see that these files have been created in the output folder.  If you examine the output files you should see that they contain the details of what languages were detected in the corresponding input files.
