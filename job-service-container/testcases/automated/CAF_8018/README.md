## CAF_8018 - Job Service - Message Forwarding Discard Message ##

Verify that messages are discarded if the job is inactive

**Test Steps**

Create a job and before the message reaches the Job Tracking Worker cancel the job

**Test Data**

N/A

**Expected Result**

The Job Tracking Worker will check that the job is active and will then discard the message

**JIRA Link** - [CAF-598](https://jira.autonomy.com/browse/CAF-598)

