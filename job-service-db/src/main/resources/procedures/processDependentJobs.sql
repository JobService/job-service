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
 *  Description:  Return a list of jobs that can run immediately. Update the eligibility run date for others.
 *                Internal - used in report_progress() and internal_report_task_completion.
 */
DROP FUNCTION IF EXISTS internal_process_dependent_jobs(in_job_id varchar(58));
CREATE FUNCTION internal_process_dependent_jobs(in_job_id varchar(58))
  RETURNS TABLE (job_id VARCHAR(48), task_classifier VARCHAR(255), task_api_version INT, task_data BYTEA,
  task_pipe VARCHAR(255), target_pipe VARCHAR(255)) AS $$
#variable_conflict use_column  
DECLARE
BEGIN

  --  Raise exception if job identifier has not been specified.
  IF in_job_id IS NULL OR in_job_id = '' THEN
    RAISE EXCEPTION 'Job identifier has not been specified';
  END IF;

      -- Get a list of jobs that can be processed without delay.
  CREATE TEMPORARY TABLE tmp_dependent_jobs_ready_to_run
    ON COMMIT DROP
  AS
  SELECT jtd.job_id, jtd.task_classifier, jtd.task_api_version, jtd.task_data, jtd.task_pipe, jtd.target_pipe
  FROM job_task_data jtd
  INNER JOIN job j
    ON j.job_id = jtd.job_id
  INNER JOIN job_dependency jd
    ON jd.job_id = jtd.job_id
  WHERE j.delay = 0
  AND jd.dependent_job_id = in_job_id
  AND NOT EXISTS (SELECT jd2.job_id
                  FROM job_dependency jd2
                  INNER JOIN job j2
                    ON j2.job_Id = jd2.dependent_job_id
                  WHERE jd2.job_id = jd.job_Id
                  AND jd2.dependent_job_id <> in_job_id
                  AND j2.status <> 'Completed');

  RETURN QUERY
  SELECT * FROM tmp_dependent_jobs_ready_to_run;

  -- Remove corresponding dependency related rows for jobs that can be processed immediately.
  DELETE FROM job_dependency jd
    USING tmp_dependent_jobs_ready_to_run tdjrtr
    WHERE tdjrtr.job_id = jd.job_id;

  DELETE FROM job_task_data jtd
    USING tmp_dependent_jobs_ready_to_run tdjrtr
    WHERE tdjrtr.job_id = jtd.job_id;

  -- For jobs awaiting a delay, update their eligibility run dates.
  CREATE TEMPORARY TABLE tmp_dependent_jobs_with_delay
    ON COMMIT DROP
  AS
  SELECT j.job_id, j.delay
  FROM job j
  INNER JOIN job_dependency jd
    ON jd.job_id = j.job_id
  WHERE j.delay <> 0
  AND jd.dependent_job_id = in_job_id
  AND NOT EXISTS (SELECT jd2.job_id
                  FROM job_dependency jd2
                  INNER JOIN job j2
                    ON j2.job_Id = jd2.dependent_job_id
                  WHERE jd2.job_id = jd.job_Id
                  AND jd2.dependent_job_id <> in_job_id
                  AND j2.status <> 'Completed');

  UPDATE job_task_data jtd
  SET eligible_to_run_date = now() AT TIME ZONE 'UTC' + (j.delay * interval '1 second')
  FROM job j
  INNER JOIN tmp_dependent_jobs_with_delay tdjwd
    ON tdjwd.job_id = j.job_id
  WHERE jtd.job_id = j.job_id;

  -- Remove job_dependency rows for jobs awaiting delay as they no longer serve any purpose.
  DELETE FROM job_dependency jd
  USING tmp_dependent_jobs_with_delay tdjwd
  WHERE tdjwd.job_id = jd.job_id;

END
$$ LANGUAGE plpgsql;
