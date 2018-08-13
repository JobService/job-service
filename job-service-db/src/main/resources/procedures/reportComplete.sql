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
 *  Name: report_complete
 *
 *  Description: Marks the specified task complete.
 */
DROP FUNCTION IF EXISTS report_complete(in_task_id varchar(58));
CREATE FUNCTION report_complete(in_task_id varchar(58))
    RETURNS TABLE(job_id VARCHAR(48),
                  task_classifier VARCHAR(255),
                  task_api_version INT,
                  task_data BYTEA,
                  task_pipe VARCHAR(255),
                  target_pipe VARCHAR(255)) AS $$
#variable_conflict use_column
BEGIN
    RETURN QUERY
    SELECT * FROM report_progress(in_task_id, 'Completed'::job_status);
END
$$ LANGUAGE plpgsql;
