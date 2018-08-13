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
 *  Name: internal_report_task_completion
 *
 *  Description:  Recursively propagate progress status to parent tasks and job.
 *                Internal - used in report_progress().
 */
DROP FUNCTION IF EXISTS internal_report_task_completion(in_task_table_name varchar(63));
CREATE OR REPLACE FUNCTION internal_report_task_completion(in_task_table_name varchar(63))
  RETURNS TABLE (job_id VARCHAR(48), task_classifier VARCHAR(255), task_api_version INT, task_data BYTEA,
  task_pipe VARCHAR(255), target_pipe VARCHAR(255)) AS $$
#variable_conflict use_column  
DECLARE
  v_parent_table_name VARCHAR(63);
  v_percentage_completed DOUBLE PRECISION;
  v_task_id VARCHAR(58);
  v_job_id VARCHAR(48);
  v_temp SMALLINT;
  v_final_task_id varchar(58);
  v_is_final_task BOOLEAN = false;
  v_sub_task_count_starting_position INT;
  v_sub_tasks_to_expect INT;
BEGIN

  --  Raise exception if task identifier has not been specified.
  IF in_task_table_name IS NULL OR in_task_table_name = '' THEN
    RAISE EXCEPTION 'Task table name has not been specified';
  END IF;

  --  Identify percentage of rows in current table with status Completed.
  --  Percentage will need adjusted though if final sub-task has not yet arrived.
  EXECUTE format('SELECT task_id FROM %1$I WHERE is_final = true', in_task_table_name) INTO v_final_task_id;
  IF v_final_task_id IS NOT NULL THEN
    --  The final sub-task has arrived but before we calculate percentage of sub-tasks complete,
    --  we need to identify how many sub-tasks have arrived in total given we know how many to expect.
    SELECT internal_get_last_position(v_final_task_id, '.') + 1 INTO v_sub_task_count_starting_position;
    v_sub_tasks_to_expect = CAST(substring(v_final_task_id, v_sub_task_count_starting_position,
            length(v_final_task_id) - v_sub_task_count_starting_position) AS INT);

    -- Base percentage completed on the number of sub tasks expected.
    EXECUTE format('
    SELECT round(((select count(task_id) from %1$I WHERE status = ''Completed'') * 100)::numeric / (%2$s), 2) AS completed_percentage',
                   in_task_table_name, v_sub_tasks_to_expect) INTO v_percentage_completed;
  ELSE
    --  Final sub-task has not yet arrived. Adjust percentage to allow for one more entry to arrive.
    EXECUTE format('
    SELECT round(((select count(task_id) from %1$I WHERE status = ''Completed'') * 100)::numeric / (select count(*)+1 from %1$I), 2) AS completed_percentage',
                   in_task_table_name) INTO v_percentage_completed;
  END IF;

  --  Identify parent table to target from task table name.
  --  If dot separator does not exist though in the specified task table name then we are dealing with the job table.
  IF position('.' in in_task_table_name) = 0 THEN
    --  Extract job id from task table name (i.e. strip task_ prefix).
    v_job_id = substring(in_task_table_name from 6);

    PERFORM 1 FROM job WHERE job_id = v_job_id FOR UPDATE;
    UPDATE job
    SET percentage_complete = v_percentage_completed,
        status = CASE WHEN v_percentage_completed = 100.00 THEN 'Completed'
                 ELSE status
                 END
    WHERE job_id = v_job_id;

    --  If job has completed, then remove task tables and return any jobs that can now be run.
    IF v_percentage_completed = 100.00 THEN
      --  Remove task tables associated with the job.
      PERFORM internal_delete_task_table(v_job_id, false);

      -- Get a list of jobs that can run immediately and update the eligibility run date for others.
      RETURN QUERY
      SELECT * FROM internal_process_dependent_jobs(v_job_id);
  END IF;

  ELSE
    v_parent_table_name = substring(in_task_table_name, 1, internal_get_last_position(in_task_table_name, '.')-1);
    --  Check if this is the final sub task (i.e. task id end with *).
    IF substr(v_parent_table_name, length(v_parent_table_name), 1) = '*' THEN
      v_is_final_task = true;
    END IF;
    --  Identify task id from task table name (i.e. strip task_ prefix) to determine which row in the parent table to target.
    v_task_id = substring(in_task_table_name from 6);

    --  Modify parent target table and update it's status and % completed.
    EXECUTE format('SELECT 1 FROM %1$I WHERE task_id = %2$L FOR UPDATE', v_parent_table_name, v_task_id) INTO v_temp;
    EXECUTE format('
      WITH upsert AS
      (
        UPDATE %1$I SET status =
            CASE
                WHEN status = ''Completed'' THEN ''Completed''::job_status
                WHEN status = ''Failed'' THEN ''Failed''::job_status
                ELSE %2$L
            END,
        percentage_complete =
            CASE
                WHEN (%2$L = ''Completed'') OR (status = ''Completed'') THEN 100.00
                ELSE percentage_complete
            END
        WHERE task_id = %4$L RETURNING *
      )
      INSERT INTO %1$I (task_id, create_date, status, percentage_complete, failure_details, is_final)
        SELECT %4$L, now() AT TIME ZONE ''UTC'', %2$L, %3$L, null, %5$L
        WHERE NOT EXISTS (SELECT * FROM upsert)
      ',
      v_parent_table_name, 'Completed', v_percentage_completed, v_task_id, v_is_final_task);

    --  If all the rows in the current task table are complete, then delete it.
    IF v_percentage_completed = 100.00 THEN
      --  Drop the table.
      EXECUTE format('DROP TABLE %I', in_task_table_name);
    END IF;

    -- Recursively call the same function for the specified v_parent_table_name
    RETURN QUERY
    SELECT * FROM internal_report_task_completion(v_parent_table_name);
  END IF;

END
$$ LANGUAGE plpgsql;