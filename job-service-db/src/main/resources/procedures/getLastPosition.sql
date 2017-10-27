--
-- Copyright 2015-2017 Hewlett Packard Enterprise Development LP.
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
 *  Name: internal_get_last_position
 *
 *  Description:  Returns the location of the last char in the specified text.
 */
CREATE OR REPLACE FUNCTION internal_get_last_position(text, char)
RETURNS INTEGER
LANGUAGE SQL AS $$
SELECT LENGTH($1) - POSITION($2 IN REVERSE($1)) + 1;
$$;