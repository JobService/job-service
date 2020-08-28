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
 *  Name: report_failure
 *
 *  Description:
 *  Update the specified task and subsequent parent tasks/job with the failure details.
 */
DROP FUNCTION IF EXISTS report_failure(
    in_partition_id VARCHAR(40),
    in_task_id VARCHAR(58),
    in_short_task_id VARCHAR(58),
    in_failure_details TEXT
);
DROP FUNCTION IF EXISTS report_failure(
    in_partition_id VARCHAR(40),
    in_task_id VARCHAR(58),
    in_short_task_id VARCHAR(58),
    in_failure_details TEXT,
    in_propagate_failures BOOLEAN
);
CREATE OR REPLACE FUNCTION report_failure(
    in_partition_id VARCHAR(40),
    in_task_id VARCHAR(58),
    in_failure_details TEXT,
    in_propagate_failures BOOLEAN
)
RETURNS VOID
LANGUAGE plpgsql
AS $$
DECLARE
    v_job_id VARCHAR(48);
    v_job_status job_status;

BEGIN
    -- Raise exception if task identifier has not been specified
    IF in_task_id IS NULL OR in_task_id = '' THEN
        RAISE EXCEPTION 'Task identifier has not been specified';
    END IF;

    -- Raise exception if failure details have not been specified
    IF in_failure_details IS NULL OR in_failure_details = '' THEN
        RAISE EXCEPTION 'Failure details have not been specified';
    END IF;

    -- Get the job id
    v_job_id = internal_get_job_id(in_task_id);

    -- Adding sub-task details into a specific report table for analysis purpose ( SCMOD-10455 )
    INSERT INTO subtask_report (partition_id, job_id, task_id, status, report_date, failure_details) values (in_partition_id, v_job_id, in_task_id, 'Failed', now() AT TIME ZONE 'UTC', in_failure_details);


    -- Get the job status
    -- And take out an exclusive update lock on the job row
    SELECT status INTO v_job_status
    FROM job AS j
    WHERE j.partition_id = in_partition_id
        AND j.job_id = v_job_id
    FOR UPDATE;

    -- Check that the job hasn't been deleted, cancelled or completed
    IF NOT FOUND OR v_job_status IN ('Cancelled', 'Completed') THEN
        RETURN;
    END IF;

    -- Update the task statuses in the tables
    PERFORM internal_report_task_status(in_partition_id, in_task_id, 'Failed', 0.00, in_failure_details);

    IF in_propagate_failures THEN
        PERFORM internal_process_failed_dependent_jobs(in_partition_id, v_job_id, in_failure_details);
    END IF;
END
$$;
