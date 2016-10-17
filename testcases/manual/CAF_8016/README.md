## CAF_8016 - Job Service - Message Forwarding Correct Queue ##

Verify that messages are checked that they are intended for the queue they were sent to

**Test Steps**

1. Publish a message to a workers input queue that has the "To:" field defined as the same worker input queue
2. Examine the worker stdout log file

**Test Data**

N/A

**Expected Result**

There will be a message in the stgout log file stating "(message id: 'n') on input queue demo-extract-in is intended for this worker" and the message will be processed as expected

**JIRA Link** - [CAF-598](https://jira.autonomy.com/browse/CAF-598)
