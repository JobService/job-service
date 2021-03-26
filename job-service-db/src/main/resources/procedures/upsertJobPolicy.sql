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
LANGUAGE plpgsql
VOLATILE
AS
$$
BEGIN

    IF in_policies IS NULL OR CARDINALITY(in_policies) = 0 THEN
        RETURN;
    END IF;

    INSERT
    INTO job_expiration_policy (
                                partition_id,
                                job_id,
                                job_status,
                                operation,
                                expiration_time
                                )
    SELECT in_partition_id,
           in_job_id,
           p.job_status,
           p.operation,
           p.expiration_time
    FROM UNNEST(in_policies) AS p;
    RETURN;
END
$$;
