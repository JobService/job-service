## CAF_8019 - Job Service - Full end to end test ##

Verify that messages continue through the workflow while being tracked

**Test Steps**

1. Setup a system with Job Service, Job Service Database, Batch Worker, Job Tracking Worker and Example Worker
2. Ensure that the input and output queues are defined correctly
3. Send a job to the Batch Worker via the Job Service UI with valid details in the testData field
4. Observe the output queue of the Example Worker and the logs of the Job Tracking Worker

**Test Data**

N/A

**Expected Result**

- The message output onto the Example worker output queue will not contain any tracking information
- The Job Tracking Worker stdout log file will contain messages as follows:
- Message is registered and split into separate tasks by the Batch Worker
- Separate messages are forwarded to the Example Worker input queue
- Job Status check returns Active for separated messages
- Single message forwarded to the Batch Worker output queue
- Job Status check returns Completed for separated messages
- Separate messages forwarded to the Example Worker output queue
- Tracking information is removed from separate messages

**JIRA Link** - [CAF-761](https://jira.autonomy.com/browse/CAF-761)

