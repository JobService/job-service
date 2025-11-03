# Job Service - Job Lifecycle

## Overview

This document describes the complete lifecycle of a job in the Job Service, from creation through completion or termination, including all possible state transitions and the conditions that trigger them.

---

## Job States

Jobs in the system can be in one of six states, defined by the `job_status` enum:

```sql
CREATE TYPE job_status AS ENUM (
    'Waiting',    -- Initial state; waiting for dependencies or delay
    'Active',     -- Currently executing
    'Paused',     -- Temporarily suspended by user
    'Completed',  -- Successfully finished
    'Failed',     -- Encountered an error
    'Cancelled'   -- Terminated by user request
);
```

---

## State Transition Diagram

```
                    ┌──────────────────────┐
                    │    Job Created       │
                    └──────────┬───────────┘
                               │
                               ▼
                        ┌─────────────┐
              ┌─────────┤   Waiting   ├────────┐
              │         └──────┬──────┘        │
              │                │               │
              │                │ Dependencies  │ User cancels
              │                │ satisfied     │
              │                │               │
              │                ▼               │
    User      │         ┌─────────────┐       │
    pauses    │    ┌────┤   Active    ├───┐   │
              │    │    └──────┬──────┘   │   │
              │    │           │          │   │
              │    │ User      │ Task     │   │
              │    │ pauses    │ fails    │   │
              │    │           │          │   │
              ▼    ▼           ▼          │   ▼
         ┌─────────────┐  ┌─────────┐   │ ┌──────────┐
         │   Paused    │  │ Failed  │   │ │Cancelled │
         └──────┬──────┘  └─────────┘   │ └──────────┘
                │                        │
       User     │            Task        │
       resumes  │            completes   │
                │                        │
                └────────────────────────┘
                                │
                                ▼
                          ┌──────────┐
                          │Completed │
                          └──────────┘
```

### Terminal States

These states are **final** - no further transitions are possible:
- **Completed**: Job finished successfully
- **Failed**: Job encountered an unrecoverable error
- **Cancelled**: Job was terminated by user

---

## Detailed State Descriptions

### 1. Waiting State

**Entry Conditions:**
- Job is created with dependencies that haven't completed yet
- Job is created with a delay > 0
- Job is created in a suspended partition

**Characteristics:**
- `status = 'Waiting'`
- Task data stored in `job_task_data` table
- `eligible_to_run_date` is either:
  - `NULL` (waiting on dependencies)
  - Future timestamp (waiting for delay to expire)

**Exit Conditions:**
- **→ Active**: All dependencies satisfied AND delay expired
- **→ Cancelled**: User cancels the job
- **→ Failed**: Prerequisite job fails (if propagation enabled)

**Database Queries:**
```sql
-- Jobs waiting on dependencies
SELECT * FROM job 
WHERE status = 'Waiting' 
  AND job_id IN (
      SELECT job_id FROM job_dependency
  );

-- Jobs waiting on delay
SELECT * FROM job j
INNER JOIN job_task_data jtd USING (partition_id, job_id)
WHERE j.status = 'Waiting'
  AND jtd.eligible_to_run_date > now();
```

---

### 2. Active State

**Entry Conditions:**
- Job transitions from `Waiting` when dependencies are satisfied
- Job transitions from `Paused` when resumed by user

**Characteristics:**
- `status = 'Active'`
- Task is being processed by a worker
- Progress is reported via `report_progress()` calls
- `percentage_complete` field is updated (0.00 to 100.00)

**Exit Conditions:**
- **→ Completed**: Worker reports successful completion
- **→ Failed**: Worker reports failure or encounters error
- **→ Paused**: User pauses the job
- **→ Cancelled**: User cancels the job

**Typical Flow:**
```
Worker receives task
    ↓
Worker reports progress (0%, 25%, 50%, 75%)
    ↓
Worker reports completion (100%)
    ↓
Job transitions to Completed
```

---

### 3. Paused State

**Entry Conditions:**
- User calls `pause_job()` on an Active job

**Characteristics:**
- `status = 'Paused'`
- Job execution is suspended
- Worker is notified to stop processing
- Job state is preserved for later resumption

