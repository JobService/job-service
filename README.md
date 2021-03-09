# Job Service

Manifest of the components which make up the Job Service:
* [job-service](job-service)
* [job-service-acceptance-tests](job-service-acceptance-tests)
* [job-service-caller](job-service-caller)
* [job-service-internal-client](job-service-internal-client)
* [job-service-container](job-service-container)
* [job-service-contract](job-service-contract)
* [job-service-db](job-service-db)
* [job-service-postgres-container](job-service-postgres-container)
* [job-service-html](job-service-html)
* [job-service-ui](job-service-ui)
* [job-service-scheduled-executor](job-service-scheduled-executor)
* [job-service-scheduled-executor-container](job-service-scheduled-executor-container)
* [worker-jobtracking](worker-jobtracking)
* [worker-jobtracking-container](worker-jobtracking-container)
* [worker-jobtracking-shared](worker-jobtracking-shared)

## Job Tracking Worker Modules

The job tracking worker reports the progress of jobs to the Job Database.

For more information on the functioning of the Job Tracking Worker visit [Job Tracking Worker](worker-jobtracking/README.md).

### worker-jobtracking-shared
This is the shared library defining public classes that constitute the worker interface to be used by consumers of the Job Tracking Worker.
The project can be found in [worker-jobtracking-shared](worker-jobtracking-shared).

### worker-jobtracking
This project contains the actual implementation of the Job Tracking Worker. It can be found in [worker-jobtracking](worker-jobtracking).

### worker-jobtracking-container
This project builds a Docker image that packages the Job Tracking Worker for deployment. It can be found in [worker-jobtracking-container](worker-jobtracking-container).

## Feature Testing
The testing for the Job Service is defined in [testcases](testcases).  

## Maintainers

The following people are responsible for maintaining this code:

- Andy Reid (Belfast, UK, andrew.reid@microfocus.com)
- Dermot Hardy (Belfast, UK, dermot.hardy@microfocus.com)
- Anthony McGreevy (Belfast, UK, anthony.mcgreevy@microfocus.com)
- Thilagavathi Santhoshkumar (Belfast, UK, thilagavathi.santhoshkumar@microfocus.com)
- Michael Bryson (Belfast, UK, michael.bryson@microfocus.com)
- Rahul Kulkarni (Chicago, USA, rahul.kulkarni@microfocus.com)
- Kusuma Ghosh Dastidar (Pleasanton, USA, vgkusuma@microfocus.com)
