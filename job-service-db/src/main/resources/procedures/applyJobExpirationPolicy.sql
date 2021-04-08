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
 *  Expires or deletes a job based on its expiration policy.
 */
CREATE OR REPLACE FUNCTION apply_job_expiration_policy(
)
RETURNS VOID
LANGUAGE plpgsql
VOLATILE
AS
$$
BEGIN
    -- create inner function that deletes or expires the job according to its status policy
    CREATE OR REPLACE FUNCTION delete_or_expire_job(p_id varchar, j_id varchar, j_operation EXPIRATION_OPERATION)
    RETURNS VOID AS
    $delete_or_expire_job$
    BEGIN
        IF j_operation = 'Expire' THEN
            UPDATE job j
            SET status ='Expired'
            WHERE j.partition_id = p_id
              AND j.job_id = j_id;
        ELSE
            PERFORM delete_job(p_id, j_id);
        END IF;
    END;
    $delete_or_expire_job$
        LANGUAGE plpgsql VOLATILE;


    PERFORM NULL
    FROM (

             -- We select the job details where an expiration policy is set
             SELECT j.partition_id AS p_id,
                    j.job_id       AS j_id,
                    jep.job_status AS j_status,
                    jep.operation  AS j_operation,
                    -- Whenever there is a "related date" provided (to create_date or last_update_date)
                    -- we calculate the expiration_date and check if it's expired, returning a boolean
                    CASE
                        WHEN jep.expiration_time IS NOT NULL AND LEFT(jep.expiration_time, 1) = 'l' THEN ((
                                j.last_update_date + split_part(jep.expiration_time, '+', 2)::interval)::timestamp)
                            <= now() AT TIME ZONE 'UTC'
                        WHEN jep.expiration_time IS NOT NULL AND LEFT(jep.expiration_time, 1) = 'c'
                            THEN ((j.create_date + split_part(jep.expiration_time, '+', 2)::interval)::timestamp)
                            <= now() AT TIME ZONE 'UTC'
                        ELSE (jep.expiration_time::timestamp) <= now() AT TIME ZONE 'UTC'
                        END           expired
             FROM job j
                      FULL OUTER JOIN job_expiration_policy jep
                                      ON j.partition_id = jep.partition_id
                                          AND j.job_id = jep.job_id
             WHERE jep.expiration_time != 'none'
               AND jep.expiration_time IS NOT NULL
               AND j.status != 'Expired'
         ) t1

             -- Then, we make sure that we get the latest status for the job
             CROSS JOIN LATERAL
        (
        SELECT status AS s2
        FROM get_job(p_id, j_id)
        LIMIT 1
        ) t2
             CROSS JOIN LATERAL (
        SELECT NULL
        FROM delete_or_expire_job(p_id, j_id, j_operation)
        ) t3
         -- We target only the policy matching the current status
         -- and only when the job is actually expired according to the expiration_time
    WHERE j_status = s2
      AND t1.expired IS TRUE;

END;
$$;
