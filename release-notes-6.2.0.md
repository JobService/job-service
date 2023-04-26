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
    staging queue name. The pattern must contain a named capturing group called `tenantId`, which is what is used to extract the tenant
    ID.  
    `default`: None  
    `example`: If the pattern is `^tenant-(?<tenantId>.+)$` and the partition ID is `tenant-acmecorp`, the tenant ID extracted from this
    partition ID will be `acmecorp`.

  - `CAF_WMP_TARGET_QUEUE_NAMES_PATTERN`   
    `description`: Only applies when `CAF_WMP_ENABLED` is true. Used to specify the target queue names pattern. This pattern is used
    by the Job Service Scheduled Executor to check whether it should reroute a message to a staging queue or not. Only messages destined for
    target queues that match this pattern will be rerouted to staging queues.  
    `default`: None

- US632159: The Job Service Scheduled Executor's healthcheck will now check the status of the Rabbit MQ Management API whenever the
  `CAF_WMP_ENABLED` environment variable is true.

- D632168: Updated to use Tomcat version [9.0.73](https://dlcdn.apache.org/tomcat/tomcat-9/v9.0.73/README.html)  
This includes fixes for [CVE-2023-24998](https://nvd.nist.gov/vuln/detail/CVE-2023-24998).

- US353672: Ping operation added to the Job Service's REST interface 

#### Known Issues
- None
