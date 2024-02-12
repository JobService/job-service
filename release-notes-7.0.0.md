#### Version Number
${version-number}

#### New Features
- **US771133**: Support bulk cancellation of multiple jobs through new API endpoint `/partitions/{partitionId}/jobs:cancel`.
- **US868107**: Support bulk deletion of multiple jobs through new API endpoint `/partitions/{partitionId}/jobs:delete`.

#### Breaking Changes
- **US361030**: SSL configuration environment variables changed  
  The `SSL_TOMCAT_*` environment variables are no longer respected.  
  The following environment variables are now used to configure SSL:
  - `SSL_KEYSTORE_PATH`
  - `SSL_KEYSTORE`
  - `SSL_KEYSTORE_TYPE` (Optional, defaults to `JKS`)
  - `SSL_KEYSTORE_PASSWORD`
  - `SSL_CERT_ALIAS`
  - `SSL_VALIDATE_CERTS` (Optional, defaults to `false`)

- **US361030**: Dropped CORS headers  
  The `Access-Control-Allow-*` headers are no longer returned by the Job Service.

- **D854021:** Worker Framwork V4 Format message support dropped  
  The Job Tracking Worker has been updated to use a new version of the worker framework which no longer supports the V4 format message.<br><br>
  The Job Service Scheduled Executor no longer respects the `JOB_SERVICE_MESSAGE_OUTPUT_FORMAT` environment variable.  The V3 format message is always output regardless of the setting.

#### Known Issues
- None.
