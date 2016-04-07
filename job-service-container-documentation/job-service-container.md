# Job Service Container

This is a docker container for the Job service. It consists of a Tomcat web server that connects to the database specified in the Marathon JSON files.
It uses the java:8 base image.

## Configuration

## Environment Variables

#### CAF_TRACKING_PIPE
This is the pipe output messages relating to this task should be sent, regardless of their nature (i.e. whether they are Reject messages, Retry messages, Response messages, or some other type of message). It is the responsibility of the Job Tracking Worker, which will be consuming messages sent to this pipe, to forward the message to the intended recipient, which is indicated by the 'to' field (mentioned later). Note: One exception to this is where the tracking pipe specified is the same pipe that the worker itself is consuming messages from. If this is the case then the tracking pipe should be ignored. It likely means that this is the Job Tracking Worker. Not making an exception for this case would cause to an infinite loop.

#### CAF_STATUS_CHECK_TIME
This is the time in seconds after which it is appropriate to try to confirm that the task has not been cancelled or aborted.

#### CAF_WEBSERVICE_URL
This is the URL address of the job service web api, required by the job tracking worker to access the web service.

#### CAF_DATABASE_URL
The connection string URL used by the job service to connect to the PostgreSQL database. This URL has the format jdbc:postgresql://PostgreSQLHost:portNumber/databaseName

#### CAF_DATABASE_USERNAME
The username of the PostreSQL database account used by the job service to access the PostgreSQL database.

#### CAF_DATABASE_PASSWORD
The password of the PostreSQL database account used by the job service to access the PostreSQL database.

#### CAF_RABBITMQ_HOST
The host that runs the specified queue.

#### CAF_RABBITMQ_PORT
The port exposed on the host to access the queue by. e.g. 5672

#### CAF_RABBITMQ_USERNAME
The username to access the queue server with.

#### CAF_RABBITMQ_PASSWORD
The password to access the queue server with.




