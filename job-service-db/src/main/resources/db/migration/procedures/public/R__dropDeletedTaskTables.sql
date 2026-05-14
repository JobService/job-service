--
-- Copyright 2016-2022 Micro Focus or one of its affiliates.
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
 *
 *  POPULATE phase (Phase 1) uses immediate failure-to-dead-letter: if populate fails (lock timeout, table corruption, etc.),
 *  the entry is immediately evicted to table_cleanup_failed for operator inspection and remediation. Retrying Phase 1 is unsafe
 *  because the recursive procedure with intermediate commits may have partially inserted rows into delete_log already; retrying
 *  would cause duplicates or silent conflicts. Instead, operators investigate the root cause, fix it, and re-run the procedure.
 *
 *  DROP loop (Phase 2) includes retry-and-dead-letter behaviour: each DROP is attempted up to max_retries times (default 3).
 *  On failure the retry_count, last_error, and last_attempted_at columns of delete_log are updated. Once a row reaches
 *  max_retries without a successful DROP it is evicted to the dead-letter table table_cleanup_failed (and removed from
 *  delete_log) before the batch COMMIT so that it does not block subsequent processing.
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
    max_retries CONSTANT INTEGER := 3;
    drop_error TEXT;
    populate_error TEXT;

BEGIN
    -- if populate fails, entry is immediately evicted
    selected_parent_table_names :=
                    $q$SELECT table_name FROM deleted_parent_table_log LIMIT $q$ || commit_limit || $q$ FOR UPDATE SKIP LOCKED$q$;
    WHILE EXISTS(SELECT 1 FROM deleted_parent_table_log)
    LOOP
        FOR parent_table_log_rec IN EXECUTE selected_parent_table_names
        LOOP
            BEGIN
                CALL internal_populate_delete_log_table(parent_table_log_rec.table_name, 0);
                -- Successfully populated — delete the parent table entry.
                DELETE FROM deleted_parent_table_log WHERE table_name = parent_table_log_rec.table_name;
            EXCEPTION WHEN OTHERS THEN
                -- Populate failed — immediately evict to dead-letter for operator investigation.
                GET STACKED DIAGNOSTICS populate_error = MESSAGE_TEXT;
                INSERT INTO table_cleanup_failed (table_name, last_error, last_attempted_at)
                VALUES (parent_table_log_rec.table_name, 'populate failed: ' || populate_error, now())
                ON CONFLICT (table_name) DO UPDATE
                    SET last_error        = EXCLUDED.last_error,
                        last_attempted_at = EXCLUDED.last_attempted_at;
                -- Delete the parent entry so future calls don't re-attempt.
                DELETE FROM deleted_parent_table_log WHERE table_name = parent_table_log_rec.table_name;
            END;
        END LOOP;
        COMMIT;
    END LOOP;

    selected_table_names := $q$SELECT table_name FROM delete_log WHERE retry_count < $q$ || max_retries || $q$ LIMIT $q$ || commit_limit || $q$ FOR UPDATE SKIP LOCKED$q$;

    WHILE EXISTS (SELECT 1 FROM delete_log WHERE retry_count < max_retries)
    LOOP
        FOR rec IN EXECUTE selected_table_names
        LOOP
            BEGIN
                EXECUTE 'DROP TABLE IF EXISTS ' || quote_ident(rec.table_name);
                DELETE FROM delete_log WHERE table_name = rec.table_name;
            EXCEPTION WHEN OTHERS THEN
                GET STACKED DIAGNOSTICS drop_error = MESSAGE_TEXT;
                UPDATE delete_log
                SET    retry_count       = retry_count + 1,
                       last_error        = drop_error,
                       last_attempted_at = now()
                WHERE  table_name = rec.table_name;
            END;
        END LOOP;
        -- Move exhausted entries to dead-letter table
        INSERT INTO table_cleanup_failed (table_name, last_error, last_attempted_at)
        SELECT table_name, last_error, last_attempted_at
        FROM   delete_log
        WHERE  retry_count >= max_retries
        ON CONFLICT (table_name) DO UPDATE
            SET last_error        = EXCLUDED.last_error,
                last_attempted_at = EXCLUDED.last_attempted_at;
        DELETE FROM delete_log WHERE retry_count >= max_retries;
        COMMIT;
    END LOOP;
END
$$;
