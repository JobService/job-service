---
layout: default
title: Features
last_updated: Created and last modified by Conal Smith on June 14, 2016
---

# Key Features

## Interaction with workers

The Job service offers an easier and more user friendly way to interact with workers and receive feedback about their progress processing tasks. Users will not have to concern themselves with the messaging framework used by the CAF Workers for communication.

For example, users can send tasks to workers, see the progress of these tasks and even cancel them using the Job Service Web API.

## Handling of large batches of work

Large batches of work are split into smaller batches until individual items can be processed. These items can be processed in parallel vastly improving performance. 

For example, users can send a large batch of work to the Batch Worker which will split the work into small work items. These can then be sent to individual workers, while monitoring the entire batch of work. 

## Elastically scaling

The process of batch splitting is scaled using the autoscaler. Sub-batches can be processed in parallel. Workers also scale up and down depending on the type of work to be performed on the item, vastly improving performance.



