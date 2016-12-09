## CAF_8013 - Job Service Web Service Delete Job - negative tests ##

Verify that the Delete Job web service call returns correct error codes when invalid information is provided

**Test Steps**

Using the Job Service web service perform an Delete Job, entering invalid information and then check the job service database

**Test Data**

N/A

**Expected Result**

The Delete Job call returns the expected error codes and the database is not updated with the Job not being deleted

**JIRA Link** - [CAF-605](https://jira.autonomy.com/browse/CAF-605)
