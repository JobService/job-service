--
-- Copyright 2016-2023 Open Text.
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
 *  Name: internal_get_subtask_id
 *
 *  Description:
 *  Returns the subtask portion of the specified task id, or NULL if this is the id of the top-level task (i.e. the job)
 */
CREATE OR REPLACE FUNCTION internal_get_subtask_id(in_task_id VARCHAR(70))
RETURNS INT
LANGUAGE SQL IMMUTABLE
AS $$
SELECT CAST(SUBSTRING(in_task_id FROM '\.(\d*)\*?$') AS INT);
$$;
