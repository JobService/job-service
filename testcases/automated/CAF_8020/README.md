## CAF_8020 - Job Service - Prevent Cancelled Job from being reactivated ##

Verify that Jobs that have been cancelled cannot be reactivated by the Job Service report_progress stored procedure

**Test Steps**

Using the Job Service web service create a Job and then perform a Cancel Job call. Once the Job is reported as cancelled call the stored procedure report_progress is called with status Active and a job id that doesn't include the dot separator.

**Test Data**

N/A

**Expected Result**

The Job should remain cancelled in the database

**JIRA Link** - [CAF-1015](https://jira.autonomy.com/browse/CAF-1015)
