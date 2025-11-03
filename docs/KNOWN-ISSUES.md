# Known Issues and Limitations

## Critical Issues

### 1. ⚠️ Dependent Jobs Not Updated When Parent Job is Cancelled and Recreated

**Issue ID:** N/A (Newly Documented)  
**Severity:** HIGH  
**Component:** Database Functions, Dependency Management

#### Problem Description

When a job is cancelled while running and other jobs depend on it, the dependent jobs remain in a `Waiting` state indefinitely. If the cancelled job is then deleted and recreated with the same `job_id`, the dependent jobs become orphaned and may execute without waiting for the new job instance to complete.

#### Reproduction Steps

1. Create Job A (prerequisite job)
2. Create Job B with dependency on Job A
   ```json
   {
     "prerequisiteJobIds": ["job-a"]
   }
   ```
3. Start Job A execution
4. Cancel Job A while it's running
   ```
   POST /job-service/v1/partitions/default/jobs/job-a/cancel
   ```
5. Delete Job A
   ```
   DELETE /job-service/v1/partitions/default/jobs/job-a
   ```
6. Recreate Job A with the same ID
   ```
   PUT /job-service/v1/partitions/default/jobs/job-a
   ```

#### Current Behavior

**After Step 4 (Cancellation):**
- Job A: `status='Cancelled'`
- Job B: `status='Waiting'` (stuck indefinitely)
- Dependency record: `(job_id='job-b', dependent_job_id='job-a')` still exists

**After Step 5 (Deletion):**
- Job A: Deleted from database
- Job B: `status='Waiting'`
- Dependency record: **REMAINS** (no cascade delete on `dependent_job_id`)
- Task data: `job_task_data` record still exists with `eligible_to_run_date=NULL`

**After Step 6 (Recreation):**
- New Job A: `status='Waiting'` (fresh instance)
- Job B: `status='Waiting'`, dependency record **STILL EXISTS** pointing to `job_id='job-a'`
- Job B will wait for the NEW Job A instance to complete
- When new Job A completes, Job B will be correctly triggered
- **Issue**: No way to distinguish between cancelled old instance and new instance

#### Expected Behavior

**Option 1 (Cascade Cancellation):**
- When Job A is cancelled, all dependent jobs should be cancelled recursively
- Job B should transition to `Cancelled` status

**Option 2 (Preserve Dependencies on Recreation):**
- When Job A is deleted, dependent jobs should retain their dependency information
- When Job A is recreated, dependency relationships should be restored
- Job B should wait for the new Job A instance to complete

**Option 3 (Execution Versioning):**
- Dependencies should reference specific job execution IDs, not just job IDs
- Recreated jobs get new execution IDs
- Old dependencies remain tied to old executions
- New dependencies must be explicitly created for new executions

#### Root Cause

The `job_dependency` table has a foreign key on the **dependent job** (the one waiting), NOT on the prerequisite:

```sql
ALTER TABLE job_dependency
    ADD CONSTRAINT fk_job_dependency 
    FOREIGN KEY (partition_id, job_id) 
    REFERENCES job (partition_id, job_id);
```

**Key points:**
1. The FK is on `(partition_id, job_id)` - the waiting job
2. There is **NO foreign key** on `dependent_job_id` - the prerequisite job
3. When a job is deleted via `delete_job()`, it explicitly deletes only rows where `job_id = deleted_job_id`
4. It does **NOT** delete rows where `dependent_job_id = deleted_job_id`

**Result when Job A (prerequisite) is deleted:**
- Dependency records WHERE `dependent_job_id='job-a'` **remain in the database**
- These are "dangling references" - they point to a job_id that doesn't exist
- If job-a is recreated, the dependency records now point to the NEW instance
- Job B can't distinguish between the old cancelled instance and the new one

**The actual problem:**
1. **Cancellation doesn't cascade**: `cancel_job()` doesn't update dependent jobs
2. **No execution versioning**: Dependencies reference `job_id`, not specific execution instances
3. **No audit trail**: Can't tell which execution of job-a was cancelled vs completed

#### Impact

- **Indefinite Waiting:** Dependent jobs wait forever when prerequisite is cancelled
- **No Cascade Cancellation:** Cancelling a job doesn't update its dependents
- **Ambiguous References:** Dependencies point to job_id without execution context
- **No Audit Trail:** Can't determine which execution instance was referenced
- **Manual Cleanup Required:** Must manually cancel/delete dependent jobs

#### Workaround

**For Cancelled Jobs:**
1. Manually cancel or delete all dependent jobs before recreating the parent
2. Query dependent jobs before cancellation:
   ```sql
   SELECT jd.job_id 
   FROM job_dependency jd
   WHERE jd.partition_id = 'default' 
     AND jd.dependent_job_id = 'job-a';
   ```
3. Cancel each dependent job via API