**Exit Conditions:**
- **→ Active**: User calls `resume_job()`
- **→ Cancelled**: User cancels the job

**API Operations:**
```http
POST /job-service/v1/partitions/default/jobs/{jobId}/pause
POST /job-service/v1/partitions/default/jobs/{jobId}/resume
```

**Database Function:**
```sql
SELECT pause_job('default', 'job-123');
SELECT resume_job('default', 'job-123');
```

---

### 4. Completed State (Terminal)

**Entry Conditions:**
- Worker reports task completion via `report_complete()`
- All subtasks have completed successfully

**Characteristics:**
- `status = 'Completed'`
- `percentage_complete = 100.00`
- Job cannot be modified further
- Triggers dependent jobs to start

**Side Effects:**
1. Dependency records are deleted:
   ```sql
   DELETE FROM job_dependency 
   WHERE dependent_job_id = 'completed-job-id';
   ```

2. Dependent jobs become eligible:
   - Jobs with no remaining dependencies are dispatched
   - Jobs with delays have `eligible_to_run_date` updated

3. Tracking data is cleaned up:
   ```sql
   DELETE FROM completed_subtask_report 
   WHERE job_id = 'completed-job-id';
   ```

**No Exit Conditions** (terminal state)

---

### 5. Failed State (Terminal)

**Entry Conditions:**
- Worker reports task failure via `report_failure()`
- Unhandled exception in worker
- Dependency check fails

**Characteristics:**
- `status = 'Failed'`
- `failure_details` field populated with error information:
  ```json
  {
    "failureId": "uuid",
    "failureTime": 1699012345678,
    "failureSource": "WorkerName",
    "failureMessage": "Error details..."
  }
  ```

**Side Effects:**

**If `CAF_JOB_TRACKING_PROPAGATE_FAILURES=true`:**
- All dependent jobs recursively marked as `Failed`
- Failure details include root cause reference:
  ```json
  {
    "root_failure": "default:job-a",
    "failure_details": { ... }
  }
  ```

**If `CAF_JOB_TRACKING_PROPAGATE_FAILURES=false`:**
- Dependent jobs remain in `Waiting` state indefinitely
- Manual intervention required

**Database Function:**
```sql
SELECT report_failure(
    'default', 
    'task-id', 
    '{"error": "Processing failed"}'
);
```

**No Exit Conditions** (terminal state)

---

### 6. Cancelled State (Terminal)

**Entry Conditions:**
- User calls `cancel_job()` on a job in `Waiting`, `Active`, or `Paused` state

**Characteristics:**
- `status = 'Cancelled'`
- Task processing is stopped
- Task tables are dropped
- Resources are cleaned up

**API Operation:**
```http
POST /job-service/v1/partitions/default/jobs/{jobId}/cancel
```

**Database Function:**
```sql
SELECT cancel_job('default', 'job-123');
```

**Side Effects:**
1. Task tables dropped:
   ```sql
   PERFORM internal_drop_task_tables(partition_id, job_id);
   ```

2. Completed subtask reports cleaned:
   ```sql
   PERFORM internal_cleanup_completed_subtask_report(partition_id, job_id);
   ```

3. **⚠️ Dependent jobs NOT updated** (see [Known Issues](./KNOWN-ISSUES.md))

**No Exit Conditions** (terminal state)

---

## State Transition Rules

### Valid Transitions Matrix

| From State | To State | Trigger | API/Function |
|------------|----------|---------|--------------|
| Waiting | Active | Dependencies satisfied | `get_dependent_jobs()` |
| Waiting | Cancelled | User request | `cancel_job()` |
| Waiting | Failed | Prerequisite fails (propagation on) | `internal_process_failed_dependent_jobs()` |
| Active | Completed | Task completes | `report_complete()` |
| Active | Failed | Task fails | `report_failure()` |
| Active | Paused | User request | `pause_job()` |
| Active | Cancelled | User request | `cancel_job()` |
| Paused | Active | User request | `resume_job()` |
| Paused | Cancelled | User request | `cancel_job()` |

### Invalid Transitions

