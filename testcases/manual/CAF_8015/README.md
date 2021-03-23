## CAF_8015 - Job Service - Add tracking fields to task messages ##

Verify that the Task Messages created by the Job Service contain tracking information

**Test Steps**

1. Use the Job Service swagger UI to create a job
2. Get the message from the Rabbit queue"

**Test Data**

N/A

**Expected Result**

The message will contain the following tracking information:

- jobId
- statusCheckURL
- lastStatusCheckTime
- statusCheckIntervalMilliseconds
- trackingPipe
- trackTo

**JIRA Link** - [CAF-929](https://jira.autonomy.com/browse/CAF-929)
