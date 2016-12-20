## CAF_8007 - Job Service Web Service Get Jobs - negative tests ##

Verify that the Get Jobs web service call returns correct error codes when invalid information is provided

**Test Steps**

Using the Job Service web service perform a Get Jobs call entering invalid information.  Also ensure to exercise the additional `offset`, `limit`, `statusType` and `jobIdStartsWith` parameters entering invalid information.

**Test Data**

N/A

**Expected Result**

The Get Job call returns the expected error codes

**JIRA Link** - [CAF-605](https://jira.autonomy.com/browse/CAF-605), [CAF-1826](https://jira.autonomy.com/browse/CAF-1826), [CAF-1838](https://jira.autonomy.com/browse/CAF-1838), [CAF-1909](https://jira.autonomy.com/browse/CAF-1909)
