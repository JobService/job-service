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
 *  Name: report_failure_and_delete_dependent_job
 *
 *  Description:
 *  Update the specified task and subsequent parent tasks/job with the failure details,
 *  then delete dependent jobs if the failure reporting succeeds. Both operations are
 *  executed within a single transaction.
 */
CREATE OR REPLACE FUNCTION report_failure_and_delete_dependent_job(
    in_partition_id VARCHAR(40),
    in_job_id VARCHAR(70),
    in_failure_details TEXT
)
RETURNS VOID
LANGUAGE plpgsql
AS $$
BEGIN
    PERFORM report_failure(in_partition_id, in_job_id, in_failure_details);
    PERFORM delete_dependent_job(in_partition_id, in_job_id);
END
$$;
