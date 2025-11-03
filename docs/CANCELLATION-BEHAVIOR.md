# Job Cancellation and Dependency Behavior

## Your Question

> **"What happens when a job is cancelled when it is already running and other jobs depend on that job?"**

---

## Answer

### Current Behavior

When a running job is cancelled and other jobs depend on it:

1. **The cancelled job:**
   - Status changes to `Cancelled`
   - Task execution is stopped
   - Task tables are dropped from the database
   - Subtask completion reports are cleaned up

2. **Dependent jobs:**
   - ⚠️ **Remain in `Waiting` state indefinitely**
   - Are **NOT automatically cancelled or updated**
   - Dependency records remain in the database
   - Will never execute because they're waiting for a cancelled job to complete

### Example Scenario

```
Timeline:

1. Job A is created and starts running
   Status: Active
   
2. Job B is created with prerequisiteJobIds: ["job-a"]
   Status: Waiting
   Database: job_dependency record exists (job_id='job-b', dependent_job_id='job-a')
   
3. User cancels Job A while it's running
   POST /job-service/v1/partitions/default/jobs/job-a/cancel
   
4. Result:
   ✅ Job A: Status = Cancelled
   ❌ Job B: Status = Waiting (STUCK FOREVER)
   ❌ Dependency record still exists
   ❌ No notification to Job B that Job A was cancelled
```

### Database State After Cancellation

```sql
-- Job A (cancelled)
SELECT * FROM job WHERE job_id = 'job-a';
-- job_id='job-a', status='Cancelled', last_update_date=<now>

-- Job B (stuck)
SELECT * FROM job WHERE job_id = 'job-b';
-- job_id='job-b', status='Waiting', last_update_date=<original_create_date>

-- Dependency still exists
SELECT * FROM job_dependency WHERE job_id = 'job-b';
-- job_id='job-b', dependent_job_id='job-a'

-- Task data still waiting
SELECT * FROM job_task_data WHERE job_id = 'job-b';
-- job_id='job-b', eligible_to_run_date=NULL (waiting on dependencies)
```

### Why This Happens

The `cancel_job()` database function only updates the cancelled job:

```sql
-- From R__cancelJob.sql
UPDATE job
SET status = 'Cancelled', last_update_date = now()
WHERE partition_id = in_partition_id
  AND job_id = in_job_id;

-- Drops task tables
PERFORM internal_drop_task_tables(in_partition_id, in_job_id);

-- Cleans up subtask reports
PERFORM internal_cleanup_completed_subtask_report(in_partition_id, in_job_id);

-- ⚠️ MISSING: No logic to handle dependent jobs!
```

There is **no code** that:
- Finds jobs depending on the cancelled job
- Updates their status
- Removes dependency records
- Notifies those jobs

---

## If Job is Recreated with Same ID

### The Actual Behavior

If you delete and recreate Job A with the same `job_id`:

```
Timeline:

1. Job A cancelled (status='Cancelled')
   Job B waiting on Job A (dependency exists)

2. Job A deleted
   SELECT delete_job('default', 'job-a');
   
   Result:
   - Job A deleted from job table
   - Dependency record (job_id='job-b', dependent_job_id='job-a') REMAINS! ⚠️
   - Job B still has task_data record with eligible_to_run_date=NULL
   
3. Job A recreated with same ID
   SELECT create_job('default', 'job-a', ...);
   
   Result:
   - New Job A exists (status='Waiting')
   - Dependency record STILL EXISTS (job_id='job-b', dependent_job_id='job-a')
   - Job B references a job_id that now points to a DIFFERENT instance
   - When new Job A completes, Job B will correctly be triggered
```

### Important Clarification

**I initially misunderstood the foreign key constraints.** The actual FK is:

```sql
FOREIGN KEY (partition_id, job_id) REFERENCES job (partition_id, job_id)
```

