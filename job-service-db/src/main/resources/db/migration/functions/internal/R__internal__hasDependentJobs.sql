--
-- Copyright 2016-2022 Micro Focus or one of its affiliates.
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
 *  Name: internal_has_dependent_jobs
 *
 *  Description:
 *  Checks if there are other jobs that are dependent on the specified job.
 */
CREATE OR REPLACE FUNCTION internal_has_dependent_jobs(
    in_partition_id VARCHAR(40),
    in_job_id VARCHAR(58)
)
RETURNS BOOLEAN
LANGUAGE SQL STABLE
AS $$
    -- Checks if job has any dependency
    SELECT EXISTS(
        SELECT NULL FROM job_dependency
        WHERE partition_id = in_partition_id
            AND dependent_job_id = in_job_id
    );
$$;
