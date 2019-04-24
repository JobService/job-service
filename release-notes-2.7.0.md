#### Version Number

${version-number}

#### New Features

- [SCMOD-6182](https://portal.digitalsafe.net/browse/SCMOD-6182): Add last-update-time to job
- [SCMOD-5074](https://portal.digitalsafe.net/browse/SCMOD-5074), [SCMOD-5834](https://portal.digitalsafe.net/browse/SCMOD-5834), [SCMOD-6265](https://portal.digitalsafe.net/browse/SCMOD-6265): Resolve Fortify version mismatches
- [SCMOD-4900](https://portal.digitalsafe.net/browse/SCMOD-4900): Set volatility at the DB procedure level
- [SCMOD-5725](https://portal.digitalsafe.net/browse/SCMOD-5725): The Quick Start guide now uses Kubernetes deployment files.
- [SCMOD-5211](https://portal.digitalsafe.net/browse/SCMOD-5211): The project "job-service-postgres" which built a test utility docker image containing a pre-installed database has been removed.
- [SCMOD-6351](https://portal.digitalsafe.net/browse/SCMOD-6351): Prerequisite jobs can now be declared as having been pre-created  
	Previously when a job was created with a prerequisite, and that prerequisite job was not found in the system, then it was assumed that the job was yet to be created.  This remains the default behavior, but now an options string can be specified as a suffix with the prerequisite jobs ids, and if one of the options is "`,pc`" (i.e. precreated), then if the job is not found then instead of assuming that it is yet to be created the assumption will be that it was previously created, has completed successfully, and has been deleted. This means that the prerequisite can be effectively ignored.

#### Known Issues

- None

#### Deprecated Features

- [SCMOD-5725](https://portal.digitalsafe.net/browse/SCMOD-5725): The Quick Start guide in docker-compose format has been removed in favour of Kubernetes.