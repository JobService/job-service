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
    LANGUAGE plpgsql VOLATILE
AS
$$
DECLARE
    taskId        VARCHAR(58);
    subtask_array VARCHAR[];
BEGIN
    -- Deleting the completed subtasks for that job from completed_subtask_report table
    -- Add the results to subtask_array
    WITH completed_subtask AS (
        DELETE FROM completed_subtask_report csr WHERE csr.partition_id = in_partition_id AND csr.job_id = in_job_id
            RETURNING csr.task_id
    )
    SELECT array_agg(task_id)
    FROM completed_subtask
    INTO subtask_array;

    -- Loop through subtask_array and update the job percentage_complete
    IF subtask_array IS NOT NULL THEN
    FOREACH taskId IN ARRAY subtask_array
        LOOP
            PERFORM internal_report_task_status(in_partition_id, taskId , 'Completed', 100.00, NULL);
        END LOOP;
    END IF;
END
$$;
