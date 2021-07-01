--
-- Copyright 2016-2021 Micro Focus or one of its affiliates.
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
 *  Name: internal_upsert_job_policy
 *
 *  Description:
 *  Insert the expiration_policy of a job into the job_expiration_policy table.
 */
CREATE OR REPLACE FUNCTION internal_upsert_job_policy(
    in_partition_id VARCHAR(40),
    in_job_id VARCHAR(48),
    in_policies JOB_POLICY[]
)
RETURNS VOID
LANGUAGE plpgsql VOLATILE
AS $$
DECLARE
    v_created_time TIMESTAMP;

BEGIN

    IF in_policies IS NULL
        OR CARDINALITY(in_policies) = 0 THEN RETURN;
    END IF;

    -- Storing job's create_date
    SELECT
        create_date
    FROM
        job
    WHERE
         partition_id = in_partition_id
            AND job_id = in_job_id
    INTO
        v_created_time;

    INSERT
    INTO
        job_expiration_policy (
        partition_id,
        job_id,
        job_status,
        operation,
        expiration_time,
        exact_expiry_time,
        last_modified_offset
    )
    SELECT
        in_partition_id,
        in_job_id,
        (p.b_policy).job_status,
        (p.b_policy).operation,
        (p.b_policy).expiration_time AS exp_time,
        CASE
            -- if expiration_time starts with createTime, we calculate and store the exact expiration time
            WHEN LEFT((p.b_policy).expiration_time, 1) = 'c' THEN (
                    v_created_time + split_part((p.b_policy).expiration_time, '+', 2)::INTERVAL
                )::timestamp
            -- if expiration_time equals 'none', we set expiration_time to infinity
            WHEN LEFT((p.b_policy).expiration_time, 1) = 'n' THEN
                'infinity'::timestamp
            -- if expiration_time starts with lastUpdateTime, we set null
            WHEN LEFT((p.b_policy).expiration_time, 1) = 'l' THEN
                NULL
            ELSE
                -- otherwise, we cast the date provided and store it
                (p.b_policy).expiration_time::timestamp
            END AS exact_expiry_time,
        CASE
            WHEN LEFT((p.b_policy).expiration_time, 1) = 'l' THEN split_part((p.b_policy).expiration_time, '+', 2)::INTERVAL
            END AS last_modified_offset
    FROM
        UNNEST(in_policies) AS p;

    RETURN;
END $$;
