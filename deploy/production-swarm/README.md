# Production Docker Swarm Deployment

The Production Docker Stack Deployment supports the deployment of the Job Service on Docker Swarm. This folder contains the `docker-stack.yml` file, a `rabbitmq.env` file and an `environment.sh` file.

## Service Configuration

### Docker Stack
The `docker-stack.yml` file describes the Docker deployment information required for the Job Service and Job Tracking Worker. The file uses property substitution to retrieve values from Environment variables. A number of these Environment variables are **required** for the Job Service deployment. These Environment variables are configurable in the `environment.sh` file.

### Docker Environment

The `environment.sh` file supports configurable property settings necessary for service deployment.
```
#!/usr/bin/env bash

###
# Job Service 
###

## Postgres Database Connection Details
export JOB_SERVICE_DB_HOST=192.168.56.10
export JOB_SERVICE_DB_PORT=5432
export CAF_DATABASE_USERNAME=postgres
export CAF_DATABASE_PASSWORD=root

## Job Service Web Service Connection Details
export JOB_SERVICE_PORT=9411
export JOB_SERVICE_DOCKER_HOST=192.168.56.10

###
# RabbitMQ
###

## RabbitMQ Connection Details
export CAF_RABBITMQ_HOST=192.168.56.10
export CAF_RABBITMQ_PORT=5672
export CAF_RABBITMQ_USERNAME=guest
export CAF_RABBITMQ_PASSWORD=guest
```

The `environment.sh` file specifies default values for the environment variables, however these values may require updating depending on the deployment environment.

The `rabbit.env` file is used to share the RabbitMQ configuration across multiple services within the `docker-stack.yml`.

#### Deploy

The **Deploy** section of the `docker-stack.yml` contains a number of important settings which may require updating depending on the deployment environment.

##### Replicas
* `mode` : Either global (exactly one container per swarm node) or replicated (a specified number of containers) (default replicated)
* `replicas` : If the service is replicated (which is the default), specify the number of containers that should be running at any given time

##### Resources > Limits
* `cpus`: This setting can be used to configure the amount of CPU for each container. This does not have to be a whole number
* `memory`: This configures the amount of RAM of each container. Note that this property does not configure the amount of RAM available to the container but is instead an upper limit. If the container's RAM exceeds this value it will cause docker to destroy and restart the container

##### Update Config
* `parallelism` : The number of containers to update at a time
* `delay` : The time to wait between updating a group of containers

## Execution

To deploy the stack:
* Edit `environment.sh` to ensure the Job Service and the Job Tracking Worker are pointing at the correct Postgres DB instance
* Edit `environemnt.sh` to ensure the RabbitMQ configuration shared across both Job Service and Job Tracking Worker is correct
* Ensure the Job Service DB has been created in your Postgres instance. For more info see [here](https://github.com/JobService/job-service/tree/develop/job-service-postgres-container#external-job-service-database-install)
* Ensure the versions of Job Service and Job Tracking Worker in `docker-stack.yml` are the correct version to be deployed
* Execute `source environment.sh`
* Execute `docker stack deploy --compose-file=docker-stack.yml jobServiceStack`
* The Job Service and Job Tracking Worker containers will start up

To tear down the stack:
* Execute `docker stack rm jobServiceStack`
