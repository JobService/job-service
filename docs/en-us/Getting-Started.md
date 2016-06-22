---
layout: default
title: Getting Started
last_updated: Created and last modified by Conal Smith on June 15, 2016
---

## Deploying the Job Service Web API

### Docker Images

Download docker images for both the Job Service and the Job Service Database
Installer from Artifactory:

docker pull rh7-artifactory.hpswlabs.hp.com:8443/caf/job-service:1.0

docker pull rh7-artifactory.hpswlabs.hp.com:8443/caf/job-service-db-installer:1.0

### To install the database:

Make sure your own hibernate compatible database is running (i.e. PostgreSQL).

1. Run the job-service-db-installer container with the command:

`docker run -i -t <job-service-db-installer Image ID> bash`

2. Run the installer jar using the command below, replacing the options with your own database setup:

`java -jar /job-service-db.jar -db.connection jdbc:postgresql://localhost:5432 -db.name jobservice -db.pass root -db.user postgres -fd`

### Marathon Loader

Download the marathon-loader artifact from Nexus or Artifactory:

repository: [http://cmbg-maven.autonomy.com/nexus/content/repositories/releases/](http://cmbg-maven.autonomy.com/nexus/content/repositories/releases/)

repository mirror: [http://rh7-artifactory.hpswlabs.hp.com:8081/artifactory/policyengine-release/](http://rh7-artifactory.hpswlabs.hp.com:8081/artifactory/policyengine-release/)

groupId: com.hpe.caf <br>
artifactId: marathon-loader <br>
version: 2.1 <br>
classifier: jar-with-dependencies <br>

The marathon application loader is used to start the Job Service docker container.

#### Job Service Container Configuration

Download the templates found [here](https://github.hpe.com/caf/job-service-container/tree/develop/configuration/marathon-template-json)
and when running the command below point the options to the folder where the template files reside.

See the configuration for Job Service [here](https://github.hpe.com/caf/job-service-container/blob/develop/configuration/marathon-properties.md).

Use the [marathon-properties.json](https://github.hpe.com/caf/job-service-container/blob/develop/configuration/marathon-properties.json)
file with marathon loader to deploy the audit-management-web-service.

#### Launch the Job Service using Marathon loader

Copy the container configuration marathon template folder (i.e. [marathon-template-json](https://github.hpe.com/caf/job-service-container/tree/develop/configuration)) and the corresponding [marathon-properties.json](https://github.hpe.com/caf/job-service-container/blob/develop/configuration/marathon-properties.json)
file to the same folder containing the marathon application loader artifact.

To start the Audit Management Web Service container, run the marathon application loader with:

java -jar marathon-loader-2.1-jar-with-dependencies.jar -m "./marathon-template-json" -v "./marathon-properties.json" -e http://localhost:8080 -mo "./marathon-config"

where -e is used to specify the Marathon endpoint

This will launch the container which includes both the Web Service and UI.

## Using the Job Service Web API

### Add a job

Expand the PUT /jobs/{jobId} method. Enter value for jobId. Click on the example value box on the right to fill in the
newJob body. Edit these fields with your own details:

`name`: name of the job
`description`: description of the job
`externalData`: external data
`taskClassifier`: classifier of the task
`taskApiVersion`: API version of the task
`taskData`: data of the task
`taskDataEncoding`: encoding of the task data e.g. `utf8`
`taskPipe`: rabbit queue name feeding in messages to the job tracking worker
`targetPipe`: final destination queue where tracking will stop

Press `Try it out!`, the result code will show whether the addition of the job is a success or not (201 if job is successfully added or 204 if job is successfully updated).

![Add Job](images/JobServiceUIAddJob.png)

### Get jobs

Expand the GET /jobs method. Press `Try it out!`. The list of jobs in the system will appear in the response body and you will be able to see the job you just created.

![Add Job](images/JobServiceUIGet.png)
