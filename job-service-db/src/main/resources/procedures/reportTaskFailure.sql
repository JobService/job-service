--
-- Copyright 2015-2018 Micro Focus or one of its affiliates.
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
 *  Name: internal_report_task_failure
 *
 *  Description:  Recursively propagate failure details to parent tasks and job.
 *                Internal - used in report_failure().
 */
CREATE OR REPLACE FUNCTION internal_report_task_failure(
    in_task_table_name VARCHAR(63),
    in_failure_details TEXT
)
RETURNS VOID
LANGUAGE plpgsql
AS $$
DECLARE
    v_parent_table_name VARCHAR(63);
    v_percentage_completed DOUBLE PRECISION;
    v_task_id VARCHAR(58);
    v_job_id VARCHAR(48);
    v_temp SMALLINT;

BEGIN
    -- Raise exception if task identifier has not been specified
    IF in_task_table_name IS NULL OR in_task_table_name = '' THEN
        RAISE EXCEPTION 'Task table name has not been specified';
    END IF;

    -- Raise exception if failure details have not been specified
    IF in_failure_details IS NULL OR in_failure_details = '' THEN
        RAISE EXCEPTION 'Failure details have not been specified';
    END IF;

    -- Identify parent table to target from task table name
    -- If dot separator does not exist though in the specified task table name then we are dealing with the job table
    IF internal_is_job_id(in_task_table_name) THEN
        --  Extract job id from task table name (i.e. strip task_ prefix).
        v_job_id = substring(in_task_table_name from 6);

        PERFORM 1 FROM job WHERE job_id = v_job_id FOR UPDATE;
        UPDATE job
        SET status = 'Failed', failure_details = concat(failure_details, in_failure_details || chr(10))
        WHERE job_id = v_job_id;

    ELSE
        v_parent_table_name = substring(in_task_table_name, 1, internal_get_last_position(in_task_table_name, '.') - 1);

        -- Identify task id from task table name (i.e. strip task_ prefix) to determine which row in the parent table to target
        v_task_id = substring(in_task_table_name from 6);

        -- Modify parent target table and update it's status and % completed
        EXECUTE format('SELECT 1 FROM %I WHERE task_id = %L FOR UPDATE', v_parent_table_name, v_task_id) INTO v_temp;
        EXECUTE format($FORMAT_STR$
            UPDATE %I
            SET status = 'Failed',
                failure_details = concat(failure_details, %L || chr(10))
            WHERE task_id = %L
        $FORMAT_STR$, v_parent_table_name, in_failure_details, v_task_id);

        -- Recursively call the same function for the specified v_parent_table_name
        PERFORM internal_report_task_failure(v_parent_table_name, in_failure_details);
    END IF;
END
$$;
