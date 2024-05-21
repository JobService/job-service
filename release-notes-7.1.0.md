!not-ready-for-release!

#### Version Number
${version-number}

#### New Features
US915147: New liveness and readiness endpoints added to Job Tracking Worker and TODO
- A new `/health-check?name=all&type=ALIVE` endpoint has been added on the default REST port (8080) to check if a worker is alive.
- A new `/health-check?name=all&type=READY` endpoint has been added on the default REST port (8080) to check if a worker is ready.
- See the [Worker Framework Documentation](https://github.com/WorkerFramework/worker-framework/tree/develop/worker-core#liveness-and-readiness-checks-within-the-worker-framework)
  for more details.

#### Bug Fixes
- **US893094**: Prevent creation of a job if any of its prerequisite jobs have failed.
- **I898110**: Remove worker-message-prioritization dependencies.
- **I915151**: Resolve a memory exhaustion issue in the service

#### Known Issues
- None
