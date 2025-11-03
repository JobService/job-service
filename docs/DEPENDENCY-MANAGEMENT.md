# Job Dependency Management

## Overview

The Job Service provides sophisticated dependency management, allowing jobs to be executed in a controlled order. A job can specify prerequisite jobs that must complete successfully before it begins execution.

---

## Dependency Model

### Basic Concept

```
Job A ────> Job B ────> Job C
(parent)    (child)     (grandchild)

Job B waits for Job A to complete
Job C waits for Job B to complete
```

### Database Representation

Dependencies are stored in the `job_dependency` table:

```sql
CREATE TABLE job_dependency (
    partition_id     VARCHAR(40) NOT NULL,
    job_id           VARCHAR(48) NOT NULL,  -- The waiting job
    dependent_job_id VARCHAR(48) NOT NULL,  -- The prerequisite job
    
    PRIMARY KEY (partition_id, job_id, dependent_job_id)
);
```

**Key Points:**
- `job_id`: The job that is waiting (the "dependent")
- `dependent_job_id`: The job that must complete first (the "prerequisite")
- A job can have **multiple prerequisites** (multiple rows with same `job_id`)
- A job can be a prerequisite for **multiple jobs** (multiple rows with same `dependent_job_id`)

---

## Creating Jobs with Dependencies

### API Request

When creating a job via the REST API, specify prerequisites in the `prerequisiteJobIds` array:

```json
PUT /job-service/v1/partitions/default/jobs/job-b
{
  "name": "Process Documents",
  "description": "Processes documents after ingestion completes",
  "prerequisiteJobIds": ["job-a", "job-c"],
  "delay": 60,
  "task": {
    "taskClassifier": "DocumentWorker",
    "taskApiVersion": 1,
    "taskData": {...},
    "taskPipe": "document-processing-in",
    "targetPipe": "document-processing-out"
  }
}
```

**In this example:**
- Job B will not start until both Job A and Job C complete
- After both complete, Job B waits an additional 60 seconds (delay)

### Database Flow

1. **Job Creation:**
   ```sql
   SELECT create_job(
       'default',
       'job-b',
       'Process Documents',
       'Processes documents after ingestion completes',
       NULL,
       12345,
       'DocumentWorker',
       1,
       <task_data>,
       'document-processing-in',
       'document-processing-out',
       ARRAY['job-a', 'job-c'],  -- Prerequisites
       60,                        -- Delay in seconds
       NULL
   );
   ```

2. **Dependency Records Created:**
   ```sql
   INSERT INTO job_dependency (partition_id, job_id, dependent_job_id)
   VALUES 
       ('default', 'job-b', 'job-a'),
       ('default', 'job-b', 'job-c');
   ```

3. **Task Data Stored:**
   ```sql
   INSERT INTO job_task_data (
       partition_id, job_id, task_classifier, task_api_version,
       task_data, task_pipe, target_pipe, eligible_to_run_date
   )
   VALUES (
       'default', 'job-b', 'DocumentWorker', 1,
       <task_data>, 'document-processing-in', 'document-processing-out',
       NULL  -- NULL = waiting on dependencies
   );
   ```

4. **Job Status:**
   - Job B status = `Waiting`
   - Job B will not be dispatched to workers until dependencies satisfied

---

## Dependency Resolution

### When a Prerequisite Job Completes

When Job A completes, the `report_complete()` function is called:

```sql
SELECT * FROM report_complete('default', 'job-a-task-id');
```

**What Happens:**

1. **Check for Dependent Jobs:**
   ```sql
   SELECT internal_has_dependent_jobs('default', 'job-a');
   -- Returns TRUE if any jobs depend on job-a
   ```

2. **Process Dependencies:**
   ```sql
   SELECT * FROM internal_process_dependent_jobs('default', 'job-a');
   ```

