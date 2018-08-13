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
 *  Name: report_progress
 *
 *  Description:  Modify status of task and propagate progress state to subsequent parent task and job rows.
 */
DROP FUNCTION IF EXISTS report_progress(in_task_id varchar(58), in_status job_status);
CREATE FUNCTION report_progress(in_task_id varchar(58), in_status job_status)
  RETURNS TABLE (job_id VARCHAR(48), task_classifier VARCHAR(255), task_api_version INT, task_data BYTEA,
  task_pipe VARCHAR(255), target_pipe VARCHAR(255)) AS $$
#variable_conflict use_column  
DECLARE
  v_job_id VARCHAR(48);
  v_parent VARCHAR(58);
  v_parent_table_name VARCHAR(63);
  v_topmost_parent_table_name VARCHAR(63);
  v_temp SMALLINT;
  v_is_final_task BOOLEAN = false;
BEGIN
    -- UNUSED
    RAISE EXCEPTION 'Procedure report_progress() no longer supported';
END
$$ LANGUAGE plpgsql;
