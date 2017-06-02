# Production Docker Swarm Deployment

The Production Docker Stack Deployment supports the deployment of the Job Service on Docker Swarm. This folder contains the `docker-stack.yml` file and environment files for RabbitMQ and Postgres.

## Service Configuration

### Docker Stack
The `docker-stack.yml` file describes the Docker deployment information required for the Job Service. The file uses property substitution to retrieve values from Environment variables. A number of these Environment variables are **required** for the Job Service deployment. These Environment variables are configurable in the RabbitMQ and Postgres environment files.

### Docker Environment
The `rabbit.env` file supports configurable property settings necessary for service deployment.  
* `CAF_RABBITMQ_HOST` : RabbitMQ Host  
* `CAF_RABBITMQ_PORT` : RabbitMQ Port  
* `CAF_RABBITMQ_USERNAME` : RabbitMQ Username  
* `CAF_RABBITMQ_PASSWORD` : RabbitMQ Password  

The `postgres.env` file supports configurable property settings necessary for service deployment.  
* `CAF_DATABASE_URL` : Postgres DB URL used for Job Service  
* `JOB_DATABASE_URL` : Postgres DB URL used for Job Tracking Worker  

### Additional Docker Configuration
The `docker-stack.yml` file specifies default values for a number of additional settings which you may choose to modify directly for your custom deployment. These include:  

#### Deploy

##### Restart Policy
* `condition` : One of none, on-failure or any
* `delay` : How long to wait between restart attempts, specified as a duration
* `max_attempts` : How many times to attempt to restart a container before giving up
* `window` : How long to wait before deciding if a restart has succeeded, specified as a duration

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
* Edit `rabbit.env`  
* Edit `docker-stack.env`  
* `docker stack deploy --compose-file=docker-stack.yml jobServiceProd`  

To tear down the stack:  
* `docker stack rm jobServiceProd`