# Job Service Deployment

## Introduction
The Job Service is designed to provide additional tracking and control operations for applications that are built using the [Worker Framework](https://workerframework.github.io/worker-framework/).

It makes it possible to use the Worker Framework for complex operations, especially batch operations, which are expected to take a significant length of time to complete.

The Job Service is a RESTful Web Service and provides a simple API.  It tracks tasks through the Worker Framework and can be used to report on progress or on failure, as well as allowing for the cancellation of tasks if that is required.

## Deployment Repository
This repository provides the necessary files to easily get started using the Job Service.

The only pre-requisite required to get started is that [Docker](https://www.docker.com/) must be available on the system.

The deployment files are in Docker Compose v3 format, and they are compatible with both [Docker Compose](https://docs.docker.com/compose/) and [Docker Stack](https://docs.docker.com/engine/reference/commandline/stack_deploy/).

As well as the Job Service and the Job Service Database the deployment files reference several other services.  These are just simple workers, built using the Worker Framework, that are included to provide a simple but complete demonstration of the Job Service.

## Demonstration
The Docker Compose file contains the following services:

![](images/job-service-deploy.png)

1. Job Service  
    This is the Job Service itself.  As discussed it is a RESTful Web Service and is the primary service being demonstrated here.

    By default port 9411 is used to communicate with the Job Service but if that port is not available then the `JOB_SERVICE_PORT` environment variable can be set to have a different port used.

2. Job Service Database  
    Internally the Job Service uses a PostgreSQL Database to store the Job Status information.  When the stack is started a `job-service-db` volume will be created to store the database files.

3. RabbitMQ  
    The Worker Framework is a pluggable infrastructure and technically it can use different messaging systems.  However it is most common for RabbitMQ to be used for messaging, and that is what is used here.

4. Job Tracking Worker  
    For simplicity the Job Tracking Worker is not shown on the diagram above.  The diagram shows messages passing directly between the workers, but in reality the messages are passed through the Job Tracking Worker, which acts as a proxy for them.  It routes them to their intended destination but it also updates the Job Service Database with the progress.  This means that the Job Service is able to provide accurate progress reports when they are requested.

5. GlobFilter Worker  
    This is a simple worker developed just for this demonstration.  It is a Batch Worker which takes in a glob-pattern as the Batch Definition.  Glob-patterns are generally fairly simple.  For example, `*.txt` means "all text files in the input folder".  Even more complex patterns like `**/t*.txt`, which means "all text files which start with the letter 't' and are in the input folder or in any subfolders of the input folder", are fairly easy to understand.  The worker produces a separate task for each file which matches the glob-pattern.

    By default the input folder is `./input-files`, which is a directory in this repository which contains a few sample text files in different languages.  A different input folder can be used by setting the `JOB_SERVICE_DEMO_INPUT_DIR` environment variable.

6. Language Detection Worker  
    This worker reads text files and determines what language or languages they are written in.  Typically it would return the result to another worker but for this demonstration it is configured to output the results to a folder.

    By default the output folder used is `./output-files`, but a different folder can be used by setting the `JOB_SERVICE_DEMO_OUTPUT_DIR` environment variable.

## Usage
1. Download the files from this repository  
    You can clone this repository using Git or else you can simply download the files as a Zip using the following link:  
    [https://github.com/JobService/job-service-deploy/archive/develop.zip](https://github.com/JobService/job-service-deploy/archive/develop.zip)

2. Configure the external parameters if required  
    The following parameters may be set:

    <table>
      <tr>
        <th>Environment Variable</th>
        <th>Default</th>
        <th>Description</th>
      </tr>
      <tr>
        <td>JOB_SERVICE_PORT</td>
        <td>9411</td>
        <td>This is the port that the Job Service is configured to listen on.</td>
      </tr>
      <tr>
        <td>JOB_SERVICE_DEMO_INPUT_DIR</td>
        <td>./input&#8209;files</td>
        <td>This directory is made available as a source for input files which may be read.  The glob-pattern is passed in as a parameter but this is the base directory that it starts from; it cannot read any files which are outside this directory.</td>
      </tr>
      <tr>
        <td>JOB_SERVICE_DEMO_OUTPUT_DIR</td>
        <td>./output&#8209;files</td>
        <td>This directory is used for storing the output from the Language Detection operation.</td>
      </tr>
    </table>

    In order to run multiple instances of the demonstration stack simultaneously it would be necessary to set the `JOB_SERVICE_PORT` parameter to different values for each instance.

3. Deploy the services  
    First navigate to the folder where you have downloaded the files to and then run one of the following commands, depending on whether you are using Docker Compose or Docker Stack:

    <table>
      <tr>
        <td><b>Docker Compose</b></td>
        <td>docker-compose up</td>
      </tr>
      <tr>
        <td><b>Docker Stack</b></td>
        <td>docker stack deploy --compose-file=docker-compose.yml jobservicedemo</td>
      </tr>
    </table>

4. Navigate to the Job Service UI  
    The Job Service is a RESTful Web Service and is primarily intended for programmatic access, however it also ships with a Swagger-generated user-interface.

    Using a browser, navigate to the `/job-service-ui` endpoint on the Job Service:  

        http://docker-host:9411/job-service-ui

    Adjust 'docker-host' to be the name of your own Docker Host and adjust the port if you are not using the default.

5. Try the `GET /jobStats/count` operation  
    Click on this operation and then click on the 'Try it out!' button.

    You should see the response is zero as you have not yet created any jobs.

6. Create a Job  
    Go to the `PUT /jobs/{jobId}` operation.

    - Choose a Job Id, for example, `DemoJob`, and set it in the `jobId` parameter.
    - Enter the following Job Definition into the `newJob` parameter:

        <pre><code>{
          "name": "Some job name",
          "description": "The description of the job",
          "task": {
            "taskClassifier": "BatchWorker",
            "taskApiVersion": 1,
            "taskData": {
              "batchType": "GlobPattern",
              "batchDefinition": "*.txt",
              "taskMessageType": "DocumentMessage",
              "taskMessageParams": {
                "field:binaryFile": "CONTENT",
                "field:fileName": "FILE_NAME",
                "cd:outputSubfolder": "subDir"
              },
              "targetPipe": "languageidentification-in"
            },
            "taskPipe": "globfilter-in",
            "targetPipe": "languageidentification-out"
          }
        }</code></pre>

7. Check on the Job's progress  
    Go to the `GET /jobs/{jobId}` operation.

    - Enter the Job Id that you chose when creating the job.
    - Click on the 'Try it out!' button.

    You should see a response returned from the Job Service.
    - If the job is still in progress then the `status` field will be `Active` and the `percentageComplete` field will indicate the progress of the job.
    - If the job has finished then the `status` field will be `Completed`.

    Given that the Language Detection Worker is configured to output the results to files in a folder you should see that these files have been created in the output folder.  If you examine the output files you should see that they contain the details of what languages were detected in the corresponding input files.

## Override Files
Docker Compose supports the concept of Override Files which can be used to modify the service definitions in the main Docker Compose files, or to add extra service definitions.

The following override files are supplied alongside the main Docker Compose file for the service:

<table>
  <tr>
    <th>Override File</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>docker&#8209;compose.debug.yml</td>
    <td>This override file can be used by developers to help with debugging.  It increases the logging levels, puts the services into a mode where a Java debugger can be attached to them, and exposes endpoints which are not normally exposed outside of the internal network.<p>
    <p>

    The following additional endpoints are exposed to the external network:

    <ol>
        <li>Job Service Database Connection Port</li>
        <li>RabbitMQ UI Port</li>
        <li>Java Debugging Port for all Workers</li>
        <li>Admin / HealthCheck Port for all Workers</li>
    </ol>

    The override file itself can be examined to check which ports these internal ports are exposed on.  The external ports are not used for the normal operation of the services so they can be safely modified if they clash with any other services running on the host.</td>
  </tr>
  <tr>
    <td>docker&#8209;compose.https.yml</td>
    <td>This override file can be used to activate a HTTPS port in the Job Service which can be used for secure communication.<p>
    <p>
    You must provide a keystore file either at the default path (./keystore/.keystore) or a custom path and set the <code>JOB_SERVICE_KEYSTORE</code> environment variable.<p>
    <p>
    The default port exposed for HTTPS communication is 9412 but this can be overridden by supplying the environment variable <code>JOB_SERVICE_PORT_HTTPS</code>.</td>
  </tr>
</table>

Use the -f switch to apply override files.  For example, to start the services with the docker-compose.debug.yml file applied run the following command:

    docker-compose -f docker-compose.yml -f docker-compose.debug.yml up

### Activating HTTPS endpoint

Optionally, the `docker-compose.https.yml` override can be used to activate a HTTPS endpoint for secure communication with the Job Service.

You can generate a default keystore setting both the keystore password and key password as `changeit` by running the following command:

`keytool -genkey -alias tomcat -keystore .keystore -keyalg RSA`

Generating a custom keystore with your own password/alias/protocol is not currently supported. For more information on generating keystores see these [instructions](https://tomcat.apache.org/tomcat-7.0-doc/ssl-howto.html).

Place this keystore file in a folder called `keystore` in job-service-deploy. Name it `.keystore` or else provide your own custom path by setting `JOB_SERVICE_KEYSTORE` (e.g. `./mykeystore/ks.p12`).

You can optionally override the default HTTPS port (9412) by providing the environment variable <code>JOB_SERVICE_PORT_HTTPS</code>.

Run the following command:

`docker-compose -f docker-compose.yml -f docker-compose.https.yml up`.

Additional override parameters can be set and their function is described below.

<table>
  <tr>
    <th>Environment Variable</th>
    <th>Default</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>JOB_SERVICE_PORT_HTTPS</td>
    <td>9412</td>
    <td>This is the HTTPS port to be exposed in the Job Service to allow secure communication. Unless a keystore is provided, the HTTPS port will not be active.</td>
  </tr>
  <tr>
    <td>JOB_SERVICE_KEYSTORE</td>
    <td>./keystore/.keystore</td>
    <td>If you are activating the HTTPS port, you can override the default keystore location to provide your own keystore as a volume. This is the path of the keystore file (i.e. `./mykeystore/ks.p12`).</td>
  </tr>
</table>