3. **Inside `internal_process_dependent_jobs()`:**

   a. **Find Dependent Jobs:**
   ```sql
   SELECT j.job_id, j.delay
   FROM job_dependency jd
   INNER JOIN job j ON j.partition_id = jd.partition_id 
                   AND j.job_id = jd.job_id
   WHERE jd.partition_id = 'default'
     AND jd.dependent_job_id = 'job-a';
   -- Returns: job-b (with its delay value)
   ```

   b. **Remove Dependency Record:**
   ```sql
   DELETE FROM job_dependency
   WHERE partition_id = 'default'
     AND dependent_job_id = 'job-a';
   -- Deletes: (job_id='job-b', dependent_job_id='job-a')
   ```

   c. **Check Remaining Dependencies:**
   ```sql
   SELECT COUNT(*) FROM job_dependency
   WHERE partition_id = 'default'
     AND job_id = 'job-b';
   -- If job-b still depends on job-c, count = 1
   ```

   d. **If No More Dependencies:**
   - **With Delay (delay > 0):**
     ```sql
     UPDATE job_task_data
     SET eligible_to_run_date = now() + (delay * interval '1 second')
     WHERE partition_id = 'default' AND job_id = 'job-b';
     -- Job becomes eligible after delay expires
     ```
   
   - **Without Delay (delay = 0):**
     ```sql
     DELETE FROM job_task_data
     WHERE partition_id = 'default' AND job_id = 'job-b'
     RETURNING *;
     -- Task data returned to be published immediately
     ```

4. **Job Tracking Worker:**
   - Receives returned task data
   - Publishes to RabbitMQ (task_pipe)
   - Job B begins execution

---

## Dependency Resolution Timing

### Immediate Execution (No Delay)

```
Job A Completes
      │
      ├─> Delete dependency record (job-b → job-a)
      │
      ├─> Check remaining dependencies for job-b
      │   └─> None remaining
      │
      ├─> Delete task data from job_task_data
      │
      └─> Publish Job B to RabbitMQ ──> Worker executes Job B
```

### Delayed Execution

```
Job A Completes
      │
      ├─> Delete dependency record (job-b → job-a)
      │
      ├─> Check remaining dependencies for job-b
      │   └─> None remaining
      │
      ├─> Update eligible_to_run_date = now() + 60 seconds
      │
      └─> Job Scheduled Executor polls database
              │
              └─> (60 seconds later)
                      │
                      └─> Finds job-b eligible
                          │
                          └─> Publishes Job B to RabbitMQ
```

**Scheduled Executor Role:**
- Polls database periodically using `get_dependent_jobs()`
- Finds jobs where:
  - `eligible_to_run_date <= now()`
  - No remaining dependencies
  - Not suspended
- Publishes eligible jobs to RabbitMQ

---

## Complex Dependency Scenarios

### Multiple Prerequisites (AND Logic)

```
Job A ──┐
        ├──> Job D
Job B ──┤
        └──> Job E
Job C ──┘
```

**Job D waits for A, B, and C to ALL complete:**

```sql
-- Dependency records
(job_id='job-d', dependent_job_id='job-a')
(job_id='job-d', dependent_job_id='job-b')
(job_id='job-d', dependent_job_id='job-c')
```

**Resolution:**
1. Job A completes → Dependency A deleted, Job D still has 2 dependencies
2. Job B completes → Dependency B deleted, Job D still has 1 dependency
3. Job C completes → Dependency C deleted, Job D has 0 dependencies → **Job D starts**

### Diamond Pattern

```
       Job A
      /     \
  Job B     Job C
      \     /
       Job D
```

**Job D depends on both B and C, which both depend on A:**

```sql
-- Dependencies
(job_id='job-b', dependent_job_id='job-a')
(job_id='job-c', dependent_job_id='job-a')
(job_id='job-d', dependent_job_id='job-b')
(job_id='job-d', dependent_job_id='job-c')
```

**Execution Flow:**
1. Job A completes
2. Jobs B and C become eligible (both dispatched)
3. Job B completes → Removes dependency B from Job D
4. Job C completes → Removes dependency C from Job D → **Job D starts**

