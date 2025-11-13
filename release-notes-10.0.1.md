#### Version Number
${version-number}

#### New Features
- None

#### Bug Fixes
- D1041027: Improved error handling in the Job Service Scheduled Executor.  
  - A transient error sending a message to RabbitMQ will result in the job being retried during the next execution cycle.
  - A permanent error sending a message to RabbitMQ will result in the job being marked as failed and the job
    will NOT be retried during the next execution cycle.

#### Known Issues
- None
