## CAF_8006 - Job Service Web Service Get Jobs - positive tests ##

Verify that the Get Jobs web service call works as expected

**Test Steps**

Using the Job Service web service perform an Get Jobs call, entering valid information and compare the results with  the job service database. Also ensure to exercise the additional `offset`, `limit`, `statusType` and `jobIdStartsWith` parameters.

**Test Data**

N/A

**Expected Result**

The Get Job call returns the expected results

**JIRA Link** - [CAF-605](https://jira.autonomy.com/browse/CAF-605), [CAF-1826](https://jira.autonomy.com/browse/CAF-1826), [CAF-1838](https://jira.autonomy.com/browse/CAF-1838), [CAF-1909](https://jira.autonomy.com/browse/CAF-1909)
