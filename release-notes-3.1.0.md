#### Version Number
${version-number}

#### New Features

- **SCMOD-6955**: Job types  
       Job types can be defined, and new jobs can target a type to use a well-defined, type-specific input format.
- **SCMOD-7309**: NotFinished job status filter  
        The statusType parameter accepts a new value, which includes only jobs which haven't reached a final state.
- **SCMOD-7308**: Job list sorting  
        The sort parameter has been added to the jobsGet API.

#### Bug fixes
- **SCMOD-7065**: Fixed HTTP status for parallel duplicate job creation
- **SCMOD-7143**: Fixed WorkerCallback update for TaskInformation 
- **SCMOD-7336**: Job-Tracking failure fixed during Postgres failover 

#### Known Issues
- None
