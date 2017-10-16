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
 *  Name: internal_create_task_table
 *
 *  Description:  Create a new task table when the first sub-task is reported for a job.
 *                Internal - used in report_progress().
 */
CREATE OR REPLACE FUNCTION internal_create_task_table(in_table_name varchar(63))
RETURNS VOID AS $$
DECLARE
  v_index_name VARCHAR(63);
BEGIN

  --  Raise exception if task table name has not been specified.
  IF in_table_name IS NULL OR in_table_name = '' THEN
    RAISE EXCEPTION 'Task table name has not been specified';
  END IF;

  --  Create a new task table.
  EXECUTE format('
  CREATE TABLE IF NOT EXISTS %I
  (
    task_id varchar(58) NOT NULL,
    create_date timestamp without time zone NOT NULL,
    status job_status NOT NULL DEFAULT ''Waiting''::job_status,
    percentage_complete double precision NOT NULL DEFAULT 0.00,
    failure_details text,
    is_final boolean NOT NULL DEFAULT false,
    CONSTRAINT %I PRIMARY KEY (task_id)
  )', in_table_name, 'pk_' || in_table_name);

  -- Create indexes.
  v_index_name = 'idx_' || in_table_name || '_s';
  IF (SELECT to_regclass(format('%I', v_index_name)::cstring)) IS NULL THEN
    EXECUTE format('CREATE INDEX %1$I ON %2$I (%3$I)',v_index_name, in_table_name, 'status');
  END IF;

  v_index_name = 'idx_' || in_table_name || '_if';
  IF (SELECT to_regclass(format('%I', v_index_name)::cstring)) IS NULL THEN
    EXECUTE format('CREATE INDEX %1$I ON %2$I (%3$I)',v_index_name, in_table_name, 'is_final');
  END IF;

END
$$ LANGUAGE plpgsql;
