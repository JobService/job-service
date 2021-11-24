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
 *  Name: internal_get_options
 *
 *  Description:
 *  Returns the list of options extracted from the specified options string.
 */
CREATE OR REPLACE FUNCTION internal_get_options(in_options VARCHAR(128))
RETURNS TABLE(
    option VARCHAR
)
LANGUAGE SQL IMMUTABLE
AS $$
SELECT DISTINCT * FROM regexp_split_to_table(in_options, ',') option
WHERE option <> '';
$$;
