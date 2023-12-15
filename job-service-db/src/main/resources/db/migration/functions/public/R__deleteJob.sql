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
 *  Name: delete_job
 *
 *  Description:
 *  Deletes the job row and corresponding task tables.
 */
DROP FUNCTION IF EXISTS delete_job(character varying, character varying);

CREATE OR REPLACE FUNCTION delete_job(
    in_partition_id VARCHAR(40),
    in_job_id VARCHAR(48)
)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
DECLARE
    v_tables_to_delete TEXT[];
    v_table_name TEXT;

BEGIN
    -- Raise exception if job identifier has not been specified
    IF in_job_id IS NULL OR in_job_id = '' THEN
        RAISE EXCEPTION 'The job identifier has not been specified' USING ERRCODE = '02000'; -- sqlstate no data
    END IF;

    -- Take out an exclusive update lock on the job row
    PERFORM NULL
    FROM job
    WHERE partition_id = in_partition_id
        AND job_id = in_job_id
    FOR UPDATE;

    -- Raise exception if no matching job identifier has been found
    IF NOT FOUND THEN
        RAISE EXCEPTION 'job_id {%} not found', in_job_id USING ERRCODE = 'P0002'; -- sqlstate no_data_found
    END IF;

    -- Drop the task tables associated with the specified job
    PERFORM internal_drop_task_tables(in_partition_id, in_job_id);

    -- Remove job dependency and job task data rows
    DELETE FROM job_dependency jd WHERE jd.partition_id = in_partition_id AND jd.job_id = in_job_id;
    DELETE FROM job_task_data jtd WHERE jtd.partition_id = in_partition_id AND jtd.job_id = in_job_id;

    -- Remove any associated labels
    DELETE FROM label lbl WHERE lbl.partition_id = in_partition_id AND lbl.job_id = in_job_id;

    -- Remove row from the job table
    DELETE FROM job
    WHERE partition_id = in_partition_id
        AND job_id = in_job_id;

    -- Removes all related subtasks from completed_subtask_report table
    PERFORM internal_cleanup_completed_subtask_report(in_partition_id, in_job_id);

    return true;
END
$$;
