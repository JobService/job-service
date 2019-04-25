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
 *  Name: internal_process_dependent_jobs
 *
 *  Description:
 *  Return a list of jobs that can run immediately. Update the eligibility run date for others.
 */
DROP FUNCTION IF EXISTS internal_process_dependent_jobs(in_job_id VARCHAR(58));
CREATE OR REPLACE FUNCTION internal_process_dependent_jobs(in_job_id VARCHAR(48))
RETURNS TABLE(
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
        INNER JOIN job j ON j.job_id = jd.job_id
        WHERE jd.dependent_job_id = in_job_id;

    -- lock rows
--     PERFORM NULL FROM public.job WHERE job.job_id IN (SELECT tmp_dependent_jobs.job_id FROM tmp_dependent_jobs)
--             ORDER BY job.job_id ASC FOR UPDATE;

    -- Remove corresponding dependency related rows for jobs that can be processed immediately
    DELETE
    FROM job_dependency
    WHERE dependent_job_id = in_job_id;

    --Set the eligible_to_run_date for jobs with a delay, these will be picked up by scheduled executor
    UPDATE job_task_data
    SET eligible_to_run_date = now() AT TIME ZONE 'UTC' + (tmp_dependent_jobs.delay * interval '1 second')
    FROM tmp_dependent_jobs 
        WHERE 
        job_task_data.eligible_to_run_date IS NULL
        AND tmp_dependent_jobs.job_id = job_task_data.job_id 
        AND tmp_dependent_jobs.delay <> 0 
        AND NOT EXISTS (
                        SELECT job_dependency.job_id FROM job_dependency
                            WHERE job_dependency.job_id = job_task_data.job_id);
    
    -- Return jobs with no delay that we can now run and delete the tasks
    DELETE 
        FROM job_task_data jtd
        WHERE jtd.job_id IN (
            SELECT jtd.job_id
                FROM job_task_data jtd
                    INNER JOIN tmp_dependent_jobs dp ON dp.job_id = jtd.job_id
                WHERE dp.delay = 0 AND NOT EXISTS (
                    SELECT job_dependency.job_id 
                        FROM job_dependency 
                        WHERE job_dependency.job_id = dp.job_id
                )
        )
        RETURNING jtd.job_id, jtd.task_classifier, jtd.task_api_version, jtd.task_data, jtd.task_pipe, jtd.target_pipe;

END
$$;
