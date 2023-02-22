!not-ready-for-release!

#### Version Number
${version-number}

#### New Features
- 627110: The Job Service Scheduled Executor now supports message prioritization, allowing it to reroute messages to staging queues.

  It can be configured via the following environment variables:

  - `CAF_WMP_ENABLED`  
    `description`: Determines whether the Job Service Scheduled Executor should reroute a message to a worker's staging queue or not. If
    true, a message will attempt to be rerouted. If false, a message will not be rerouted and will be sent to the target queue rather than
    to a staging queue.  
    `default`: false

  - `CAF_WMP_PARTITION_ID_PATTERN`   
    `description`: Only applies when `CAF_WMP_ENABLED` is true. Used to specify the partition ID pattern. This pattern is used
    by the Job Service Scheduled Executor to extract the tenant ID from the partition ID. The tenant ID is then used to construct the
    staging queue name.
    `default`: ^tenant-(.+)$  
    `example`: If the pattern is `^tenant-(.+)$` and the partition ID is `tenant-acmecorp`, the tenant ID extracted from this partition
    ID will be `acmecorp`.

  - `CAF_WMP_TARGET_QUEUE_NAMES_PATTERN`   
    `description`: Only applies when `CAF_WMP_ENABLED` is true. Used to specify the target queue names pattern. This pattern is used
    by the Job Service Scheduled Executor to check whether it should reroute a message to a staging queue or not. Only messages destined for
    target queues that match this pattern will be rerouted to staging queues.  
    `default`: ^(?>dataprocessing-.*-in|worker-grammar-in|ingestion-batch-in|data-enrichment-batch-in)$

  - `CAF_WMP_USE_TARGET_QUEUE_CAPACITY_TO_REROUTE`  
    `description`: Only applies when `CAF_WMP_ENABLED` is true. Determines whether the Job Service Scheduled Executor should use the target
    queue's capacity when making a decision on whether to reroute a message. If true, a message will only be rerouted to a staging
    queue if the target queue does not have capacity for it. If false, a message will **always** be rerouted to a staging queue,
    ignoring the target queue's capacity.  
    `default`: false

  - `CAF_WMP_KUBERNETES_NAMESPACES`  
    `description`: Used to specify the Kubernetes namespaces, comma separated, in which to search for a worker's labels. These labels
    contain information about each worker's target queue, such as its name and maximum length. A non-null and non-empty value must be
    provided for this environment variable if `CAF_WMP_USE_TARGET_QUEUE_CAPACITY_TO_REROUTE` is true. If
    `CAF_WMP_USE_TARGET_QUEUE_CAPACITY_TO_REROUTE` is false, this environment variable is not used.  
    `default`: None

  - `CAF_WMP_KUBERNETES_LABEL_CACHE_EXPIRY_MINUTES`   
    `description`: Used to specify the 'expire after write' minutes after which a Kubernetes label that has been added to the cache
    should be removed. Set this to 0 to disable caching. Only used when `CAF_WMP_USE_TARGET_QUEUE_CAPACITY_TO_REROUTE` is true. If
    `CAF_WMP_USE_TARGET_QUEUE_CAPACITY_TO_REROUTE` is false, this environment variable is not used.  
    `default`: 60

#### Known Issues
