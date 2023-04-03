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
 *  Name: pause_job
 *
 *  Description:
 *  Pauses the specified job.
 */
CREATE OR REPLACE FUNCTION pause_job(
    in_partition_id VARCHAR(40),
    in_job_id VARCHAR(48)
)
RETURNS VOID
LANGUAGE plpgsql
AS $$
DECLARE
    v_job_status job_status;

BEGIN
    -- Raise exception if job identifier has not been specified
    IF in_job_id IS NULL OR in_job_id = '' THEN
        RAISE EXCEPTION 'The job identifier has not been specified' USING ERRCODE = '02000'; -- sqlstate no data;
    END IF;

    -- Only support Pause operation on jobs with current status 'Active', 'Paused' or 'Waiting'
    SELECT status INTO v_job_status
    FROM job
    WHERE partition_id = in_partition_id
        AND job_id = in_job_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'job_id {%} not found', in_job_id USING ERRCODE = 'P0002'; -- sqlstate no_data_found
    END IF;

    IF v_job_status NOT IN ('Active', 'Paused', 'Waiting') THEN
        RAISE EXCEPTION 'job_id {%} cannot be paused as it has a status of {%}. '
                        'Only jobs with a status of Active or Waiting can be paused.',
            in_job_id, v_job_status USING ERRCODE = '02000';
    END IF;

    -- Mark the job paused in the job table
    UPDATE job
    SET status = 'Paused', last_update_date = now() AT TIME ZONE 'UTC'
    WHERE partition_id = in_partition_id
        AND job_id = in_job_id
        AND status != 'Paused';
END
$$;
