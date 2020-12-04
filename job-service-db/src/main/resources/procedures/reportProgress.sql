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
CREATE OR REPLACE FUNCTION report_progress(
    in_partition_id VARCHAR(40),
    in_task_id VARCHAR(58),
    in_percentage_complete DOUBLE PRECISION
)
RETURNS VOID
LANGUAGE plpgsql
AS $$
DECLARE
    v_job_id VARCHAR(48);
    v_status job_status:='Active';
BEGIN

    -- Raise exception if task progress is < 0 or > 100
    IF in_percentage_complete < 0 OR in_percentage_complete > 100 THEN
        RAISE EXCEPTION 'Invalid in_percentage_complete %', in_percentage_complete USING ERRCODE = '22023'; -- invalid_parameter_value
    END IF;

    -- Round down if 100 value passed in
    IF in_percentage_complete = 100 THEN
       in_percentage_complete=99.9;
    END IF;

    -- Get the job id
    v_job_id = internal_get_job_id(in_task_id);

    -- Raise exception if the job identifier has not been specified
    IF v_job_id IS NULL OR v_job_id = '' THEN
        RAISE EXCEPTION 'Invalid Job identifier' USING ERRCODE = '02000'; -- sqlstate no data
    END IF;

    -- Take out an exclusive update lock on the job row
    PERFORM NULL
    FROM job j
    WHERE j.partition_id = in_partition_id
      AND j.job_id = v_job_id
        FOR UPDATE;

    -- Process outstanding job updates
    PERFORM internal_report_task_status(in_partition_id, in_task_id , v_status, in_percentage_complete, NULL);

END
$$;