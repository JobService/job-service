#### Version Number
${version-number}

#### New Features
- Support added for dependent jobs. The Job Service can now accept a job and a list of dependent jobs. The Job Service 
  causes the job to wait until all dependent jobs have completed before automatically executing the job.
- [CAF-2349](https://jira.autonomy.com/browse/CAF-2349): Health check added for job tracking worker. 
- [CAF-3656](https://jira.autonomy.com/browse/CAF-3656): Liquibase database installation and upgrade changes.
- Enable multi-process logging. All processes running inside the container now output their log entries to stdout which can be accessed via docker logs.

#### Known Issues
