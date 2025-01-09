--
-- Copyright 2016-2024 Micro Focus or one of its affiliates.
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
 *  Name: update_job_progress
 *
 *  Description:
 *  This procedure updates the percentage_complete in the job table with data from completed_subtask_report table.
 */
CREATE OR REPLACE PROCEDURE public.update_job_progress(
	IN in_num_of_tasks_to_update integer DEFAULT 100)
LANGUAGE plpgsql
AS $$
DECLARE
    task RECORD;
BEGIN

    FOR task IN
        SELECT partition_id, job_id
        FROM completed_subtask_report
        GROUP BY partition_id, job_id
        ORDER BY random()
        LIMIT in_num_of_tasks_to_update
    LOOP
        -- Take out an exclusive update lock on the job row
        PERFORM NULL
        FROM job j
        WHERE j.partition_id = task.partition_id
          AND j.job_id = task.job_id
          FOR UPDATE;

        -- Perform update on the row
        PERFORM internal_update_job_progress(task.partition_id, task.job_id);

        -- Complete transaction to update row
        COMMIT;
    END LOOP;
END
$$;
