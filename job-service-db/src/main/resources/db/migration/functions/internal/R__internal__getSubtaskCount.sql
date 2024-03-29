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
 *  Name: internal_get_subtask_count
 *
 *  Description:
 *  Returns the number of subtasks that should be or will be in the specified task table, or NULL if it is not yet known.
 */
CREATE OR REPLACE FUNCTION internal_get_subtask_count(
    in_task_table_name VARCHAR(63)
)
RETURNS INT
LANGUAGE plpgsql STABLE
AS $$
DECLARE
    subtask_count INT;

BEGIN
    EXECUTE format($FORMAT_STR$
        SELECT(
            SELECT subtask_id
            FROM %1$I
            WHERE is_final
        )
    $FORMAT_STR$, in_task_table_name)
    INTO STRICT subtask_count;

    RETURN subtask_count;
END
$$;
