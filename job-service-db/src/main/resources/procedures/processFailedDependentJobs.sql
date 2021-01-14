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
 *  Name: internal_process_failed_dependent_jobs
 */
DROP FUNCTION IF EXISTS internal_process_failed_dependent_jobs(in_partition_id VARCHAR(40), in_job_id VARCHAR(58),
                                                               in_failure_details TEXT);
CREATE OR REPLACE FUNCTION internal_process_failed_dependent_jobs(
    in_partition_id VARCHAR(40),
    in_job_id VARCHAR(48),
    in_failure_details TEXT
)
RETURNS VOID
LANGUAGE plpgsql
AS $$
DECLARE
    failure_details_var TEXT;
BEGIN
    CREATE TEMPORARY TABLE tmp_dependent_jobs
        ON COMMIT DROP
    AS
        WITH RECURSIVE all_job_dependencies AS (
            SELECT partition_id, dependent_job_id, job_id
            FROM job_dependency
            UNION ALL
            SELECT adj.partition_id, adj.dependent_job_id, jd.job_id
            FROM all_job_dependencies adj
            INNER JOIN job_dependency jd ON adj.partition_id = jd.partition_id AND adj.job_id = jd.dependent_job_id
        )
        SELECT job_id FROM all_job_dependencies
        WHERE dependent_job_id = in_job_id AND partition_id = in_partition_id;

    -- Ensure that no other `job_dependency` deletion can run until we've committed.
    -- Lock ALL possibly conflicting rows up-front to avoid deadlocks.
    PERFORM NULL FROM job_dependency AS jd
    WHERE jd.partition_id = in_partition_id
        AND jd.job_id IN (SELECT tmp_dependent_jobs.job_id FROM tmp_dependent_jobs)
    ORDER BY jd.partition_id, jd.job_id, jd.dependent_job_id
    FOR UPDATE;

    -- Ensure that no other `job` updates can run on these jobs until we've committed
    -- Lock ALL possibly conflicting rows up-front to avoid deadlocks.
    PERFORM NULL FROM job AS j
    WHERE j.partition_id = in_partition_id
        AND j.job_id IN (SELECT tmp_dependent_jobs.job_id FROM tmp_dependent_jobs)
    ORDER BY j.partition_id, j.job_id
    FOR UPDATE;

    failure_details_var = '{"root_failure": "' || in_partition_id || ':' || in_job_id || '", "failure_details": ' || in_failure_details || '}';

    UPDATE job AS j
    SET status = 'Failed', percentage_complete = 0.00, failure_details = failure_details_var, last_update_date = now() AT TIME ZONE 'UTC'
    WHERE j.job_id IN (SELECT tmp_dependent_jobs.job_id FROM tmp_dependent_jobs) AND partition_id = in_partition_id;

    DELETE
    FROM job_dependency AS jd
    WHERE jd.partition_id = in_partition_id
        AND jd.job_id IN (SELECT tmp_dependent_jobs.job_id FROM tmp_dependent_jobs);
END;
$$;
