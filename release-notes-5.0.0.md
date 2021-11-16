#### Version Number
${version-number}

#### New Features
- None

#### Breaking Changes
- **SCMOD-13865**: `propagate_failures` option has been removed.  
The job service now propagates all failures and it is no longer possible to disable the propagate failures option. Note that previously it was set to false by default.
- **SCMOD-13870**: Increase resilience in the case of rabbitMQ outage.  
New env variable to be added to job-service-scheduled-executor: CAF_RABBITMQ_MAX_PRIORITY to be set to 5

#### Bug Fixes
- **SCMOD-15074**: Fixed job cancellation and job deletion out of memory issues.

#### Known Issues
- None