**For Preventing Reuse:**
- Never reuse job IDs
- Always generate new UUIDs for each job instance
- Use job labels to track logical groupings instead of ID reuse

#### Proposed Solutions

**Short-term Fix (Recommended):**

Add cascade cancellation to `cancel_job()`:

```sql
CREATE OR REPLACE FUNCTION internal_cancel_dependent_jobs(
    in_partition_id VARCHAR(40),
    in_job_id VARCHAR(48)
)
RETURNS VOID
LANGUAGE plpgsql
AS $$
BEGIN
    -- Recursively find all dependent jobs
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
        failure_details = '{"reason": "prerequisite_cancelled", "root_job": "' 
                          || in_partition_id || ':' || in_job_id || '"}',
        last_update_date = now()
    WHERE partition_id = in_partition_id
      AND job_id IN (SELECT job_id FROM all_dependents);
      
    -- Remove dependency records
    DELETE FROM job_dependency
    WHERE partition_id = in_partition_id
      AND job_id IN (SELECT job_id FROM all_dependents);
END;
$$;

-- Modify cancel_job to call this
CREATE OR REPLACE FUNCTION cancel_job(
    in_partition_id VARCHAR(40),
    in_job_id VARCHAR(48)
)
RETURNS VOID
AS $$
BEGIN
    -- Existing cancellation logic
    UPDATE job SET status = 'Cancelled', last_update_date = now()
    WHERE partition_id = in_partition_id AND job_id = in_job_id;
    
    PERFORM internal_drop_task_tables(in_partition_id, in_job_id);
    PERFORM internal_cleanup_completed_subtask_report(in_partition_id, in_job_id);
    
    -- NEW: Cancel dependent jobs
    PERFORM internal_cancel_dependent_jobs(in_partition_id, in_job_id);
END;
$$;
```

**Long-term Fix (Breaking Change):**

Introduce execution versioning:

```sql
-- Add execution ID column
ALTER TABLE job 
ADD COLUMN job_execution_id UUID NOT NULL DEFAULT gen_random_uuid();

-- Update dependency table
ALTER TABLE job_dependency
ADD COLUMN dependent_job_execution_id UUID;

-- Update foreign key
ALTER TABLE job_dependency
DROP CONSTRAINT fk_job_dependency;

ALTER TABLE job_dependency
ADD CONSTRAINT fk_job_dependency_execution
FOREIGN KEY (partition_id, dependent_job_id, dependent_job_execution_id)
REFERENCES job(partition_id, job_id, job_execution_id)
ON DELETE CASCADE;
```

---

## Moderate Issues

### 2. Active Jobs Can Be Deleted Without Validation

**Severity:** MEDIUM  
**Component:** Database Functions, Job Management

#### Problem

The `delete_job()` function does not check the job's status before deletion. You can delete jobs that are currently `Active` or `Paused`, which causes several problems.

#### Reproduction Steps

1. Create and start Job A (status becomes `Active`)
2. Create Job B with dependency on Job A
3. Execute: `DELETE /job-service/v1/partitions/default/jobs/job-a`
4. Result: Job A is deleted from database while worker is still processing

#### Current Behavior

```sql
-- No status validation in delete_job()
PERFORM NULL FROM job
WHERE partition_id = in_partition_id AND job_id = in_job_id
FOR UPDATE;

IF NOT FOUND THEN
    RAISE EXCEPTION 'job_id {%} not found', in_job_id;
END IF;

-- ⚠️ Proceeds to delete regardless of status
DELETE FROM job WHERE partition_id = in_partition_id AND job_id = in_job_id;
```

#### Impact

**For the Deleted Job:**
- Worker continues processing, unaware of deletion
- Worker cannot report progress/completion (job not found errors)
- No graceful shutdown of task processing
- Partial work may be lost

**For Dependent Jobs:**
- Dependency records become dangling references (point to non-existent job)
- Jobs stuck in `Waiting` state forever
- No way to satisfy the dependency
- Manual intervention required to clean up

**Example:**
```
Job A: Active (being processed by worker)
Job B: Waiting (depends on job-a)

DELETE Job A →
  Job A: Deleted from DB
  Worker: Still processing (no notification)
  Job B: Waiting, dependency points to non-existent job 💥
```

#### Workaround

**Always follow this workflow:**
```bash
# 1. Check for dependents
SELECT COUNT(*) FROM job_dependency 
WHERE dependent_job_id = 'job-a';

# 2. If active, cancel first
POST /jobs/job-a/cancel

# 3. Wait for cancellation
GET /jobs/job-a/status  # Wait for 'Cancelled'

# 4. Handle dependent jobs
POST /jobs/job-b/cancel

# 5. Finally, delete
DELETE /jobs/job-a
```

#### Solution

