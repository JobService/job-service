# Production Kubernetes Testing

The Production Kubernetes Testing deployment supports the deployment of the components required to smoke test a Job Service deployment on Kubernetes. This folder contains the Kubernetes environment and template files that are required to deploy the Glob Filter and Language Detection Workers.

## Service Configuration

### Kubernetes Template
The `jobservice-testing-deployment.yaml` template file describes the Kubernetes deployment information required for starting the Glob Filter and Language Detection Workers. The template file uses property substitution to get values for configurable properties **required** for service deployment. These properties are configured in the Kubernetes environment file `kubernetes.env`.

The `worker-datastore-persistentvolumeclaim.yaml` template file describes the Kubernetes persistent volume information required by the Glob Filter and Language Detection Workers.

### Kubernetes Environment
The `kubernetes.env` file supports configurable property settings necessary for service deployment. These include:

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

## Service Deployment

1. Deploy the Production Kubernetes Prerequisite services as described [here](../production-kubernetes-prerequisites/README.md)

2. Deploy the Production Kubernetes services as described [here](../production-kubernetes/README.md).

3. Edit the `kubernetes.env` file adding relevant values for all the environment variables.

4. Deploy the persistent volume, issue the following command from the `production-kubernetes-testing` directory:

        kubectl create -f worker-datastore-persistentvolumeclaim.yaml

5. Deploy the testing containers for Job Service by issuing the following command from the `production-kubernetes-testing` directory:

		source ./kubernetes.env \
            ; cat jobservice-testing-deployment.yaml \
            | perl -pe 's/\$\{(\w+)\}/(exists $ENV{$1} && length $ENV{$1} > 0 ? $ENV{$1} : "NOT_SET_$1")/eg' \
            | kubectl create -f -

6. Navigate to the Job Service UI  
    The Job Service is a RESTful Web Service and is primarily intended for programmatic access, however it also ships with a Swagger-generated user-interface.

    Using a browser, navigate to the `/job-service-ui` endpoint on the Job Service:  

        http://<DOCKER-HOST>:<JOB-SERVICE-PORT>/job-service-ui

    Adjust '<DOCKER-HOST>` and `<JOB-SERVICE-PORT>' to be the name of your own environment.

7. Try the `GET /jobStats/count` operation  
    Click on this operation and then click on the 'Try it out!' button.

    You should see the response is zero as you have not yet created any jobs.

8. Create a Job  
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
                "cd:outputSubfolder": "subDir"
              },
              "targetPipe": "languageidentification-in"
            },
            "taskPipe": "globfilter-in",
            "targetPipe": "languageidentification-out"
          }
        }</code></pre>

9. Check on the Job's progress  
    Go to the `GET /jobs/{jobId}` operation.

    - Enter the Job Id that you chose when creating the job.
    - Click on the 'Try it out!' button.

    You should see a response returned from the Job Service.
    - If the job is still in progress then the `status` field will be `Active` and the `percentageComplete` field will indicate the progress of the job.
    - If the job has finished then the `status` field will be `Completed`.

    Given that the Language Detection Worker is configured to output the results to files in a folder you should see that these files have been created in the output folder.  If you examine the output files you should see that they contain the details of what languages were detected in the corresponding input files.