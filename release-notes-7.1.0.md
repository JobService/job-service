#### Version Number
${version-number}

#### New Features
- US915147: New liveness and readiness endpoints added to the Job Tracking Worker.
  - A new `/health-check?name=all&type=ALIVE` endpoint has been added on the default REST port (8080) to check if a worker is alive.
  - A new `/health-check?name=all&type=READY` endpoint has been added on the default REST port (8080) to check if a worker is ready.
  - See the [Worker Framework Documentation](https://github.com/WorkerFramework/worker-framework/tree/develop/worker-core#liveness-and-readiness-checks-within-the-worker-framework)
    for more details.

- US914145: New liveness and readiness endpoints added to the Job Service.
  - A new `/health-check?name=all&type=ALIVE` endpoint has been added on the default REST port (8080) to return the result of the last
    liveness check.
  - A new `/health-check?name=all&type=READY` endpoint has been added on the default REST port (8080) to return the result of the last
    readiness check.
  - The liveness and readiness checks are run on a schedule, which can be configured by the environment variables described in the
    [README.md](https://github.com/JobService/job-service/blob/develop/job-service-container/README.md).

- US914157: New `ports-alive` and `ports-ready` liveness and readiness checks added to the Job Service.  
  - `ports-alive` checks that the server's ports (application and admin) are started and open
  - `ports-ready` checks that the server's ports (application and admin) are accepting connections

#### Bug Fixes
- **US893094**: Prevent creation of a job if any of its prerequisite jobs have failed.
- **I898110**: Remove worker-message-prioritization dependencies.
- **I915151**: Resolve a memory exhaustion issue in the service

#### Known Issues
- None
