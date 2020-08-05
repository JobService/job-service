#### Version Number
${version-number}

#### New Features  
- **SCMOD-8484**: Propagate failures through subtasks  
Functionality has been added that allows for the failure of a task to propagate up through any tasks that are no longer able to run because the task has failed.
- **SCMOD-9780**: Updated images to use Java 11

#### Known Issues
- None

#### Bug Fixes
- **SCMOD-9455**: Updated Job Service Contract
The Job Service contract has been updated to be a true reflection of what is actually returned by the `getJob` and `getJobs` api. 
This change means that the `createTime` and `lastUpdatedTime` properties of a Job will now be represented by a Long Epoch value instead of a String in date-time format.  