This means:
- The FK references the **dependent job** (the one that is waiting)
- There is **NO foreign key** on `dependent_job_id` (the prerequisite)
- Deleting a job does NOT cascade-delete dependency records where that job is a prerequisite

**What Actually Happens:**

```sql
-- Before deletion
job: (job_id='job-a', status='Cancelled')
job: (job_id='job-b', status='Waiting')
job_dependency: (job_id='job-b', dependent_job_id='job-a')

-- After delete_job('job-a')
job: <job-a deleted>
job: (job_id='job-b', status='Waiting')
job_dependency: (job_id='job-b', dependent_job_id='job-a')  -- STILL EXISTS!

-- After recreating job-a
job: (job_id='job-a', status='Waiting')  -- New instance
job: (job_id='job-b', status='Waiting')
job_dependency: (job_id='job-b', dependent_job_id='job-a')  -- Points to new instance

-- When new job-a completes
-- internal_process_dependent_jobs() finds job-b via dependent_job_id='job-a'
-- Job-b is correctly triggered
```

### The Real Issue

The problem is NOT with deletion/recreation breaking dependencies. The **real issue** is:

1. **Job B is waiting for a job_id reference**, not a specific job instance
2. **When Job A is cancelled**, Job B continues waiting indefinitely (dependency record remains)
3. **If Job A is recreated**, Job B will wait for the NEW instance to complete
4. **No execution versioning exists** to distinguish between different runs of the same job_id

### Important: Recreation with Different Prerequisites

**Question:** If Job A is recreated with different prerequisite jobs, will Job B (which depends on Job A) still be triggered?

**Answer: YES!** The dependency resolution only looks at `job_id`:

```sql
-- When new Job A completes, internal_process_dependent_jobs() runs:
SELECT j.job_id, j.delay
FROM job_dependency jd
INNER JOIN job j ON j.partition_id = jd.partition_id AND j.job_id = jd.job_id
WHERE jd.dependent_job_id = 'job-a';  -- Only checks job_id!
```

**Example:**
```
Original:
  Job A (prerequisites: [])
    ├─> Job B (waits for job-a)
    └─> Job C (waits for job-a)

After cancellation and recreation:
  Job A (prerequisites: ['job-x', 'job-y'])  -- DIFFERENT prerequisites!
    ├─> Job B (still waits for job-a)
    └─> Job C (still waits for job-a)

When new Job A completes:
  ✅ Job B is triggered (dependency on 'job-a' satisfied)
  ✅ Job C is triggered (dependency on 'job-a' satisfied)
```

**Key Point:** The prerequisite jobs of the recreated Job A (job-x, job-y) are **completely irrelevant** to whether Job B and Job C will run. They only care that something with `job_id='job-a'` completed.

---

## Practical Use Case: Adding Prerequisites to an Existing Job

### Scenario

You need to add new prerequisites to a job, but:
- The job may already be running or waiting
- The REST API doesn't expose existing prerequisites when you GET a job
- You need to preserve existing prerequisites while adding new ones

### Solution: Store Prerequisites in Labels

Since job definitions are immutable, you must delete and recreate. Here's the safe approach:

**Step 1: Store prerequisites as labels when creating jobs**

```bash
PUT /job-service/v1/partitions/default/jobs/job-a
{
  "name": "Job A",
  "prerequisiteJobIds": ["job-x", "job-y"],
  "labels": {
    "prereqs": "job-x,job-y",  # Store as comma-separated string
    "owner": "alice",
    "project": "batch-001"
  },
  "task": { ... }
}
```

**Step 2: Later, retrieve existing prerequisites from labels**

```bash
GET /job-service/v1/partitions/default/jobs/job-a
# Response:
{
  "id": "job-a",
  "status": "Waiting",  # May be any status
  "labels": {
    "prereqs": "job-x,job-y",  # Can parse this!
    "owner": "alice"
  }
}
```

**Step 3: Parse existing prerequisites**

