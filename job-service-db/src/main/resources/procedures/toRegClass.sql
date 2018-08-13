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
 *  Name: internal_to_regclass
 *
 *  Description:  Returns the object identifier of the named relation.
 *                Internal - used in internal_create_task_table().
 */
CREATE OR REPLACE FUNCTION internal_to_regclass(rel_name varchar(63))
RETURNS regclass AS $$
BEGIN

    -- Add backwards compatibility support for to_regclass argument type change introduced in Postgres 9.6.
    IF current_setting('server_version_num')::integer < 90600 THEN
        RETURN to_regclass(rel_name::cstring);
    ELSE
        RETURN to_regclass(rel_name::text);
    END IF;

END
$$ LANGUAGE plpgsql;
