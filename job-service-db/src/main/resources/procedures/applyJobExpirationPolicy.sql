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
 *  Policies can be found in 2 tables:
 *      - job_expiration_policy if the user provided it on job's creation ( alias user_p)
 *      - default_job_expiration_policy ( alias default_p)
 *  Each of those tables have two columns related respectively to create_date and last_update_date as:
 *      - default_job_expiration_policy -> create_date_offset, last_modified_offset
 *      - job_expiration_policy         -> exact_expiry_time, last_modified_offset
 *  Only 1 of those fields maximum, by table, can be filled (the other one should be null)
 *
 *  User's policy is used in priority if provided, default is applied otherwise
 *
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
                 COALESCE( user_p.operation, default_p.operation ) AS operation
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
                     CROSS JOIN default_job_expiration_policy default_p
                     LEFT JOIN job_expiration_policy user_p
                               USING (
                                      partition_id,
                                      job_id,
                                      job_status
                                   )
             WHERE
                   job_status = status
                   -- Order:
                   --   user_p.exact_expiry_time
                   --   user_p.last_modified_offset
                   --   default_p.last_modified_offset
                   --   default_p.create_date_offset
               AND COALESCE(
                           COALESCE(
                                   user_p.exact_expiry_time,
                                   last_update_date + COALESCE(
                                           user_p.last_modified_offset,
                                           CASE
                                               WHEN
                                                   default_p.last_modified_offset IS NOT NULL
                                                   THEN
                                                   default_p.last_modified_offset::INTERVAL
                                               END
                                       )
                               )
                       ,
                           CASE
                               WHEN default_p.create_date_offset = 'infinity'
                                   THEN 'infinity'::timestamp
                               WHEN default_p.create_date_offset IS NULL
                                   THEN NULL
                               ELSE
                                   (create_date + default_p.create_date_offset::INTERVAL)::timestamp
                               END
                       ) <= now()
        ) expired_jobs
             CROSS JOIN LATERAL
        delete_or_expire_job(
                partition_id,
                job_id,
                operation
            );
END;
$$;
