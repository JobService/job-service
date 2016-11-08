## CAF_8022 - Recursion of batches goes to two levels or more ##

Verify that arrays of arrays are successfully split before sending through the Job Service.

**Test Steps**

- Deploy job-service, batch worker and example worker using the Chateau toolset. You will need the latest worker-batch-plugins-package.tar.gz.
NOTE, please do not use the latest job-service-db just yet as you will want to reproduce the original issue first.
Using the Job-Service UI, send a message to the batch-worker making use of the assetid-batch-plugin. A sample message is below which you may need to modify:

```
{
  "name": "ExampleBatchPlugin_Job_1",
  "description": "example-batch-worker-plugin-test",
  "externalData": "",
  "task": {
    "taskClassifier": "BatchWorker",
    "taskApiVersion": 1,
    "taskData": "{\"batchDefinition\":\"[\\\"66d1c33ec8c74fee99d207e7521a93ec/9f97451f75024be2a4017d2c17dca0b4\\\",\\\"66d1c33ec8c74fee99d207e7521a93ec/9f97451f75024be2a4017d2c17dca0b4\\\",\\\"66d1c33ec8c74fee99d207e7521a93ec/9f97451f75024be2a4017d2c17dca0b4\\\",\\\"66d1c33ec8c74fee99d207e7521a93ec/9f97451f75024be2a4017d2c17dca0b4\\\",\\\"66d1c33ec8c74fee99d207e7521a93ec/9f97451f75024be2a4017d2c17dca0b4\\\",\\\"66d1c33ec8c74fee99d207e7521a93ec/9f97451f75024be2a4017d2c17dca0b4\\\",\\\"66d1c33ec8c74fee99d207e7521a93ec/9f97451f75024be2a4017d2c17dca0b4\\\",\\\"66d1c33ec8c74fee99d207e7521a93ec/cfd59ecef76d4ae99581ac62409c3302\\\"]\",\"batchType\":\"AssetIdBatchPlugin\",\"taskMessageType\":\"ExampleWorkerTaskBuilder\",\"taskMessageParams\":{\"datastorePartialReference\":\"66d1c33ec8c74fee99d207e7521a93ec\",\"action\":\"REVERSE\"},\"targetPipe\":\"dataprocessing-example-in\"}",
    "taskDataEncoding": "utf8",
    "taskPipe": "dataprocessing-batch-in",
    "targetPipe": "dataprocessing-example-out"
  }
}
```

- Verify the job has been created but it should have failed with the error reported in the JIRA in the jobtracking-worker logs.
- If you are keeping an eye on the DB while the message is being processed, you should see multiple task tables being created for the Job.
- Replace database with the latest job service db and repeat step 2.

**Test Data**

N/A

**Expected Result**

Verify the job is created and reported completed as expected without the error reported in the JIRA.

**JIRA Link** - [CAF-1740](https://jira.autonomy.com/browse/CAF-1740)

