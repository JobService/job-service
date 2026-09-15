!not-ready-for-release!

#### Version Number
${version-number}

#### New Features
- **US1138334**: Updated to run on Java 25.
- **US1107289**: Add support for PQC TLS Hybrid exchange.
- **US1212235**: OpenTelemetry support is added to `job-service` by using the OTel configured `oraclelinux-jre25-otel` base image and 
  conditionally enabling Java auto-instrumentation at startup when `OTEL_JAVAAGENT_ENABLED=true`.

#### Known Issues
