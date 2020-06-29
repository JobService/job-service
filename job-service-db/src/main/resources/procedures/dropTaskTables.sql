--
-- Copyright 2016-2020 Micro Focus or one of its affiliates.
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
 *  Name: internal_drop_task_tables
 *
 *  Description:
 *  Drops all task tables belonging to the specified task and all its subtasks
 *
 *   - in_short_task_id: identification of the task - see
 *                       com.hpe.caf.services.job.util.JobTaskId#getShortId
 */
DROP FUNCTION IF EXISTS internal_drop_task_tables(
    in_short_task_id VARCHAR(58)
);
CREATE OR REPLACE FUNCTION internal_drop_task_tables(
    in_task_id VARCHAR(63)
)
RETURNS VOID
LANGUAGE plpgsql
AS $$
DECLARE
    task_table_ident TEXT;
    subtask_suffix TEXT;
    v_job_id VARCHAR(63);

BEGIN
    v_job_id = SUBSTRING(in_task_id FROM position(':' IN in_task_id) + 1);
    -- Put together the task table identifier
    task_table_ident = quote_ident(internal_get_identity_based_task_table_name(v_job_id));

    -- Check if the table exists
    IF internal_to_regclass(task_table_ident) IS NOT NULL THEN
        -- Drop the referenced subtask tables
        FOR subtask_suffix IN
        EXECUTE $ESC$SELECT '.' || subtask_id || CASE WHEN is_final THEN '*' ELSE '' END AS subtask_suffix FROM $ESC$ || task_table_ident
        LOOP
            PERFORM internal_drop_task_tables(in_task_id || subtask_suffix);
        END LOOP;

        -- Drop the table itself
        EXECUTE 'DROP TABLE ' || task_table_ident;
    END IF;
END
$$;