### Linear Chain

```
Job A → Job B → Job C → Job D
```

**Sequential execution:**
```sql
(job_id='job-b', dependent_job_id='job-a')
(job_id='job-c', dependent_job_id='job-b')
(job_id='job-d', dependent_job_id='job-c')
```

---

## Pre-created Dependencies

### Referencing Jobs Not Yet Created

You can create a job that depends on a job that doesn't exist yet:

```sql
SELECT create_job(
    'default',
    'job-b',
    ...,
    ARRAY['job-a'],  -- job-a doesn't exist yet
    ...
);
```

**Behavior:**
- Dependency record created: `(job_id='job-b', dependent_job_id='job-a')`
- Job B status = `Waiting`
- When Job A is eventually created and completes, Job B will be triggered

**Use Case:**
- Dynamic job creation where order isn't guaranteed
- Job templates where dependencies are defined before jobs exist

---

## Failure Handling

### Configuration: `CAF_JOB_TRACKING_PROPAGATE_FAILURES`

This environment variable controls whether failures cascade through dependent jobs.

#### Propagation Disabled (default: `false`)

**When Job A fails:**
- Job A status = `Failed`
- Dependent jobs (Job B, Job C, etc.) remain in `Waiting` state **indefinitely**
- Manual intervention required to cancel or delete stuck jobs

**Example:**
```
Job A (Failed) → Job B (Waiting) → Job C (Waiting)
                 ↓
              STUCK FOREVER
```

#### Propagation Enabled (`true`)

**When Job A fails:**
- Job A status = `Failed`
- `internal_process_failed_dependent_jobs()` called
- All dependent jobs marked as `Failed` recursively
- Dependency records removed

**Database Flow:**
```sql
-- Job A fails
UPDATE job SET status = 'Failed', failure_details = '...'
WHERE job_id = 'job-a';

-- Propagate failure
SELECT internal_process_failed_dependent_jobs('default', 'job-a', '...');
```

**Inside `internal_process_failed_dependent_jobs()`:**

1. **Find All Descendants (Recursive):**
   ```sql
   WITH RECURSIVE all_job_dependencies AS (
       SELECT partition_id, dependent_job_id, job_id
       FROM job_dependency
       WHERE partition_id = 'default' 
         AND dependent_job_id = 'job-a'
       UNION
       SELECT adj.partition_id, adj.dependent_job_id, jd.job_id
       FROM all_job_dependencies adj
       INNER JOIN job_dependency jd 
           ON adj.partition_id = jd.partition_id 
          AND adj.job_id = jd.dependent_job_id
   )
   SELECT DISTINCT job_id FROM all_job_dependencies;
   -- Returns: job-b, job-c, job-d (entire dependency tree)
   ```

2. **Mark All as Failed:**
   ```sql
   UPDATE job
   SET status = 'Failed',
       percentage_complete = 0.00,
       failure_details = '{"root_failure": "default:job-a", ...}',
       last_update_date = now()
   WHERE job_id IN ('job-b', 'job-c', 'job-d');
   ```

3. **Remove Dependency Records:**
   ```sql
   DELETE FROM job_dependency
   WHERE job_id IN ('job-b', 'job-c', 'job-d');
   ```

**Result:**
```
Job A (Failed) → Job B (Failed) → Job C (Failed)
                      ↓
                All marked as failed with root cause reference
```

---

## Cancellation and Dependencies

### When a Job is Cancelled

**Current Behavior:**

```sql
SELECT cancel_job('default', 'job-a');
```

**What Happens:**
1. Job A status = `Cancelled`
2. Task tables dropped
3. Completed subtask reports cleaned up
4. **⚠️ Dependency records are NOT removed**
5. **⚠️ Dependent jobs (Job B, Job C) are NOT updated**

