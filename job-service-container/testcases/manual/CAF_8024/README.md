## CAF_8024 - Job Service Web Service Get Jobs Stats Count - negative tests ##

Verify that the Get Jobs Stats Count web service call returns correct error codes when invalid information is provided.

**Test Steps**

Using the Job Service web service perform a Get Jobs Stats Count call entering invalid information.  Also ensure to exercise the additional `statusType` and `jobIdStartsWith` parameters entering invalid information.

**Test Data**

N/A

**Expected Result**

The Get Job Stats Count call returns the expected error codes

**JIRA Link** - [CAF-1941](https://jira.autonomy.com/browse/CAF-1941)