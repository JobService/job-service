# Job Service Database Schema

## Overview

The Job Service uses **PostgreSQL 11+** as its database, managed through **Flyway** migrations. The schema is designed to support job orchestration, dependency management, progress tracking, and task distribution.

**Schema Version:** Managed by Flyway (see `V3__create_schema.sql` baseline)

---

## Core Tables

### 1. `job` Table

Stores the main job records with their status and metadata.

```sql
CREATE TABLE public.job (
    job_id              VARCHAR(48)  NOT NULL,
    name                VARCHAR(255) NULL,
    description         TEXT         NULL,
    data                TEXT         NULL,
    create_date         TIMESTAMP    NOT NULL,
    status              job_status   NOT NULL DEFAULT 'Waiting',
    percentage_complete FLOAT        NOT NULL DEFAULT 0.00,
    failure_details     TEXT         NULL,
    job_hash            INT          NULL,
    delay               INT          NULL     DEFAULT 0,
    last_update_date    TIMESTAMP    NOT NULL DEFAULT now(),
    partition_id        VARCHAR(40)  NOT NULL DEFAULT 'default',
    identity            SERIAL       NOT NULL,
    
    CONSTRAINT pk_job PRIMARY KEY (partition_id, job_id)
);
```

**Key Fields:**
- `partition_id` + `job_id`: Composite primary key for multi-tenancy
- `status`: Current job state (see `job_status` enum)
- `percentage_complete`: Progress tracking (0.00 to 100.00)
- `job_hash`: Hash for detecting duplicate job submissions
- `delay`: Seconds to wait after dependencies complete before execution
- `last_update_date`: Tracks most recent state change
- `identity`: Auto-incrementing sequence for ordering

**Important:** The `job_id` is the **permanent identifier** for a job across all executions. There is **no separate `job_execution_id`** column in the current schema - each job has only one instance in this table.

---

### 2. `job_dependency` Table

Defines prerequisite relationships between jobs.

```sql
CREATE TABLE public.job_dependency (
    job_id           VARCHAR(48) NOT NULL,
    dependent_job_id VARCHAR(48) NOT NULL,
    partition_id     VARCHAR(40) NOT NULL DEFAULT 'default',
    
    CONSTRAINT pk_job_dependency PRIMARY KEY (partition_id, job_id, dependent_job_id),
    CONSTRAINT fk_job_dependency FOREIGN KEY (partition_id, job_id) 
        REFERENCES job (partition_id, job_id)
);
```

**Relationship Semantics:**
- `job_id`: The job that has dependencies (the "waiting" job)
- `dependent_job_id`: The job that must complete first (the "prerequisite" job)
- A job can have multiple prerequisites
- Dependencies are removed when prerequisite jobs complete

**Example:**
```sql
-- Job B depends on Job A completing first
INSERT INTO job_dependency (partition_id, job_id, dependent_job_id)
VALUES ('default', 'job-b', 'job-a');
```

---

### 3. `job_task_data` Table

Stores task execution information for jobs waiting on dependencies or delays.

```sql
CREATE TABLE public.job_task_data (
    job_id               VARCHAR(48)  NOT NULL,
    task_classifier      VARCHAR(255) NOT NULL,
    task_api_version     INT          NOT NULL,
    task_data            BYTEA        NOT NULL,
    task_pipe            VARCHAR(255) NOT NULL,
    target_pipe          VARCHAR(255) NULL,
    eligible_to_run_date TIMESTAMP    NULL,
    partition_id         VARCHAR(40)  NOT NULL DEFAULT 'default',
    suspended            BOOLEAN      NOT NULL DEFAULT false,
    
    CONSTRAINT pk_job_task_data PRIMARY KEY (partition_id, job_id),
    CONSTRAINT fk_job_task_data FOREIGN KEY (partition_id, job_id) 
        REFERENCES job (partition_id, job_id)
);
```

