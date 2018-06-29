--
-- Copyright 2015-2017 EntIT Software LLC, a Micro Focus company.
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
 *  Name: report_failure
 *
 *  Description:  Update the specified task and subsequent parent tasks/job with the failure details.
 */
CREATE OR REPLACE FUNCTION report_failure(in_task_id varchar(58), in_failure_details TEXT)
RETURNS VOID AS $$
DECLARE
  v_job_id VARCHAR(48);
  v_parent VARCHAR(58);
  v_parent_table_name VARCHAR(63);
  v_temp SMALLINT;
BEGIN

  --  Raise exception if task identifier has not been specified.
  IF in_task_id IS NULL OR in_task_id = '' THEN
    RAISE EXCEPTION 'Task identifier has not been specified';
  END IF;

  --  Raise exception if failure details have not been specified.
  IF in_failure_details IS NULL OR in_failure_details = '' THEN
    RAISE EXCEPTION 'Failure details have not been specified';
  END IF;

  --  If dot separator exists then we are dealing with a task, otherwise a job.
  IF position('.' in in_task_id) = 0 THEN
    v_job_id = in_task_id;

    --  Modify job row.
    PERFORM 1 FROM job WHERE job_id = v_job_id FOR UPDATE;
    UPDATE job
    SET status = 'Failed',
        failure_details = concat(failure_details, in_failure_details || chr(10))
    WHERE job_id = v_job_id;

  ELSE
    --  Extract job id and parent task details from the specified task id.
    v_job_id = substring(in_task_id, 1, position('.' IN in_task_id)-1);
    v_parent = substring(in_task_id, 1, internal_get_last_position(in_task_id, '.')-1);
    v_parent_table_name = 'task_' || v_parent;

    --  Create parent task table if it does not exist.
    IF NOT EXISTS (SELECT 1 FROM pg_class where relname = v_parent_table_name )
    THEN
      PERFORM internal_create_task_table(v_parent_table_name);
    END IF;

    --  Insert row into parent table for the specified task id.
    EXECUTE format('SELECT 1 FROM %I WHERE task_id = %L FOR UPDATE', v_parent_table_name, in_task_id) INTO v_temp;
    EXECUTE format('
      UPDATE %I SET status = ''Failed'', failure_details = concat(failure_details, %L || chr(10)) WHERE task_id = %L;
      ',
      v_parent_table_name, in_failure_details, in_task_id);

    --  Propagate failure details up to subsequent parent(s).
    PERFORM internal_report_task_failure(v_parent_table_name, in_failure_details);

  END IF;

END
$$ LANGUAGE plpgsql;