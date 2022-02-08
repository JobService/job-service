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
 **************************************************************
 **************** DROP UNUSED FUNCTIONS ***********************
 **************************************************************
 */

DROP FUNCTION IF EXISTS cancel_job(
    in_partition_id VARCHAR(40),
    in_job_id VARCHAR(48),
    in_short_job_id VARCHAR(48)
);
DROP FUNCTION IF EXISTS create_job(
    in_partition_id VARCHAR(40),
    in_job_id VARCHAR(48),
    in_name VARCHAR(255),
    in_description TEXT,
    in_data TEXT,
    in_job_hash INT
);
DROP FUNCTION IF EXISTS create_job(
    in_partition_id VARCHAR(40),
    in_job_id VARCHAR(48),
    in_name VARCHAR(255),
    in_description TEXT,
    in_data TEXT,
    in_job_hash INT,
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
    in_delay INT
);
DROP FUNCTION IF EXISTS delete_job(
    in_partition_id VARCHAR(40),
    in_job_id VARCHAR(48),
    in_short_job_id VARCHAR(48)
);
DROP FUNCTION IF EXISTS internal_delete_task_table(
    in_job_id VARCHAR(48),
    in_ignore_status BOOLEAN
);
DROP FUNCTION IF EXISTS internal_drop_task_tables(
    in_short_task_id VARCHAR(58)
);
DROP FUNCTION IF EXISTS internal_drop_task_tables(
    in_partition_id VARCHAR(40),
    in_task_id VARCHAR(58)
);
DROP FUNCTION IF EXISTS get_job(in_job_id VARCHAR(58));
DROP FUNCTION IF EXISTS get_job(
    in_partition_id VARCHAR(40),
    in_job_id VARCHAR(58)
);
DROP FUNCTION IF EXISTS get_job_can_be_progressed(
    in_partition_id VARCHAR(40),
    in_job_id VARCHAR(48)
);
DROP FUNCTION IF EXISTS get_job_exists(
    in_partition_id VARCHAR(40),
    in_job_id VARCHAR(48),
    in_job_hash INT
);
DROP FUNCTION IF EXISTS internal_get_job_id(in_task_id VARCHAR(58));
DROP FUNCTION IF EXISTS get_jobs(
    in_job_id_starts_with VARCHAR(58),
    in_status_type VARCHAR(20),
    in_limit INT,
    in_offset INT);
DROP FUNCTION IF EXISTS get_jobs(
    in_job_id_starts_with VARCHAR(58),
    in_status_type VARCHAR(20),
    in_limit INT,
    in_offset INT,
    in_labels VARCHAR(255)[]);
DROP FUNCTION IF EXISTS get_jobs(
    in_partition_id VARCHAR(40),
    in_job_id_starts_with VARCHAR(48),
    in_status_type VARCHAR(20),
    in_limit INT,
    in_offset INT,
    in_sort_field VARCHAR(20),
    in_sort_ascending BOOLEAN,
    in_labels VARCHAR(255)[],
    in_filter VARCHAR(255));
