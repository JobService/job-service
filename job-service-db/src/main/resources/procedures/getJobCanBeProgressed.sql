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
 *  Name: get_job_can_be_progressed
 *
 *  Description:
 *  Returns a record indicating whether the job can be progressed.
 */
CREATE OR REPLACE FUNCTION get_job_can_be_progressed(
    in_partition_id VARCHAR(40),
    in_job_id VARCHAR(48)
)
RETURNS TABLE(
    can_be_progressed BOOLEAN
)
LANGUAGE plpgsql STABLE
AS $$
BEGIN
    RETURN QUERY
    SELECT NOT EXISTS(
        SELECT 1 FROM job_task_data as jtd
        WHERE jtd.partition_id = in_partition_id
            AND jtd.job_id = in_job_id
    );
END
$$;
