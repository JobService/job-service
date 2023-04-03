--
-- Copyright 2016-2022 Micro Focus or one of its affiliates.
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
 *  Name: report_progress
 *
 *  Description:
 *  Reports a task progress to the database
 */
CREATE OR REPLACE FUNCTION report_progress(
    in_partition_id VARCHAR(40),
    in_task_id VARCHAR(70),
    in_percentage_complete DOUBLE PRECISION
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

    -- Raise exception if task progress is < 0 or > 100
    IF in_percentage_complete < 0 OR in_percentage_complete > 100 THEN
        RAISE EXCEPTION 'Invalid in_percentage_complete %', in_percentage_complete USING ERRCODE = '22023'; -- invalid_parameter_value
    END IF;

    -- Get the job id
    v_job_id = internal_get_job_id(in_task_id);

    -- Get the job status
    -- And take out an exclusive update lock on the job row
    SELECT status INTO v_job_status
    FROM job
    WHERE partition_id = in_partition_id
      AND job_id = v_job_id
    FOR UPDATE;

    -- Check that the job hasn't been deleted, cancelled or completed
    IF NOT FOUND OR v_job_status IN ('Cancelled', 'Completed') THEN
        RETURN;
    END IF;

    -- Update the task tables
    PERFORM internal_report_task_status(in_partition_id, in_task_id, 'Active', LEAST(in_percentage_complete, 99.9), NULL);
END
$$;
