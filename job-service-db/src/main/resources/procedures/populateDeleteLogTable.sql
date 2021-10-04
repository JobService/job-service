--
-- Copyright 2016-2021 Micro Focus or one of its affiliates.
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
 *  Name: populate_delete_log_table
 *
 *  Description:
 *  This procedure populates deleted_log table with the names of all task tables that are to be dropped.
 */

CREATE OR REPLACE PROCEDURE populate_delete_log_table(
    in_task_id VARCHAR(70),
    query_count INTEGER DEFAULT 0
)
    LANGUAGE plpgsql
AS
$$
DECLARE
    task_table_ident TEXT;
    subtask_suffix   TEXT;
    commit_limit     INTEGER := 10;

BEGIN
    task_table_ident = quote_ident(in_task_id);

    -- Check if the table exists
    IF internal_to_regclass(task_table_ident) IS NOT NULL THEN
        FOR subtask_suffix IN
            EXECUTE $ESC$SELECT '.' || subtask_id || CASE WHEN is_final THEN '*' ELSE '' END AS subtask_suffix FROM $ESC$ ||
                    task_table_ident
            LOOP
                query_count := query_count + 1;
                IF query_count >= commit_limit THEN
                    COMMIT;
                    query_count = 0;
                END IF;
                CALL populate_delete_log_table(in_task_id || subtask_suffix, query_count);
            END LOOP;
        -- Insert table name to be dropped 
        PERFORM internal_insert_delete_log(in_task_id);
    END IF;
END
$$;
