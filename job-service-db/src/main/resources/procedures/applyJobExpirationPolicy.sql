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
LANGUAGE plpgsql
AS
$$
BEGIN

    PERFORM NULL
    FROM
        (
            SELECT *  FROM (
                               SELECT
                                   c.partition_id AS partition_id,
                                   c.job_id AS job_id,
                                   COALESCE(up.job_status, c.job_status) AS job_status,
                                   COALESCE(up.operation, c.operation) AS operation

                               FROM
                                   job_expiration_policy_cartesian c -- see views
                                       LEFT JOIN job_user_policy up ON  -- see views
                                               c.partition_id = up.partition_id
                                           AND c.job_id = up.job_id
                                           AND c.job_status = up.job_status
                               WHERE COALESCE (
                                             CASE
                                                 WHEN
                                                     up.exact_expiry_time IS NULL
                                                            AND up.last_modified_offset IS NULL
                                                     THEN
                                                       COALESCE(up.exact_expiry_time, c.exact_expiry_time)
                                                 ELSE
                                                     up.exact_expiry_time
                                             END,
                                             up.last_update_date+COALESCE(up.last_modified_offset, c.last_modified_offset)
                                         ) <= now()
                           ) t1
                               CROSS JOIN LATERAL (
                -- Getting the latest status
                SELECT status AS current_status
                FROM
                    get_job(
                            partition_id, job_id
                        )
                LIMIT 1
                ) latest_status
            WHERE job_status = current_status
        ) expired_jobs
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
