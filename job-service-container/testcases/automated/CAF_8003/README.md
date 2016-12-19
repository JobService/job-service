## CAF_8003 - Job Service Web Service Add Job - negative tests##

Verify that the Add Job web service call returns correct error codes when invalid information is provided

**Test Steps**

Using the Job Service web service perform an Add Job, entering invalid information and then check the job service database

**Test Data**

N/A

**Expected Result**

The Add Job call returns the expected error codes and the database is not updated with the relevant Job information

**JIRA Link** - [CAF-605](https://jira.autonomy.com/browse/CAF-605)