These transitions are **not allowed** and will result in errors:

- **From Completed**: No transitions (terminal)
- **From Failed**: No transitions (terminal)
- **From Cancelled**: No transitions (terminal)
- **Waiting → Paused**: Can only pause Active jobs
- **Completed → Active**: Cannot restart completed jobs

**Error Example:**
```sql
-- Attempt to cancel completed job
SELECT cancel_job('default', 'completed-job-id');

-- ERROR: job_id {completed-job-id} cannot be cancelled
-- SQLSTATE: 02000
```

---

## Job Creation Flows

### Simple Job (No Dependencies)

```
1. Client submits job via API
        ↓
2. create_job() called
        ↓
3. INSERT INTO job (status='Waiting', ...)
        ↓
4. No dependencies found
        ↓
5. Task dispatched immediately (no job_task_data record)
        ↓
6. Job transitions to Active
        ↓
7. Worker processes task
        ↓
8. report_complete() called
        ↓
9. Job transitions to Completed
```

### Job with Dependencies

```
1. Client submits Job B with prerequisiteJobIds=['job-a']
        ↓
2. create_job() called
        ↓
3. INSERT INTO job (status='Waiting', ...)
        ↓
4. INSERT INTO job_dependency (job_id='job-b', dependent_job_id='job-a')
        ↓
5. INSERT INTO job_task_data (eligible_to_run_date=NULL)
        ↓
6. Job B waits in Waiting state
        ↓
        │
        │ [Meanwhile, Job A completes]
        │
        ↓
7. report_complete('job-a-task-id') called
        ↓
8. internal_process_dependent_jobs('job-a') called
        ↓
9. DELETE FROM job_dependency WHERE dependent_job_id='job-a'
        ↓
10. Job B has no remaining dependencies
        ↓
11. DELETE FROM job_task_data WHERE job_id='job-b' RETURNING *
        ↓
12. Task data published to RabbitMQ
        ↓
13. Job B transitions to Active
```

### Job with Delay

```
1. Client submits Job C with delay=300 (5 minutes)
        ↓
2. create_job() called
        ↓
3. INSERT INTO job (status='Waiting', delay=300, ...)
        ↓
4. INSERT INTO job_task_data (
       eligible_to_run_date = now() + 300 seconds
   )
        ↓
5. Job C waits in Waiting state
        ↓
        │
        │ [Scheduled Executor polls every N seconds]
        │
        ↓
6. get_dependent_jobs() finds Job C (eligible_to_run_date <= now())
        ↓
7. DELETE FROM job_task_data WHERE job_id='job-c' RETURNING *
        ↓
8. Task data published to RabbitMQ
        ↓
9. Job C transitions to Active
```

---

## Progress Tracking

### Progress Reporting Flow

Workers report progress periodically during execution:

```
Worker starts processing
    ↓
report_progress('task-id', 0.00)    → percentage_complete = 0%
    ↓
[Processing...]
    ↓
report_progress('task-id', 25.00)   → percentage_complete = 25%
    ↓
[Processing...]
    ↓
report_progress('task-id', 50.00)   → percentage_complete = 50%
    ↓
[Processing...]
    ↓
report_progress('task-id', 75.00)   → percentage_complete = 75%
    ↓
[Processing...]
    ↓
report_complete('task-id')          → percentage_complete = 100%, status = 'Completed'
```

### Aggregated Progress (Future Enhancement)

For jobs with subtasks, progress could be calculated as:

```sql
percentage_complete = 
    (completed_subtasks / total_subtasks) * 100.00
```

Currently, progress is reported at the task level, not aggregated across subtasks.

---

## Job Termination

### Graceful Completion

```
Active Job
    ↓
Worker completes all tasks successfully
    ↓
report_complete() called
    ↓
Job status = 'Completed'
    ↓
Dependent jobs triggered
    ↓
Task data cleaned up
```

### Failure Termination

```
Active Job
    ↓
Worker encounters error
    ↓
report_failure() called
    ↓
Job status = 'Failed'
    ↓
failure_details populated
    ↓
IF propagation enabled:
    └─> Dependent jobs marked Failed
    └─> Dependency records deleted
ELSE:
    └─> Dependent jobs stuck in Waiting
```

