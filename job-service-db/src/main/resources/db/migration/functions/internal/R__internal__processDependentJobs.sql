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
 *  Name: internal_process_dependent_jobs
 *
 *  Description:
 *  Return a list of jobs that can run immediately. Update the eligibility run date for others.
 */
CREATE OR REPLACE FUNCTION internal_process_dependent_jobs(
    in_partition_id VARCHAR(40),
    in_job_id VARCHAR(48)
)
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
    -- Get a list of jobs that depend on in_job_id
    CREATE TEMPORARY TABLE tmp_dependent_jobs
        ON COMMIT DROP
    AS
        SELECT j.job_id, j.delay
        FROM job_dependency jd
        INNER JOIN job j ON j.partition_id = jd.partition_id AND j.job_id = jd.job_id
        WHERE jd.partition_id = in_partition_id
            AND jd.dependent_job_id = in_job_id;

    -- Ensure that no other `job_dependency` deletion can run until we've committed, so we don't
    -- miss the deletion of the last dependency.  Lock ALL possibly conflicting rows up-front to
    -- avoid deadlocks.
    PERFORM NULL FROM job_dependency AS jd
    WHERE jd.partition_id = in_partition_id
        AND jd.job_id IN (SELECT tmp_dependent_jobs.job_id FROM tmp_dependent_jobs)
    ORDER BY jd.partition_id, jd.job_id, jd.dependent_job_id
    FOR UPDATE;

    -- Remove corresponding dependency related rows for jobs that can be processed immediately
    DELETE
    FROM job_dependency AS jd
    WHERE jd.partition_id = in_partition_id
        AND jd.dependent_job_id = in_job_id;

    --Set the eligible_to_run_date for jobs with a delay, these will be picked up by scheduled executor
    UPDATE job_task_data
    SET eligible_to_run_date = now() AT TIME ZONE 'UTC' + (tmp_dependent_jobs.delay * interval '1 second')
    FROM tmp_dependent_jobs 
        WHERE 
        job_task_data.eligible_to_run_date IS NULL
        AND job_task_data.partition_id = in_partition_id
        AND tmp_dependent_jobs.job_id = job_task_data.job_id 
        AND tmp_dependent_jobs.delay <> 0 
        AND NOT EXISTS (
            SELECT job_dependency.job_id FROM job_dependency
            WHERE job_dependency.partition_id = job_task_data.partition_id
                AND job_dependency.job_id = job_task_data.job_id
        );
    
    -- Return jobs with no delay that we can now run and delete the tasks
    RETURN QUERY
    WITH del_result AS (DELETE
        FROM job_task_data jtd
        WHERE jtd.partition_id = in_partition_id AND jtd.job_id IN (
            SELECT jtd.job_id
                FROM job_task_data jtd
                    INNER JOIN tmp_dependent_jobs dp ON dp.job_id = jtd.job_id
                WHERE dp.delay = 0 AND NOT EXISTS (
                    SELECT job_dependency.job_id 
                    FROM job_dependency
                    WHERE job_dependency.partition_id = in_partition_id
                        AND job_dependency.job_id = dp.job_id
                )
        )
        RETURNING
            jtd.partition_id,
            jtd.job_id,
            jtd.task_classifier,
            jtd.task_api_version,
            jtd.task_data,
            jtd.task_pipe,
            jtd.target_pipe
    )
    SELECT
        del_result.partition_id,
        del_result.job_id,
        del_result.task_classifier,
        del_result.task_api_version,
        del_result.task_data,
        del_result.task_pipe,
        del_result.target_pipe
    FROM del_result;

END
$$;
