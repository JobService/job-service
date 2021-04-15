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
- SCMOD-12730: Workers built using framework versions prior to 5.0.0 are no longer supported.
  - The `statusCheckUrl` now points to the `status` endpoint instead of the `isActive` endpoint.
  - Instead of returning `true` or `false`, the `statusCheckUrl` will now return one of `Active`, `Cancelled`, `Completed`, `Failed`,
  `Paused`, or `Waiting`
  - If the `statusCheckUrl` refers to a job that does not exist, the response from the endpoint will return a HTTP 404 status. Previously,
  the `statusCheckUrl` would have returned a HTTP 200 status with a response body of `false` when a job did not exist.
  - The `CAF_STATUS_CHECK_TIME` environment variable has been renamed to `CAF_STATUS_CHECK_INTERVAL_SECONDS`.
  - A new `CAF_JOB_SERVICE_RESUME_JOB_QUEUE` environment variable is required. This should point to the input queue of the worker that
    is responsible for dispatching task messages for resumed jobs to the appropriate workers.

#### Known Issues
- None
