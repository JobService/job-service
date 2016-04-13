# Marathon Properties


## Properties List
### Common CAF Properties
#### docker-registry
- The docker repository that images will be pulled from. The template json files use this to build up the image name they tell Marathon to pull down. The  image name will start with this property value and then a name specific to the application in the json file and a version tag, an example of this is shown in the excerpt from 'marathon-workflow-policy-worker.json' below;<pre><code>"image": "${docker-registry}/policy/worker:${workflow-worker-version}",</code></pre>

#### marathon-group
- This name will be used to group together applications in Marathon, enabling the ability to make applications dependant on others. Changing this between runs of Marathon Loader would allow you to launch separate sets of the applications alongside each other that would be independent.

#### force-pull
- This is used to tell Marathon that it should always pull the docker image for the application rather than relying on a locally cached version of the image. This should be set to either 'true' or 'false', defaulted to 'false' in the properties file.

#### marathon-uris-root
- The root folder that contains all files available for Marathon to copy into containers.

####service-config-location
- The folder within 'marathon-uris-root' that the configuration files for the service reside e.g. If the folder on disk was '/vagrant/config', 'marathon-uris-root' would be '/vagrant' and 'service-config-location' would be 'config'. This should be the config output folder set for Marathon-loader.

#### docker-login-config
- Following the steps [here](https://mesosphere.github.io/marathon/docs/native-docker-private-registry.html) this property specifies the path to the tar containing your docker login configuration file. This is required for each container to be able to pull their image down from Artifactory. **Note this value must be replaced.**

### RabbitMQ
#### rabbit-host
- The host address where RabbitMQ is running.

#### rabbit-port
- The port number where RabbitMQ is accepting messages.

#### rabbit-user
- The username to allow access to the RabbitMQ service.

#### rabbit-password
- The password to allow access to the RabbitMQ service.

### Job Service
#### job-service-cpus
- Configures the amount of CPU of each CAF Job Service container. This does not have to be a whole number.

#### job-service-mem
- Configures the amount of RAM of each CAF Job Service container. Note that this property does not configure the amount of RAM available to the container but is instead an upper limit. If the container's RAM exceeds this value it will cause docker to destroy and restart the container.

#### job-service-instances
- Configures the number of instances of the CAF Job Service container to start on launch.

#### job-service-version
- This property specifies the version number of the CAF Job Service to pull down from Artifactory.

#### job-service-8080-serviceport
- This property specifies the external port number on the host machine that will be forwarded to the containers internal 8080 port. This port is used to call the CAF Job Service web service.

#### job-service-healthcheck-graceperiodseconds
- This property specifies the time in seconds Marathon must wait before calling the CAF Job Service's health check. This allows the application time to finish starting up as a premature health check will return a failure.

#### job-service-healthcheck-intervalseconds
- This property specifies the time in seconds between each health check call.

#### job-service-healthcheck-maxconsecutivefailures
- This property specifies the maximum number of times the CAF Job Service can fail its health check before Marathon considers the application failed and restarts it.

#### job-service-healthcheck-timeoutseconds
- This property specifies the time in seconds Marathon will wait for the health check to return a response before failing the check.

#### job-service-java-mem-min
- Configures the minimum memory size available to Java. This value is used by the JVM to reserve an amount of system RAM on start up.

#### job-service-java-mem-max
- Configures the maximum memory size available to Java. This value is used by the JVM to specify the upper limit of system RAM the Java can consume. This limits the issue of Workers attempting to consume more memory that the container allows causing the application to fail.

#### job-service-config-path
- This property specifies the path to the directory containing the config.properties which can be used as an alternative means of specifying the database environment variables.

#### Job-service-database.username
- This property specifies the username of the database account used by the CAF Job Service.

#### Job-service-database.password
- This property specifies the password of the database account used by the CAF Job Service.

#### Job-service-database.url
- This property specifies the JDBC address of the database account used by the CAF Job Service.

#### Job-service-tracking-pipe
- This is the pipe where the Job Tracking Worker is listening.

#### Job-service-status-check-time
- This is the amount of seconds after which it is appropriate to try to confirm that the task has not been cancelled or aborted.

#### Job-service-web-service-url
- This is the URL address of the job service web api.
