---
layout: default
title: Getting Started
last_updated: Created and last modified by Conal Smith on June 15, 2016
---

# Getting Started

## Deploying the CAF Job Service

### Docker Images

In order to deploy the CAF Job Service you need to download docker images for both the Job Service and the Job Service Database Installer from Artifactory.

In your docker virtual machine run the two commands:

`docker pull rh7-artifactory.hpswlabs.hp.com:8443/caf/job-service:1.0`

`docker pull rh7-artifactory.hpswlabs.hp.com:8443/caf/job-service-db-installer:1.0`

### To install the Job database:

Make sure your own hibernate compatible database is running (i.e. PostgreSQL).

1. First, find the image id of the job-service-db-installer downloaded above and run the job-service-db-installer container with the command:

`docker run -i -t <job-service-db-installer Image ID> bash`

2. Then execute the installer jar from within the container using the command below, replacing the options with your own database setup:

`java -jar /job-service-db.jar -db.connection jdbc:postgresql://localhost:5432 -db.name jobservice -db.pass root -db.user postgres -fd`

> Note: if you aren't running the database in your Docker virtual machine, make sure you enable client authentication and allow TCP/IP socket and restart your database server. E.g. for Postgres setup: <br>
> * `vi /var/lib/pgsql/data/pg_hba.conf` append the line `host    all             all             0.0.0.0/0            md5` <br>
> * `vi /var/lib/pgsql/data/postgresql.conf` change the listen addresses to `listen_addresses='*'`

### Marathon Loader

To start the Job Service docker container use Marathon loader with the configuration files below.

Download the marathon-loader artifact from Nexus or Artifactory:

