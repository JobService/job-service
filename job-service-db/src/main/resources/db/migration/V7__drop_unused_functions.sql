--
-- Copyright 2016-2025 Open Text.
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
 **************************************************************
 **************** DROP UNUSED FUNCTIONS ***********************
 **************************************************************
 */

DROP FUNCTION IF EXISTS get_jobs(
    in_partition_id VARCHAR(40),
    in_job_id_starts_with VARCHAR(48),
    in_status_type VARCHAR(20),
    in_limit INT,
    in_offset INT,
    in_sort_field VARCHAR(20),
    in_sort_label VARCHAR(255),
    in_sort_ascending BOOLEAN,
    in_labels VARCHAR(255)[],
    in_filter VARCHAR(255)
);
DROP FUNCTION IF EXISTS get_jobs_count(
    in_partition_id VARCHAR(40),
    in_job_id_starts_with VARCHAR(48),
    in_status_type VARCHAR(20),
    in_filter VARCHAR(255)
);

