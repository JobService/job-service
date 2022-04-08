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
 *  Name: get_job
 *
 *  Description:
 *  Returns the job definition for the specified job.
 */
CREATE OR REPLACE FUNCTION get_job(
    in_partition_id VARCHAR(40),
    in_job_id VARCHAR(48)
)
RETURNS TABLE(
    job_id VARCHAR(48),
    name VARCHAR(255),
    description TEXT,
    data TEXT,
    create_date TEXT,
    last_update_date TEXT,
    status job_status,
    percentage_complete DOUBLE PRECISION,
    failure_details TEXT,
    actionType CHAR(6),
    label VARCHAR(255),
    label_value VARCHAR(255),
    expiration_status job_status,
    operation expiration_operation,
    expiration_time VARCHAR(58),
    policer expiration_policer
)
LANGUAGE plpgsql VOLATILE
AS $$
BEGIN
    -- Raise exception if the job identifier has not been specified
    IF in_job_id IS NULL OR in_job_id = '' THEN
        RAISE EXCEPTION 'Job identifier has not been specified' USING ERRCODE = '02000'; -- sqlstate no data
    END IF;

    -- Take out an exclusive update lock on the job row
    PERFORM NULL
    FROM job j
    WHERE j.partition_id = in_partition_id
        AND j.job_id = in_job_id
    FOR UPDATE;

    -- Process outstanding job updates
    PERFORM internal_update_job_progress(in_partition_id, in_job_id);

    -- Return job metadata belonging to the specified job_id
    -- 'WORKER' is the only supported action type for now
    RETURN QUERY
    SELECT job.job_id,
           job.name,
           job.description,
           job.data,
           to_char(job.create_date, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'),
           to_char(job.last_update_date, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'),
           job.status,
           job.percentage_complete,
           job.failure_details,
           CAST('WORKER' AS CHAR(6)) AS actionType,
           lbl.label,
           lbl.value,
           djep.job_status,
           COALESCE (jep.operation, djep.operation) AS operation,
           COALESCE (jep.expiration_time, djep.expiration_time ) AS expiration_time,
           CASE
               WHEN jep.operation IS NULL THEN
                   'System'::EXPIRATION_POLICER
               ELSE
                   'User'::EXPIRATION_POLICER
           END
    FROM job
    CROSS JOIN default_job_expiration_policy djep
    LEFT JOIN public.label lbl
           ON lbl.partition_id = job.partition_id
               AND lbl.job_id = job.job_id

    LEFT JOIN job_expiration_policy jep
           ON jep.partition_id = job.partition_id
               AND jep.job_id = job.job_id
               AND djep.job_status = jep.job_status
    WHERE job.partition_id = in_partition_id
        AND job.job_id = in_job_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'job_id {%} not found', in_job_id USING ERRCODE = 'P0002'; -- sqlstate no_data_found
    END IF;
END
$$;
