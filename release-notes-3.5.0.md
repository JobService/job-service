#### Version Number
${version-number}

#### New Features

- SCMOD-9697 - Added support to getJobs REST calls to have the ability to sort by any fields.
- SCMOD-9988 - Lyra RabbitMQ client has been replaced by the RabbitMQ Java client.
- SCMOD-10455 - Fix delay between job completion and reporting  
The jobTracking behavior has been adjusted, so the job  progress isn't updated on the fly but instead stored into a specific table (completed_subtask_report) that will be used "on demand" to update the job progress.
  Also, a new capability has been introduced to process the task report as a bulk instead of individually only.
  A transient exception has been added to the processing of completed tasks so for any issue, the information is kept and resent to the queue instead of being discarded.
  This decreases the pressure on the Postgres instance and allows better performance while reporting a job progress, specifically in the case of "metadata only" job.
- SCMOD-11144: Upgrade postgresql to 42.2.5
- SCMOD-11919: Implement report_progress  
That new feature allows to report a partially completed task to the database. Before that, a partial completion would be simply logged and only completed or failed tasks would be reported.
- SCMOD-11850: Optimized tests to use conditional wait statement

#### Bug Fixes

- SCMOD-11149 - Job-Service only checks if target queue of a job exists instead of attempt to create it.  
  Job-service previously attempted to create the target queue of a new job. If the queue already exists with different settings, this results in a channel error. The old lyra client would silently handle this and recover the channel while the new client does not.
- SCMOD-10792: Don't delete dependent jobs until they're published  
Before that fix, when calling "get_dependent_jobs()", the corresponding rows in the job_task_data table would be removed as part of the process, then the dependent_jobs list would be sent back. If any exception was occurring in between, this would delete the job_task_data rows and result in the job progress not completing. This change would delete the rows only once the report is sent.
- SCMOD-12164: Add publish confirm and retry message 
  The error handling around publishing messages in the job service has now been updated to wait for confirmation from Rabbit that the message was successfully published to the queue.
  A retry has also been added if the publishing fails, and a more helpful error should be logged if this occurs.
  
#### Known Issues

- None
