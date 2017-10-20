--
-- Copyright 2015-2017 Hewlett Packard Enterprise Development LP.
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
 *  Name: report_progress
 *
 *  Description:  Modify status of task and propagate progress state to subsequent parent task and job rows.
 */
DROP FUNCTION IF EXISTS report_progress(varchar(58),job_status);
CREATE FUNCTION report_progress(in_task_id varchar(58), in_status job_status)
  RETURNS TABLE (job_id VARCHAR(48), task_classifier VARCHAR(255), task_api_version INT, task_data BYTEA,
  task_pipe VARCHAR(255), target_pipe VARCHAR(255)) AS $$
#variable_conflict use_column  
DECLARE
  v_job_id VARCHAR(48);
  v_parent VARCHAR(58);
  v_parent_table_name VARCHAR(63);
  v_temp SMALLINT;
  v_is_final_task BOOLEAN = false;
BEGIN

  --  Raise exception if task identifier has not been specified.
  IF in_task_id IS NULL OR in_task_id = '' THEN
    RAISE EXCEPTION 'Task identifier has not been specified';
  END IF;

  --  If dot separator exists then we are dealing with a task, otherwise a job.
  IF position('.' in in_task_id) = 0 THEN
    v_job_id = in_task_id;

    --  Modify job row but need to disallow cancelled jobs from being reactivated.
    IF in_status = 'Active' THEN
      PERFORM 1 FROM job WHERE job.job_id = v_job_id FOR UPDATE;
      UPDATE job
      SET status = in_status
      WHERE job_id = v_job_id
      AND status <> 'Cancelled';
    ELSE
      PERFORM 1 FROM job WHERE job_id = v_job_id FOR UPDATE;
      UPDATE job
      SET status = in_status,
          percentage_complete = CASE WHEN in_status = 'Completed' THEN 100.00 ELSE percentage_complete END
      WHERE job_id = v_job_id;
    END IF;

    IF in_status = 'Completed' THEN
      --  If job is completed, then remove task tables associated with the job.
      PERFORM internal_delete_task_table(v_job_id,false);

      -- Get list of jobs that can now be run.
      RETURN QUERY
      SELECT jtd.job_id, jtd.task_classifier, jtd.task_api_version, jtd.task_data, jtd.task_pipe, jtd.target_pipe
      FROM job_task_data jtd
      INNER JOIN job_dependency jd
        ON jd.job_id = jtd.job_id
      WHERE jd.dependent_job_id = v_job_id
      AND NOT EXISTS (SELECT jd2.job_id
                      FROM job_dependency jd2
                      INNER JOIN job j
                        ON j.job_Id = jd2.dependent_job_id
                      WHERE jd2.job_id = jd.job_Id
                      AND jd2.dependent_job_id <> v_job_id
                      AND j.status <> 'Completed');

      -- Remove corresponding dependency related rows.
      DELETE FROM job_dependency jd WHERE jd.job_id = v_job_id;
      DELETE FROM job_task_data jtd WHERE jtd.job_id = v_job_id;

    END IF;

  ELSE
    --  Extract job id and parent task details from the specified task id.
    v_job_id = substring(in_task_id, 1, position('.' IN in_task_id)-1);
    v_parent = substring(in_task_id, 1, internal_get_last_position(in_task_id, '.')-1);
    v_parent_table_name = 'task_' || v_parent;

    --  Make sure the job actually exists before we register/update the task.
    IF NOT EXISTS (SELECT 1 FROM job WHERE job_id = v_job_id) THEN
      RAISE EXCEPTION 'A job for the specified task does not exist.';
    END IF;

    --  Check if this is the final sub task (i.e. task id end withs *).
    IF substr(in_task_id, length(in_task_id), 1) = '*' THEN
      v_is_final_task = true;
    END IF;

    --  This could be the first progress report against a job task. Make sure the job
    --  status is made Active if currently in state 'Waiting'.
    PERFORM 1 FROM job WHERE job_id = v_job_id AND status = 'Waiting' FOR UPDATE;
    UPDATE job
    SET status = 'Active'
    WHERE job_id = v_job_id AND status = 'Waiting';

    --  Create parent task table if it does not exist.
    IF NOT EXISTS (SELECT 1 FROM pg_class where relname = v_parent_table_name )
    THEN
      PERFORM internal_create_task_table(v_parent_table_name);
    END IF;

    --  Insert row into parent table for the specified task id.
    EXECUTE format('SELECT 1 FROM %1$I WHERE task_id = %2$L FOR UPDATE', v_parent_table_name, in_task_id) INTO v_temp;
    EXECUTE format('
      WITH upsert AS
      (
        UPDATE %1$I SET status = %2$L, percentage_complete = CASE WHEN %2$L = ''Completed'' THEN 100.00 ELSE percentage_complete END WHERE task_id = %3$L RETURNING *
      )
      INSERT INTO %1$I (task_id, create_date, status, percentage_complete, failure_details, is_final)
        SELECT %3$L, now(), %2$L, 0.00, null, %4$L
        WHERE NOT EXISTS (SELECT * FROM upsert)
      ',
      v_parent_table_name, in_status, in_task_id, v_is_final_task);

    --  If status is completed, then rollup task completion status to parent(s).
    IF in_status = 'Completed' THEN
      PERFORM internal_report_task_completion(v_parent_table_name);
    END IF;

  END IF;

END
$$ LANGUAGE plpgsql;