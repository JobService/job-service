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
       j.status,
       djep.job_status,
       djep.operation,
       djep.expiration_time,
       CASE
           -- if expiration_time starts with createTime, we calculate and store the exact expiration time
           WHEN LEFT(djep.expiration_time, 1) = 'c' THEN (
               j.create_date + split_part(djep.expiration_time, '+', 2)::INTERVAL
               )::timestamp
           -- if expiration_time equals 'none', we set expiration_time to infinity
           WHEN LEFT(djep.expiration_time, 1) = 'n' THEN
               'infinity'::timestamp
           -- if expiration_time starts with lastUpdateTime, we set null
           WHEN LEFT(djep.expiration_time, 1) = 'l' THEN
               NULL
           ELSE
               -- otherwise, we cast the date provided and store it
               djep.expiration_time::timestamp
           END AS exact_expiry_time,
       CASE
           WHEN LEFT(djep.expiration_time, 1) = 'l' THEN split_part(djep.expiration_time, '+', 2)::INTERVAL
           END AS last_modified_offset
FROM job j
         CROSS JOIN default_job_expiration_policy djep;
