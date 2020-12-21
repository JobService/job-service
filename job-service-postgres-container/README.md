# Job Service PostgreSQL Container

## Summary

A Docker container encapsulating the job service database. This container has been built to operate as a service (by default) but can also be used as a 'utility' container to install the job service database on an external postgres server instance.

## Service Mode

By default, the container runs the job service database as a service. On start up, a database (named 'jobservice' by default) is installed on a postgres 11 instance inside the container. The postgres instance will not be available for connection until the database set up is complete. The completion can be seen when the container log outputs the line "Completed installation of Job Service database.".

### Environment Variables

The following environment variables can be passed to the container to control its behaviour on startup.

#### POSTGRES_DB
- The name to use for the installed job service database. Defaults to 'jobservice'.

#### POSTGRES_PASSWORD

- The postgres password that will be used for database communication.

#### POSTGRES_USER

- The postgres user that will be created with permissions for the installed job service database.

## Utility Mode

The container is packaged with an installer for the job service database. This can be used to install to an external database server instance by passing the relevant install command to the docker container on startup. On completion of the install command the container will exit.

### PostgreSQL
The job service database requires PostgreSQL 9.4 or later to be installed and configured. 

### External Job Service Database Install

The script to install the job service database can be invoked by running the container. The location of the install script inside the container, i.e. './install_job_service_db.sh' as well as the database install configuration arguments will need to be specified. An example is given next:

	docker run --rm jobservice/job-service-postgres:2.0.0 \
	    ./install_job_service_db.sh \
	    -db.connection jdbc:postgresql://<postgres host>:5432/ \
	    -db.name jobservice \
	    -db.user postgres \
	    -db.pass root

where:

*   db.connection  : Specifies the jdbc connection string to the external database service. This does not include the database name.  e.g. `jdbc:postgresql://<postgres host>:5432/`.
*   db.name  :  Specifies the name of the database to be created or updated.
*   db.user  :  Specifies the username to access the database.
*   db.pass  :  Specifies the password to access the database.

The jdbc database connection, user and password string arguments will need changed to match your external PostgreSQL 9.4 or later setup.

#### Additional Install Arguments
The following install arguments can also be specified:

*   fd  :  Enables the deletion of the existing database for a fresh install, rather than updating the database.
*   log : Specifies the logging level of the installer. Valid options are: [debug, info, warning, severe, off].