DROP FUNCTION IF EXISTS get_jobs_count(
    in_partition_id VARCHAR(40),
    in_job_id_starts_with VARCHAR(48),
    in_status_type VARCHAR(20)
);
DROP FUNCTION IF EXISTS internal_get_last_position(TEXT, CHAR);
DROP FUNCTION IF EXISTS internal_get_parent_task_id(in_task_id VARCHAR(58));
DROP FUNCTION IF EXISTS get_status(in_job_id VARCHAR(48));
DROP FUNCTION IF EXISTS internal_get_subtask_id(in_task_id VARCHAR(58));
DROP FUNCTION IF EXISTS internal_get_task_table_name(
    in_short_task_id VARCHAR(58)
);
DROP FUNCTION IF EXISTS internal_get_task_table_name(
    in_partition_id VARCHAR(40),
    in_task_id VARCHAR(58)
);
DROP FUNCTION IF EXISTS internal_is_final_task(in_task_id VARCHAR(58));
DROP FUNCTION IF EXISTS internal_is_task_completed(
    in_partition_id VARCHAR(40),
    in_task_id VARCHAR(58),
    in_short_task_id VARCHAR(58)
);
DROP FUNCTION IF EXISTS internal_is_task_completed(
    in_partition_id VARCHAR(40),
    in_task_id VARCHAR(58)
);
DROP FUNCTION IF EXISTS pause_job(
    in_partition_id VARCHAR(40),
    in_job_id VARCHAR(48),
    in_short_job_id VARCHAR(48)
);
DROP FUNCTION IF EXISTS internal_process_dependent_jobs(in_job_id VARCHAR(58));
DROP FUNCTION IF EXISTS internal_process_failed_dependent_jobs(
    in_partition_id VARCHAR(40),
    in_job_id VARCHAR(58),
    in_failure_details TEXT
);
DROP FUNCTION IF EXISTS report_complete(
    in_partition_id VARCHAR(40),
    in_task_id VARCHAR(58),
    in_short_task_id VARCHAR(58)
);
DROP FUNCTION IF EXISTS report_complete(
    in_partition_id VARCHAR(40),
    in_task_id VARCHAR(58)
);
DROP FUNCTION IF EXISTS report_complete_bulk(
    in_partition_id VARCHAR(40),
    in_job_id VARCHAR(48),
    in_task_ids VARCHAR(58)[]
);
DROP FUNCTION IF EXISTS report_failure(
    in_partition_id VARCHAR(40),
    in_task_id VARCHAR(58),
    in_short_task_id VARCHAR(58),
    in_failure_details TEXT
);
DROP FUNCTION IF EXISTS report_failure(
    in_partition_id VARCHAR(40),
    in_task_id VARCHAR(58),
    in_short_task_id VARCHAR(58),
    in_failure_details TEXT,
    in_propagate_failures BOOLEAN
);
DROP FUNCTION IF EXISTS report_failure(
    in_partition_id VARCHAR(40),
    in_task_id VARCHAR(58),
    in_failure_details TEXT,
    in_propagate_failures BOOLEAN
);
DROP FUNCTION IF EXISTS report_failure(
    in_partition_id VARCHAR(40),
    in_task_id VARCHAR(70),
    in_failure_details TEXT,
    in_propagate_failures BOOLEAN
);
DROP FUNCTION IF EXISTS report_progress(
    in_task_id VARCHAR(58),
    in_status job_status
);
DROP FUNCTION IF EXISTS report_progress(
    in_partition_id VARCHAR(40),
    in_task_id VARCHAR(58),
    in_percentage_complete DOUBLE PRECISION
);
DROP FUNCTION IF EXISTS internal_report_task_completion(in_task_table_name VARCHAR(63));
DROP FUNCTION IF EXISTS internal_report_task_failure(
    in_task_table_name VARCHAR(63),
    in_failure_details TEXT
);
DROP FUNCTION IF EXISTS internal_report_task_status(
    in_partition_id VARCHAR(40),
    in_task_id VARCHAR(58),
    in_short_task_id VARCHAR(58),
    in_status job_status,
    in_percentage_complete DOUBLE PRECISION,
    in_failure_details TEXT
);
DROP FUNCTION IF EXISTS internal_report_task_status(
    in_partition_id VARCHAR(40),
    in_task_id VARCHAR(58),
    in_status job_status,
    in_percentage_complete DOUBLE PRECISION,
    in_failure_details TEXT
);
DROP FUNCTION IF EXISTS resume_job(
    in_partition_id VARCHAR(40),
    in_job_id VARCHAR(48),
    in_short_job_id VARCHAR(48)
);
DROP FUNCTION IF EXISTS internal_upsert_into_task_table(
    in_task_table_name VARCHAR(63),
    in_task_id VARCHAR(58),
    in_status job_status,
    in_percentage_complete DOUBLE PRECISION,
    in_failure_details TEXT
);
