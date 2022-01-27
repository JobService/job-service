!not-ready-for-release!

#### Version Number
${version-number}

#### New Features
- **359648** taskData no longer sent as Base64.  
    taskData is no longer Base64 encoded in order to reduce message size. 
- **353257**: Flyway replaces Liquibase for database migrations.  
    The job-service-db is now using Flyway for its versioning and migrations.

#### Breaking Changes
- **359648**: Workers built using framework version prior to 5.1.0 are no longer supported.
  - The format of the messages sent to the workers has been changed ( Base64 format no longer used )

#### Known Issues
- None
