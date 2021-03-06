---
layout: default
title: Job Types

banner:
    icon: 'assets/img/fork-lift.png'
    title: Job Service
    subtitle: Orchestration, Management and Monitoring of Data Processing
    links:
        - title: GitHub
          url: https://github.com/JobService/job-service
---

# Job Types

Job Service can be configured with job types by including a number of job type definition files.  A new job can specify a job type by omitting the `task` object in the request body, and instead providing `type` and `parameters`.  The job type definition defines how to construct the omitted `task` object from the `parameters` object.

## Configuring Job Types

To configure Job Service with job types, define the environment variable `CAF_JOB_SERVICE_JOB_TYPE_DEFINITIONS_DIR`.  This is the path to a directory containing job type definition files with the '.yaml' extension.  The filename, excluding the '.yaml' extension, is used as a unique identifier for the job type.  Changes to these files do not take effect until Job Service is restarted.

A job type definition may specify additional properties (see `configurationProperties` below). To configure these values, define environment variables for them.  For example, to define the additional property `storeName`, this environment variable must be defined:
 
 - `STORENAME`

## Defining a Job Type

A definition is a YAML file containing an object.  The subsections here correspond to properties of that object.

### configurationProperties

- required: false (default: empty array)
- type: array of objects

Properties to look up in the Job Service configuration.  The subsections here correspond to properties of each object.

#### name

- required: true
- type: string

Property's name.  This defines how the configuration should be provided.

#### description

- required: false (default: empty string)
- type: string

Notes on what the property means, and what value should be configured for it.

### jobParametersSchema

- required: false (default: input must be missing or null)
- type: YAML (not a string containing JSON or YAML)

An embedded schema which is used to validate parameters provided with the job before executing `taskScript` (below).  The schema language is JSON Schema (`draft-03` or `draft-04`); language reference can be found here:

- <http://json-schema.org/specification-links.html#draft-4>

Parameters can only be specified as an object, so the schema must expect an object as the outer value.  The schema also serves as documentation for job creators, so it is recommended to use the `description` annotation throughout.

### taskScript

- required: true
- type: string

An embedded script which is executed to construct the `task` object.  The script language is JSLT (version 0.1.9), configured to preserve all output values.  Language reference can be found here:

- <https://github.com/schibsted/jslt/blob/0.1.9/tutorial.md>
- <https://github.com/schibsted/jslt/blob/0.1.9/functions.md>

The script input is an object with the following properties:

- `configuration`: object with property names from `configurationProperties` (above), and values all strings resolved from Job Service configuration
- `taskPipe`: the job's `taskPipe`
- `targetPipe`: the job's `targetPipe`; this is missing if a value is not configured
- `partitionId`: the job's partition ID
- `jobId`: the job's ID
- `parameters`: as provided with the submitted job, and already validated according to `jobParameterSchema`

The script output is the `task` value, and should be an object with the following properties:

Required properties:
 - `taskClassifier`: String
 - `taskApiVersion`: Integer
 - `taskData`: Object
 - `taskPipe`: String

Optional properties:
 - `targetPipe`: String

