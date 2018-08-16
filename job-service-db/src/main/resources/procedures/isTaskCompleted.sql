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
 *  Name: internal_is_task_completed
 *
 *  Description:
 *  Checks if the specified task has already been marked complete.
 */
CREATE OR REPLACE FUNCTION internal_is_task_completed(
    in_task_id VARCHAR(58)
)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
DECLARE
    v_parent_task_id VARCHAR(58);
    v_parent_task_table VARCHAR(63);
    v_is_task_completed BOOLEAN;

BEGIN
    -- Get the parent task id
    v_parent_task_id = internal_get_parent_task_id(in_task_id);

    -- Check if we are dealing with the top level job or a subtask
    IF v_parent_task_id IS NULL THEN

        -- Check the status in the job table
        -- If the job isn't present then throw an error
        SELECT status = 'Completed'
        INTO STRICT v_is_task_completed
        FROM job
        WHERE job_id = in_task_id;

    -- Check if the parent task has completed
    ELSIF internal_is_task_completed(v_parent_task_id) THEN

        -- Since the parent task has completed then we can say that this task has
        v_is_task_completed = TRUE;

    ELSE

        -- Put together the parent task table name
        v_parent_task_table = 'task_' || v_parent_task_id;

        -- Check if the parent task table exists
        IF internal_does_table_exist(v_parent_task_table) THEN

            -- Lookup the status in the parent task table
            EXECUTE format($FORMAT_STR$
                SELECT COALESCE((
                    SELECT status = 'Completed'
                    FROM %1$I
                    WHERE subtask_id = $1
                ), FALSE)
            $FORMAT_STR$, v_parent_task_table)
            USING internal_get_subtask_id(in_task_id)
            INTO STRICT v_is_task_completed;

        ELSE

            -- Since the parent task table doesn't exist, and we established earlier that it has not completed,
            -- we can say that this task has not completed.
            v_is_task_completed = FALSE;

        END IF;

    END IF;

    -- Return whether the task has been completed
    RETURN v_is_task_completed;

END
$$;
