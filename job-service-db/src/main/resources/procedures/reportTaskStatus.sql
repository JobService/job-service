--
-- Copyright 2016-2020 Micro Focus or one of its affiliates.
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
 *  Name: internal_report_task_status
 *
 *  Description:
 *  Updates the status of the specified task, and rolls the status update up to all the parent tasks.
 *
 *   - in_short_task_id: additional identification for the same task - see
 *                       com.hpe.caf.services.job.util.JobTaskId#getShortId
 */
CREATE OR REPLACE FUNCTION internal_report_task_status(
    in_partition_id VARCHAR(40),
    in_task_id VARCHAR(58),
    in_short_task_id VARCHAR(58),
    in_status job_status,
    in_percentage_complete DOUBLE PRECISION,
    in_failure_details TEXT
)
RETURNS VOID
LANGUAGE plpgsql
AS $$
DECLARE
    v_parent_task_id VARCHAR(58);
    v_parent_short_task_id VARCHAR(58);
    v_parent_task_table VARCHAR(63);

BEGIN
    -- Ignore the status report if the task has already been completed
    IF internal_is_task_completed(in_partition_id, in_task_id, in_short_task_id) THEN
        RETURN;
    END IF;

    -- If the task is being marked completed, then drop any subtask tables
    IF in_status = 'Completed' THEN
        PERFORM internal_drop_task_tables(in_short_task_id);
    END IF;

    -- Get the parent task id
    v_parent_task_id = internal_get_parent_task_id(in_task_id);
    v_parent_short_task_id = internal_get_parent_task_id(in_short_task_id);

    -- Check if we are dealing with the top level job or a subtask
    IF v_parent_task_id IS NULL THEN
        -- Mark up the job status in the job table
        UPDATE job
        SET status = internal_resolve_status(status, in_status),
            percentage_complete = round(in_percentage_complete::numeric, 2),
            failure_details = in_failure_details,
            last_update_date = now() AT TIME ZONE 'UTC'
        WHERE partition_id = in_partition_id
            AND job_id = in_task_id;
    ELSE
        -- Put together the parent task table name
        v_parent_task_table = internal_get_task_table_name(v_parent_short_task_id);

        -- Create the parent task table if necessary
        PERFORM internal_create_task_table(v_parent_task_table);

        -- Mark up the task status in the parent task table
        PERFORM internal_upsert_into_task_table(
            v_parent_task_table,
            in_task_id,
            in_status,
            in_percentage_complete,
            in_failure_details);

        -- Get the overall status of the parent task and recursively call into this function to update the parent tasks
        PERFORM internal_report_task_status(
            in_partition_id,
            v_parent_task_id,
            v_parent_short_task_id,
            status,
            percentage_complete,
            failure_details)
        FROM internal_get_task_status(v_parent_task_table);
    END IF;
END
$$;
