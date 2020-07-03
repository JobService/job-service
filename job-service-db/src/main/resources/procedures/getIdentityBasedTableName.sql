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
 *  Name: internal_get_identity_based_task_table_name
 *
 *  Description:
 *  Creates the task table nambe based on the identity value from the root job.
 */
CREATE OR REPLACE FUNCTION internal_get_identity_based_task_table_name(
    in_task_id VARCHAR(63)
)
RETURNS VARCHAR(63)
LANGUAGE plpgsql
AS $$
DECLARE
    v_root_job_id VARCHAR(63);
    v_root_job_identity VARCHAR(63);
    v_parent_job_id VARCHAR(63);
    v_job_id_tail VARCHAR(63);
    v_parent_table_name VARCHAR(63);
    v_is_main_job BOOLEAN;
    v_job_id VARCHAR(63);
    v_job_identity bigint;

BEGIN
    -- Is this the root id already?
    v_parent_job_id = internal_get_parent_task_id(in_task_id);
    v_root_job_id = v_parent_job_id;
    -- Loop to get to root job idif the parent id is not null
    IF v_parent_job_id IS NOT NULL THEN
        v_is_main_job = FALSE;
        LOOP
            v_parent_job_id = internal_get_parent_task_id(v_parent_job_id);
            IF v_parent_job_id IS NOT NULL THEN
                v_root_job_id = v_parent_job_id;
            END IF;
            EXIT WHEN v_parent_job_id IS NULL;
        END LOOP;
    ELSE
        v_root_job_id = in_task_id;
        v_is_main_job = TRUE;
    END IF;

    -- Get jobs identity from job table
    SELECT identity INTO v_job_identity FROM job WHERE job_id = v_root_job_id;

    -- Concatenate root job identity to tail of task id to get table name
    v_parent_table_name = 'tsk_' || CAST(v_job_identity AS VARCHAR);
    IF v_is_main_job = TRUE THEN
        RETURN v_parent_table_name;
    ELSE
        RETURN v_parent_table_name || SUBSTRING(in_task_id FROM position('.' IN in_task_id));
    END IF;
END
$$;
