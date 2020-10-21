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
 *  Name: internal_drop_task_tables2
 *
 *  Description:
 *  Drops all task tables belonging to the specified task and all its subtasks
 *  This is a procedure that commits on bulks whose size is defined by the maxBeforeCommit variable.
 */

CREATE OR REPLACE PROCEDURE internal_drop_task_tables2(
    in_partition_id VARCHAR(40),
    in_task_id VARCHAR(58)
)
LANGUAGE plpgsql
AS $$
DECLARE
    task_table_ident TEXT;
    subtask_suffix TEXT;
    counter integer :=0;
    maxBeforeCommit integer:=1000;

BEGIN
    -- Put together the task table identifier
    task_table_ident = quote_ident(internal_get_task_table_name(in_partition_id, in_task_id));

    -- Check if the table exists
    IF internal_to_regclass(task_table_ident) IS NOT NULL THEN
        -- Drop the referenced subtask tables
        FOR subtask_suffix IN
            EXECUTE $ESC$SELECT '.' || subtask_id || CASE WHEN is_final THEN '*' ELSE '' END AS subtask_suffix FROM $ESC$ || task_table_ident
            LOOP
                CALL internal_drop_task_tables2(in_partition_id, in_task_id || subtask_suffix);

                -- we commit on each maxBeforeCommit
                IF (MOD(counter,maxBeforeCommit)=0)
                THEN
                    COMMIT;
                END IF;
                -- we increment the counter
                counter :=counter+1;
            END LOOP;

        -- Drop the table itself
        EXECUTE 'DROP TABLE ' || task_table_ident;
    END IF;
END
$$;