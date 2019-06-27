#### Version Number
${version-number}

#### Breaking Changes

- All API requests must provide a partition ID in the request path.

#### New Features

- [SCMOD-6622](https://portal.digitalsafe.net/browse/SCMOD-6622): Job partitions  
       Jobs are organised into partitions, which provide isolation between groups of jobs.

#### Bug Fixes

- [SCMOD-6620](https://portal.digitalsafe.net/browse/SCMOD-6620): Deadlock creating a job  
        It was previously possible for a deadlock to occur when creating a job, or when a job completed.

#### Known Issues

- None
