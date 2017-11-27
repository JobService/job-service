# Production Marathon Prerequisites

The Production Marathon Prerequisites deployment supports the deployment of the Job Service Database on Mesos/Marathon. This folder contains the marathon environment and template files that are required to deploy the Job Service Database and RabbitMQ.

## Service Configuration

### Marathon Template
The `marathon.json.b` template file describes the marathon deployment information required for starting the Job Service Database. The template file uses property substitution to get values for configurable properties **required** for service deployment. These properties are configured in the marathon environment file `marathon.env`.

### Marathon Environment
The `marathon.env` file supports configurable property settings necessary for service deployment. These include:

- `JOB_SERVICE_DB_PORT`: This configures the external port number on the host machine that will be forwarded to the Job Service Database containers internal 5432 port. This port is used to connect to the Job Service Database.

- `JOB_SERVICE_DB_USER`: The username for the Postgres database.

- `JOB_SERVICE_DB_PASSWORD`: The password for the Postgres database.

### Additional Marathon Configuration
The `marathon.json.b` deployment template file specifies default values for a number of additional settings which you may choose to modify directly for your custom deployment. These include:

##### Application CPU, Memory and Instances

- `cpus` : This setting can be used to configure the amount of CPU for the Job Service Database container. This does not have to be a whole number. **Default Value: 0.5**

- `mem`: This configures the amount of RAM for the Job Service Database container. Note that this property does not configure the amount of RAM available to the container but is instead an upper limit. If the container's RAM exceeds this value it will cause docker to destroy and restart the container. **Default Value: 1024**

- `instances`: This setting specifies the number of instances of the Job Service Database container to start on launch. **Default value: 1.**

## Service Deployment
In order to deploy the service application, issue the following command from the 'production-marathon' directory:

	source ./marathon.env ; \
	     cat marathon.json.b \
	     | perl -pe 's/\$\{(\w+)\}/(exists $ENV{$1} && length $ENV{$1} > 0 ? $ENV{$1} : "NOT_SET_$1")/eg' \
	     | curl -H "Content-Type: application/json" -d @- http://localhost:8080/v2/groups/
