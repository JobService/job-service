# Production Marathon Testing

The Production Marathon Testing deployment supports the deployment of the components required to smoke test a Job Service deployment on Mesos/Marathon. This folder contains the marathon environment and template files that are required to deploy the Glob Filter and Language Detection Workers.

## Prerequisites

### Docker login
Before attempting to perform the Marathon deployments, a `docker login` command must be issued in order to make it possible to pull the required images from Docker Hub. The generic username and password for this are as follows:

- **Username:** hpeemployee
- **Password:** tomicrofocusandbeyond 

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
- `CAF_GLOB_WORKER_BINARY_DATA_INPUT_FOLDER`: The directory where the test files are located

- `CAF_WORKER_LANGDETECT_INPUT_QUEUE`: The RabbitMQ queue on which the Language Detection worker listens
- `CAF_WORKER_LANGDETECT_OUTPUT_QUEUE`: The RabbitMQ queue on which the Language Detection worker outputs messages
- `CAF_LANG_DETECT_WORKER_OUTPUT_FOLDER`: The folder in which the Language Detection worker places result files

- `JOB_SERVICE_DEMO_INPUT_DIR`: The directory where the test files are located
- `JOB_SERVICE_DEMO_OUTPUT_DIR`: The output directory for test results

### Additional Marathon Configuration
The `marathon.json.b` deployment template file specifies default values for a number of additional settings which you may choose to modify directly for your custom deployment. These include:

##### Application CPU, Memory and Instances

- `cpus` : This setting can be used to configure the amount of CPU for the Glob Filter and Language Detection Worker containers. This does not have to be a whole number. **Default Value: 0.5**

- `mem`: This configures the amount of RAM for the Glob Filter and Language Detection Worker containers. Note that this property does not configure the amount of RAM available to the containers but is instead an upper limit. If the container's RAM exceeds this value it will cause docker to destroy and restart the container. **Default Value: 1024**

- `instances`: This setting specifies the number of instances of the Glob Filter and Language Detection Worker containers to start on launch. **Default value: 1.**

## Service Deployment

1. Deploy the Production Marathon Prerequisite services as described [here](../production-marathon-prerequisites/README.md)

2. Deploy the Production Marathon services as described [here](../production-marathon/README.md).

3. Deploy the testing Docker containers for Job Service

In order to deploy the testing Docker containers, issue the following command from the 'production-marathon' directory:

	source ./marathon.env ; \
	     cat marathon.json.b \
	     | perl -pe 's/\$\{(\w+)\}/(exists $ENV{$1} && length $ENV{$1} > 0 ? $ENV{$1} : "NOT_SET_$1")/eg' \
	     | curl -H "Content-Type: application/json" -d @- http://localhost:8080/v2/groups/