**Key Fields:**
- `task_classifier`: Type of worker that will process this task
- `task_api_version`: API version for compatibility
- `task_data`: Serialized task payload (binary)
- `task_pipe`: RabbitMQ queue for task distribution
- `target_pipe`: Queue for result delivery
- `eligible_to_run_date`: When the job becomes eligible (NULL = waiting on dependencies)
- `suspended`: Flag for partition-level suspension

**Lifecycle:**
- Created when job has dependencies or a delay
- Updated when dependencies complete (sets `eligible_to_run_date`)
- Deleted when job is dispatched to workers

---

### 4. `label` Table

Stores key-value metadata associated with jobs.

```sql
CREATE TABLE public.label (
    partition_id VARCHAR(40)  NOT NULL DEFAULT 'default',
    job_id       VARCHAR(48)  NOT NULL,
    label        VARCHAR(255) NOT NULL,
    value        VARCHAR(255) NULL,
    
    CONSTRAINT label_pkey PRIMARY KEY (partition_id, job_id, label),
    CONSTRAINT fk_label_job FOREIGN KEY (partition_id, job_id) 
        REFERENCES job (partition_id, job_id)
);
```

**Usage:**
- Flexible metadata for jobs (tags, owners, categories)
- Used for filtering and searching jobs
- Label keys restricted to alphanumeric, `_`, `-`, `:` characters

---

## Supporting Tables

### 5. `completed_subtask_report` Table

Tracks completion of subtasks for jobs without dependencies.

```sql
CREATE TABLE public.completed_subtask_report (
    partition_id VARCHAR(40) NOT NULL,
    job_id       VARCHAR(48) NOT NULL,
    task_id      VARCHAR(70) NOT NULL,
    report_date  TIMESTAMP   NOT NULL
);
```

---

### 6. `stowed_task` Table

Stores task state information (context, status, tracking URLs).

```sql
CREATE TABLE public.stowed_task (
    partition_id                               VARCHAR(40)  NOT NULL,
    job_id                                     VARCHAR(48)  NOT NULL,
    task_classifier                            VARCHAR(255) NOT NULL,
    task_api_version                           INT          NOT NULL,
    task_data                                  BYTEA        NOT NULL,
    task_status                                VARCHAR(255) NOT NULL,
    context                                    BYTEA        NOT NULL,
    to                                         VARCHAR(255) NOT NULL,
    tracking_info_job_task_id                  VARCHAR(255) NOT NULL,
    tracking_info_last_status_check_time       BIGINT       NULL,
    tracking_info_status_check_interval_millis BIGINT       NULL,
    tracking_info_status_check_url             TEXT         NULL,
    tracking_info_tracking_pipe                VARCHAR(255) NULL,
    tracking_info_track_to                     VARCHAR(255) NULL,
    source_info                                BYTEA        NULL,
    correlation_id                             VARCHAR(255) NULL,
    
    CONSTRAINT fk_stowed_task FOREIGN KEY (partition_id, job_id) 
        REFERENCES job (partition_id, job_id)
);
```

---

### 7. Housekeeping Tables

- `deleted_parent_table_log`: Tracks deleted task tables
- `delete_log`: Audit log for deletions

---

## Enumerations

### `job_status` Enum

```sql
CREATE TYPE job_status AS ENUM (
    'Active',      -- Job is currently executing
    'Cancelled',   -- Job was cancelled by user
    'Completed',   -- Job finished successfully
    'Failed',      -- Job encountered an error
    'Paused',      -- Job execution is paused
    'Waiting'      -- Job is waiting for dependencies or delay
);
```

**State Transitions:**
```
Waiting ──────────> Active ──────────> Completed
   │                   │
   │                   └──────────────> Failed
   │                   │
   │                   └──────────────> Paused ─────> Active
   │
   └──────────────────────────────────> Cancelled
```

---

## Indexes

Key indexes for performance (defined in migration scripts):

