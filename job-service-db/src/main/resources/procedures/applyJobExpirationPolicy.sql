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
 *  Name: apply_job_expiration_policy
 *
 *  Description:
 *  Expires or deletes jobs in accordance with their expiry policies.
 */
CREATE OR REPLACE PROCEDURE apply_job_expiration_policy(
    in_propagate_failures BOOLEAN
)
LANGUAGE plpgsql AS
$$
BEGIN
    PERFORM NULL
    FROM (
             SELECT *
             FROM (
                      SELECT job.partition_id      AS p_id,
                             job.job_id            AS j_id,
                             jep.operation       AS j_operation,

                             -- Whenever there is a "related date" provided (to create_date or last_update_date)
                             -- we calculate the expiration_date and check if it's expired, returning a boolean

                             CASE
                                 WHEN expiration_time IS NOT NULL
                                     AND LEFT(expiration_time, 1) = 'l' THEN (
                                        (
                                             last_update_date + split_part(expiration_time, '+', 2)::INTERVAL
                                        )::timestamp
                                     ) <= now() AT TIME ZONE 'UTC'
                                 WHEN expiration_time IS NOT NULL
                                     AND LEFT(expiration_time, 1) = 'c' THEN (
                                        (
                                             create_date + split_part(expiration_time, '+', 2)::INTERVAL
                                        )::timestamp
                                     ) <= now() AT TIME ZONE 'UTC'
                                 ELSE (
                                          expiration_time::timestamp
                                          ) <= now() AT TIME ZONE 'UTC'
                                 END                expired
                      FROM job
                               CROSS JOIN LATERAL (
                          -- Getting the latest status
                          SELECT status AS current_status
                          FROM
                              get_job(
                                      partition_id, job_id
                                  )
                          LIMIT 1
                          ) latest_status
                          -- Joining where the policy status matched the current status
                               INNER JOIN job_expiration_policy jep ON
                                  job.partition_id = jep.partition_id
                              AND job.job_id = jep.job_id
                              AND current_status = jep.job_status
                      WHERE LEFT(expiration_time, 4) != 'none'
                        AND expiration_time IS NOT NULL
                  ) current_job_status_and_policy
             -- Selecting only the expired jobs
             WHERE expired IS TRUE
         ) expired_jobs
             CROSS JOIN LATERAL (
        SELECT NULL
        FROM delete_or_expire_job(
                     p_id, j_id, j_operation, in_propagate_failures
                 ) expiring_action
        ) global_selection;
END;

$$;
