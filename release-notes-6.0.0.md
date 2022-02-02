!not-ready-for-release!

#### Version Number
${version-number}

#### New Features
- **359648**: taskData no longer sent as Base64.  
    taskData is no longer Base64 encoded in order to reduce message size. 
- **353257**: Flyway replaces Liquibase for database migrations.  
    The job-service-db is now using Flyway for its versioning and migrations.

#### Breaking Changes
- **359648**: Workers built using framework version prior to 5.1.0 are no longer supported.
  - The format of the messages sent to the workers has been changed ( Base64 format no longer used )
- **353257**: The following environment variables are no longer supported:`JOB_SERVICE_DATABASE_URL`, `JOB_SERVICE_DATABASE_HOSTNAME`, 
  `CAF_DATABASE_URL`, `JOB_DATABASE_USERNAME`, `JOB_DATABASE_PASSWORD`, `JOB_DATABASE_APPNAME`, `CAF_DATABASE_USERNAME`, 
  `CAF_DATABASE_PASSWORD`, `CAF_DATABASE_APPNAME`. The variables now align with our [standard](https://github.com/CAFapi/opensuse-base-image#database-creation-script) such as:
    - `JOB_DATABASE_USERNAME` and `CAF_DATABASE_USERNAME` are replaced with `JOB_SERVICE_DATABASE_USERNAME`
    - `JOB_DATABASE_PASSWORD` and `CAF_DATABASE_PASSWORD` are replaced with `JOB_SERVICE_DATABASE_PASSWORD`
    - `JOB_DATABASE_APPNAME` and `CAF_DATABASE_APPNAME` are replaced with `JOB_SERVICE_DATABASE_APPNAME`
    - `JOB_SERVICE_DATABASE_HOSTNAME` is replaced with `JOB_SERVICE_DATABASE_HOST`
    - `JOB_SERVICE_DATABASE_URL` and `CAF_DATABASE_URL` are replaced with `JOB_SERVICE_DATABASE_HOST`, `JOB_SERVICE_DATABASE_PORT` and `JOB_SERVICE_DATABASE_NAME` altogether.

#### Known Issues
- None
