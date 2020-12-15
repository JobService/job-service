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
 *  Name: get_dependent_jobs
 *
 *  Description:
 *  Returns a list of dependent jobs that are now eligible to run.
 */
DROP FUNCTION IF EXISTS get_dependent_jobs();
CREATE FUNCTION get_dependent_jobs()
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
BEGIN
    CREATE TEMPORARY TABLE tmp_dependent_jobs
        ON COMMIT DROP
    AS
        SELECT
            jtd.partition_id,
            jtd.job_id,
            jtd.task_classifier,
            jtd.task_api_version,
            jtd.task_data,
            jtd.task_pipe,
            jtd.target_pipe
        FROM job_task_data jtd
        LEFT JOIN job_dependency jd
            ON jd.partition_id = jtd.partition_id AND jd.job_id = jtd.job_id
        WHERE NOT jtd.suspended
            AND jtd.eligible_to_run_date IS NOT NULL
            AND jtd.eligible_to_run_date <= now() AT TIME ZONE 'UTC'  -- now eligible for running
            AND jd.job_id IS NULL;  -- no other dependencies to wait on

    -- Get list of jobs that can now be run
    RETURN QUERY
    SELECT *
    FROM tmp_dependent_jobs;

END
$$;
