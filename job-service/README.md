# Job Service

The job-service API is used to create, retrieve, update, delete, suspend, cancel and check the status of jobs. HTTP requests are made to the job service which runs with a base Tomcat image. The job service then stores and retrieves data from the database.

Jobs can be suspended by setting the `CAF_JOB_SERVICE_SUSPENDED_PARTITIONS_REGEX` environment variable.
The variable can be set to a regular expression and can be used to control which partitions should be suspended.

For example to suspend only the "tenant-acme-corp" and "tenant-acme-com" partitions it can be set to `^tenant-acme-co(?rp|m)$`.
If a job is created for a suspended partition then rather than the job being kicked off immediately as it normally would be, the Job Service marks it as suspended and it is not eligible to run at this time.

Suspended jobs can be resumed by updating the `job_task_data` table.
```
UPDATE public.job_task_data
SET suspended=false
WHERE partition_id ~ '^tenant-acme-co(?rp|m)$';

## Job Service Links

[Overview](https://jobservice.github.io/job-service/pages/en-us/Overview)

[Getting Started](https://jobservice.github.io/job-service/pages/en-us/Getting-Started)

[API](https://jobservice.github.io/job-service/pages/en-us/API)

[Features](https://jobservice.github.io/job-service/pages/en-us/Features)

[Architecture](https://jobservice.github.io/job-service/pages/en-us/Architecture)
