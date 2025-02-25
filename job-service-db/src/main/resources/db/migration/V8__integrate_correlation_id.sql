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
 ********** Update tables and drop unused functions ***********
 **************************************************************
 */

ALTER TABLE job
    ADD COLUMN IF NOT EXISTS correlation_id VARCHAR(48) NULL;

ALTER TABLE job_task_data
    ADD COLUMN IF NOT EXISTS correlation_id VARCHAR(48) NULL;

DROP FUNCTION IF EXISTS get_dependent_jobs();

DROP FUNCTION IF EXISTS create_job(
    in_partition_id VARCHAR(40),
    in_job_id VARCHAR(48),
    in_name VARCHAR(255),
    in_description TEXT,
    in_data TEXT,
    in_job_hash INT,
    in_task_classifier VARCHAR(255),
    in_task_api_version INT,
    in_task_data BYTEA,
    in_task_pipe VARCHAR(255),
    in_target_pipe VARCHAR(255),
    in_delay INT,
    in_labels VARCHAR(255)[][]
);

DROP FUNCTION IF EXISTS create_job(
    in_partition_id VARCHAR(40),
    in_job_id VARCHAR(48),
    in_name VARCHAR(255),
    in_description TEXT,
    in_data TEXT,
    in_job_hash INT,
    in_task_classifier VARCHAR(255),
    in_task_api_version INT,
    in_task_data BYTEA,
    in_task_pipe VARCHAR(255),
    in_target_pipe VARCHAR(255),
    in_prerequisite_job_ids VARCHAR(128)[],
    in_delay INT,
    in_labels VARCHAR(255)[][],
    in_suspended_partition BOOLEAN
);

DROP FUNCTION IF EXISTS internal_create_job(
    in_partition_id VARCHAR(40),
    in_job_id VARCHAR(48),
    in_name VARCHAR(255),
    in_description TEXT,
    in_data TEXT,
    in_delay INT,
    in_job_hash INT,
    in_labels VARCHAR(255)[][]
);
