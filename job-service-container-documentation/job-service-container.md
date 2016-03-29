# Job Service Container

This is a docker container for the Job service. It consists of a Tomcat web server that connects to the database specified in the Marathon JSON files.
It uses the java:8 base image.

## Configuration

## Environment Variables

#### database.url
The connection string URL used by the job service to connect to the PostgreSQL database. This URL has the format jdbc:postgresql://PostgreSQLHost:portNumber/databaseName

#### database.username
The username of the PostreSQL database account used by the job service to access the PostgreSQL database.

#### database.password
The password of the PostreSQL database account used by the job service to access the PostreSQL database.

#### rabbitmq.host
The host that runs the specified queue.

#### rabbitmq.port
The port exposed on the host to access the queue by. e.g. 5672

#### rabbitmq.username
The username to access the queue server with.

#### rabbitmq.password
The password to access the queue server with.




