!not-ready-for-release!

#### Version Number
${version-number}

#### New Features
- US1124088: job-service enhancement.
    - Splits the `DocumentWorkerTask` job with large payload containing many subdocuments into multiple smaller messages to prevent Out-of-Memory (OOM) errors while maintaining single job identity and existing progress tracking.
    - Enabled via `DocumentWorkerSubdocumentBatcher() ` prefix in the task pipe for top-level job definitions.
    - Added environment variable `JOB_SERVICE_PAYLOAD_BATCH_SIZE` in job-service-scheduled-executor config to configure the batch size which defaults to `200` if not found.
- **US1138334**: Updated to run on Java 25.

#### Known Issues
