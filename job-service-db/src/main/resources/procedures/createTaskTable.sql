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
 *  Name: internal_create_task_table
 *
 *  Description:  Create a new task table when the first sub-task is reported for a job.
 *                Internal - used in report_progress() and report_failure().
 */
CREATE OR REPLACE FUNCTION internal_create_task_table(in_table_name VARCHAR(63))
RETURNS VOID
LANGUAGE plpgsql
AS $$
DECLARE
    v_status_index_name VARCHAR(63);
    v_is_final_index_name VARCHAR(63);

BEGIN
    -- Raise exception if task table name has not been specified
    IF in_table_name IS NULL OR in_table_name = '' THEN
        RAISE EXCEPTION 'Task table name has not been specified';
    END IF;

    -- Create a new task table
    EXECUTE format($FORMAT_STR$
        CREATE TABLE IF NOT EXISTS %I
        (
            task_id VARCHAR(58) NOT NULL,
            create_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
            status job_status NOT NULL DEFAULT 'Waiting'::job_status,
            percentage_complete DOUBLE PRECISION NOT NULL DEFAULT 0.00,
            failure_details TEXT,
            is_final BOOLEAN NOT NULL DEFAULT FALSE,
            CONSTRAINT %I PRIMARY KEY (task_id)
        )
    $FORMAT_STR$, in_table_name, 'pk_' || in_table_name);

    -- Create indexes
    SELECT 'idx_' || MD5(random()::TEXT) || '_status' INTO v_status_index_name;
    EXECUTE format('CREATE INDEX %1$I ON %2$I (%3$I)', v_status_index_name, in_table_name, 'status');

    SELECT 'idx_' || MD5(random()::TEXT) || '_is_final' INTO v_is_final_index_name;
    EXECUTE format('CREATE INDEX %1$I ON %2$I (%3$I)', v_is_final_index_name, in_table_name, 'is_final');
END
$$;
