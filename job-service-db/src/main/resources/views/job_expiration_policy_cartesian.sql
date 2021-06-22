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
 *  Name: job_expiration_policy_cartesian
 *
 *  Description:
 *  Returns the cartesian product of default expiration policy for the jobs.
 */
SELECT j.partition_id,
       j.job_id,
       djep.job_status,
       djep.operation,
       CASE
           WHEN create_date_offset = 'infinity' THEN
               'infinity'::timestamp
           WHEN create_date_offset IS NULL THEN
               NULL
           ELSE
               (
                   j.create_date + create_date_offset::INTERVAL
                   )::timestamp
           END AS exact_expiry_time,
       CASE
           WHEN last_modified_offset IS NOT null THEN
               last_modified_offset::INTERVAL
           END AS last_modified_offset
FROM job j
         CROSS JOIN default_job_expiration_policy djep;
