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
 *  Name: delete_job
 *
 *  Description:  Deletes the job row and corresponding task tables.
 */
CREATE OR REPLACE FUNCTION delete_job(in_job_id VARCHAR(48))
RETURNS VOID AS $$
DECLARE
  v_tables_to_delete text[];
  v_table_name text;
BEGIN
  --  Raise exception if job identifier has not been specified.
  IF in_job_id IS NULL OR in_job_id = '' THEN
    RAISE EXCEPTION 'The job identifier has not been specified' USING ERRCODE = '02000'; -- sqlstate no data;
  END IF;

  --  Raise exception if no matching job identifier has not been found.
  IF NOT EXISTS (SELECT 1 FROM job where job_id = in_job_id) THEN
    RAISE EXCEPTION 'job_id {%} not found', in_job_id USING ERRCODE = 'P0002'; -- sqlstate no_data_found
  END IF;

  --  Identify task tables associated with the specified job.
  EXECUTE 'SELECT ARRAY(SELECT relname FROM pg_class WHERE relname LIKE $1)' INTO v_tables_to_delete  USING 'task_' || in_job_id || '%';

  --  Loop through each task table.
  FOREACH v_table_name IN ARRAY v_tables_to_delete
  LOOP
    --  Drop the table.
    EXECUTE format('DROP TABLE %I', v_table_name);
  END LOOP;

  --  Remove row from the job table.
  DELETE FROM job WHERE job_id =  in_job_id;
END
$$ LANGUAGE plpgsql;