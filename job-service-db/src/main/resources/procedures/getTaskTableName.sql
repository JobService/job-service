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
 *  Name: internal_get_task_table_name
 *
 *  Description:
 *  Subtask structure is stored in dynamically-created tables.  This function builds the table name
 *  used for a given parent task.
 *
 *   - in_short_task_id: identification of the task - see
 *                       com.hpe.caf.services.job.util.JobTaskId#getShortId
 */
CREATE OR REPLACE FUNCTION internal_get_task_table_name(
    in_short_task_id VARCHAR(58)
)
RETURNS VARCHAR(63)
LANGUAGE SQL IMMUTABLE
AS $$
    SELECT 'tsk2_' || in_short_task_id;
$$;
