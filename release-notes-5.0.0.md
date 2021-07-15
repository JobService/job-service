!not-ready-for-release!

#### Version Number
${version-number}

#### New Features

#### Breaking Changes
- **SCMOD-13865:** `propagate_failures` option has been removed.  
The job service now propagates all failures and it is no longer possible to disable the propagate failures option. Note that previously it was set to false by default.
- **SCMOD-13870** Increase resilience in the case of rabbitMQ outage.  
New env variable to be added to job-service-scheduled-executor: CAF_RABBITMQ_MAX_PRIORITY to be set to 5

#### Known Issues
- None
