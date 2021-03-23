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
 *  Name: report_complete_bulk
 *
 *  Description:
 *  Marks the specified tasks complete.
 *  Takes in an array of tasks
 */
CREATE OR REPLACE FUNCTION report_complete_bulk(
    in_partition_id VARCHAR(40),
    in_job_id VARCHAR(48),
    in_task_ids VARCHAR(58)[]
)
RETURNS TABLE(
    partition_id VARCHAR(40),
    job_id VARCHAR(48),
    task_classifier VARCHAR(255),
    task_api_version INT,
    task_data BYTEA,
    task_pipe VARCHAR(255),
    target_pipe VARCHAR(255)
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_job_status job_status;
    v_task_id VARCHAR(58);

BEGIN
    -- Raise exception if task identifier has not been specified
    IF array_length(in_task_ids, 1) = 0 THEN
        RAISE EXCEPTION 'Task identifier has not been specified';
    END IF;

    -- Get the job status
    -- And take out an exclusive update lock on the job row
    SELECT status INTO v_job_status
    FROM job j
    WHERE j.partition_id = in_partition_id
        AND j.job_id = in_job_id
    FOR UPDATE;

    -- Check that the job hasn't been deleted, cancelled or completed
    IF NOT FOUND OR v_job_status IN ('Cancelled', 'Completed') THEN
        RETURN;
    END IF;

    -- Check if the job has dependencies
    IF internal_has_dependent_jobs(in_partition_id, in_job_id) THEN

        -- Update the task statuses in the tables
        FOREACH v_task_id IN ARRAY in_task_ids
        LOOP
            PERFORM internal_report_task_status(in_partition_id, v_task_id, 'Completed', 100.00, NULL);
        END LOOP;

        -- If job has just completed, then return any jobs that can now be run
        IF internal_is_task_completed(in_partition_id, in_job_id) THEN
            -- Get a list of jobs that can run immediately and update the eligibility run date for others
            RETURN QUERY
            SELECT * FROM internal_process_dependent_jobs(in_partition_id, in_job_id);
        END IF;

    ELSE

        -- Insert all the incoming tasks into the completed_subtask_report table
        INSERT INTO completed_subtask_report (partition_id, job_id, task_id, report_date)
        SELECT in_partition_id, in_job_id, x.task_id, now() AT TIME ZONE 'UTC'
        FROM unnest(in_task_ids) AS x(task_id);

    END IF;
END
$$;
