# Production Kubernetes Prerequisites

The Production Kubernetes Prerequisites deployment supports the deployment of the Job Service Database on Kubernetes. This folder contains the Kubernetes environment and template files that are required to deploy the Job Service Database and RabbitMQ.

## Service Configuration

### Kubernetes Template
The `jobservice-prerequisites-deployment.yaml` template file describes the Kubernetes deployment information required for starting the Job Service Database and RabbitMQ. The template file uses property substitution to get values for configurable properties **required** for service deployment. These properties are configured in the Kubernetes environment file `kubernetes.env`.

The `jobservice-prerequisites-persistentvolumeclaim.yaml` template file describes the Kubernetes persistent volume  information required by the Job Service database and RabbitMQ.

### Kubernetes Environment
The `kubernetes.env` file supports configurable property settings necessary for service deployment. These include:

- `JOB_SERVICE_DB_PORT`: This configures the external port number on the host machine that will be forwarded to the Job Service Database containers internal 5432 port. This port is used to connect to the Job Service Database.
- `JOB_SERVICE_DB_USER`: The username for the Postgres database.
- `JOB_SERVICE_DB_PASSWORD`: The password for the Postgres database.
- `CAF_RABBITMQ_HOST`: The hostname for the RabbitMQ instance
- `CAF_RABBITMQ_PORT`: The port for the RabbitMQ instance

## Service Deployment

1. Edit the `kubernetes.env` file adding relevant values for all the environment variables.

2. Deploy the persistent volume, issue the following command from the `production-kubernetes-prerequisites` directory:

    kubectl create -f jobservice-prerequisites-persistentvolumeclaim.yaml

3. Deploy the database and RabbitMQ by issuing the following command from the `production-kubernetes-prerequisites` directory:

    source ./kubernetes.env \
            ; cat jobservice-prerequisites-deployment.yaml \
            | perl -pe 's/\$\{(\w+)\}/(exists $ENV{$1} && length $ENV{$1} > 0 ? $ENV{$1} : "NOT_SET_$1")/eg' \
            | kubectl create -f -
