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
 *  Name: internal_delete_task_table
 *
 *  Description:  Deletes the task tables belonging to the specified job.
 *                Can be used to only drop tables if all sub tasks reported for the job have been completed (i.e. NOT in_ignore_status).
 *                Internal - used in report_progress() and internal_report_task_completion().
 */
CREATE OR REPLACE FUNCTION internal_delete_task_table(
    in_job_id VARCHAR(48),
    in_ignore_status BOOLEAN
)
RETURNS VOID
LANGUAGE plpgsql
AS $$
DECLARE
    v_tables_to_delete TEXT[];
    v_table_name TEXT;
    v_uncompleted_rows_found SMALLINT;

BEGIN
    -- Raise exception if job identifier has not been specified
    IF in_job_id IS NULL OR in_job_id = '' THEN
        RAISE EXCEPTION 'The job identifier has not been specified';
    END IF;

    -- Identify task tables associated with the specified job
    EXECUTE 'SELECT ARRAY(SELECT relname FROM pg_class WHERE relname LIKE $1 AND relkind = ''r'')'
    INTO v_tables_to_delete
    USING 'task_' || in_job_id || '%';

    -- Loop through each task table
    FOREACH v_table_name IN ARRAY v_tables_to_delete
    LOOP
        -- We may be asked to delete the task table only if all sub tasks reported for the job have been completed
        IF NOT in_ignore_status THEN
            -- Make sure all rows are complete in the table, otherwise raise an exception
            EXECUTE format('SELECT 1 FROM %I WHERE status <> ''Completed''', v_table_name) INTO v_uncompleted_rows_found;

            IF v_uncompleted_rows_found IS NOT NULL THEN
                RAISE EXCEPTION 'Task table deletion abandoned due to uncompleted task rows in table {%}', v_table_name;
                EXIT;
            END IF;
        END IF;

        -- Drop the table
        EXECUTE format('DROP TABLE %I', v_table_name);
    END LOOP;
END
$$;
