# worker-jobtracking-container

This is a docker container for the Job Tracking Worker. It consists of the Job Tracking Worker which can be run by passing in the required configuration files to the container. It uses the 'java:8' base image.

## Configuration

### Configuration Files
The worker requires configuration files to be passed through for;

* JobTrackingWorkerConfiguration
* RabbitWorkerQueueConfiguration
* StorageServiceDataStoreConfiguration

### Environment Variables
##### CAF\_CONFIG\_PATH
The location of the configuration files to be used by the worker. Common to all workers.

##### DROPWIZARD\_CONFIG\_PATH
The full path of the dropwizard configuration file that the worker should use. This can be used to control various dropwizard options such as logging output level. This is optional and the worker shall default to the configuration file included in the image if this is not provided.

##### JOB\_DATABASE\_URL
The address of the Job Service Database.

##### JOB\_DATABASE\_USERNAME
The database username to connect to the Job Service Database.

##### JOB_DATABASE_PASSWORD
The database password to connect to the Job Service Database.
