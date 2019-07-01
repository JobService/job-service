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
 *  Name: internal_create_job
 *
 *  Description:
 *  Create a new row in the job table.  Returns true if the job doesn't already exist, and false if
 *  the job already exists with the same hash.
 */
CREATE OR REPLACE FUNCTION internal_create_job(
    in_partition_id VARCHAR(40),
    in_job_id VARCHAR(48),
    in_name VARCHAR(255),
    in_description TEXT,
    in_data TEXT,
    in_delay INT,
    in_job_hash INT
)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
BEGIN

    INSERT INTO public.job(
        partition_id,
        job_id,
        name,
        description,
        data,
        create_date,
        last_update_date,
        status,
        percentage_complete,
        failure_details,
        delay,
        job_hash
    ) VALUES (
        in_partition_id,
        in_job_id,
        in_name,
        in_description,
        in_data,
        now() AT TIME ZONE 'UTC',
        now() AT TIME ZONE 'UTC',
        'Waiting',
        0.00,
        NULL,
        in_delay,
        in_job_hash
    );

    RETURN TRUE;

EXCEPTION WHEN unique_violation THEN

    IF EXISTS(
        SELECT 1 FROM job
        WHERE job.partition_id = in_partition_id
          AND job.job_id = in_job_id
          AND job.job_hash = in_job_hash
    ) THEN
        RETURN FALSE;
    ELSE
        RAISE;
    END IF;

END
$$;