```javascript
const response = await fetch('/jobs/job-a');
const job = await response.json();
const existingPrereqs = job.labels.prereqs 
  ? job.labels.prereqs.split(',') 
  : [];
// existingPrereqs = ["job-x", "job-y"]
```

**Step 4: Delete the job (safe regardless of status)**

```bash
DELETE /job-service/v1/partitions/default/jobs/job-a
```

**Important:** This is safe even if:
- Job is still `Waiting` (hasn't started yet)
- Job is `Active` (currently running) - worker will fail to report completion
- Other jobs depend on job-a - their dependency records will persist

**Step 5: Recreate with combined prerequisites**

```bash
PUT /job-service/v1/partitions/default/jobs/job-a
{
  "name": "Job A",
  "prerequisiteJobIds": ["job-x", "job-y", "job-z"],  # Existing + new!
  "labels": {
    "prereqs": "job-x,job-y,job-z",  # Update label
    "owner": "alice",
    "project": "batch-001"
  },
  "task": { ... }
}
```

**Result:**
- ✅ New Job A has all prerequisites (old + new)
- ✅ Jobs that depend on job-a will still be triggered when new job-a completes
- ✅ Dependency records preserved (dangling during deletion, but valid after recreation)
- ✅ Audit trail maintained through labels

### Why This Works

The dependency resolution is **unidirectional** and **identity-based**:

```sql
-- When job-a completes, this finds dependent jobs:
SELECT j.job_id 
FROM job_dependency jd
INNER JOIN job j ON j.partition_id = jd.partition_id AND j.job_id = jd.job_id
WHERE jd.dependent_job_id = 'job-a';  -- Only checks the string 'job-a'!
```

It doesn't matter:
- ❌ What prerequisites job-a has
- ❌ Whether job-a was deleted and recreated
- ❌ How many times job-a's prerequisites changed

It only cares:
- ✅ That a job with `job_id='job-a'` exists
- ✅ That the job completed successfully

### Alternative: Use JSON for Complex Prerequisites

If you need to track more metadata:

```javascript
// When creating
labels: {
  "prereqs_json": JSON.stringify({
    prereqs: ["job-x", "job-y"],
    added_on: "2025-11-03",
    reason: "Initial setup"
  })
}

// When recreating
const prereqData = JSON.parse(job.labels.prereqs_json);
const newPrereqData = {
  prereqs: [...prereqData.prereqs, "job-z"],
  added_on: new Date().toISOString(),
  reason: "Added new dependency"
};

labels: {
  "prereqs_json": JSON.stringify(newPrereqData)
}
```

### Risks and Mitigations

**Risk 1: Job deleted while active**
- Worker continues processing, fails to report completion
- Mitigation: Check status before deletion if possible, or accept that active jobs may fail

**Risk 2: Race condition (job completes between DELETE and PUT)**
- Small window where dependent jobs might be triggered prematurely
- Mitigation: Keep this window as small as possible, or add application-level locking

**Risk 3: Dependent jobs have dangling references during deletion**
- Brief period where dependent jobs reference non-existent job
- Mitigation: This is temporary and self-healing when job is recreated

### Complete Example Code

```javascript
async function addPrerequisiteToJob(jobId, newPrereqId) {
  // 1. Get existing job
  const response = await fetch(`/job-service/v1/partitions/default/jobs/${jobId}`);
  const existingJob = await response.json();
  
  // 2. Parse existing prerequisites from labels
  const existingPrereqs = existingJob.labels?.prereqs 
    ? existingJob.labels.prereqs.split(',')
    : [];
  
  // 3. Check if prerequisite already exists
  if (existingPrereqs.includes(newPrereqId)) {
    console.log('Prerequisite already exists');
    return;
  }
  
  // 4. Combine prerequisites
  const combinedPrereqs = [...existingPrereqs, newPrereqId];
  
  // 5. Delete existing job
  await fetch(`/job-service/v1/partitions/default/jobs/${jobId}`, {
    method: 'DELETE'
  });
  
  // 6. Recreate with combined prerequisites
  await fetch(`/job-service/v1/partitions/default/jobs/${jobId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      name: existingJob.name,
      description: existingJob.description,
      prerequisiteJobIds: combinedPrereqs,
      labels: {
        ...existingJob.labels,
        prereqs: combinedPrereqs.join(','),
        last_updated: new Date().toISOString()
      },
      task: existingJob.task  // Assuming you stored this somewhere
    })
  });
  
  console.log(`Added prerequisite ${newPrereqId} to job ${jobId}`);
}
```

### Summary

| Question | Answer |
|----------|--------|
| Can I delete and recreate with different prereqs? | ✅ YES |
| Will dependent jobs still be triggered? | ✅ YES |
| Can I store prereqs in labels? | ✅ YES (recommended!) |
| Is it safe to delete a waiting/active job? | ⚠️ Mostly (worker may fail) |
| Do I need the original task definition? | ✅ YES (store it or retrieve before deletion) |

**Recommended:** Always store prerequisites in labels for this exact use case!

---

## Workarounds

### Option 1: Enable Failure Propagation (Recommended)

Set this environment variable for both Job Tracking Worker and Scheduled Executor:

```bash
CAF_JOB_TRACKING_PROPAGATE_FAILURES=true
CAF_JOB_SCHEDULER_PROPAGATE_FAILURES=true
```

**How it helps:**
- When Job A **fails**, all dependent jobs are automatically marked as `Failed`
- Provides clear audit trail (failure details include root cause)
- Prevents jobs from waiting indefinitely

**Limitation:**
- Only works for **failed** jobs, not **cancelled** jobs
- Cancellation still leaves dependent jobs in `Waiting` state

### Option 2: Manually Cancel Dependent Jobs

Before cancelling a job, find and cancel its dependents:

```bash
# 1. Find dependent jobs
curl "http://localhost:8080/job-service/v1/partitions/default/jobs?status=Waiting" | \
  jq '.[] | select(.prerequisiteJobIds[] == "job-a")'

