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
 *  Name: create_job
 *
 *  Description:
 *  Create a new row in the job table.  Returns a single row, with true if the job doesn't already
 *  exist, and false if the job already exists with the same hash.
 */
CREATE OR REPLACE FUNCTION create_job(
    in_partition_id VARCHAR(40),
    in_job_id VARCHAR(48),
    in_name VARCHAR(255),
    in_description TEXT,
    in_data TEXT,
    in_job_hash INT,
    in_task_classifier VARCHAR(255),
    in_task_api_version INT,
    in_task_data BYTEA,
    in_task_pipe VARCHAR(255),
    in_target_pipe VARCHAR(255),
    in_delay INT,
    in_labels VARCHAR(255)[][] default null
)
RETURNS TABLE(
    job_created BOOLEAN
)
LANGUAGE plpgsql
AS $$
BEGIN
    -- Raise exception if job identifier has not been specified
    IF in_job_id IS NULL OR in_job_id = '' THEN
        RAISE EXCEPTION 'Job identifier has not been specified' USING ERRCODE = '02000'; -- sqlstate no data
    END IF;

    -- Raise exception if job taskClassifier has not been specified
    IF in_task_classifier IS NULL OR in_task_classifier = '' THEN
        RAISE EXCEPTION 'Job taskClassifier has not been specified' USING ERRCODE = '02000'; -- sqlstate no data
    END IF;

    -- Raise exception if job taskApiVersion has not been specified
    IF in_task_api_version IS NULL THEN
        RAISE EXCEPTION 'Job taskApiVersion has not been specified' USING ERRCODE = '02000'; -- sqlstate no data
    END IF;

    -- Raise exception if job taskData has not been specified
    IF in_task_data IS NULL THEN
        RAISE EXCEPTION 'Job taskData has not been specified' USING ERRCODE = '02000'; -- sqlstate no data
    END IF;

    -- Raise exception if job taskPipe has not been specified
    IF in_task_pipe IS NULL OR in_task_pipe = '' THEN
        RAISE EXCEPTION 'Job taskPipe has not been specified' USING ERRCODE = '02000'; -- sqlstate no data
    END IF;

    -- Raise exception if job targetPipe is empty. Null targetPipe is valid
    IF in_target_pipe = '' THEN
        RAISE EXCEPTION 'Job targetPipe has not been specified' USING ERRCODE = '02000'; -- sqlstate no data
    END IF;

    -- Set default value for delay if no value provided
    IF in_delay IS NULL THEN
        in_delay = 0;
    END IF;

    IF NOT internal_create_job(in_partition_id, in_job_id, in_name, in_description, in_data, in_delay, in_job_hash, in_labels) THEN
        RETURN QUERY SELECT FALSE;
        RETURN;
    END IF;

        INSERT INTO public.job_task_data(
            partition_id,
            job_id,
            task_classifier,
            task_api_version,
            task_data,
            task_pipe,
            target_pipe,
            eligible_to_run_date
        ) VALUES (
            in_partition_id,
            in_job_id,
            in_task_classifier,
            in_task_api_version,
            in_task_data,
            in_task_pipe,
            in_target_pipe,
            now() AT TIME ZONE 'UTC' + (in_delay * interval '1 second')
        );

    RETURN QUERY SELECT TRUE;

END
$$;
