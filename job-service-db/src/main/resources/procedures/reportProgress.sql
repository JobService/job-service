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
 *  Name: report_progress
 *
 *  Description:
 *  Reports a task progress to the database
 */
DROP FUNCTION IF EXISTS report_progress(
    in_task_id VARCHAR(58),
    in_status job_status
);
CREATE OR REPLACE PROCEDURE report_progress(
    in_partition_id VARCHAR(40),
    in_job_id VARCHAR(58),
    in_task_id VARCHAR(58),
    in_task_completion DOUBLE PRECISION
)
    LANGUAGE plpgsql
AS $$
DECLARE
    in_status job_status;
BEGIN

    -- Raise exception if the job identifier has not been specified
    IF in_job_id IS NULL OR in_job_id = '' THEN
        RAISE EXCEPTION 'Job identifier has not been specified' USING ERRCODE = '02000'; -- sqlstate no data
    END IF;

    -- Exit the procedure if task progress is 0
    IF in_task_completion = 0
        THEN RETURN;
    END IF;

    -- Take out an exclusive update lock on the job row
    PERFORM NULL
    FROM job j
    WHERE j.partition_id = in_partition_id
      AND j.job_id = in_job_id
        FOR UPDATE;

    IF in_task_completion = 100
    THEN
        in_status = 'Completed';
    ELSE
        in_status = 'Active';
    END IF;

    -- Process outstanding job updates
    PERFORM internal_report_task_status(in_partition_id, in_task_id , in_status, in_task_completion, NULL);

END
$$;