# 2. Cancel each dependent job
curl -X POST http://localhost:8080/job-service/v1/partitions/default/jobs/job-b/cancel
curl -X POST http://localhost:8080/job-service/v1/partitions/default/jobs/job-c/cancel

# 3. Cancel the original job
curl -X POST http://localhost:8080/job-service/v1/partitions/default/jobs/job-a/cancel
```

**Using SQL:**
```sql
-- Find all jobs waiting on job-a
SELECT jd.job_id, j.status, j.name
FROM job_dependency jd
INNER JOIN job j ON j.partition_id = jd.partition_id 
                AND j.job_id = jd.job_id
WHERE jd.partition_id = 'default'
  AND jd.dependent_job_id = 'job-a';

-- Cancel each one via API or manually
```

### Option 3: Never Reuse Job IDs

**Best Practice:**
- Always generate new UUIDs for jobs
- Never delete and recreate with the same ID
- Use labels for logical grouping instead

```bash
# Good: Unique IDs
job-a: "550e8400-e29b-41d4-a716-446655440000"
job-b: "6ba7b810-9dad-11d1-80b4-00c04fd430c8"

# Bad: Reusing IDs
job-a (deleted and recreated) ❌
```

---

## Proposed Solutions

### Short-term: Add Cascade Cancellation

Modify `cancel_job()` to propagate cancellation:

```sql
-- New internal function
CREATE OR REPLACE FUNCTION internal_cancel_dependent_jobs(
    in_partition_id VARCHAR(40),
    in_job_id VARCHAR(48)
)
RETURNS VOID
AS $$
BEGIN
    -- Find all jobs that depend on this one (recursively)
    WITH RECURSIVE all_dependents AS (
        SELECT job_id
        FROM job_dependency
        WHERE partition_id = in_partition_id 
          AND dependent_job_id = in_job_id
        UNION
        SELECT jd.job_id
        FROM all_dependents ad
        INNER JOIN job_dependency jd 
            ON jd.partition_id = in_partition_id 
           AND jd.dependent_job_id = ad.job_id
    )
    -- Mark all as cancelled
    UPDATE job
    SET status = 'Cancelled',
        failure_details = json_build_object(
            'reason', 'prerequisite_cancelled',
            'root_job', in_partition_id || ':' || in_job_id
        )::text,
        last_update_date = now()
    WHERE partition_id = in_partition_id
      AND job_id IN (SELECT job_id FROM all_dependents)
      AND status NOT IN ('Completed', 'Failed', 'Cancelled');
      
    -- Remove dependency records
    DELETE FROM job_dependency
    WHERE partition_id = in_partition_id
      AND job_id IN (SELECT job_id FROM all_dependents);
      
    -- Clean up task data
    DELETE FROM job_task_data
    WHERE partition_id = in_partition_id
      AND job_id IN (SELECT job_id FROM all_dependents);
