#### Version Number
${version-number}

#### New Features
- [SCMOD-6182](https://portal.digitalsafe.net/browse/SCMOD-6182): Last Updated Time field  
	When retrieving the status of a job a new `lastUpdateTime` field is returned which provides the last time any activity happened on the job, such as when it was last progressed or completed.

- [SCMOD-5725](https://portal.digitalsafe.net/browse/SCMOD-5725): Kubernetes Quick Start  
	A Quick Start guide has been added for using the Job Service with Kubernetes.

- [SCMOD-4900](https://portal.digitalsafe.net/browse/SCMOD-4900): Added volatility classifications for Job service Database.

#### Bug Fixes

- [SCMOD-6216](https://portal.digitalsafe.net/browse/SCMOD-6216): Prerequisite jobs could be ignored  
	Previously a job which had multiple prerequisite jobs might have been erroneously started before all of the prerequisite jobs had been completed.  In particular this could happen if some of the prerequisite jobs had not yet been created.  This has been corrected.

- Specified delay could be ignored  
	Previously if a delay was specified then it was ignored if there were no prerequisite jobs or if they were already complete.  This has been corrected.

#### Deprecated Features
- [SCMOD-4883](https://portal.digitalsafe.net/browse/SCMOD-4883): Pre-installed PostgreSQL Docker Image  
	Previous versions contained a PostgreSQL Docker Image which had the Job Service pre-installed.  This of course was never useful for production but was useful for testing.  It is not available for this release.  It may or may not be re-introduced in the future.

- [SCMOD-5725](https://portal.digitalsafe.net/browse/SCMOD-5725): Docker Compose Quick Start  
	The Docker Compose Quick Start guide has been removed.  The Job Service can of course still be used with Docker Compose and this is still supported.  The Docker Compose Quick Start guide may be re-introduced in the future.

- [SCMOD-5211](https://portal.digitalsafe.net/browse/SCMOD-5211): The project "job-service-postgres" which built a test utility docker image containing a pre-installed job-service database has been removed.

#### Known Issues
- None
