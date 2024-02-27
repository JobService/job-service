# Job Service Database Installer

This is the database schema and provisioning tool for the [Job Service](../job-service). 

## Setup

The database is installed using the Flyway database change management tool and you can choose to install from the database artifact 
directly or you can use the Docker image that is  also available.

### PostgreSQL
The Job Service database requires PostgreSQL 11 or later to be installed and configured. 

### Database Artifact
With PostgreSQL 11 or later installed and configured, download the [job-service-db-X.X.X-XXX](https://repo1.
maven.org/maven2/com/github/jobservice/job-service-db/) jar from Artifactory into a folder where the rest of its dependency jars are present.

From the folder with the jars run:

	java -classpath *:classpath com.github.cafapi.util.flywayinstaller.Application -db.host localhost -db.port 5432 -db.name jobservice -db.pass root -db.user postgres

The database connection, user and password string arguments will need changed to match your PostgreSQL 11 or later setup.

### Docker Image
This is available as a Docker container - see [job-service-postgres-container](../job-service-postgres-container).

With PostgreSQL 11 or later installed and configured, pull the installer image from artifactory. For example:

	docker pull jobservice/job-service-postgres

then run the image using:

	docker run --rm jobservice/job-service-postgres \
	./install_job_service_db.sh \
	-db.host <postgres host> \
	-db.port 5432 \
	-db.name jobservice \
	-db.user postgres \
	-db.pass root

where:

*   db.host  :  Specifies the database host name.  e.g. `localhost`.
*   db.port  :  Specifies the database port.  e.g. `5432`.
*   db.name  :  Specifies the name of the database to be created or updated.
*   db.user  :  Specifies the username to access the database.
*   db.pass  :  Specifies the password to access the database.
*   log      :  Specifies the log level.

The jdbc database connection, user and password string arguments will need changed to match your external PostgreSQL 11 or later setup.
