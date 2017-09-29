#!/usr/bin/python
#
# Copyright 2015-2017 EntIT Software LLC, a Micro Focus company.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

#
# ******************************************************************************
# * Script to create a new job using the CAF Job Service. The script continues
# * to poll the CAF Job Service until the job has completed or failed.
# *****************************************************************************
#
# usage: createJob.py [-h] -j jobId -u jobWebServiceURL [-c correlationId] [-f jobDefinitionFilename] [-p pollingInterval]

import argparse                 # for command line parsing
from datetime import datetime   # for printing date and time as part of logging
import json                     # for JSON parsing and validation
import requests                 # for calling the CAF Job Service
import sys
import time
import re                       # for string substitution

# CAF Job Service endpoint.
job_service_api_endpoint = '/job-service/v1/jobs/'

# Global job identifier and definition.
job_id = ''
job_definition = ''

# Log message to standard output.
def log(message):
    print (datetime.now(), message)

def successful_termination():
    sys.exit()

def abnormal_termination():
    sys.exit(1)

# Parse and validate command line arguments.
def parse_args():
    parser = argparse.ArgumentParser(description='Create a new job.')

    # Required arguments.
    required_named = parser.add_argument_group('required arguments')
    required_named.add_argument('-j', '--job', dest='jobId', help='job identifier', required=True, metavar='jobId')
    required_named.add_argument('-u', '--url', dest='jobWebServiceURL', help='job web service url', required=True, metavar='jobWebServiceURL')

    # Optional arguments.
    optional = parser.add_argument_group('optional arguments')
    optional.add_argument('-c', '--correlation', dest='correlationId', default='', help='correlation identifier', metavar='correlationId')
    optional.add_argument('-f', '--filename', dest='jobDefinitionFilename', type=argparse.FileType('r'), default=sys.stdin, help='file name comprising the definition of the job to create', metavar='jobDefinitionFilename')
    optional.add_argument('-p', '--polling', dest='pollingInterval', default=30, type=int, help='polling interval', metavar='pollingInterval')

    args = parser.parse_args()

    # Validate input arguments.
    validate_args(args)

    return args

# Attempt to validate input JSON.
def is_valid_json(json_to_validate):
    try:
        json.loads(json_to_validate)
    except ValueError:
        return False
    return True

# Validate command line arguments.
def validate_args(args):
    log('Validating command line arguments ...')

    # Make sure input arguments are not empty.
    if args.jobId == "":
        log('The job identifier argument is empty. Exiting.')
        abnormal_termination()
    else:
        # Replace unsupported job identifier characters.
        global job_id
        job_id = re.sub(r'[.,:;*?!|()]',r'_',args.jobId)

        # Job identifiers longer than 48 chars not supported by Job Service.
        if len(job_id) > 48:
            log('The job identifier is too long. Exiting.')
            abnormal_termination()

    if args.jobDefinitionFilename is not None:
        # Make sure the supplied job definition is valid JSON.
        try:
            global job_definition
            job_definition = json.dumps(json.load(args.jobDefinitionFilename))

            if not is_valid_json(job_definition):
                log('The job definition provided is not valid JSON. Exiting.')
                abnormal_termination()
        except IOError:
            log('Error reading job definition file. Exiting.')
            abnormal_termination()
    else:
        log('The job definition has not been provided. Exiting.')
        abnormal_termination()

    if args.jobWebServiceURL == "":
        log('The job web service url argument is empty. Exiting.')
        abnormal_termination()

# Create a new job via the CAF Job Service.
def call_put_job_service(uri, job_definition):
    log('Calling CAF Job Service to create a new job with job definition %s ...' % job_definition)

    headers = {'Content-Type': 'application/json', 'Accept': 'application/json'}

    # Issue call to the CAF Job Service to create a new job using the specified job definition.
    put_response = requests.put(uri, data=job_definition, headers=headers)

    # For successful API call, response code should be 200 (OK).
    if not put_response.ok:
        log('Job creation has failed with %s ...' % put_response.raise_for_status())
        abnormal_termination()

# Retrieve job metadata via the CAF Job Service including status.
def call_get_job_service(uri):
    log('Calling CAF Job Service to retrieve job status ...')

    # Issue call to the CAF Job Service to retrieve the job metadata.
    headers = {'Accept': 'application/json'}
    get_response = requests.get(uri, headers=headers)

    # For successful API call, response code should be 200 (OK).
    if get_response.ok:
        # Load the job metadata into a dict variable and return.
        json_object = json.loads(get_response.content)
        return json_object
    else:
        log(get_response.raise_for_status())
        abnormal_termination()

# Continue to poll the CAF Job Service until job has completed or failed.
def poll_job_service(uri, polling_interval):
    log('Polling CAF Job Service awaiting job completion ...')

    is_completed = False
    while not is_completed:
        job_response = call_get_job_service(uri)

        if job_response["status"] == 'Completed' or job_response["status"] == 'Cancelled':
            # Job has completed.
            is_completed = True
            log('Job creation completed.')
            successful_termination()
        elif job_response["status"] == 'Failed':
            # Job has failed.
            log('Job creation failed ...')
            abnormal_termination()
        else:
            # Job is still in active state. Go to sleep and retry.
            log('Going to sleep for ' + str(polling_interval) + ' seconds awaiting job completion ...')
            time.sleep(polling_interval)

def main():
    # Validate and parse command line arguments.
    args = parse_args()

    # Construct uri to be used in calls to the CAF Job Service.
    uri = args.jobWebServiceURL + job_service_api_endpoint + job_id

    # Call CAF Job Service to create a new job.
    call_put_job_service(uri, job_definition)

    # Poll CAF Job Service awaiting completion of the newly created job.
    poll_job_service(uri, args.pollingInterval)

if __name__ == '__main__':
    # Only want to run this as the main file.
    main()