### User-Initiated Cancellation

```
Waiting/Active/Paused Job
    ↓
User calls cancel_job()
    ↓
Job status = 'Cancelled'
    ↓
Task tables dropped
    ↓
Subtask reports cleaned
    ↓
⚠️ Dependent jobs NOT updated (see Known Issues)
```

---

## Lifecycle Timestamps

Jobs track two timestamps:

### 1. `create_date`
- Set when job is created
- Never updated
- Used for sorting, filtering, retention policies

### 2. `last_update_date`
- Updated on every state change
- Updated on progress reports
- Used to detect stale jobs

**Query stale jobs:**
```sql
SELECT * FROM job
WHERE status = 'Active'
  AND last_update_date < now() - interval '1 hour';
-- Jobs that haven't updated in over 1 hour
```

---

## Job Deletion

Jobs can be deleted via `delete_job()` or `delete_jobs()`:

```sql
SELECT delete_job('default', 'job-123');
```

**Cascade Effects:**
- Deletes from `job` table
- CASCADE deletes from `job_task_data`
- CASCADE deletes from `job_dependency` (where `job_id = 'job-123'`)
- CASCADE deletes from `label`
- CASCADE deletes from `stowed_task`
- **⚠️ Does NOT delete dependencies where `dependent_job_id = 'job-123'`**
  - This causes the issue documented in [Known Issues](./KNOWN-ISSUES.md)

**Restrictions:**
- No validation prevents deletion of `Active` jobs (see [Known Issues](./KNOWN-ISSUES.md))
- Should check for dependent jobs before deleting

---

## Monitoring Job Lifecycle

### Key Metrics to Monitor

1. **Jobs stuck in Waiting:**
   ```sql
   SELECT COUNT(*) FROM job
   WHERE status = 'Waiting'
     AND create_date < now() - interval '1 day';
   ```

2. **Jobs with no progress:**
   ```sql
   SELECT COUNT(*) FROM job
   WHERE status = 'Active'
     AND last_update_date < now() - interval '30 minutes';
   ```

3. **Failed job rate:**
   ```sql
   SELECT 
       COUNT(*) FILTER (WHERE status = 'Failed') / COUNT(*)::float * 100
   FROM job
   WHERE create_date > now() - interval '1 day';
   ```

4. **Average job duration:**
   ```sql
   SELECT AVG(last_update_date - create_date) as avg_duration
   FROM job
   WHERE status = 'Completed'
     AND create_date > now() - interval '1 day';
   ```

---

## Best Practices

### 1. **Design for Idempotency**
- Jobs should be safe to retry
- Use `job_hash` to detect duplicates
- Handle partial completion scenarios

### 2. **Set Realistic Progress Expectations**
- Report progress frequently (every 5-10% recommended)
- Don't report 100% until truly complete
- Include meaningful progress messages

### 3. **Handle Cancellation Gracefully**
- Check for cancellation periodically during processing
- Clean up resources on cancellation
- Save partial results if valuable

### 4. **Enable Failure Propagation**
- Set `CAF_JOB_TRACKING_PROPAGATE_FAILURES=true`
- Prevents jobs from waiting indefinitely
- Provides clear failure audit trail

### 5. **Monitor Job Health**
- Alert on jobs stuck in `Waiting` > threshold
- Alert on jobs stuck in `Active` with no progress
- Track failure rates by job type

### 6. **Use Delays Appropriately**
- Prefer delays over dependencies for time-based sequencing
- Avoid very long delays (> 1 day)
- Consider scheduled jobs for recurring patterns

---

## Related Documentation

- [Database Schema](./DATABASE-SCHEMA.md) - Table structures and constraints
- [Dependency Management](./DEPENDENCY-MANAGEMENT.md) - How dependencies affect lifecycle
- [Known Issues](./KNOWN-ISSUES.md) - Current limitations
- [Architecture Overview](./ARCHITECTURE.md) - System design
- [Cancellation Behavior](./CANCELLATION-BEHAVIOR.md) - Detailed cancellation analysis

