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
 *  Name: delete_dependent_job
 *
 *  Description:
 *  Deletes dependent jobs related to a specific job
 */
CREATE OR REPLACE FUNCTION delete_dependent_job(
    in_partition_id VARCHAR(40),
    in_job_id VARCHAR(48))
RETURNS void
LANGUAGE plpgsql VOLATILE
AS $$

BEGIN
DELETE FROM job_task_data
    WHERE in_partition_id = partition_id
    AND in_job_id = job_id;
END
$$;
