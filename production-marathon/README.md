# Production Marathon Deployment

The Production Marathon deployment supports the deployment of the Job Service on Mesos/Marathon. This folder contains the marathon environment and template files that are required to deploy both the Job Service and Job Tracking Worker.

## Service Configuration

### Marathon Template
The `marathon.json.b` template file describes the marathon deployment information required for starting both the Job Service and Job Tracking Worker. The template file uses property substitution to get values for configurable properties **required** for service deployment. These properties are configured in the marathon environment file `marathon.env`.

### Marathon Environment
The `marathon.env` file supports configurable property settings necessary for service deployment. These include:

- `DOCKER_REGISTRY`: This setting configures the docker repository that pulls the Job Service and Job Tracking images.

- `JOB_SERVICE_8080_SERVICE_PORT`: This configures the external port number on the host machine that will be forwarded to the Job Service containers internal 8080 port. This port is used to call the Job Service web service. 

- `JOB_TRACKING_8080_SERVICE_PORT`: This configures the external port number on the host machine that will be forwarded to the Job Tracking workers internal 8080 port. This port is used to call the workers health check.

- `JOB_TRACKING_8081_SERVICE_PORT`: This configures the external port number on the host machine that will be forwarded to the Job Tracking workers internal 8081 port. This port is used to retrieve metrics from the worker.

- `POSTGRES_DB_HOSTNAME`: This configures the host name for the Postgres database.

- `POSTGRES_DB_PORT`: This configures the port for the Postgres database.

- `POSTGRES_JOB_SERVICE_DB_USER`: The username for the Postgres database.

- `POSTGRES_JOB_SERVICE_DB_PASSWORD`: The password for the Postgres database.

- `CAF_RABBITMQ_HOST`: This configures the host address for RabbitMQ.

- `CAF_RABBITMQ_PORT`: This configures the port where RabbitMQ is accepting messages.

- `CAF_RABBITMQ_USERNAME`: This configures the username for RabbitMQ.

- `CAF_RABBITMQ_PASSWORD`: This configures the password for RabbitMQ.


### Additional Marathon Configuration
The `marathon.json.b` deployment template file specifies default values for a number of additional settings which you may choose to modify directly for your custom deployment. These include:

##### Application CPU, Memory and Instances

- `cpus` : This setting can be used to configure the amount of CPU of each Job Service and Job Tracking container. This does not have to be a whole number. **Default Value: 0.5**


- `mem`: This configures the amount of RAM of each Job Service and Job Tracking container. Note that this property does not configure the amount of RAM available to the container but is instead an upper limit. If the container's RAM exceeds this value it will cause docker to destroy and restart the container. **Default Value: 1024**

- `instances`: This setting specifies the number of instances of the Job Service and Job Tracking containers to start on launch. **Default value: 1.**


## Service Deployment
In order to deploy the service application, issue the following command from the 'production-marathon' directory:

	source ./marathon.env ; \
	     cat marathon.json.b \
	     | perl -pe 's/\$\{(\w+)\}/(exists $ENV{$1} && length $ENV{$1} > 0 ? $ENV{$1} : "NOT_SET_$1")/eg' \
	     | curl -H "Content-Type: application/json" -d @- http://localhost:8080/v2/groups/
