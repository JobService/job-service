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
 ************ FORWARD DECLARE INTERNAL FUNCTIONS **************
 **************************************************************
 */

DO $$
BEGIN
    CREATE FUNCTION internal_cleanup_completed_subtask_report(
        in_partition_id VARCHAR(40),
        in_job_id VARCHAR(48)
    )
    RETURNS VOID
    LANGUAGE plpgsql VOLATILE
    AS 'BEGIN /* Forward Declaration */ END';
EXCEPTION WHEN duplicate_function THEN
END $$;

DO $$
BEGIN
    CREATE FUNCTION internal_create_task_table(in_table_name VARCHAR(63))
    RETURNS VOID
    LANGUAGE plpgsql
    AS 'BEGIN /* Forward Declaration */ END';
EXCEPTION WHEN duplicate_function THEN
END $$;

DO $$
BEGIN
    CREATE FUNCTION internal_to_regclass(rel_name VARCHAR(63))
    RETURNS regclass
    LANGUAGE plpgsql STABLE
    AS 'BEGIN /* Forward Declaration */ END';
EXCEPTION WHEN duplicate_function THEN
END $$;

DO $$
BEGIN
    CREATE FUNCTION internal_does_table_exist(in_table_name VARCHAR(63))
    RETURNS BOOLEAN
    LANGUAGE SQL STABLE
    AS '/* Forward Declaration */ SELECT NULL::BOOLEAN;';
EXCEPTION WHEN duplicate_function THEN
END $$;

DO $$
BEGIN
    CREATE FUNCTION internal_drop_task_tables(
        in_partition_id VARCHAR(40),
        in_task_id VARCHAR(70)
    )
    RETURNS VOID
    LANGUAGE plpgsql
    AS 'BEGIN /* Forward Declaration */ END';
EXCEPTION WHEN duplicate_function THEN
END $$;

DO $$
BEGIN
    CREATE FUNCTION internal_get_job_id(in_task_id VARCHAR(70))
    RETURNS VARCHAR(48)
    LANGUAGE SQL IMMUTABLE
    AS '/* Forward Declaration */ SELECT NULL';
EXCEPTION WHEN duplicate_function THEN
END $$;

DO $$
BEGIN
    CREATE FUNCTION internal_get_options(in_options VARCHAR(128))
    RETURNS TABLE(
        option VARCHAR
    )
    LANGUAGE SQL IMMUTABLE
    AS '/* Forward Declaration */ SELECT NULL';
EXCEPTION WHEN duplicate_function THEN
END $$;

DO $$
BEGIN
    CREATE FUNCTION internal_get_parent_task_id(in_task_id VARCHAR(70))
    RETURNS VARCHAR(70)
    LANGUAGE SQL IMMUTABLE
    AS '/* Forward Declaration */ SELECT NULL';
EXCEPTION WHEN duplicate_function THEN
END $$;

DO $$
BEGIN
CREATE FUNCTION internal_get_prereq_job_id_options(job_id_with_opts VARCHAR(128))
    RETURNS TABLE(
        job_id VARCHAR(48),
        options_string VARCHAR(128),
        precreated BOOLEAN,
        unknown_options BOOLEAN
    )
    LANGUAGE SQL IMMUTABLE
    AS '/* Forward Declaration */ SELECT NULL, NULL, NULL::BOOLEAN, NULL::BOOLEAN;';
EXCEPTION WHEN duplicate_function THEN
END $$;

DO $$
BEGIN
    CREATE FUNCTION internal_get_subtask_count(
        in_task_table_name VARCHAR(63)
    )
    RETURNS INT
    LANGUAGE plpgsql STABLE
    AS 'BEGIN /* Forward Declaration */ END';
EXCEPTION WHEN duplicate_function THEN
END $$;

DO $$
BEGIN
CREATE FUNCTION internal_get_subtask_id(in_task_id VARCHAR(70))
    RETURNS INT
    LANGUAGE SQL IMMUTABLE
    AS '/* Forward Declaration */ SELECT NULL::INT';
EXCEPTION WHEN duplicate_function THEN
END $$;

DO $$
BEGIN
    CREATE FUNCTION internal_get_task_status(
        in_task_table_name VARCHAR(63)
    )
    RETURNS TABLE(
        status job_status,
        percentage_complete DOUBLE PRECISION,
        failure_details TEXT
    )
    LANGUAGE plpgsql STABLE
    AS 'BEGIN /* Forward Declaration */ END';
EXCEPTION WHEN duplicate_function THEN
END $$;

DO $$
BEGIN
    CREATE FUNCTION internal_get_task_table_name(
        in_partition_id VARCHAR(40),
        in_task_id VARCHAR(70)
    )
    RETURNS VARCHAR(63)
    LANGUAGE SQL STABLE
    AS '/* Forward Declaration */ SELECT NULL';
EXCEPTION WHEN duplicate_function THEN
END $$;

