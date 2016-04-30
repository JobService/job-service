# Job Service Database Installer

This is the database schema and provisioning tool for the [Job Service](https://github.hpe.com/caf/job-service). 

## Setup

The Job Service database requires PostgreSQL 9.5. The database is installed using the Liquibase database change management tool and you can choose to install from the database artifact directly or you can use the Docker image that is  also available.

### Database Artifact
Download the [job-service-db-1.0-jar-with-dependencies](http://cmbg-maven.autonomy.com/nexus/content/repositories/releases/com/hpe/caf/job-service-db/1.0/job-service-db-1.0-jar-with-dependencies.jar) jar from Nexus and then run:

	java -jar /job-service-db-1.0-jar-with-dependencies.jar -db.connection jdbc:postgresql://localhost:5432 -db.name jobservice -db.pass root -db.user postgres -fd

the database connection, user and password string arguments will need changed to match your database setup:

### Docker Image
This is available as a Docker container - see [job-service-db-container](https://github.hpe.com/caf/job-service-db-container).

Pull the installer image from artifactory using:

	docker pull rh7-artifactory.hpswlabs.hp.com:8443/caf/job-service-db-installer:1.0

then run the image using:

	docker run -i -t <containerId> bash

then the following to run the jar from the `job-service-db-installer` container:

	java -jar /job-service-db-1.0-jar-with-dependencies.jar -db.connection jdbc:postgresql://localhost:5432 -db.name jobservice -db.pass root -db.user postgres -fd

