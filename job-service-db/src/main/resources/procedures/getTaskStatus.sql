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
 *  Name: internal_get_task_status
 *
 *  Description:
 *  Returns the overall status and percentage complete for the task from the subtask table
 */
CREATE OR REPLACE FUNCTION internal_get_task_status(
    in_task_table_name VARCHAR(63)
)
RETURNS TABLE(
    status job_status,
    percentage_complete DOUBLE PRECISION,
    failure_details TEXT
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_subtask_count INT;
    v_completed_task_count INT;

BEGIN
    -- Get the total number of subtasks
    v_subtask_count = internal_get_subtask_count(in_task_table_name);

    -- Get the number of completed subtasks
    EXECUTE format($FORMAT_STR$
        SELECT COUNT(*) FROM %1$I
        WHERE status = 'Completed';
    $FORMAT_STR$, in_task_table_name)
    INTO STRICT v_completed_task_count;

    -- Return the overall status
    IF v_subtask_count = v_completed_task_count THEN
        -- All the subtasks are completed so just return this
        RETURN QUERY SELECT 'Completed'::job_status, CAST(100.00 AS DOUBLE PRECISION), CAST(NULL AS TEXT);
    ELSE
        -- Add an extra 'Active' row in to represent missing rows
        -- (arguably we should only do this if some rows are already complete)
        RETURN QUERY EXECUTE format($FORMAT_STR$
            SELECT status, percentage_complete, failure_details FROM
            (
                SELECT status,
                CASE status
                    WHEN 'Failed' THEN 1
                    WHEN 'Cancelled' THEN 2
                    WHEN 'Paused' THEN 3
                    WHEN 'Active' THEN 4
                    WHEN 'Waiting' THEN 5
                    WHEN 'Completed' THEN 6
                END AS importance
                FROM
                (
                    SELECT status
                    FROM %1$I
                    UNION ALL
                    SELECT 'Active'
                ) tbl
                ORDER BY importance
                LIMIT 1
            ) tbl1
            CROSS JOIN
            (
                SELECT LEAST(COALESCE(SUM(percentage_complete) / COALESCE($1, MAX(subtask_id) + 1), 0.00), 99.7) AS percentage_complete
                FROM %1$I
            ) tbl2
            CROSS JOIN
            (
                SELECT string_agg(failure_details, E'\n') AS failure_details
                FROM %1$I
                WHERE status = 'Failed'
            ) tbl3
        $FORMAT_STR$, in_task_table_name)
        USING v_subtask_count;
    END IF;
END
$$;
