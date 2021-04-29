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
CREATE OR REPLACE PROCEDURE apply_job_expiration_policy()
LANGUAGE plpgsql
AS $$
BEGIN
    PERFORM NULL
    FROM (
           SELECT *
           FROM (
               SELECT p_id,
                      j_id,
                      j_operation,
                      jep.expiration_time AS j_expiration_time,
                      -- Whenever there is a "related date" provided (to create_date or last_update_date)
                      -- we calculate the expiration_date and check if it's expired, returning a boolean
                      CASE
                             WHEN jep.expiration_time IS NOT NULL
                             AND LEFT(jep.expiration_time, 1) = 'l' THEN (
                                     ( j_last_update_date + split_part(jep.expiration_time, '+', 2)::INTERVAL
                                    )::timestamp
                                ) <= now() AT TIME ZONE 'UTC'
                             WHEN jep.expiration_time IS NOT NULL
                             AND LEFT(jep.expiration_time, 1) = 'c' THEN (
                                     ( j_create_date + split_part(jep.expiration_time, '+', 2)::INTERVAL
                                     )::timestamp
                                ) <= now() AT TIME ZONE 'UTC'
                             ELSE (
                                    jep.expiration_time::timestamp
                                  ) <= now() AT TIME ZONE 'UTC'
                             END                expired
               FROM (
                     SELECT *
                     FROM (
                             SELECT j.partition_id     AS p_id,
                                    j.job_id           AS j_id,
                                    jep.job_status     AS j_status,
                                    jep.operation      AS j_operation,
                                    j.last_update_date AS j_last_update_date,
                                    j.create_date      AS j_create_date
                             FROM job j
                             INNER JOIN job_expiration_policy jep ON
                             j.partition_id = jep.partition_id
                             AND j.job_id = jep.job_id
                          ) t1
                     -- We make sure that we get the latest status for the job
                     CROSS JOIN LATERAL (
                             SELECT status AS current_status
                             FROM get_job( p_id, j_id )
                             LIMIT 1
                     ) t2
                     WHERE j_status = current_status
               ) t3
               INNER JOIN job_expiration_policy jep ON
                   p_id = jep.partition_id
                   AND j_id = jep.job_id
                   AND j_status = jep.job_status
                   ) T4
               WHERE expired IS TRUE
                   AND LEFT(j_expiration_time, 4) != 'none'
                   AND j_expiration_time IS NOT NULL
         ) t5
    CROSS JOIN LATERAL (
        SELECT NULL
        FROM
            delete_or_expire_job(
                    p_id, j_id, j_operation
                )
    ) t6;
END;
$$;
