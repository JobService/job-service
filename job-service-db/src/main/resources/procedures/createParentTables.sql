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

CREATE OR REPLACE FUNCTION internal_create_parent_tables(in_job_id VARCHAR(48))
RETURNS VOID
LANGUAGE plpgsql
AS $$
DECLARE
    v_job_id VARCHAR(48);
    v_parent VARCHAR(58);
    v_parent_table_name VARCHAR(63);
    v_root_parent_table_name VARCHAR(63);

BEGIN
    v_job_id = in_job_id;
    LOOP EXIT WHEN internal_is_job_id(v_job_id);
        v_parent = substring(v_job_id, 1, internal_get_last_position(v_job_id, '.') - 1);
        v_parent_table_name = 'task_' || v_parent;
        IF NOT EXISTS (SELECT 1 FROM pg_class where relname = v_parent_table_name )
        THEN
            PERFORM internal_create_task_table(v_parent_table_name);
        END IF;
        v_job_id = v_parent;
    END LOOP;

    v_root_parent_table_name = 'task_' || v_job_id;

    IF NOT EXISTS (SELECT 1 FROM pg_class where relname = v_root_parent_table_name)
    THEN
        PERFORM internal_create_task_table(v_root_parent_table_name);
    END IF;
END
$$;
