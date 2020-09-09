--
-- Copyright 2016-2020 Micro Focus or one of its affiliates.
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
 *  Name: internal_update_job_progress
 *
 *  Description:
 *  Updates the percentage_complete in the job table with data from subtask_report table.
 */
CREATE FUNCTION internal_update_job_progress(in_partition_id VARCHAR(40),
                                             in_job_id VARCHAR(48))
    RETURNS VOID
    LANGUAGE plpgsql
AS
$$
DECLARE
    t_id          varchar(58);
    subtask_array varchar[];
BEGIN

    -- deleting the completed subtasks for that job from subtask_report table
    -- adding result to an array (subtask_array)

    WITH subtask_completed AS (
        delete from subtask_report sr where sr.job_id = in_job_id and sr.status = 'Completed'
            RETURNING task_id
    )
    SELECT array_agg(task_id)
    FROM subtask_completed
    INTO subtask_array;

    -- looping into subtask_array to update the job percentage_complete

    if array_length(subtask_array, 1) > 0 then
        FOREACH t_id IN ARRAY subtask_array
            loop
                RAISE NOTICE 'in_partition_id: %, task_id: %', in_partition_id, t_id;
                PERFORM internal_report_task_status(in_partition_id, t_id , 'Completed', 100.00, NULL);
                END LOOP;
    end if;
END
$$;