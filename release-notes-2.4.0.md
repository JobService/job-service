#### Version Number
${version-number}

#### New Features
 - [CAF-3881](https://jira.autonomy.com/browse/CAF-3881): Clean tables up as early as possible  
    The Job Service has been changed to clean up the dynamic task tables in the database when all subtasks belonging to that task have been completed. Previously the tables were cleaned up only when the entire job was completed.

 - [CAF-3893](https://jira.autonomy.com/browse/CAF-3893): Job Tracking Worker logging  
    Job Tracking Worker logging is now controllable via the `CAF_LOG_LEVEL` environment variable.

 - [CAF-3904](https://jira.autonomy.com/browse/CAF-3904): Errors reporting progress  
    The Job Tracking Worker has been changed to support a catch and retry mechanism in the event of a database concurrency related exception being detected.

 - [CAF-3920](https://jira.autonomy.com/browse/CAF-3920): Health check exposed on separate port  
    The Job Service has been updated to expose a `/healthcheck` endpoint which can be used by Docker or the container orchestrator.  This endpoint is exposed on a separate port from the service's main operations.

#### Known Issues
 - None

#### Changes
 - The complete list of changes is available [here](https://jira.autonomy.com/issues/?jql=project%20%3D%20CAF%20AND%20fixVersion%20%3D%20"Job%20Service%202.4.0").
