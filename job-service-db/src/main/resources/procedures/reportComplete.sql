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
 *  Name: report_complete
 *
 *  Description:
 *  Marks the specified task complete.
 */
DROP FUNCTION IF EXISTS report_complete(
    in_partition_id VARCHAR(40),
    in_task_id VARCHAR(58),
    in_short_task_id VARCHAR(58)
);
CREATE OR REPLACE FUNCTION report_complete(
    in_partition_id VARCHAR(40),
    in_task_id VARCHAR(58)
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
    v_job_id VARCHAR(48);
   -- v_job_status job_status;

BEGIN
    -- Raise exception if task identifier has not been specified
    IF in_task_id IS NULL OR in_task_id = '' THEN
        RAISE EXCEPTION 'Task identifier has not been specified';
    END IF;

/*    -- Check that the job hasn't been deleted, cancelled or completed
    IF NOT FOUND OR v_job_status IN ('Cancelled', 'Completed') THEN
        RETURN;
    END IF;*/


    -- Get the job id
    v_job_id = internal_get_job_id(in_task_id);

    -- Insert values into table
    INSERT INTO completed_subtask_report (partition_id, job_id, task_id, report_date) VALUES (in_partition_id, v_job_id, in_task_id, now() AT TIME ZONE 'UTC');

END
$$;
