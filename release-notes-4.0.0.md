!not-ready-for-release!

#### Version Number
${version-number}

#### New Features
- SCMOD-12707: Added support for pausing and resuming jobs.
  - Three new endpoints have been added to pause, resume, and get the status of a job.

#### Breaking Changes
- SCMOD-12505: Various updates to the 'Job Types' functionality.  
  - The `taskDataScript` property should no longer be provided when adding a new job type yaml file. Instead, a `taskScript` property
  should be provided. See the [Job-Types](https://jobservice.github.io/job-service/pages/en-us/Job-Types) documentation for more
  information.
  - The names used for the `configurationProperties` now have a direct mapping to the environment variables used to populate them. For
  example, if a configuration property is named `TASK_PIPE` in a job type yaml definition, then the value for that configuration property
 is expected to be available in an environment variable named `TASK_PIPE`.

#### Known Issues
- None
