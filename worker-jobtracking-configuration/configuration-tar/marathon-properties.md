#Marathon Properties
##Using Properties
The marathon-properties.json file lists properties relevant to someone using the Marathon Loader application to launch an environment that includes the Job Tracking Worker. Properties are listed in the file in the form of "property name" and "property value". The value used for a property will be taken from the properties file, inserted into the template json and template config files and sent to Marathon to launch applications.

e.g. A property with name 'marathon-group' and value 'demo' is defined in the marathon-properties.json file as;
<pre><code>...
"marathon-group":"demo",
...</code></pre>
This property name is then present in the template json files that will be passed to Marathon Loader, seen below in an excerpt from the 'marathon-jobtracking-worker.json' file.
<pre><code>...
"id": "${marathon-group}/jobtracking",
...</code></pre>
The '${x}' syntax tells Marathon Loader that the value inside the brackets is the name of a property and that the value of the property should be inserted here. The output sent to Marathon would therefore be;
<pre><code>...
"id": "/demo/workflow",
...</code></pre>
In the marathon-template-config folder there are template configs which have their filenames set to start with 'cfg\_${marathon-group}'.
e.g. for Job Tracking worker a template config file is named 'cfg\_${marathon-group}\_jobtracking\_JobTrackingWorkerConfiguration', and after being run through Marathon Loader would be sent to Marathon as 'cfg\_demo\_jobtracking\_JobTrackingWorkerConfiguration'. Values inside template config files are replaced in the same way they are for values inside the template json files.

###Properties that must be set by user

The properties file has defaults set for the majority of properties however some properties cannot be defaulted and are required to be set before running Marathon Loader. These are identifiable by their value starting with '<MUST_REPLACE:'. It is important that these values are set as failure to do so may cause applications launched using the Marathon Loader to fail. Note that the connectionstring properties only require that part of the property value be replaced, not the entire value.

##Properties List
###Common Properties
These properties are used across multiple json template files rather than being just for a specific application.
####docker-registry
- The docker repository that images will be pulled from. The template json files use this to build up the image name they tell Marathon to pull down. The  image name will start with this property value and then a name specific to the application in the json file and a version tag, an example of this is shown in the excerpt from 'marathon-jobtracking-worker.json' below;<pre><code>"image": "${artifactory}/caf/worker-jobtracking:${jobtracking-version}",</code></pre>

####marathon-group
- This name will be used to group together applications in Marathon, enabling the ability to make applications dependant on others. It will also be used as a prefix for queue names used with workers and to name the configuration files passed to workers. Changing this between runs of Marathon Loader would allow you to launch separate sets of the applications alongside each other that would be independent.

####force-pull
- This is used to tell Marathon that it should always pull the docker image for the application rather than relying on a locally cached version of the image. This should be set to either 'true' or 'false', defaulted to 'false' in the properties file.

####caf-fs-storage-hostPath
- If using the file system based CAF Datastore, this property specifies the path on the host machine to the folder acting as the Datastore. This should be a folder docker can reach to be able to mount the folder on each container.

####marathon-uris-root
- The root folder that contains all files available for Marathon to copy into containers.

####worker-config-location
- The folder within 'marathon-uris-root' that the configuration files for workers reside e.g. If the folder on disk was '/vagrant/config', 'marathon-uris-root' would be '/vagrant' and 'worker-config-location' would be 'config'. This should be the config output folder set for Marathon-loader.

