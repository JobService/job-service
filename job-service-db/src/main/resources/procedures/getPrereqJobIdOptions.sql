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
 *  Name: internal_get_prereq_job_id_options
 *
 *  Description:
 *  Extracts the options from the specified job id with options string.
 */
CREATE OR REPLACE FUNCTION internal_get_prereq_job_id_options(job_id_with_opts VARCHAR(128))
RETURNS TABLE(
    job_id VARCHAR(48),
    options_string VARCHAR(128),
    precreated BOOLEAN,
    unknown_options BOOLEAN
)
LANGUAGE SQL IMMUTABLE
AS $$
    WITH job_id_with_opts_tbl AS
    (
        SELECT SUBSTRING(job_id_with_opts FROM '[^,]*') AS job_id,
               SUBSTRING(job_id_with_opts FROM ',(.*)') AS options_string
    )
    SELECT job_id,
           options_string,
           EXISTS(SELECT NULL FROM internal_get_options(options_string) WHERE option = 'pc'),
           EXISTS(SELECT NULL FROM internal_get_options(options_string) WHERE option NOT IN ('pc'))
    FROM job_id_with_opts_tbl;
$$;
