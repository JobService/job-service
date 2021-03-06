---
layout: default
title: Features

banner:
    icon: 'assets/img/fork-lift.png'
    title: Job Service
    subtitle: Orchestration, Management and Monitoring of Data Processing
    links:
        - title: GitHub
          url: https://github.com/JobService/job-service
---

# Key Features

## Worker Interaction

The Job Service offers an easy and user-friendly way to interact with workers and receive feedback about progress on their tasks. You do not have to concern yourself with the messaging framework used by the workers for communication. For example, you can send tasks to workers, see the progress of these tasks, and even cancel them using the Job Service web API.

## Large Batch Handling

The Job Service splits large batches of work into smaller batches, until the individual items can be processed. The system can process these items in parallel, vastly improving performance. For example, you can send a large batch of work to the Batch Worker, which splits the work into small work items. These small work items are then sent to individual workers, while monitoring the entire batch of work.

## Elastic Scaling

The process of batch splitting is scaled with the autoscaler. Sub-batches can be processed in parallel. Workers also scale up and down depending on the type of work to be performed on the item, which makes maximum use of the available resources.

## Dependent Jobs

The Job Service can accept a job and a list of dependent jobs.  The Job Service causes the job to wait until all dependent jobs have completed before automatically executing the job.

## Job Types

Job Service can be configured with [job types](Job-Types).  If a new job targets a job type, it can be defined using a format specific to that type, which can simplify job creation, and restrict the actions that a job can perform.


