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
 *  Name: cancel_job
 *
 *  Description:
 *  Cancels the specified job.
 *
 *   - in_short_job_id: additional identification for the same job - see
 *                      com.hpe.caf.services.job.util.JobTaskId#getShortId
 */
DROP FUNCTION IF EXISTS cancel_job(
    in_partition_id VARCHAR(40),
    in_job_id VARCHAR(48),
    in_short_job_id VARCHAR(48)
);
CREATE OR REPLACE FUNCTION cancel_job(
    in_partition_id VARCHAR(40),
    in_job_id VARCHAR(48)
)
RETURNS VOID
LANGUAGE plpgsql
AS $$
DECLARE
    v_is_finished BOOLEAN;

BEGIN
    -- Raise exception if job identifier has not been specified
    IF in_job_id IS NULL OR in_job_id = '' THEN
        RAISE EXCEPTION 'The job identifier has not been specified' USING ERRCODE = '02000'; -- sqlstate no data;
    END IF;

    -- Only support Cancel operation on jobs with current status 'Waiting', 'Active' or 'Paused'
    -- And take out an exclusive update lock on the job row
    SELECT status IN ('Completed', 'Failed') INTO v_is_finished
    FROM job
    WHERE partition_id = in_partition_id
        AND job_id = in_job_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'job_id {%} not found', in_job_id USING ERRCODE = 'P0002'; -- sqlstate no_data_found
    END IF;

    IF v_is_finished THEN
        RAISE EXCEPTION 'job_id {%} cannot be cancelled', in_job_id USING ERRCODE = '02000';
    END IF;

    -- Mark the job cancelled in the job table
    UPDATE job
    SET status = 'Cancelled', last_update_date = now() AT TIME ZONE 'UTC'
    WHERE partition_id = in_partition_id
        AND job_id = in_job_id
        AND status != 'Cancelled';

    -- Drop any task tables relating to the job
    PERFORM internal_drop_task_tables(in_job_id);
END
$$;
