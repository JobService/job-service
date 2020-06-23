#### Version Number
${version-number}

#### New Features  
- SCMOD-8484: Propagate failures through subtasks  
Functionality has been added that allows for the failure of a task to propagate up through any tasks that are no longer able to run because the task has failed.  

#### Bug Fixes
- SCMOD-9913: The JSLT parser used for parsing the [taskDataScript](https://github.com/JobService/job-service/blob/develop/docs/pages/en-us/Job-Types.md#taskdatascript)
has been changed so that JSON properties are excluded from the output if the property value is null, an empty array, or an empty object.

#### Known Issues
