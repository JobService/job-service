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
 *  Name: view_job_user_policy
 *
 *  Description:
 *  Returns the expiration policies for the jobs
 */

SELECT j.partition_id,
       j.job_id,
       j.create_date,
       j.last_update_date,
       jep.job_status,
       jep.operation,
       jep.exact_expiry_time,
       jep.last_modified_offset
FROM job j
         JOIN job_expiration_policy jep
              ON j.partition_id = jep.partition_id
                  AND j.job_id = jep.job_id;