END;
$$;

-- Modified cancel_job
CREATE OR REPLACE FUNCTION cancel_job(
    in_partition_id VARCHAR(40),
    in_job_id VARCHAR(48)
)
RETURNS VOID
AS $$
BEGIN
    -- Existing logic
    UPDATE job SET status = 'Cancelled', last_update_date = now()
    WHERE partition_id = in_partition_id AND job_id = in_job_id;
    
    PERFORM internal_drop_task_tables(in_partition_id, in_job_id);
    PERFORM internal_cleanup_completed_subtask_report(in_partition_id, in_job_id);
    
    -- NEW: Cancel all dependent jobs
    PERFORM internal_cancel_dependent_jobs(in_partition_id, in_job_id);
END;
$$;
```

**Benefits:**
- ✅ Consistent behavior (similar to failure propagation)
- ✅ No orphaned jobs
- ✅ Clear audit trail
- ✅ Minimal code change

**Drawbacks:**
- ⚠️ May cancel more jobs than user expects
- ⚠️ No way to "uncancel" if done by mistake

### Long-term: Execution Versioning

Introduce `job_execution_id` to distinguish job instances:

```sql
-- Add execution ID column
ALTER TABLE job 
ADD COLUMN job_execution_id UUID NOT NULL DEFAULT gen_random_uuid();

-- Make job_id + job_execution_id unique
ALTER TABLE job 
ADD CONSTRAINT uk_job_execution UNIQUE (partition_id, job_id, job_execution_id);

-- Update dependency table
ALTER TABLE job_dependency
ADD COLUMN dependent_job_execution_id UUID NOT NULL;

-- Update foreign key to reference execution
ALTER TABLE job_dependency
DROP CONSTRAINT fk_job_dependency;