####docker-login-config
- Following the steps [here](https://mesosphere.github.io/marathon/docs/native-docker-private-registry.html) this property specifies the path to the tar containing your docker login configuration file. This is required for each container to be able to pull their image down from Artifactory. **Note this value must be replaced.**

####config-uri
- The folder container the configuration files of the workers. This must either contain complete configuration files or be the the configuration output folder of the Marathon Loader _(specified with the -co argument)_

####storage_service-server
- If using the CAF Storage Service Datastore, this property specifies the URL of the Storage Service. This will be used by all workers to send storage requests. **Note this value must be replaced.**

####storage_service-port
- If using the CAF Storage Service Datastore, this property specifies the port number of the storage Service. **Note this value must be replaced.**

####storage_service-auth-server
- If using the CAF Storage Service Datastore, this property specifies the URL of the Storage Service authentication server. This is the authentication server from which tokens may be obtained for authentication with the Storage Service.**Note this value must be replaced.**

####storage_service-auth-port
- If using the CAF Storage Service Datastore, this property specifies the port number of the Storage Service authentication server. **Note this value must be replaced.**

####storage_service-auth-userName
- If using the CAF Storage Service Datastore, this property specifies the user name to use when obtaining tokens from the Storage Service authentication server. **Note this value must be replaced.**

####storage_service-auth-password
- If using the CAF Storage Service Datastore, this property specifies the password to use when obtaining tokens from the Storage Service authentication server. **Note this value must be replaced.**

####storage_service-auth-clientName
- If using the CAF Storage Service Datastore, this property specifies the client name to use when obtaining tokens from the Storage Service authentication server. **Note this value must be replaced.**

####storage_service-auth-clientSecret
- If using the CAF Storage Service Datastore, this property specifies the client secret to use when obtaining tokens from the Storage Service authentication server. **Note this value must be replaced.**

####storage_service-auth-realm
- If using the CAF Storage Service Datastore, this property specifies the realm to use when obtaining tokens from the Storage Service authentication server. **Note this value must be replaced.**

###Rabbit properties
####rabbit-id
- Specifies the name of the RabbitMQ Server application in Marathon. Changing this will allow the creation of distinct RabbitMQ applications.

####rabbit-cpus:
- Configures the amount of CPU of each RabbitMQ container. This does not have to be a whole number.

####rabbit-mem
- Configures the amount of RAM of each RabbitMQ container. Note that this property does not configure the amount of RAM available to the container but is instead an upper limit. If the container's RAM exceeds this value it will cause docker to destroy and restart the container.

####rabbit-instances
- Configures the number of instances of the RabbitMQ container to start on launch. This value also specifies the minimum number of instances the Autoscaler will scale the application down to.

####rabbit-user
- This property specifies the username that RabbitMQ will use to create it's internal database on start up. This is the username supplied to each worker as part of the RabbitMQ connection configuration as well as the username to use to log into RabbitMQ's UI.

####rabbit-password
- This property specifies the password that RabbitMQ will use for to create it's internal database on start up. This is the password supplied to each worker as part of the RabbitMQ connection configuration as well as the password to use to log into RabbitMQ's UI.

####rabbit-erlang-cookie
- This property is an arbitrary alphanumeric string that RabbitMQ uses to determine whether different Rabbit nodes in a cluster can communicate with each other. If two or more nodes share an identical cookie then RabbitMQ enables the nodes to communicate. Changing this value for another deployment of the RabbitMQ application will mean the two instances will be unable to cluster correctly.

####rabbit-host
- This property is used to specify the IP or hostname of the machine the RabbitMQ application is running on. The property is not used by Rabbit itself but by each worker as part of the RabbitMQ configuration. Misconfiguring this value is the most common cause of a worker failing on start up. **Note this value must be replaced.**

####rabbit-port
-This property is used to specify the Port number of the RabbitMQ application is listening on. The property is not used by Rabbit itself but by each worker as part of the RabbitMQ configuration. The default value Rabbit listens to is 5672.

####rabbit-maxattempts
- This property is used to specify the maximum number of connection attempts a worker will made before throwing a failure to connect exception and shutting down. This property is passed to all workers.

####rabbit-backoffInterval
- This property is used by all workers to specify the time in seconds between each failed attempt to connect to the RabbitMQ server. This value will exponentially increase after each failure up to the value specified by `rabbit-maxBackoffInterval`

####rabbit-maxBackoffInterval
- This property is used by all workers to specify the maximum time in seconds the back off interval can grow to after each consecutive failure.

####rabbit-deadLetterExchange
- This property is meaningless, it is an arbitrary string that specifies a Dead Letter Exchange to the workers that the CAF framework never uses.

###Job Tracking Properties
####jobtracking-cpu
- Configures the amount of CPU of each Job Tracking Worker container. This does not have to be a whole number.

####jobtracking-mem
- Configures the amount of RAM of each Job Tracking Worker container. Note that this property does not configure the amount of RAM available to the container but is instead an upper limit. If the container's RAM exceeds this value it will cause docker to destroy and restart the container.

####jobtracking-java-mem-min
- Configures the minimum memory size available to Java. This value is used by the JVM to reserve an amount of system RAM on start up.

####jobtracking-java-mem-max
- Configures the maximum memory size available to Java. This value is used by the JVM to specify the upper limit of system RAM the Java can consume. This limits the issue of Workers attempting to consume more memory that the container allows causing the application to fail.

####jobtracking-8080-serviceport
- This property specifies the external port number on the host machine that will be forwarded to the Workers internal 8080 port. This port is used to call the workers health check.

####jobtracking-8081-serviceport
- This property specifies the external port number on the host machine that will be forwarded to the Workers internal 8081 port. This port is used to retrieve metrics from the worker.

####jobtracking-autoscale.metrics
- This property maps to a label in the Job Tracking workers application JSON that specifies to the autoscaler which metrics to use for scaling. This should ways be `rabbitmq`.

####jobtracking-autoscale.scalingprofile
- The name of a scaling profile that has already been configured in the RabbitWorkloadAnalyserConfiguration resource within the autoscaler.

####jobtracking-autoscale.maxinstances
- This number represents the maximum instances of the Job Tracking worker the autoscaler have running.

####jobtracking-autoscale.mininstances
- This number represents the minimum instances of the Job Tracking worker the autoscaler have running. Cannot be set below zero.

####jobtracking-healthcheck-graceperiodseconds
- This property specifies the time in seconds Marathon must wait before calling the Job Tracking worker's health check. This allows the application time to finish starting up as a premature health check will return a failure.

####jobtracking-healthcheck-intervalseconds
- This property specifies the time in seconds between each health check call.

####jobtracking-healthcheck-maxconsecutivefailures
- This property specifies the maximum number of times the Job Tracking worker can fail its health check before Marathon considers the application failed and restarts it.

####jobtracking-healthcheck-timeoutseconds
- This property specifies the time in seconds Marathon will wait for the health check to return a response before failing the check.

####jobtracking-resultsize-threshold
- This property specifies the result size limit (in bytes) of the Job Tracking worker at which the result will
be written to the DataStore rather than held in a byte array.

####jobtracking-version
- This property specifies the version number of the Job Tracking Worker to pull down from Artifactory.

####jobtracking-database-url
- This property specifies the address of the Job Service Database.

####jobtracking-database-username
- This property specifies the database username to connect to the Job Service Database.

####jobtracking-database-password
- This property specifies the database password to connect to the Job Service Database.

####jobtracking-threads
- This property configures the number of threads the Job Tracking Worker runs with.
