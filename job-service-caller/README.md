# job-service-caller
A script to call the Job Service which creates a new job and waits on job completion or failure. The script requires a job identifier, a
job definition and the web service url for the CAF Job Service.

## Usage

The CAF Job Service should be deployed prior to running the script.
See the [Getting Started](https://jobservice.github.io/job-service/pages/en-us/Getting-Started) guide for this.

The script can be invoked using Python:

    python createJob.py [-h] -j jobId -u jobWebServiceURL [-c correlationId] [-f jobDefinitionFilename] [-p pollingInterval]

where:

- **-j** or **--job jobId** (required)
    - specifies the job identifier.
- **-u** or **--url jobWebServiceURL** (required)
    - specifies the CAF Job Service URL.
- **-h** or **--help** (optional) 
    - used to print a short description of all command line options.
- **-c** or **--correlation correlationId** (optional) 
    - specifies the correlation identifier.
- **-f** or **--filename jobDefinitionFilename** (optional) 
    - specifies the file name comprising the definition of the job to create.
      If the file name is not specified, the job definition will be read from stdin.
- **-p** or **--polling pollingInterval** (optional) 
    - specifies the polling interval in seconds used to wait on job completion.

### Display Help

Run the following to print a short description of the command line options:

    python createJob.py -h

Output:

    usage: createJob.py [-h] -j jobId -u jobWebServiceURL [-c correlationId] [-f jobDefinitionFilename] [-p pollingInterval]
    
    Create a new job.
    
    required arguments:
      -j jobId, --job jobId                                             job identifier
      -u jobWebServiceURL, --url jobWebServiceURL                       job web service url
    
    optional arguments:
      -h, --help                                                        show this help message and exit
      -c correlationId, --correlation correlationId                     correlation identifier
      -f jobDefinitionFilename, --filename jobDefinitionFilename        file name comprising the definition of the job to create
      -p pollingInterval, --polling pollingInterval                     polling interval



### Create a New Job

Sample usage instructions for creating a new job are provided next.

#### Create Job Definition JSON File

A JSON file (e.g. MyJobDefinition.json) comprising the job definition should be generated in the directory containing the job creation
script. A sample JSON has been provided next.

    {
        "name": "TestJob",
        "description": "Test description for end-to-end test job.",
        "externalData": "string",
        "task": {
            "taskClassifier": "BatchWorker",
            "taskApiVersion": 1,
            "taskData": "{\"batchDefinition\":\"[\\\"b591d8c6615c4af99d7915719b01259c/3a44156891e645c6828cfe47667f159f\\\"]\",\"batchType\":\"AssetIdBatchPlugin\",\"taskMessageType\":\"ExampleWorkerTaskBuilder\",\"taskMessageParams\":{\"datastorePartialReference\":\"b591d8c6615c4af99d7915719b01259c\",\"action\":\"REVERSE\"},\"targetPipe\":\"dataprocessing-example-in\"}",
            "taskDataEncoding": "utf8",
            "taskPipe": "dataprocessing-batch-in",
            "targetPipe": "dataprocessing-example-out"
        }
    }


#### Run Script

##### Job Definition by File
The JSON file created above can be used to create a new job by running the following:

    python createJob.py -j TestJob_1 -u http://vagrant-mesos:9410 -f MyJobDefinition.json

where:

- `TestJob` is the job identifier.
- `http://vagrant-mesos:9410` is the CAF Job Web Service URL.
- `MyJobDefinition.json` is the name of the file in the current directory comprising a job definition.

##### Job Definition by Standard Input
The job definition can also be provided via stdin by running the following:

    cat MyJobDefinition.json | python createJob.py -j TestJob_2 -u http://vagrant-mesos:9410
