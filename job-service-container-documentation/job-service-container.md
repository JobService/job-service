# Job Service Container

This is a docker container for the Job service. It consists of a Tomcat web server that connects to the database specified in the Marathon JSON files.
It uses the java:8 base image.

## Configuration

## Environment Variables

#### CAF\_TRACKING\_PIPE
This is the pipe output messages relating to this task should be sent.

#### CAF\_STATUS\_CHECK\_TIME
This is the number of seconds after which it is appropriate to try to confirm that the task has not been cancelled or aborted.

#### CAF\_WEBSERVICE\_URL
This is the URL address of the job service web api.

#### CAF\_DATABASE\_URL
The connection string URL used by the job service to connect to the PostgreSQL database. This URL has the format jdbc:postgresql://PostgreSQLHost:portNumber/databaseName

#### CAF\_DATABASE\_USERNAME
The username of the PostreSQL database account used by the job service to access the PostgreSQL database.

#### CAF\_DATABASE\_PASSWORD
The password of the PostreSQL database account used by the job service to access the PostreSQL database.

#### CAF\_RABBITMQ\_HOST
The host that runs the specified queue.

#### CAF\_RABBITMQ\_PORT
The port exposed on the host to access the queue by. e.g. 5672

#### CAF\_RABBITMQ\_USERNAME
The username to access the queue server with.

#### CAF\_RABBITMQ\_PASSWORD
The password to access the queue server with.




