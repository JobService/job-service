## CAF_8011 - Job Service Web Service Cancel Job - negative tests ##

Verify that the Cancel Job web service call returns correct error codes when invalid information is provided

**Test Steps**

Using the Job Service web service perform an Cancel Job, entering invalid information and then check the job service database

**Test Data**

N/A

**Expected Result**

The Cancel Job call returns the expected error codes and the database is not updated

**JIRA Link** - [CAF-605](https://jira.autonomy.com/browse/CAF-605)
