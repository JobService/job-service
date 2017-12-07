!not-ready-for-release!

#### Version Number
${version-number}

#### New Features
 - [CAF-3893](https://jira.autonomy.com/browse/CAF-3893): Job Tracking Worker logging  
    Job Tracking Worker logging is now controllable via the CAF_LOG_LEVEL environment variable.
 - [CAF-3881](https://jira.autonomy.com/browse/CAF-3881): Clean tables up as we go  
    The Job Service has been changed to clean up the dynamic task tables in the database when all sub tasks belonging to that task are completed. Before, the tables were
    cleaned up when the entire job was completed.

#### Known Issues