**Implication:**
- Dependent jobs remain in `Waiting` state indefinitely
- They will never execute because Job A will never complete
- Manual cleanup required (cancel or delete dependent jobs)

### ⚠️ Critical Issue: Job Recreation After Cancellation

**Scenario:**
1. Job A is cancelled
2. Job B depends on Job A
3. Job A is deleted and recreated with the same `job_id`

**What Actually Happens:**
```sql
-- Initial state
job: (job_id='job-a', status='Cancelled')
job_dependency: (job_id='job-b', dependent_job_id='job-a')

-- Delete Job A
SELECT delete_job('default', 'job-a');
-- Note: Foreign key is on (partition_id, job_id), NOT on dependent_job_id
-- Dependency record WHERE dependent_job_id='job-a' REMAINS!

-- Recreate Job A
SELECT create_job('default', 'job-a', ...);

-- Current state
job: (job_id='job-a', status='Waiting')  -- New instance
job_dependency: (job_id='job-b', dependent_job_id='job-a')  -- STILL EXISTS!
job_task_data: (job_id='job-b', eligible_to_run_date=NULL)
```

**Result:**
- Dependency record **still exists** (no cascade delete on `dependent_job_id`)
- Job B will wait for the **NEW** Job A instance to complete
- When new Job A completes, Job B will be correctly triggered
- **Issue**: No execution versioning means Job B can't distinguish between:
  - The old cancelled Job A instance
  - The new Job A instance

**The Real Problem:**
- Not a data consistency issue with deleted dependencies
- Rather, **dependencies reference job_id without execution context**
- Job B will wait for whichever instance of job-a exists
- No audit trail showing which execution was originally intended

**Will Job B still run if Job A is recreated with different prerequisites?**

**YES!** The dependency resolution only cares about `job_id`:

```sql
-- When Job A completes, this query finds dependent jobs:
SELECT j.job_id FROM job_dependency jd
WHERE jd.dependent_job_id = 'job-a';  -- Only checks the string job_id
```

**Example:**
```
Timeline:

1. Original setup:
   Job A (no prerequisites)
   Job B (prerequisiteJobIds: ['job-a'])
   Job C (prerequisiteJobIds: ['job-a'])

2. Job A is cancelled and deleted

3. Job A recreated with DIFFERENT prerequisites:
   SELECT create_job('default', 'job-a', ..., 
                     prerequisite_job_ids=['job-x', 'job-y'], ...);
   
   Dependencies on job-x and job-y are created for Job A
   But Jobs B and C still have dependency records pointing to job-a

4. When new Job A completes:
   - internal_process_dependent_jobs('job-a') is called
   - Finds Job B and Job C (via dependent_job_id='job-a')
   - Deletes their dependency records
   - Triggers Job B and Job C to run ✅

Result: Job B and Job C run after the NEW Job A completes,
        regardless of what prerequisites the NEW Job A had!
```

**Key Insight:** Dependency resolution is **unidirectional and identity-based**:
- It only looks at the string `job_id` in the `dependent_job_id` column
- It doesn't care about the job's prerequisites, status history, or any other attributes
- The prerequisite jobs of the recreated job are irrelevant to downstream dependents

---

## Recommended Solutions for Cancellation Issues

### Option 1: Cascade Cancellations (Immediate Fix)

Modify `cancel_job()` to propagate cancellation:

```sql
CREATE OR REPLACE FUNCTION cancel_job(
    in_partition_id VARCHAR(40),
    in_job_id VARCHAR(48)
)
RETURNS VOID
AS $$
BEGIN
    -- Mark job as cancelled
    UPDATE job SET status = 'Cancelled', last_update_date = now()
    WHERE partition_id = in_partition_id AND job_id = in_job_id;
    
    -- Drop task tables
    PERFORM internal_drop_task_tables(in_partition_id, in_job_id);
    
    -- NEW: Cancel all dependent jobs recursively
    PERFORM internal_cancel_dependent_jobs(in_partition_id, in_job_id);
END;
$$;
```

