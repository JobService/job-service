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
 *  Name: internal_upsert_into_task_table
 *
 *  Description:
 *  Inserts or updates the specified task table row.
 */
CREATE OR REPLACE FUNCTION internal_upsert_into_task_table(
    in_task_table_name VARCHAR(63),
    in_task_id VARCHAR(70),
    in_status job_status,
    in_percentage_complete DOUBLE PRECISION,
    in_failure_details TEXT
)
RETURNS VOID
LANGUAGE plpgsql
AS $$
BEGIN
    -- ASSERT support was only introduced in PostgresSQL 9.5
    --ASSERT in_task_table_name = 'task_' || internal_get_parent_task_id(in_task_id),
    --       'Invalid arguments passed to internal_upsert_into_task_table';

    EXECUTE format($FORMAT_STR$
        WITH upsert AS
        (
            UPDATE %1$I
            SET status = $3,
                percentage_complete = $4,
                failure_details = $5
            WHERE subtask_id = $2
            RETURNING NULL
        )
        INSERT INTO %1$I(subtask_id, create_date, status, percentage_complete, failure_details, is_final)
        SELECT $2, now() AT TIME ZONE 'UTC', $3, $4, $5, internal_is_final_task($1)
        WHERE NOT EXISTS(SELECT * FROM upsert);
    $FORMAT_STR$, in_task_table_name)
    USING in_task_id, internal_get_subtask_id(in_task_id), in_status, in_percentage_complete, in_failure_details;
END
$$;
