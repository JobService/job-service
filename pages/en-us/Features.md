---
layout: default
title: Features
last_updated: Last modified by Frank Rovitto on July 5, 2016

banner:
    icon: 'assets/img/fork-lift.png'
    title: Job Service
    subtitle: Analyze a Larger Range of Formats
    links:
        - title: GitHub
          url: https://github.hpe.com/caf/job-service
---

# Key Features

## Worker Interaction

The Job Service offers an easy and user-friendly way to interact with workers and receive feedback about progress on their tasks. You do not have to concern yourself with the messaging framework used by the workers for communication. For example, you can send tasks to workers, see the progress of these tasks, and even cancel them using the Job Service web API.

## Large Batch Handling

The Job Service splits large batches of work into smaller batches, until the individual items can be processed. The system can process these items in parallel, vastly improving performance. For example, you can send a large batch of work to the Batch Worker, which splits the work into small work items. These small work items are then sent to individual workers, while monitoring the entire batch of work.

## Elastic Scaling

The process of batch splitting is scaled with the autoscaler. Sub-batches can be processed in parallel. Workers also scale up and down depending on the type of work to be performed on the item, which makes maximum use of the available resources.



