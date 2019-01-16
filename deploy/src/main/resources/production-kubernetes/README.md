# Production Kubernetes Deployment

The Production Kubernetes deployment supports the deployment of the Job Service on Kubernetes. This folder contains the kubernetes environment and template files that are required to deploy the Job Service, Job Service Scheduled Executor and Job Tracking Worker.

## Service Configuration

### Kubernetes Template
The `marathon.json.b` template file describes the kubernetes deployment information required for starting the Job Service, Job Service Scheduled Executor and Job Tracking Worker. The template file uses property substitution to get values for configurable properties **required** for service deployment. These properties are configured in the marathon environment file `kubernetes.env`.

### Kubernetes Environment
The `kubernetes.env` file supports configurable property settings necessary for service deployment. These include:

- `JOB_SERVICE_HOST`: This configures the host name for the CAF_WEBSERVICE_URL.

- `JOB_SERVICE_8080_SERVICE_PORT`: This configures the external port number on the host machine that will be forwarded to the Job Service containers internal 8080 port. This port is used to call the Job Service web service. 

- `JOB_TRACKING_8080_SERVICE_PORT`: This configures the external port number on the host machine that will be forwarded to the Job Tracking workers internal 8080 port. This port is used to call the workers health check.

- `JOB_TRACKING_8081_SERVICE_PORT`: This configures the external port number on the host machine that will be forwarded to the Job Tracking workers internal 8081 port. This port is used to retrieve metrics from the worker.

- `JOB_SERVICE_DB_HOSTNAME`: This configures the host name for the Postgres database.

- `JOB_SERVICE_DB_PORT`: This configures the port for the Postgres database.

- `JOB_SERVICE_DB_USER`: The username for the Postgres database.

- `JOB_SERVICE_DB_PASSWORD`: The password for the Postgres database.

- `CAF_RABBITMQ_HOST`: This configures the host address for RabbitMQ.

- `CAF_RABBITMQ_PORT`: This configures the port where RabbitMQ is accepting messages.

- `CAF_RABBITMQ_USERNAME`: This configures the username for RabbitMQ.

- `CAF_RABBITMQ_PASSWORD`: This configures the password for RabbitMQ.

## Service Deployment
In order to deploy the service application, issue the following command from the `production-kubernetes` directory:

	source ./kubernetes.env \
            ; cat jobservice-deployment.yaml \
            | perl -pe 's/\$\{(\w+)\}/(exists $ENV{$1} && length $ENV{$1} > 0 ? $ENV{$1} : "NOT_SET_$1")/eg' \
            | kubectl create -f -