Add validation to `delete_job()`:

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
    SELECT status INTO v_status FROM job
    WHERE partition_id = in_partition_id AND job_id = in_job_id;
    
    -- Prevent deletion of active jobs
    IF v_status IN ('Active', 'Paused') THEN
        RAISE EXCEPTION 
            'Cannot delete job {%} - job is %', 
            in_job_id, v_status
        USING ERRCODE = 'P0001';
    END IF;
    
    -- Prevent deletion if other jobs depend on it
    SELECT COUNT(*) INTO v_dependent_count
    FROM job_dependency
    WHERE partition_id = in_partition_id 
      AND dependent_job_id = in_job_id;
    
    IF v_dependent_count > 0 THEN
        RAISE EXCEPTION 
            'Cannot delete job {%} - % jobs depend on it', 
            in_job_id, v_dependent_count
        USING ERRCODE = 'P0001';
    END IF;
    
    -- Proceed with deletion...
END;
$$;
```

---

### 3. Jobs Stuck in Waiting State After Prerequisite Failure (When Propagation Disabled)

**Severity:** MEDIUM  
**Component:** Dependency Management

#### Problem

When `CAF_JOB_TRACKING_PROPAGATE_FAILURES=false` (default), jobs remain in `Waiting` status indefinitely if their prerequisites fail.

#### Example

```
Job A (Failed) → Job B (Waiting) → Job C (Waiting)
```

Job B and Job C will never execute because Job A will never complete.

#### Impact

- Resource waste (jobs remain in database)
- Confusion for operators
- Requires manual cleanup

#### Workaround

Set `CAF_JOB_TRACKING_PROPAGATE_FAILURES=true` in both:
- Job Tracking Worker configuration
- Job Scheduled Executor configuration

#### Solution

Change default to `true` or add monitoring alerts for jobs waiting longer than a threshold.

---

### 4. No Circular Dependency Detection

**Severity:** MEDIUM  
**Component:** Job Creation

#### Problem

The system does not detect circular dependencies at creation time:

```
Job A depends on Job B
Job B depends on Job C
Job C depends on Job A  ← Creates infinite loop
```

#### Behavior

All jobs remain in `Waiting` status forever (deadlock).

#### Impact

- Jobs never execute
- Manual intervention required
- No error message

#### Workaround

- Design dependencies carefully
- Use external tooling to validate dependency graphs
- Implement dependency visualization

#### Solution

Add cycle detection in `create_job()`:

```sql
-- Check for cycles before inserting dependency
IF internal_has_dependency_cycle(
    in_partition_id, 
    in_job_id, 
    in_prerequisite_job_ids
) THEN
    RAISE EXCEPTION 'Circular dependency detected'
    USING ERRCODE = 'P0001';
END IF;
```

---

### 5. No API to Query Dependency Graph

**Severity:** LOW  
**Component:** REST API

#### Problem

The REST API provides no endpoints to:
- List jobs that depend on a given job
- List prerequisites for a given job
- Visualize the entire dependency graph

#### Impact

- Difficult to troubleshoot dependency issues
- Cannot programmatically analyze dependencies
- Manual database queries required

#### Workaround

Query database directly:

```sql
-- Get dependent jobs
SELECT jd.job_id, j.status
FROM job_dependency jd
INNER JOIN job j ON j.partition_id = jd.partition_id 
                AND j.job_id = jd.job_id
WHERE jd.dependent_job_id = 'job-a';

-- Get prerequisites
SELECT jd.dependent_job_id, j.status
FROM job_dependency jd
INNER JOIN job j ON j.partition_id = jd.partition_id 
                AND j.job_id = jd.dependent_job_id
WHERE jd.job_id = 'job-a';
```

#### Solution

Add REST endpoints:

```
GET /partitions/{partitionId}/jobs/{jobId}/dependents
GET /partitions/{partitionId}/jobs/{jobId}/prerequisites
GET /partitions/{partitionId}/jobs/{jobId}/dependency-graph
```

---

## Minor Issues

### 6. Job Hash Not Used for Duplicate Detection

**Severity:** LOW  
**Component:** Job Creation

#### Problem

The `job_hash` field is populated but not actively used to prevent duplicate job submissions.

#### Impact

- Multiple identical jobs can be submitted
- Wastes resources processing duplicates
- No built-in idempotency

#### Workaround

Implement deduplication in client code before submission.

#### Solution

Add unique constraint or check in `create_job()`:

```sql
IF EXISTS (
    SELECT 1 FROM job
    WHERE partition_id = in_partition_id
      AND job_hash = in_job_hash
      AND status NOT IN ('Completed', 'Failed', 'Cancelled')
) THEN
    RETURN QUERY SELECT FALSE;  -- Job already exists
    RETURN;
