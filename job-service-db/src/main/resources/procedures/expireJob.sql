--
-- Copyright 2016-2021 Micro Focus or one of its affiliates.
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--      http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

/*
 *  Name: internal_expire_job
 *
 *  Description:
 *  Expires the specified job.
 */
CREATE OR REPLACE PROCEDURE internal_expire_job(
    in_partition_id VARCHAR(40),
    in_job_id VARCHAR(48)
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_is_finished BOOLEAN;

BEGIN
    -- Only support Expire operation on jobs with current status 'Waiting', 'Active' or 'Paused'
    -- And take out an exclusive update lock on the job row
    SELECT status IN ('Failed', 'Cancelled', 'Completed') INTO v_is_finished
    FROM job
    WHERE partition_id = in_partition_id
      AND job_id = in_job_id
        FOR UPDATE;

    IF NOT FOUND OR v_is_finished THEN
        RETURN;
    END IF;

    -- Mark the job cancelled in the job table
    UPDATE job
    SET status = 'Expired', last_update_date = now() AT TIME ZONE 'UTC'
    WHERE partition_id = in_partition_id
      AND job_id = in_job_id
      AND status != 'Expired';

    -- Drop any task tables relating to the job
    PERFORM internal_drop_task_tables(in_partition_id, in_job_id);

    -- Removes all related subtasks from completed_subtask_report table
    PERFORM internal_cleanup_completed_subtask_report(in_partition_id, in_job_id);
END
$$;
