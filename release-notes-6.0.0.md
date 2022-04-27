#### Version Number
${version-number}

#### New Features
- **359648**: taskData no longer sent as Base64.  
    taskData is no longer Base64 encoded in order to reduce message size. 
- **353257**: Flyway replaces Liquibase for database migrations.  
    The job-service-db is now using Flyway for its versioning and migrations.
- **US359648 / US397417**: The Job Service can publish `V4` messages.  
    The Job Service can now send `V3` or `V4` format messages ( `V3` is default ).  
    The `JOB_SERVICE_MESSAGE_OUTPUT_FORMAT` environment variable on the `job-service-scheduled-executor` can be used to control it.

#### Breaking Changes
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
