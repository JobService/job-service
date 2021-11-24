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
 *  Name: internal_is_final_task
 *
 *  Description:
 *  Checks if the specified task id is the final one out of a group of subtasks
 */
DROP FUNCTION IF EXISTS internal_is_final_task(in_task_id VARCHAR(58));
CREATE OR REPLACE FUNCTION internal_is_final_task(in_task_id VARCHAR(70))
RETURNS BOOLEAN
LANGUAGE SQL IMMUTABLE
AS $$
SELECT SUBSTRING(in_task_id FROM '\..*\*$') IS NOT NULL;
$$;