ALTER TABLE job_dependency
ADD CONSTRAINT fk_job_dependency_execution
FOREIGN KEY (partition_id, dependent_job_id, dependent_job_execution_id)
REFERENCES job(partition_id, job_id, job_execution_id)
ON DELETE CASCADE;
```

**Benefits:**
- ✅ Complete audit trail (can see all executions)
- ✅ Dependencies tied to specific execution instances
- ✅ Job recreation doesn't break dependencies
- ✅ Can have multiple executions of same job_id

**Drawbacks:**
- ⚠️ Major schema change (breaking)
- ⚠️ Requires data migration
- ⚠️ All clients must be updated
- ⚠️ Increased complexity

---

## Summary

### Current State
- ❌ Cancelled jobs do **NOT** update dependent jobs
- ❌ Dependent jobs remain stuck in `Waiting` indefinitely
- ❌ Manual cleanup required
- ✅ Job recreation with same ID DOES preserve dependencies (dependency records remain)
- ⚠️ However, no execution versioning means Job B can't distinguish between cancelled and new instances

### The Core Problem
The system lacks **execution-level tracking**:
- Jobs are identified by `job_id` only (no `job_execution_id`)
- Dependencies reference `job_id`, not specific executions
- When a job is cancelled and recreated, dependent jobs wait for the NEW instance
- No audit trail to show which execution was cancelled vs. completed

### Immediate Actions You Can Take
1. ✅ Enable `CAF_JOB_TRACKING_PROPAGATE_FAILURES=true` (helps with failures, not cancellations)
2. ✅ Never reuse job IDs (eliminates ambiguity about which instance is referenced)
3. ✅ Manually check and cancel dependent jobs before cancelling parent
4. ✅ Monitor jobs stuck in `Waiting` state

### What Should Be Fixed
1. **Primary Issue**: Implement cascade cancellation in `cancel_job()` function
2. **Secondary Issue**: Add execution versioning for proper audit trail
3. **Enhancement**: Add API endpoints to query dependency relationships

---

## Can I Update a Waiting Job with Different Prerequisites?

### Short Answer: **NO** ❌

You **cannot** update a waiting job's prerequisites after it has been created.

### Why Not?

The `createOrUpdateJob` API endpoint (PUT operation) uses the `internal_create_job()` function which:

1. **Attempts to INSERT a new job** with the given `job_id`
2. **If the job_id already exists**, it checks the `job_hash`:
   - If the hash **matches** (same job definition) → Returns `FALSE` (no-op, job already exists)
   - If the hash **differs** (different job definition) → **Raises EXCEPTION** (conflict)

```sql
-- From internal_create_job()
EXCEPTION WHEN unique_violation THEN
    -- updating the job is disallowed, so on conflict we can only succeed if the hash
    -- indicates the provided job is exactly the same as the existing job
    IF EXISTS(
        SELECT 1 FROM job
        WHERE job.partition_id = in_partition_id
          AND job.job_id = in_job_id
          AND job.job_hash = in_job_hash
    ) THEN
        RETURN FALSE;  -- Same job, no-op
    ELSE
        RAISE;  -- Different job, error!
    END IF;
```

**Key Insight:** The comment says **"updating the job is disallowed"** - this is by design!

### What Happens When You Try?

**Scenario:** Job B exists with `prerequisiteJobIds: ['job-a']`, you want to change it to `['job-x', 'job-y']`

```bash
# Attempt to update
PUT /job-service/v1/partitions/default/jobs/job-b
{
  "name": "Job B",
  "prerequisiteJobIds": ["job-x", "job-y"],  # DIFFERENT!
  "task": { ... }
}

# Result: ERROR (unique_violation exception)
# The job_hash will be different, causing the function to raise an exception
```

**You'll get an error** because:
1. The `job_hash` is calculated from the job definition (including prerequisites)
2. Different prerequisites = different hash
3. Different hash with same job_id = exception

### Workaround: Delete and Recreate

The **only way** to change prerequisites is:

```bash
# 1. Delete the existing job
DELETE /job-service/v1/partitions/default/jobs/job-b

# 2. Create a new job with the same ID but different prerequisites
PUT /job-service/v1/partitions/default/jobs/job-b
{
  "name": "Job B",
  "prerequisiteJobIds": ["job-x", "job-y"],  # NEW prerequisites
  "task": { ... }
}
```

**Warning:** This approach has consequences:
- Any jobs that depend on `job-b` will have their dependency records preserved
- When the new `job-b` completes, those dependent jobs will be triggered
- See the "Recreation with Different Prerequisites" section above for details

### Alternative: Use a New Job ID

**Recommended approach:**

```bash
# Create a new job with new ID
PUT /job-service/v1/partitions/default/jobs/job-b-v2
{
  "name": "Job B (Updated)",
  "prerequisiteJobIds": ["job-x", "job-y"],
  "task": { ... }
}

