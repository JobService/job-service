# Job Service

![Overview](images/overview.PNG)

The job-service API is used to create, retrieve, update, delete, cancel and check the status of jobs. HTTP requests are made to the job service which runs with a base Tomcat image. The job service then stores and retrieves data from the database.

## Deployment

This is available as a Docker container hosted in Apache Tomcat - see [job-service-container](https://github.hpe.com/caf/job-service-container).

## Usage

### Database Setup

The job service database will need to be installed. It is available as a Docker container - see [job-service-db-container](https://github.hpe.com/caf/job-service-db-container).

### Web UI

To start using the web service, the endpoints can be exercised by accessing the Web UI at the following URL:

	http://<docker.ip.address>:<port>/job-service-ui

Replace `<docker.ip.address>` and `<port>` as necessary.