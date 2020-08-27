#### Version Number
${version-number}

#### New Features  
- **SCMOD-8484**: Propagate failures through subtasks  
Functionality has been added that allows for the failure of a task to propagate up through any tasks that are no longer able to run because the task has failed.
- **SCMOD-9780**: Updated images to use Java 11 image as their base image
- **SCMOD-9456**: Task tables for subjobs now created base on the identity of the root parent job rather than the short hash, this allows for smaller table names. 
- **SCMOD-9780**: Added job failure roll up functionality. A failure in a prerequisite job can now cause all jobs waiting on it to be set to failure.
- **SCMOD-9552**: Job failure information updated to contain more detailed failure information, if a job fails due to a failure in a prerequisite job, that job id is listed in the failure source.
- **SCMOD-9347**: Added filtering on job searching  
Job searches can now supply filter params based on RSQL to filter which jobs should be returned, these filters can operate on labels, job id's, status, job name, last modified and create dates, partition id and percentage complete.

#### Known Issues
- None

#### Bug Fixes
- **SCMOD-9455**: Updated Job Service Contract
The Job Service contract has been updated to be a true reflection of what is actually returned by the `getJob` and `getJobs` api. 
This change means that the `createTime` and `lastUpdatedTime` properties of a Job will now be represented by a Long Epoch value instead of a String in date-time format.  
- **SCMOD-9529**: Corrected job sorting  
Functionality was previously broken and was returning jobs in a random order, this is now fixed and the order params for get jobs calls returns jobs in the specified orders. 

#### Repository tidy
- **SCMOD-10185**: Removed redundant database procedure creation changesets. 
- **None**: Removed thread from logging statement as this is logged as part of the standard CAF logging format. 
- **None**: Moved frequent log statements to debug level as logs were filling up with. 
