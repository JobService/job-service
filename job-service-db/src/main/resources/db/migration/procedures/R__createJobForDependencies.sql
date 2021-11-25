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
 *  Name: create_job
 *
 *  Description:
 *  Create a new row in the job table.  Returns a single row, with true if the job doesn't already
 *  exist, and false if the job already exists with the same hash.
 *  The function also stores task data and job dependency details if any of
 *  the specified prerequisite job identifiers are not yet complete.
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
    in_prerequisite_job_ids VARCHAR(128)[],
    in_delay INT,
    in_labels VARCHAR(255)[][] default null,
    in_suspended_partition BOOLEAN default false
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

    -- Store task data and job dependency rows if any of the prerequisite job identifiers are not yet complete

    -- Store dependency rows for those prerequisite job identifiers not yet complete
    -- Include prerequisite job identifiers not yet in the system
    WITH prereqs_with_opts(job_id_with_opts) AS
    (
        SELECT unnest(in_prerequisite_job_ids)::VARCHAR(128)
    ),
    prereqs AS
    (
        -- Remove any duplicate pre-requisites, and if a pre-req is mentioned multiple times then merge the options
        SELECT job_id, precreated FROM
        (
            SELECT ROW_NUMBER() OVER (PARTITION BY job_id ORDER BY precreated DESC), job_id, precreated
            FROM prereqs_with_opts
            CROSS JOIN internal_get_prereq_job_id_options(job_id_with_opts)
        ) tbl
        WHERE row_number = 1
    ),
    locked_jobs AS
    (
        -- Lock table job for update
        SELECT * FROM job
        WHERE partition_id = in_partition_id
            AND job_id IN (SELECT job_id FROM prereqs)
        ORDER BY partition_id, job_id
        FOR UPDATE
    ),
    updated_jobs AS
    (
        -- Process outstanding job updates
        SELECT * FROM internal_update_job_progress(in_partition_id, (SELECT ARRAY(SELECT job_id FROM locked_jobs)))
    ),
    prereqs_created_but_not_complete AS
    (
        SELECT * FROM updated_jobs uj
        WHERE uj.partition_id = in_partition_id
            AND uj.job_id IN (SELECT job_id FROM prereqs)
            AND uj.status <> 'Completed'
    ),
    prereqs_not_created_yet AS
    (
        SELECT * FROM prereqs
        WHERE NOT precreated AND job_id NOT IN (
            SELECT job_id FROM job WHERE partition_id = in_partition_id
        )
    ),
    all_incomplete_prereqs(prerequisite_job_id) AS
    (
        SELECT job_id FROM prereqs_created_but_not_complete
        UNION
        SELECT job_id FROM prereqs_not_created_yet
    )

    INSERT INTO public.job_dependency(partition_id, job_id, dependent_job_id)
    SELECT in_partition_id, in_job_id, prerequisite_job_id
    FROM all_incomplete_prereqs;

    IF FOUND OR in_delay > 0 OR in_suspended_partition THEN
        INSERT INTO public.job_task_data(
            partition_id,
            job_id,
            task_classifier,
            task_api_version,
            task_data,
            task_pipe,
            target_pipe,
            eligible_to_run_date,
            suspended
        ) VALUES (
            in_partition_id,
            in_job_id,
            in_task_classifier,
            in_task_api_version,
            in_task_data,
            in_task_pipe,
            in_target_pipe,
            CASE WHEN NOT FOUND THEN now() AT TIME ZONE 'UTC' + (in_delay * interval '1 second') END,
            in_suspended_partition
        );
    END IF;

    RETURN QUERY SELECT TRUE;
END
$$;