```sql
-- Optimized for job lookups by status and partition
CREATE INDEX idx_job_status ON job(partition_id, status);

-- Optimized for job ID lookups
CREATE INDEX idx_job_jobid ON job(partition_id, job_id);

-- Optimized for dependency lookups
CREATE INDEX idx_job_dependency_dependent ON job_dependency(partition_id, dependent_job_id);
```

---

## Database Functions & Procedures

The Job Service implements complex operations as PostgreSQL functions for:
- Transaction safety
- Performance optimization
- Business logic encapsulation

### Public Functions

Located in: `src/main/resources/db/migration/functions/public/`

#### Job Management
- **`create_job(...)`**: Creates a new job (with or without dependencies)
- **`cancel_job(partition_id, job_id)`**: Cancels a job
- **`cancel_jobs(...)`**: Bulk cancellation with filters
- **`delete_job(partition_id, job_id)`**: Deletes a job
- **`delete_jobs(...)`**: Bulk deletion with filters
- **`pause_job(partition_id, job_id)`**: Pauses job execution
- **`resume_job(partition_id, job_id)`**: Resumes paused job

#### Job Queries
- **`get_job(partition_id, job_id)`**: Retrieves job details
- **`get_jobs(...)`**: Lists jobs with filtering, sorting, pagination
- **`get_jobs_count(...)`**: Counts jobs matching criteria
- **`get_dependent_jobs()`**: Returns jobs ready to run (dependencies satisfied)

#### Progress Reporting
- **`report_complete(partition_id, task_id)`**: Marks task complete, triggers dependent jobs
- **`report_complete_bulk(...)`**: Bulk completion reporting
- **`report_failure(partition_id, task_id, failure_details)`**: Marks task failed
- **`report_progress(partition_id, task_id, percentage)`**: Updates progress

### Internal Functions

Located in: `src/main/resources/db/migration/functions/internal/`

- **`internal_process_dependent_jobs(partition_id, job_id)`**: Finds and activates dependent jobs
- **`internal_process_failed_dependent_jobs(partition_id, job_id, failure_details)`**: Propagates failures
- **`internal_has_dependent_jobs(partition_id, job_id)`**: Checks if job has dependents
- **`internal_is_task_completed(partition_id, job_id)`**: Verifies task completion
- **`internal_create_job(...)`**: Core job creation logic
- **`internal_update_job_progress(...)`**: Recalculates job progress

---

## Key Database Workflows

### 1. Creating a Job with Dependencies

```sql
-- Create Job A (no dependencies)
SELECT create_job(
    'default',                    -- partition_id
    'job-a',                      -- job_id
    'Job A',                      -- name
    'First job',                  -- description
    NULL,                         -- data
    123456,                       -- job_hash
    'DocumentWorker',             -- task_classifier
    1,                            -- task_api_version
    E'\\x...',                    -- task_data (bytea)
    'document-input-queue',       -- task_pipe
    'document-output-queue',      -- target_pipe
    NULL,                         -- prerequisite_job_ids (no dependencies)
    0,                            -- delay
    ARRAY[['owner', 'alice']]     -- labels
);

-- Create Job B that depends on Job A
SELECT create_job(
    'default',                    -- partition_id
    'job-b',                      -- job_id
    'Job B',                      -- name
    'Depends on Job A',           -- description
    NULL,                         -- data
    789012,                       -- job_hash
    'ProcessWorker',              -- task_classifier
    1,                            -- task_api_version
    E'\\x...',                    -- task_data
    'process-input-queue',        -- task_pipe
    'process-output-queue',       -- target_pipe
    ARRAY['job-a'],               -- prerequisite_job_ids (depends on job-a)
    0,                            -- delay
    ARRAY[['owner', 'alice']]     -- labels
);
```

**Result:**
- Job A: Status = `Waiting`, task data deleted immediately (no dependencies)
- Job B: Status = `Waiting`, task data stored in `job_task_data` (waiting on job-a)
- Dependency record created: `(job_id='job-b', dependent_job_id='job-a')`

