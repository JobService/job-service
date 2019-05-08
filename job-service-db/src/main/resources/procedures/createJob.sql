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
 *  Name: create_job
 *
 *  Description:
 *  Create a new row in the job table.
 */
CREATE OR REPLACE FUNCTION create_job(
    in_partition VARCHAR(40),
    in_job_id VARCHAR(48),
    in_name VARCHAR(255),
    in_description TEXT,
    in_data TEXT,
    in_job_hash INT
)
RETURNS VOID
LANGUAGE plpgsql
AS $$
BEGIN
    -- Raise exception if job identifier has not been specified
    IF in_job_id IS NULL OR in_job_id = '' THEN
        RAISE EXCEPTION 'Job identifier has not been specified' USING ERRCODE = '02000'; -- sqlstate no data
    END IF;

    -- Create new row in job and return the job_id
    INSERT INTO public.job(
        partition,
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
        in_partition,
        in_job_id,
        in_name,
        in_description,
        in_data,
        now() AT TIME ZONE 'UTC',
        now() AT TIME ZONE 'UTC',
        'Waiting',
        0.00,
        NULL,
        0,
        in_job_hash
    );
END
$$;