repository: [http://cmbg-maven.autonomy.com/nexus/content/repositories/releases/](http://cmbg-maven.autonomy.com/nexus/content/repositories/releases/)

repository mirror: [http://rh7-artifactory.hpswlabs.hp.com:8081/artifactory/policyengine-release/](http://rh7-artifactory.hpswlabs.hp.com:8081/artifactory/policyengine-release/)

> groupId: com.hpe.caf <br>
> artifactId: marathon-loader <br>
> version: 2.1 <br>
> classifier: jar-with-dependencies <br>

#### Job Service Marathon Loader Configuration

Download the [marathon-template-json](https://github.hpe.com/caf/job-service-container/tree/develop/configuration/marathon-template-json) folder.

Download the [marathon-properties.json](https://github.hpe.com/caf/job-service-container/blob/develop/configuration/marathon-properties.json) and change the properties to your own setup.

For documentation on the properties see [marathon-properties.md](https://github.hpe.com/caf/job-service-container/blob/develop/configuration/marathon-properties.md).

#### Launching Job Service using Marathon loader

Run the marathon loader application changing the options to point to the relevant locations:

`java -jar marathon-loader-2.1-jar-with-dependencies.jar -m "./marathon-template-json" -v "./marathon-properties.json" -e http://localhost:8080 -mo "./marathon-config"`

* -m specifies the location of the marathon-template-json folder
* -v specifies the location of the marathon-properties.json file
* -e is used to specify the Marathon endpoint
* -mo specifies the location where the generated marathon configs will be output

This will launch the container which includes both the Job Service Web API and the Job Service Swagger UI.

## Using the Job Service Web API

A handy user interface is provided and accessible on the same host and port as the Web service. The Swagger UI page will be accessible at the following address:

`http://<docker-host-address>:<service-port specified in marathon-properties.json>/job-service-ui`

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

![Add Job](images/JobServiceUIAddJob.PNG)

### Get jobs

Expand the GET /jobs method. Press `Try it out!`. The list of jobs in the system will appear in the response body and you will be able to see the job you just created.

![Add Job](images/JobServiceUIGet.PNG)

## Deploying End-To-End System

### Docker Images

In order to run an end-to-end Job Service system you need to download the Job Service docker image and set up your Job Service Database as explained above. There are a few other key components. You must download the following docker images from Artifactory:

In your docker virtual machine run the commands:

`docker pull rh7-artifactory.hpswlabs.hp.com:8443/caf/worker-batch:1.6-SNAPSHOT`

`docker pull rh7-artifactory.hpswlabs.hp.com:8443/caf/worker-jobtracking:1.5-SNAPSHOT`

`docker pull rh7-artifactory.hpswlabs.hp.com:8443/caf/worker-example:1.0-SNAPSHOT`

### Marathon Loader Configruation

Download the files in the marathon-template-config folders below and place in your own marathon-template-config folder in the same directory as marathon loader.

* Batch Worker [marathon-template-config](https://github.hpe.com/caf/worker-batch-container/tree/develop/worker-batch-configuration/configuration-tar/marathon-template-config).

* Jobtracking Worker [marathon-template-config](https://github.hpe.com/caf/worker-jobtracking-container/tree/develop/worker-jobtracking-configuration/configuration-tar/marathon-template-config).

* Example Worker [marathon-template-config](https://github.hpe.com/caf/worker-example-container/tree/develop/configuration/marathon-template-config).

Download the marathon-template-json.json files to a marathon-template-json folder in the same directory as marathon loader.

* Batch Worker [marathon-batch-worker.json](https://github.hpe.com/caf/worker-batch-container/blob/develop/worker-batch-configuration/configuration-tar/marathon-template-json/marathon-batch-worker.json).

* Jobtracking Worker [marathon-jobtracking-worker.json](https://github.hpe.com/caf/worker-jobtracking-container/blob/develop/worker-jobtracking-configuration/configuration-tar/marathon-template-json/marathon-jobtracking-worker.json).

* Example Worker [marathon-example-worker.json](https://github.hpe.com/caf/worker-example-container/blob/develop/configuration/marathon-template-json/marathon-example-worker.json).

* RabbitMQ [marathon-rabbit.json](https://github.hpe.com/caf/worker-example-container/blob/develop/configuration/marathon-template-json/marathon-rabbit.json)

Add the properties specific to each component to your marathon-properties.json file in the same directory as marathon loader. This supplies the properties to the templates.

* Batch Worker [marathon-properties.json](https://github.hpe.com/caf/worker-batch-container/blob/develop/worker-batch-configuration/configuration-tar/marathon-properties.json).

* Jobtracking Worker [marathon-properties.json](https://github.hpe.com/caf/worker-jobtracking-container/blob/develop/worker-jobtracking-configuration/configuration-tar/marathon-properties.json).

* Example Worker [marathon-properties.json](https://github.hpe.com/caf/worker-example-container/blob/develop/configuration/marathon-properties.json).

### Other Prerequisites

Download the batch-plugins.tar.gz from Nexus which contains the plugins necessary for using the Batch Worker, and place in a batch-plugins folder on your docker host machine. Specify this location as the `"batch-plugin-location"` property in marathon-properties.json.

http://cmbg-maven.autonomy.com/nexus/content/repositories/snapshots/com/hpe/caf/worker/batch/worker-batch-plugins-package/1.1-SNAPSHOT/

You must now tailor the marathon properties to your fit your own setup. By the end you will have a marathon-properties.json file like this with your own setup details:

```
{
	"docker-registry": "rh7-artifactory.hpswlabs.hp.com:8443",
	"marathon-group": "demo",
	"force-pull": false,
	"caf-fs-storage-hostPath": "/vagrant/store",
	"marathon-uris-root": "/vagrant",
	"worker-config-location": "marathon-config",
	"docker-login-config": "<MUST_REPLACE: Location of docker login config tar>",

	"storage_service-server": "<MUST_REPLACE: Server for storage service>",
	"storage_service-port": "<MUST_REPLACE: Port for storage service>",

	"storage_service-auth-server": "<MUST_REPLACE: Server for storage service authentication>",
	"storage_service-auth-port": "<MUST_REPLACE: Port for storage service authentication>",
	"storage_service-auth-userName": "<MUST_REPLACE: User name for storage service authentication>",
	"storage_service-auth-password": "<MUST_REPLACE: Password for storage service authentication>",
	"storage_service-auth-clientName": "<MUST_REPLACE: Client name for storage service authentication>",
	"storage_service-auth-clientSecret": "<MUST_REPLACE: Client secret for storage service authentication>",
	"storage_service-auth-realm": "<MUST_REPLACE: Realm for storage service authentication>",

	"rabbit-id": "rabbit-01",
	"rabbit-cpus": 0.4,
	"rabbit-mem": "512",
	"rabbit-instances": "1",
	"rabbit-erlang-cookie": "chocolatechip",
	"rabbit-host": "<MUST_REPLACE: IP>",
	"rabbit-user": "user",
	"rabbit-password": "pass",
	"rabbit-maxattempts": "3",
	"rabbit-backoffInterval": "5",
	"rabbit-maxBackoffInterval": "15",
	"rabbit-port": "5672",

	"batch-cpus": "0.4",
	"batch-mem": "768",
	"batch-java-mem-min": "512",
	"batch-java-mem-max": "512",
	"batch-8080-serviceport": "33080",
	"batch-8081-serviceport": "33081",
	"batch-autoscale.metric": "rabbitmq",
	"batch-autoscale.scalingprofile": "default",
	"batch-autoscale.maxinstances": "10",
	"batch-autoscale.mininstances": "1",
	"batch-healthcheck-graceperiodseconds": "300",
	"batch-healthcheck-intervalseconds": "120",
	"batch-healthcheck-maxconsecutivefailures": "5",
	"batch-healthcheck-timeoutseconds": "20",
	"batch-version": "1.5-SNAPSHOT",
	"batch-threads": "1",
	"batch-cache-expire": "120",
	"batch-plugin-location": "<MUST_REPLACE: Location of Batch Worker Plugins>",

	"jobtracking-cpus":"0.5",
	"jobtracking-mem":"1024",
	"jobtracking-java-mem-min":"512",
	"jobtracking-java-mem-max":"512",
	"jobtracking-8080-serviceport":"34080",
	"jobtracking-8081-serviceport":"34081",
	"jobtracking-autoscale.metric":"rabbitmq",
	"jobtracking-autoscale.scalingprofile":"default",
	"jobtracking-autoscale.maxinstances":"4",
	"jobtracking-autoscale.mininstances":"1",
	"jobtracking-healthcheck-graceperiodseconds":"300",
	"jobtracking-healthcheck-intervalseconds":"120",
	"jobtracking-healthcheck-maxconsecutivefailures":"5",
	"jobtracking-healthcheck-timeoutseconds":"20",
	"jobtracking-resultsize-threshold": "1024",
	"jobtracking-version":"1.5-SNAPSHOT",
	"jobtracking-database-url":"<MUST REPLACE: Job database address e.g. jdbc:postgresql://192.168.56.200:5432/jobservice",
	"jobtracking-database-username":"<MUST REPLACE: Job database username credential e.g. postgres",
	"jobtracking-database-password":"<MUST REPLACE: Job database password credential e.g. root",
	"jobtracking-threads": "1",

	"example-cpus":"0.4",
	"example-mem":"1024",
	"example-java-mem-min":"256",
	"example-java-mem-max":"256",
	"example-8080-serviceport":"31647",
	"example-8081-serviceport":"31648",
	"example-autoscale.metric":"rabbitmq",
	"example-autoscale.scalingprofile":"default",
	"example-autoscale.maxinstances":"10",
	"example-autoscale.mininstances":"1",
	"example-healthcheck-graceperiodseconds":"300",
	"example-healthcheck-intervalseconds":"120",
	"example-healthcheck-maxconsecutivefailures":"5",
	"example-healthcheck-timeoutseconds":"20",
	"example-version":"1.0-SNAPSHOT",
	"example-threads": "1",
	"example-resultSizeThreshold": "1024"
}
```

### Running Marathon Loader

Run the Marathon Loader jar with the following command:

`java -jar marathon-loader-2.1-SNAPSHOT-jar-with-dependencies.jar -m "./marathon-template-json" -c "./marathon-template-config" -v "./marathon-properties.json" -e http://192.168.56.10:8080 -co "./marathon-config"`

* -m specifies the location of the marathon-template-json folder
* -v specifies the location of the marathon-properties.json file
* -e is used to specify the Marathon endpoint
* -mo specifies the location where the generated marathon configs will be output

The status of the services can be viewed on Marathon at the following URL:

`http://<docker-host-address>:8080/ui/#`

Here you will be able to see the health of the workers and services.

![Alt text](images/MarathonAllHealthy.png)

### Send a Job

Open the Swagger UI as explained above.

Add a job following the template:

```
{
  "name": "Job_1",
  "description": "end-to-end",
  "externalData": "string",
  "task": {
    "taskClassifier": "BatchWorker",
    "taskApiVersion": 1,
    "taskData": "{\"batchDefinition\":\"[\\\"0a1b8e24cc44490888b3d8d8ebba8309/2469351c8b9d439bade38f0cf89b7087\\\",\\\"0a1b8e24cc44490888b3d8d8ebba8309/2469351c8b9d439bade38f0cf89b7087\\\"]\",\"batchType\":\"AssetIdBatchPlugin\",\"taskMessageType\":\"ExampleWorkerTaskBuilder\",\"taskMessageParams\":{\"datastorePartialReference\":\"0a1b8e24cc44490888b3d8d8ebba8309\",\"action\":\"REVERSE\"},\"targetPipe\":\"demo-example-in\"}",
    "taskDataEncoding": "utf8",
    "taskPipe": "demo-batch-in",
    "targetPipe": "demo-example-out"
  }
}
```

* Task classifier must be set to `BatchWorker`.

* Task Api Version must be set to 1.

* Task Data must include a batchDefinition

* Set taskPipe to the name of the queue consumed by the Job Tracking Worker i.e. `demo-jobtracking-in`.

* Point targetPipe to the name of the final worker where tracking will stop i.e. `demo-example-out`.

### Verification of correct setup

Observe that the message output to the Example Worker output queue demo-example-out won't contain any tracking info.

Open the stdout log file for the Job Tracking Worker and verify:

* Message is registered and split into separate tasks by the Batch Worker

* Separate messages are forwarded to the Example Worker input queue

* Job Status check returns Active for separated messages

* Single message forwarded to the Batch Worker output queue

* Job Status check returns Completed for separated messages

* Separate messages forwarded to the Example Worker output queue

* Tracking information is removed from separate messages