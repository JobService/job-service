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
)
LANGUAGE plpgsql
AS
$$
BEGIN

PERFORM NULL
FROM (
          SELECT
             partition_id,
             job_id,
             operation
          FROM
             (
                 SELECT
                     partition_id,
                     job_id,
                     job_status,
                     last_update_date ,
                     COALESCE (
                             jep.operation,
                             djep.operation
                         ) AS operation,
                     CASE
                         WHEN create_date_offset = 'infinity' THEN 'infinity'::timestamp
                         WHEN create_date_offset IS NULL THEN NULL
                         ELSE (
                                 create_date + create_date_offset::INTERVAL
                             )::timestamp
                         END AS djep_exact_expiry_time,
                     jep.exact_expiry_time AS jep_exact_expiry_time,
                     CASE
                         WHEN djep.last_modified_offset IS NOT NULL THEN djep.last_modified_offset::INTERVAL
                         END AS djep_last_modified_offset,
                     jep.last_modified_offset AS jep_last_modified_offset
                 FROM (
                          SELECT
                              partition_id ,
                              job_id,
                              status,
                              create_date,
                              last_update_date
                          FROM
                              job j
                                  CROSS JOIN LATERAL (
                                      -- Getting the latest status
                                      SELECT
                                          status AS current_status
                                      FROM
                                          get_job(
                                                  partition_id,
                                                  job_id
                                          )
                                      LIMIT 1
                                  ) latest_status
                          WHERE
                              status = current_status
                      ) AS jobs
                          CROSS JOIN default_job_expiration_policy djep
                          LEFT JOIN job_expiration_policy jep
                                    USING (
                                           partition_id,
                                           job_id,
                                           job_status
                                        )
                 WHERE
                         job_status = status
             ) t1
         WHERE
                 COALESCE (
                         CASE
                             WHEN jep_exact_expiry_time IS NULL
                                 AND jep_last_modified_offset IS NULL THEN COALESCE(jep_exact_expiry_time, djep_exact_expiry_time)
                             ELSE jep_exact_expiry_time
                             END,
                         last_update_date + COALESCE(jep_last_modified_offset, djep_last_modified_offset)
                     ) <= now() ) expired_jobs
         CROSS JOIN LATERAL (
    SELECT
        NULL
    FROM
        delete_or_expire_job(
                partition_id,
                job_id,
                operation
            ) expiring_action
    ) global_selection;
END;
$$;
