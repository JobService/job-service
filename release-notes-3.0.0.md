!not-ready-for-release!

#### Version Number
${version-number}

#### Breaking Changes

- [SCMOD-6622](https://portal.digitalsafe.net/browse/SCMOD-6622): Job partitions  
       Jobs are organised into partitions.  All API requests must provide a partition ID in the request path.

#### New Features

#### Known Issues

- [SCMOD-6618](https://portal.digitalsafe.net/browse/SCMOD-6618): Trying to updating a job could incorrectly succeed  
       When creating a job with the same ID as an existing job in the partition, but with a different value for some request body properties, the request would succeed without making any changes to the job.  Now, such requests will fail.
