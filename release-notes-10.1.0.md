!not-ready-for-release!

#### Version Number
${version-number}

#### New Features
- None

#### Bug Fixes
- D1041027: Improved error handling (Job Service Scheduled Executor)   
  - Non-transient errors when sending a message to the target queue will now result in the job being marked as failed 
  in the database and the job will not be retried.

#### Known Issues
- None