END IF;
```

---

### 7. ExternalData Field Deprecated But Still Used

**Severity:** LOW  
**Component:** API Contract, Database Schema

#### Problem

The `externalData` field in the `newJob` schema is marked as deprecated in favor of `labels`, but:
- Still accepted in API requests
- Still stored in database
- Documentation inconsistent

#### Impact

- API clients unclear which to use
- Migration path not defined
- Potential for data duplication

#### Workaround

Use `labels` for new jobs. Migrate existing `externalData` to labels.

#### Solution

- Set deprecation timeline (e.g., remove in v10.0.0)
- Add migration function to convert `externalData` to labels
- Return deprecation warnings in API responses

---

### 8. Limited Job Status Caching

**Severity:** LOW  
**Component:** REST API

#### Problem

The `CacheableJobStatus` header is returned for some endpoints but:
- Caching logic not documented
- No cache invalidation strategy defined
- Cache TTL not configurable

#### Impact

- Stale data returned to clients
- Unnecessary database queries
- Inconsistent caching behavior

#### Workaround

Implement application-level caching with short TTLs.

#### Solution

- Document caching behavior in API specification
- Add configuration for cache TTL
- Implement cache invalidation on job updates

---

## Performance Considerations

### 9. Large Dependency Trees Can Cause Performance Issues

**Severity:** MEDIUM  
**Component:** Database Functions

#### Problem

When a job with many dependents completes, `internal_process_dependent_jobs()` can:
- Lock many rows simultaneously
- Hold locks for extended duration
- Cause database contention

#### Example

```
Job A (root)
├─> 1000 dependent jobs
└─> Each triggers 10 more jobs = 10,000 total
```

#### Impact

- Slow job completion
- Database lock timeouts
- Reduced throughput

#### Workaround

- Limit fan-out in dependency design
- Use batching for large job sets
- Consider alternative architectures (e.g., event streaming)

#### Solution

- Add batch processing in `internal_process_dependent_jobs()`
- Process dependents in smaller chunks
- Use advisory locks instead of row locks where possible

---

### 10. No Pagination for Dependency Resolution

**Severity:** LOW  
**Component:** Database Functions

#### Problem

`get_dependent_jobs()` returns all eligible jobs in a single result set, which can be large.

#### Impact

- High memory usage
- Long query times
- Workers overwhelmed with messages

#### Workaround

Configure scheduled executor polling interval to limit batch sizes.

#### Solution

Add `LIMIT` parameter to `get_dependent_jobs()` function.

---

## Documentation Gaps

### 11. Incomplete API Documentation for Error Codes

**Severity:** LOW

#### Problem

API specification lists HTTP status codes but doesn't document:
- Detailed error response formats
- Error codes for specific failure scenarios
- Recovery actions for each error

#### Solution

Enhance OpenAPI specification with:
- Error response schemas
- Example error payloads
- Troubleshooting guidance

---

### 12. No Architecture Diagrams in Repository

**Severity:** LOW

#### Problem

Understanding system architecture requires reading code and database schema.

#### Solution

Add visual documentation:
- Component interaction diagrams
- Database ER diagrams
- Sequence diagrams for key flows

**Status:** Addressed in this documentation (see [ARCHITECTURE.md](./ARCHITECTURE.md))

---

## Workarounds Summary

| Issue | Recommended Workaround |
|-------|----------------------|
| Cancelled job recreation | Never reuse job IDs; use UUIDs |
| Jobs stuck after prerequisite fails | Enable `CAF_JOB_TRACKING_PROPAGATE_FAILURES=true` |
| Circular dependencies | Validate dependency graphs externally |
| No dependency query API | Query database directly |
| Duplicate jobs | Implement client-side deduplication |
| Large dependency trees | Limit fan-out in job design |

---

## Migration Path for Breaking Changes

If executing the long-term fixes that involve schema changes:

1. **Add new columns** (nullable initially):
   ```sql
   ALTER TABLE job ADD COLUMN job_execution_id UUID;
   ALTER TABLE job_dependency ADD COLUMN dependent_job_execution_id UUID;
   ```

2. **Backfill existing data**:
   ```sql
   UPDATE job SET job_execution_id = gen_random_uuid() 
   WHERE job_execution_id IS NULL;
   ```

3. **Update application code** to use new columns

4. **Make columns NOT NULL** after migration complete

5. **Update foreign keys** to reference new columns

6. **Remove old columns** in next major version

---

## Reporting Issues

If you encounter additional issues:

1. Check existing GitHub issues
2. Collect:
   - Job IDs involved
   - Database logs
   - API request/response logs
   - Dependency graph visualization
3. Create issue with reproducible steps

---

## Related Documentation

- [Dependency Management](./DEPENDENCY-MANAGEMENT.md) - How dependencies work
- [Database Schema](./DATABASE-SCHEMA.md) - Table structures
- [Architecture Overview](./ARCHITECTURE.md) - System design
- [Job Lifecycle](./JOB-LIFECYCLE.md) - State transitions

