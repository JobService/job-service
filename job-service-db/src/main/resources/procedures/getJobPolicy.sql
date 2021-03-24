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
 *  Name: get_job_policy
 *
 *  Description:
 *  Returns expiration_policy for a given job.
 */
CREATE OR REPLACE FUNCTION get_job_policy(
    in_partition_id VARCHAR(40),
    in_job_id VARCHAR(58)
)
RETURNS setof JOB_POLICY
LANGUAGE plpgsql STABLE
AS
$$
BEGIN
    RETURN QUERY SELECT *
                 FROM job_expiration_policy
                 WHERE partition_id = in_partition_id
                   AND job_id = in_job_id
                 ORDER BY job_status;
END
$$
;
