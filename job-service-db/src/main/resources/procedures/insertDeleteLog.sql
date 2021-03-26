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
 *  Name: internal_drop_task_tables
 *
 *  Description:
 *  Drops all task tables belonging to the specified task and all its subtasks
 */
DROP FUNCTION IF EXISTS insert_delete_log(
    task_table_name VARCHAR
);

CREATE OR REPLACE FUNCTION insert_delete_log(
    task_table_name VARCHAR
)
RETURNS VOID
LANGUAGE plpgsql
AS $$
DECLARE
BEGIN
        INSERT INTO public.delete_log VALUES ( task_table_name );
END
$$;