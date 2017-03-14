# Job Service Database Installer

This is the database schema and provisioning tool for the [Job Service](../job-service). 

## Setup

The database is installed using the Liquibase database change management tool and you can choose to install from the database artifact directly or you can use the Docker image that is  also available.

### PostgreSQL
The Job Service database requires PostgreSQL 9.4 or later to be installed and configured. 

### Database Artifact
With PostgreSQL 9.4 or later installed and configured, download the [job-service-db-1.9.1-78-jar-with-dependencies](http://rh7-artifactory.svs.hpeswlab.net:8081/artifactory/libs-release-local/com/hpe/caf/job-service-db/1.9.1-78/job-service-db-1.9.1-78-jar-with-dependencies.jar) jar from Artifactory and then run:

	java -jar /job-service-db-1.9.1-78-jar-with-dependencies.jar -db.connection jdbc:postgresql://localhost:5432 -db.name jobservice -db.pass root -db.user postgres -fd

the database connection, user and password string arguments will need changed to match your PostgreSQL 9.4 or later setup.

### Docker Image
This is available as a Docker container - see [job-service-postgres-container](../job-service-postgres-container).

With PostgreSQL 9.4 or later installed and configured, pull the installer image from artifactory. For example:

	docker pull rh7-artifactory.svs.hpeswlab.net:8444/caf/job-service-postgres:1.10.0-<buildnum>

then run the image using:

	docker run --rm rh7-artifactory.svs.hpeswlab.net:8444/caf/job-service-postgres:1.10.0-<buildnum> \
	./install_job_service_db.sh \
	-db.connection jdbc:postgresql://<postgres host>:5432/ \
	-db.name jobservice \
	-db.user postgres \
	-db.pass root

where:

*   db.connection  : Specifies the jdbc connection string to the database service. This does not include the database name.  e.g. `jdbc:postgresql://<postgres host>:5432/`.
*   db.name  :  Specifies the name of the database to be created or updated.
*   db.user  :  Specifies the username to access the database.
*   db.pass  :  Specifies the password to access the database.

The jdbc database connection, user and password string arguments will need changed to match your external PostgreSQL 9.4 or later setup.