### Option 2: Add Execution Versioning (Long-term Solution)

Introduce `job_execution_id` to distinguish between job instances:

```sql
ALTER TABLE job ADD COLUMN job_execution_id UUID NOT NULL DEFAULT gen_random_uuid();

ALTER TABLE job_dependency 
    ADD COLUMN dependent_job_execution_id UUID NOT NULL;

ALTER TABLE job_dependency
    ADD CONSTRAINT fk_job_dependency_execution
    FOREIGN KEY (partition_id, dependent_job_id, dependent_job_execution_id)
    REFERENCES job(partition_id, job_id, job_execution_id);
```

**Benefits:**
- Dependencies tied to specific executions
- Job recreation doesn't affect old dependencies
- Complete audit trail

**Drawbacks:**
- Major schema change
- Requires migration of existing data
- Breaking change for all clients

### Option 3: Prevent Job Deletion with Dependents

Add validation in `delete_job()`:

```sql
CREATE OR REPLACE FUNCTION delete_job(...)
AS $$
BEGIN
    -- Check if other jobs depend on this one
    IF EXISTS (
        SELECT 1 FROM job_dependency
        WHERE partition_id = in_partition_id
          AND dependent_job_id = in_job_id
    ) THEN
        RAISE EXCEPTION 'Cannot delete job % - other jobs depend on it', in_job_id
        USING ERRCODE = 'P0001';
    END IF;
    
    -- Proceed with deletion
    ...
END;
$$;
```

---

## Querying Dependencies

### Get Jobs Waiting on a Specific Job

```sql
SELECT jd.job_id, j.name, j.status
FROM job_dependency jd
INNER JOIN job j ON j.partition_id = jd.partition_id 
                AND j.job_id = jd.job_id
WHERE jd.partition_id = 'default'
  AND jd.dependent_job_id = 'job-a';
```

### Get Prerequisites for a Specific Job

```sql
SELECT jd.dependent_job_id, j.name, j.status
FROM job_dependency jd
INNER JOIN job j ON j.partition_id = jd.partition_id 
                AND j.job_id = jd.dependent_job_id
WHERE jd.partition_id = 'default'
  AND jd.job_id = 'job-b';
```

### Get All Jobs Ready to Run

```sql
SELECT * FROM get_dependent_jobs();
```

Returns jobs where:
- `eligible_to_run_date <= now()`
- No remaining dependencies
- Not suspended

---

## Best Practices

### 1. **Keep Dependency Graphs Shallow**
- Avoid deeply nested chains (Job A → B → C → D → E...)
- Prefer flat dependencies (A → B, A → C, A → D)
- Easier to understand and debug

### 2. **Use Delays for Time-Based Sequencing**
- Don't create dependencies just for timing
- Use `delay` parameter for time-based separation
- Example: Wait 5 minutes after ingestion before processing

### 3. **Enable Failure Propagation in Production**
- Set `CAF_JOB_TRACKING_PROPAGATE_FAILURES=true`
- Prevents jobs from waiting indefinitely on failed prerequisites
- Provides clear failure root cause in `failure_details`

### 4. **Monitor Waiting Jobs**
- Alert on jobs stuck in `Waiting` status for extended periods
- Indicates potential dependency issues or cancelled prerequisites

### 5. **Use Meaningful Job IDs**
- UUIDs ensure uniqueness
- Include context (e.g., `batch-2024-11-03-uuid`)
- Avoid reusing job IDs

### 6. **Document Complex Dependencies**
- Use labels to document dependency relationships
- Example: `{"dependency_group": "batch-123", "sequence": "2"}`

---

## Related Documentation

- [Database Schema](./DATABASE-SCHEMA.md) - Table structures
- [Job Lifecycle](./JOB-LIFECYCLE.md) - Job state transitions
- [Known Issues](./KNOWN-ISSUES.md) - Current limitations and workarounds
- [API Reference](./API-REFERENCE.md) - REST API for job creation

