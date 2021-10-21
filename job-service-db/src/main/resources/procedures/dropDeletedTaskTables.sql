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
 *  Name: drop_deleted_task_tables
 *
 *  Description:
 *  This procedure reads parent task table names from deleted_parent_table_log table and populates delete_log table with names of
 *  parent as well as child task tables to be dropped. After populating the tables, it then reads the table names from delete_log table
 *  and drops them. 
 *  All the above is done through batch commits. The batch is defined by commit_limit variable. Default batch size being 10.
 */
CREATE OR REPLACE PROCEDURE drop_deleted_task_tables()
LANGUAGE plpgsql
AS $$
DECLARE
    selected_table_names VARCHAR;
    selected_parent_table_names VARCHAR;
    commit_limit INTEGER:=10;
    parent_table_log_rec RECORD;
    rec RECORD;

BEGIN
    -- insert table names into delete_log
    selected_parent_table_names :=
                    $q$SELECT table_name FROM deleted_parent_table_log LIMIT $q$ || commit_limit || $q$ FOR UPDATE SKIP LOCKED$q$;
    WHILE EXISTS(SELECT 1 FROM deleted_parent_table_log)
    LOOP
        FOR parent_table_log_rec IN EXECUTE selected_parent_table_names
        LOOP
            CALL internal_populate_delete_log_table(parent_table_log_rec.table_name, 0);
            -- delete the parent table name from parent table.
            DELETE FROM deleted_parent_table_log WHERE table_name = parent_table_log_rec.table_name;
        END LOOP;
        COMMIT;
    END LOOP;

    selected_table_names := $q$SELECT table_name FROM delete_log LIMIT $q$ || commit_limit || $q$ FOR UPDATE SKIP LOCKED$q$;

    WHILE EXISTS (SELECT 1 FROM delete_log)
    LOOP
        FOR rec IN EXECUTE selected_table_names
        LOOP
            EXECUTE 'DROP TABLE IF EXISTS ' || quote_ident(rec.table_name);
            DELETE FROM delete_log WHERE table_name = rec.table_name;
        END LOOP;
        COMMIT;
    END LOOP;
END
$$;
