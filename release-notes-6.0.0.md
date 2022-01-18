!not-ready-for-release!

#### Version Number
${version-number}

#### New Features
- **[SCMOD-5524](https://portal.digitalsafe.net/browse/SCMOD-5524)** taskData no longer sent as Base64.  
    taskData is no longer Base64 encoded in order to reduce message size. 
  - **SCMOD-15887**: job-service-db migrated from Liquibase to Flyway. If the database does not exist or is empty, it will be created, then upgraded to the latest version.

#### Breaking Changes
- **378627**: Workers built using framework version prior to 5.1.0 are no longer supported.
  - The format of the messages sent to the workers has been changed ( Base64 format no longer used )

#### Known Issues
- None
