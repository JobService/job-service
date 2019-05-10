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
 *  Name: internal_get_task_table_name
 *
 *  Description:
 *  Subtask structure is stored in dynamically-created tables.  This function builds the table name
 *  used for a given parent task.
 *
 * Requires the `pgcrypto` module.
 */
CREATE OR REPLACE FUNCTION internal_get_task_table_name(
    in_partition_id VARCHAR(40),
    in_task_id VARCHAR(58)
)
RETURNS VARCHAR(51)
LANGUAGE SQL IMMUTABLE
AS $$
    SELECT 'taskv2_' || encode(digest(in_partition_id || ':' || in_task_id, 'sha256'), 'base64');
$$;
