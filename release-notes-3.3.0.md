#### Version Number
${version-number}

#### New Features
- None

#### Bug Fixes 
- [SCMOD-9193](https://portal.digitalsafe.net/browse/SCMOD-9193): Add SQL failover handling  
During an SQL failover event the Worker Job Tracking project has been updated to treat SQL failures as transient issues so that tasks are retried and not thrown away as fatal failures.  

#### Known Issues  
- None  
