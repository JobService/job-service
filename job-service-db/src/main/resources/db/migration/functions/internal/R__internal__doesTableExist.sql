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
 *  Name: internal_does_table_exist
 *
 *  Description:
 *  Checks if there is a table with the specified name
 */
CREATE OR REPLACE FUNCTION internal_does_table_exist(in_table_name VARCHAR(63))
RETURNS BOOLEAN
LANGUAGE SQL STABLE
AS $$
SELECT internal_to_regclass(quote_ident(in_table_name)) IS NOT NULL;
$$;