### 2. Completing a Job and Triggering Dependents

```sql
-- Mark Job A as complete
SELECT * FROM report_complete('default', 'job-a-task-id');
```

**What Happens:**
1. Job A status updated to `Completed`
2. `internal_process_dependent_jobs('default', 'job-a')` called
3. Dependency record deleted: `(job_id='job-b', dependent_job_id='job-a')`
4. Job B task data removed from `job_task_data` and returned (ready to run)
5. Job B published to RabbitMQ for execution

### 3. Cancelling a Job

```sql
SELECT cancel_job('default', 'job-a');
```

**What Happens:**
1. Job A status updated to `Cancelled`
2. Task tables dropped via `internal_drop_task_tables()`
3. Completed subtask reports cleaned up
4. **Important:** Dependent jobs (Job B) are NOT automatically updated

---

## Important Schema Behaviors

### ⚠️ No Execution Versioning

**Critical Finding:** The current schema does **NOT** have a separate `job_execution_id` column. Each `job_id` represents a single job instance.

**Implication:** If you delete and recreate a job with the same `job_id`, it's treated as a completely new job from the database perspective, but there's no execution history tracking.

### Dependency Resolution Logic

Dependencies are stored by `job_id` only (not execution-specific):

```sql
-- Job B depends on Job A
job_dependency: (job_id='job-b', dependent_job_id='job-a')
```

**When Job A completes:**
1. All jobs where `dependent_job_id = 'job-a'` are found
2. Their dependency records are deleted
3. If no more dependencies exist for Job B, it becomes eligible to run

**When Job A is cancelled then recreated:**
1. Job A status = `Cancelled` (dependency record still exists)
2. Job B remains stuck because `dependent_job_id='job-a'` points to cancelled job
3. If Job A is deleted and recreated with same ID:
   - Old dependency record is deleted (FK cascade)
   - New Job A is created but NO dependency record exists for Job B → Job A
   - Job B will never see the new Job A

### Failure Propagation

Controlled by configuration (`CAF_JOB_TRACKING_PROPAGATE_FAILURES`):

- **Enabled:** Failures cascade through entire dependency tree
- **Disabled:** Dependent jobs remain in `Waiting` state indefinitely

---

## Migration Management

### Flyway Conventions

- **Versioned Migrations:** `V<version>__<description>.sql` (e.g., `V3__create_schema.sql`)
- **Repeatable Migrations:** `R__<description>.sql` (e.g., `R__cancelJob.sql`)
  - Re-run whenever checksum changes
  - Used for functions/procedures that can be replaced

### Current Baseline

- **V3__create_schema.sql**: Baseline schema creation
- **V4__drop_unused_functions.sql**: Cleanup
- **V5__forward_declarations.sql**: Function dependencies
- **V6__forward_declarations.sql**: Additional declarations
- **V7__drop_unused_functions.sql**: More cleanup

---

## Best Practices

### Job ID Design
- Use unique, meaningful identifiers (UUIDs recommended)
- Max length: 48 characters
- Avoid special characters: `.,:;*?!|()`

### Partition Strategy
- Use partition IDs for multi-tenancy isolation
- Default partition: `'default'`
- Max length: 40 characters

### Label Usage
- Prefer labels over `externalData` field (deprecated)
- Use for filtering, searching, and grouping jobs
- Example: `owner`, `project`, `environment`, `version`

### Dependency Design
- Keep dependency graphs shallow (avoid deep trees)
- Use delays for time-based sequencing
- Consider failure propagation impact on dependent jobs

---

## Related Documentation

- [Architecture Overview](./ARCHITECTURE.md)
- [Job Lifecycle](./JOB-LIFECYCLE.md)
- [Dependency Management](./DEPENDENCY-MANAGEMENT.md)
- [Known Issues](./KNOWN-ISSUES.md)