# Optional: Cancel or delete the old job-b
POST /job-service/v1/partitions/default/jobs/job-b/cancel
```

**Benefits:**
- ✅ No ambiguity about which job is which
- ✅ Clearer audit trail
- ✅ Follows the best practice of never reusing job IDs
- ✅ No risk of confusion with dependent jobs

### Summary Table

| Operation | Job Exists? | Same Hash? | Result |
|-----------|-------------|------------|--------|
| PUT new job | No | N/A | ✅ Created |
| PUT identical job | Yes | Yes | ✅ No-op (already exists) |
| PUT different job | Yes | No | ❌ **ERROR** (conflict) |

**Conclusion:** Job definitions are **immutable** once created. To change prerequisites, you must delete and recreate (or use a new job_id).

---

## Can I Delete a Running Job?

### Short Answer: **YES** ✅ (But You Probably Shouldn't!)

The `delete_job()` function **does NOT check the job's status** before deleting it. You can delete a job regardless of whether it's `Waiting`, `Active`, `Paused`, `Completed`, `Failed`, or `Cancelled`.

### What the Database Does

From the `delete_job()` function:

```sql
-- Take out an exclusive update lock on the job row
PERFORM NULL
FROM job
WHERE partition_id = in_partition_id
    AND job_id = in_job_id
FOR UPDATE;

-- Raise exception if no matching job identifier has been found
IF NOT FOUND THEN
    RAISE EXCEPTION 'job_id {%} not found', in_job_id USING ERRCODE = 'P0002';
END IF;

-- ⚠️ NO STATUS CHECK!
-- Proceeds to delete the job regardless of status
```

**What happens:**
1. ✅ Job row deleted from database
2. ✅ Task tables dropped
3. ✅ Dependencies deleted (where this job is the dependent)
4. ✅ Task data deleted
5. ✅ Labels deleted
6. ⚠️ **Dependencies where this job is a prerequisite remain** (dangling references!)

### Consequences of Deleting an Active Job

**Immediate Effects:**
```bash
DELETE /job-service/v1/partitions/default/jobs/job-a  # Job A is Active
```

1. **Database:** Job A record is immediately deleted
2. **Worker:** May still be processing the task
   - Worker has no immediate notification
   - Worker will eventually try to report progress/completion
   - Progress reports will fail (job not found)
3. **Dependent Jobs:** Jobs waiting for job-a will have dangling references
   - Dependency records pointing to a non-existent job
   - Will remain in `Waiting` state indefinitely
   - No way to satisfy the dependency

### Example Scenario

```
Initial State:
  Job A: status='Active' (worker is processing)
  Job B: status='Waiting', depends on job-a
  Job C: status='Waiting', depends on job-a

User executes:
  DELETE /jobs/job-a