DO $$
BEGIN
    CREATE FUNCTION internal_has_dependent_jobs(
        in_partition_id VARCHAR(40),
        in_job_id VARCHAR(58)
    )
    RETURNS BOOLEAN
    LANGUAGE SQL STABLE
    AS '/* Forward Declaration */ SELECT NULL::BOOLEAN';
EXCEPTION WHEN duplicate_function THEN
END $$;

DO $$
BEGIN
    CREATE FUNCTION internal_insert_delete_log(
        task_table_name VARCHAR
    )
    RETURNS VOID
    LANGUAGE plpgsql VOLATILE
    AS 'BEGIN /* Forward Declaration */ END';
EXCEPTION WHEN duplicate_function THEN
END $$;

DO $$
BEGIN
    CREATE FUNCTION internal_insert_parent_table_to_delete (
        task_table_name VARCHAR
    )
    RETURNS VOID
    LANGUAGE plpgsql VOLATILE
    AS 'BEGIN /* Forward Declaration */ END';
EXCEPTION WHEN duplicate_function THEN
END $$;

DO $$
BEGIN
    CREATE FUNCTION internal_create_job(
        in_partition_id VARCHAR(40),
        in_job_id VARCHAR(48),
        in_name VARCHAR(255),
        in_description TEXT,
        in_data TEXT,
        in_delay INT,
        in_job_hash INT,
        in_labels VARCHAR(255)[][] default null
    )
    RETURNS BOOLEAN
    LANGUAGE plpgsql
    AS 'BEGIN /* Forward Declaration */ END';
EXCEPTION WHEN duplicate_function THEN
END $$;

DO $$
BEGIN
    CREATE FUNCTION internal_is_final_task(in_task_id VARCHAR(70))
    RETURNS BOOLEAN
    LANGUAGE SQL IMMUTABLE
    AS '/* Forward Declaration */ SELECT NULL::BOOLEAN';
EXCEPTION WHEN duplicate_function THEN
END $$;

DO $$
BEGIN
    CREATE FUNCTION internal_is_task_completed(
        in_partition_id VARCHAR(40),
        in_task_id VARCHAR(70)
    )
    RETURNS BOOLEAN
    LANGUAGE plpgsql STABLE
    AS 'BEGIN /* Forward Declaration */ END';
EXCEPTION WHEN duplicate_function THEN
END $$;

DO $$
BEGIN
    CREATE FUNCTION internal_process_dependent_jobs(
        in_partition_id VARCHAR(40),
        in_job_id VARCHAR(48)
    )
    RETURNS TABLE(
        partition_id VARCHAR(40),
        job_id VARCHAR(48),
        task_classifier VARCHAR(255),
        task_api_version INT,
        task_data BYTEA,
        task_pipe VARCHAR(255),
        target_pipe VARCHAR(255)
    )
    LANGUAGE plpgsql
    AS 'BEGIN /* Forward Declaration */ END';
EXCEPTION WHEN duplicate_function THEN
END $$;

DO $$
BEGIN
    CREATE FUNCTION internal_process_failed_dependent_jobs(
        in_partition_id VARCHAR(40),
        in_job_id VARCHAR(48),
        in_failure_details TEXT
    )
    RETURNS VOID
    LANGUAGE plpgsql
    AS 'BEGIN /* Forward Declaration */ END';
EXCEPTION WHEN duplicate_function THEN
END $$;

DO $$
BEGIN
    CREATE FUNCTION internal_report_task_status(
        in_partition_id VARCHAR(40),
        in_task_id VARCHAR(70),
        in_status job_status,
        in_percentage_complete DOUBLE PRECISION,
        in_failure_details TEXT
    )
    RETURNS VOID
    LANGUAGE plpgsql
    AS 'BEGIN /* Forward Declaration */ END';
EXCEPTION WHEN duplicate_function THEN
END $$;

DO $$
BEGIN
    CREATE FUNCTION internal_resolve_status(
        in_current_job_status job_status,
        in_proposed_job_status job_status
    )
    RETURNS job_status
    LANGUAGE SQL IMMUTABLE
    AS '/* Forward Declaration */ SELECT NULL::job_status';
EXCEPTION WHEN duplicate_function THEN
END $$;

DO $$
    BEGIN
    CREATE FUNCTION internal_update_job_progress(
        in_partition_id VARCHAR(40),
        in_job_id ANYELEMENT
    )
    RETURNS TABLE(
        partition_id VARCHAR(40),
        job_id VARCHAR(48),
        status job_status
    )
    LANGUAGE plpgsql VOLATILE
AS 'BEGIN /* Forward Declaration */ END';
EXCEPTION WHEN duplicate_function THEN
END $$;

DO $$
BEGIN
    CREATE FUNCTION internal_upsert_into_task_table(
        in_task_table_name VARCHAR(63),
        in_task_id VARCHAR(70),
        in_status job_status,
        in_percentage_complete DOUBLE PRECISION,
        in_failure_details TEXT
    )
    RETURNS VOID
    LANGUAGE plpgsql
    AS 'BEGIN /* Forward Declaration */ END';
EXCEPTION WHEN duplicate_function THEN
END $$;
