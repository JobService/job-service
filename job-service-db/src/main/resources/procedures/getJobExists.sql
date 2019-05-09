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
 *  Name: get_job_exists
 *
 *  Description:
 *  Returns a record indicating whether the job exists with a specific value by comparing the hash.
 */
CREATE OR REPLACE FUNCTION get_job_exists(
    in_partition VARCHAR(40),
    in_job_id VARCHAR(48),
    in_job_hash INT
)
RETURNS TABLE(
    job_exists BOOLEAN
)
LANGUAGE plpgsql STABLE
AS $$
BEGIN
    RETURN QUERY
    SELECT EXISTS(
        SELECT 1 FROM job
        WHERE job.partition = in_partition
            AND job.job_id = in_job_id
            AND job.job_hash = in_job_hash
    );
END
$$;