Result:
  Job A: DELETED from database ❌
  Worker: Still processing (doesn't know job was deleted)
  Job B: status='Waiting', dependency points to non-existent job-a 💥
  Job C: status='Waiting', dependency points to non-existent job-a 💥

When worker tries to report completion:
  report_complete('job-a-task-id') → ERROR: job not found

Jobs B and C:
  STUCK FOREVER - waiting for a job that no longer exists
```

### Comparison: Cancel vs Delete

| Action | Active Job? | Database | Worker | Dependents |
|--------|-------------|----------|--------|------------|
| **Cancel** | ✅ Allowed | Status='Cancelled' | Notified to stop | ⚠️ Stuck waiting |
| **Delete** | ✅ Allowed | Row deleted | Not notified | 💥 Dangling references |

### Best Practices

**DO:**
- ✅ Cancel active jobs first: `POST /jobs/{jobId}/cancel`
- ✅ Wait for cancellation to complete
- ✅ Then delete if needed: `DELETE /jobs/{jobId}`
- ✅ Check for dependent jobs before deleting

**DON'T:**
- ❌ Delete active jobs directly
- ❌ Delete jobs that other jobs depend on
- ❌ Delete jobs without checking dependencies first

### Recommended Workflow

```bash
# 1. Check if job has dependents
SELECT jd.job_id, j.status
FROM job_dependency jd
INNER JOIN job j ON j.partition_id = jd.partition_id 
                AND j.job_id = jd.job_id
WHERE jd.partition_id = 'default'
  AND jd.dependent_job_id = 'job-a';

# 2. If active, cancel first
POST /job-service/v1/partitions/default/jobs/job-a/cancel

# 3. Wait for status to be 'Cancelled'
GET /job-service/v1/partitions/default/jobs/job-a/status

# 4. Handle dependent jobs (cancel or update them)
POST /job-service/v1/partitions/default/jobs/job-b/cancel
POST /job-service/v1/partitions/default/jobs/job-c/cancel

# 5. Finally, delete
DELETE /job-service/v1/partitions/default/jobs/job-a
```

### When Is It Safe to Delete?

**Relatively Safe:**
- Job status is `Completed`, `Failed`, or `Cancelled`
- Job has **NO** dependent jobs waiting on it
- All task processing is confirmed finished

**Unsafe:**
- Job status is `Active` or `Paused`
- Other jobs have dependencies on this job
- Workers may still be processing tasks

### Query to Check Before Deleting

```sql
-- Check job status
SELECT job_id, status, percentage_complete
FROM job
WHERE partition_id = 'default' AND job_id = 'job-a';

-- Check if other jobs depend on this one
SELECT COUNT(*) as dependent_count
FROM job_dependency
WHERE partition_id = 'default' 
  AND dependent_job_id = 'job-a';

-- If dependent_count > 0, DON'T DELETE!
```

### What SHOULD Be Implemented

The `delete_job()` function should include validation:

```sql
CREATE OR REPLACE FUNCTION delete_job(
    in_partition_id VARCHAR(40),
    in_job_id VARCHAR(48)
)
RETURNS VOID
AS $$
DECLARE
    v_status job_status;
    v_dependent_count INT;
BEGIN
    -- Get job status
    SELECT status INTO v_status
    FROM job
    WHERE partition_id = in_partition_id AND job_id = in_job_id;
    
    -- Check if job is active
    IF v_status IN ('Active', 'Paused') THEN
        RAISE EXCEPTION 
            'Cannot delete job {%} - job is currently %', 
            in_job_id, v_status
        USING ERRCODE = 'P0001';
    END IF;
    
    -- Check for dependent jobs
    SELECT COUNT(*) INTO v_dependent_count
    FROM job_dependency
    WHERE partition_id = in_partition_id 
      AND dependent_job_id = in_job_id;
    
    IF v_dependent_count > 0 THEN
        RAISE EXCEPTION 
            'Cannot delete job {%} - % other jobs depend on it', 
            in_job_id, v_dependent_count
        USING ERRCODE = 'P0001';
    END IF;
    
    -- Proceed with deletion...
    -- ...existing code...
END;
$$;
```

### Summary

| Question | Answer |
|----------|--------|
| Can I delete a running job? | ✅ YES (technically) |
| Should I delete a running job? | ❌ NO! |
| What's the safe approach? | Cancel first, then delete |
| What about dependent jobs? | Check and handle them first |
| Does the system prevent it? | ❌ NO validation currently |

**Bottom Line:** While the system allows deleting active jobs, it's a dangerous operation that can leave the system in an inconsistent state. Always cancel first, handle dependents, then delete.

---

## References

For more details, see:
- **[Known Issues](./KNOWN-ISSUES.md)** - Full analysis and proposed solutions
- **[Dependency Management](./DEPENDENCY-MANAGEMENT.md)** - Complete dependency documentation
- **[Database Schema](./DATABASE-SCHEMA.md)** - Database structure and functions
- **[Job Lifecycle](./JOB-LIFECYCLE.md)** - State transitions and rules

