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
 *
 *  This is the Baseline for the Flyway configuration
 *
 */

-- Create 'job_status' type if not existing
DO
$$
    BEGIN
        IF NOT EXISTS(SELECT 1 FROM pg_type WHERE typname = 'job_status') THEN
            CREATE TYPE job_status AS ENUM (
                'Active',
                'Cancelled',
                'Completed',
                'Failed',
                'Paused',
                'Waiting');
        END IF;
    END
$$;

-------- public.completed_subtask_report

CREATE TABLE IF NOT EXISTS public.completed_subtask_report
(
    partition_id varchar(40) NOT NULL,
    job_id       varchar(48) NOT NULL,
    task_id      varchar(70) NOT NULL,
    report_date  timestamp   NOT NULL
);

ALTER TABLE completed_subtask_report
    ALTER COLUMN task_id type VARCHAR(70);

-------- public.deleted_parent_table_log

CREATE TABLE IF NOT EXISTS public.deleted_parent_table_log
(
    table_name varchar(63) NOT NULL
);

-------- public.delete_log

CREATE TABLE IF NOT EXISTS public.delete_log
(
    table_name varchar(63) NOT NULL
);

-------- public.job

CREATE TABLE IF NOT EXISTS public.job
(
    job_id              varchar(48)  NOT NULL,
    "name"              varchar(255) NULL,
    description         text         NULL,
    "data"              text         NULL,
    create_date         timestamp    NOT NULL,
    status              job_status   NOT NULL DEFAULT 'Waiting'::job_status,
    percentage_complete float8       NOT NULL DEFAULT 0.00,
    failure_details     text         NULL,
    job_hash            int4         NULL,
    delay               int4         NULL     DEFAULT 0,
    last_update_date    timestamp    NOT NULL DEFAULT now(),
    partition_id        varchar(40)  NOT NULL DEFAULT 'default'::character varying,
    identity            serial4      NOT NULL
);
ALTER TABLE job
    ADD COLUMN IF NOT EXISTS partition_id VARCHAR(40) NOT NULL default 'default';

ALTER TABLE job
    ADD COLUMN IF NOT EXISTS identity SERIAL NOT NULL;

ALTER TABLE job
    ADD COLUMN IF NOT EXISTS last_update_date TIMESTAMP NOT NULL default now();

ALTER TABLE job
    ADD COLUMN IF NOT EXISTS delay INT default 0;

ALTER TABLE job
    DROP CONSTRAINT IF EXISTS pk_job CASCADE;

ALTER TABLE job
    ADD CONSTRAINT pk_job PRIMARY KEY (partition_id, job_id);

-------- public.job_dependency

CREATE TABLE IF NOT EXISTS public.job_dependency
(
    job_id           varchar(48) NOT NULL,
    dependent_job_id varchar(48) NOT NULL,
    partition_id     varchar(40) NOT NULL DEFAULT 'default'::character varying
);

ALTER TABLE job_dependency
    ADD COLUMN IF NOT EXISTS partition_id VARCHAR(40) NOT NULL default 'default';

ALTER TABLE job_dependency
    DROP CONSTRAINT IF EXISTS fk_job_dependency CASCADE;

ALTER TABLE job_dependency
    ADD CONSTRAINT fk_job_dependency FOREIGN KEY (partition_id, job_id) REFERENCES job (partition_id, job_id);

ALTER TABLE job_dependency
    DROP CONSTRAINT IF EXISTS pk_job_dependency CASCADE;

ALTER TABLE job_dependency
    ADD CONSTRAINT pk_job_dependency PRIMARY KEY (partition_id, job_id, dependent_job_id);

-------- public.job_task_data

CREATE TABLE IF NOT EXISTS public.job_task_data
(
    job_id               varchar(48)  NOT NULL,
    task_classifier      varchar(255) NOT NULL,
    task_api_version     int4         NOT NULL,
    task_data            bytea        NOT NULL,
    task_pipe            varchar(255) NOT NULL,
    target_pipe          varchar(255) NULL,
    eligible_to_run_date timestamp    NULL,
    partition_id         varchar(40)  NOT NULL DEFAULT 'default'::character varying,
    suspended            bool         NOT NULL DEFAULT false
);

ALTER TABLE job_task_data
    ADD COLUMN IF NOT EXISTS suspended BOOLEAN NOT NULL default false;

ALTER TABLE job_task_data
    ADD COLUMN IF NOT EXISTS partition_id VARCHAR(40) NOT NULL default 'default';

ALTER TABLE job_task_data
    ALTER COLUMN target_pipe drop not null;

ALTER TABLE job_task_data
    DROP CONSTRAINT IF EXISTS fk_job_task_data CASCADE;

ALTER TABLE job_task_data
    DROP CONSTRAINT IF EXISTS pk_job_task_data CASCADE;

ALTER TABLE job_task_data
    ADD CONSTRAINT fk_job_task_data FOREIGN KEY (partition_id, job_id) REFERENCES job (partition_id, job_id);

ALTER TABLE job_task_data
    ADD CONSTRAINT pk_job_task_data PRIMARY KEY (partition_id, job_id);

-------- public."label"

CREATE TABLE IF NOT EXISTS public."label"
(
    partition_id varchar(40)  NOT NULL DEFAULT 'default'::character varying,
    job_id       varchar(48)  NOT NULL,
    "label"      varchar(255) NOT NULL,
    value        varchar(255) NULL,
    CONSTRAINT label_pkey PRIMARY KEY (partition_id, job_id, label),
    CONSTRAINT fk_label_job FOREIGN KEY (partition_id, job_id) REFERENCES public.job (partition_id, job_id)
);

-------- public.stowed_task

CREATE TABLE IF NOT EXISTS public.stowed_task
(
    partition_id                               varchar(40)  NOT NULL,
    job_id                                     varchar(48)  NOT NULL,
    task_classifier                            varchar(255) NOT NULL,
    task_api_version                           int4         NOT NULL,
    task_data                                  bytea        NOT NULL,
    task_status                                varchar(255) NOT NULL,
    context                                    bytea        NOT NULL,
    "to"                                       varchar(255) NOT NULL,
    tracking_info_job_task_id                  varchar(255) NOT NULL,
    tracking_info_last_status_check_time       int8         NULL,
    tracking_info_status_check_interval_millis int8         NULL,
    tracking_info_status_check_url             text         NULL,
    tracking_info_tracking_pipe                varchar(255) NULL,
    tracking_info_track_to                     varchar(255) NULL,
    source_info                                bytea        NULL,
    correlation_id                             varchar(255) NULL,
    CONSTRAINT fk_stowed_task FOREIGN KEY (partition_id, job_id) REFERENCES public.job (partition_id, job_id)
);

-- Drop Indexes
DROP INDEX IF EXISTS idx_job_jobid_status;

-- Create Indexes
CREATE INDEX IF NOT EXISTS idx_deleted_parent_table_log
    ON public.deleted_parent_table_log
        USING btree (table_name);

CREATE INDEX IF NOT EXISTS idx_delete_log_table_name
    ON public.delete_log
        USING btree (table_name);

CREATE INDEX IF NOT EXISTS idx_job_create_date
    ON public.job
        USING btree (create_date);

CREATE INDEX IF NOT EXISTS idx_partition_id_and_job_id
    ON public.stowed_task
        USING btree (partition_id, job_id);

CREATE INDEX IF NOT EXISTS idx_job_partition_id_and_dependent_job_id
    ON public.job_dependency
        USING btree (partition_id, dependent_job_id);

-----------------------------------------------
------ DROP UNUSED EXTERNAL FUNCTIONS ---------
------------------- AND -----------------------
----- FORWARD DECLARE INTERNAL FUNCTIONS ------

DROP FUNCTION IF EXISTS cancel_job(
    in_partition_id VARCHAR(40),
    in_job_id VARCHAR(48),
    in_short_job_id VARCHAR(48)
);
CREATE OR REPLACE FUNCTION internal_cleanup_completed_subtask_report(
    in_partition_id VARCHAR(40),
    in_job_id VARCHAR(48)
)
    RETURNS VOID
    LANGUAGE plpgsql VOLATILE
AS $$
BEGIN
END
$$;
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
CREATE OR REPLACE FUNCTION internal_create_task_table(in_table_name VARCHAR(63))
    RETURNS VOID
    LANGUAGE plpgsql
AS $$
BEGIN
END
$$;
DROP FUNCTION IF EXISTS delete_job(
    in_partition_id VARCHAR(40),
    in_job_id VARCHAR(48),
    in_short_job_id VARCHAR(48)
);
DROP FUNCTION IF EXISTS internal_delete_task_table(
    in_job_id VARCHAR(48),
    in_ignore_status BOOLEAN
);
CREATE OR REPLACE FUNCTION internal_to_regclass(rel_name VARCHAR(63))
    RETURNS regclass
    LANGUAGE plpgsql STABLE
AS $$
BEGIN
    SELECT NULL;
END
$$;
CREATE OR REPLACE FUNCTION internal_does_table_exist(in_table_name VARCHAR(63))
    RETURNS BOOLEAN
    LANGUAGE SQL STABLE
AS $$
SELECT TRUE;
$$;
DROP FUNCTION IF EXISTS internal_drop_task_tables(
    in_short_task_id VARCHAR(58)
);
DROP FUNCTION IF EXISTS internal_drop_task_tables(
    in_partition_id VARCHAR(40),
    in_task_id VARCHAR(58)
);
CREATE OR REPLACE FUNCTION internal_drop_task_tables(
    in_partition_id VARCHAR(40),
    in_task_id VARCHAR(70)
)
    RETURNS VOID
    LANGUAGE plpgsql
AS $$
BEGIN
END
$$;
DROP FUNCTION IF EXISTS get_dependent_jobs();
DROP FUNCTION IF EXISTS get_job(in_job_id VARCHAR(58));
DROP FUNCTION IF EXISTS get_job(in_partition_id VARCHAR(40), in_job_id VARCHAR(58));
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
CREATE OR REPLACE FUNCTION internal_get_job_id(in_task_id VARCHAR(70))
    RETURNS VARCHAR(48)
    LANGUAGE SQL IMMUTABLE
AS $$
SELECT NULL;
$$;
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
CREATE OR REPLACE FUNCTION internal_get_options(in_options VARCHAR(128))
    RETURNS TABLE(
        option VARCHAR
                 )
    LANGUAGE SQL IMMUTABLE
AS $$
SELECT NULL;
$$;
DROP FUNCTION IF EXISTS internal_get_parent_task_id(in_task_id VARCHAR(58));
CREATE OR REPLACE FUNCTION internal_get_parent_task_id(in_task_id VARCHAR(70))
    RETURNS VARCHAR(70)
    LANGUAGE SQL IMMUTABLE
AS $$
SELECT NULL;
$$;
CREATE OR REPLACE FUNCTION internal_get_prereq_job_id_options(job_id_with_opts VARCHAR(128))
    RETURNS TABLE(
                     job_id VARCHAR(48),
                     options_string VARCHAR(128),
                     precreated BOOLEAN,
                     unknown_options BOOLEAN
                 )
    LANGUAGE SQL IMMUTABLE
AS $$
SELECT NULL, NULL, TRUE, TRUE;
$$;
DROP FUNCTION IF EXISTS get_status(in_job_id VARCHAR(48));
CREATE OR REPLACE FUNCTION internal_get_subtask_count(
    in_task_table_name VARCHAR(63)
)
    RETURNS INT
    LANGUAGE plpgsql STABLE
AS $$
BEGIN
    SELECT 0;
END
$$;
DROP FUNCTION IF EXISTS internal_get_subtask_id(in_task_id VARCHAR(58));
CREATE OR REPLACE FUNCTION internal_get_subtask_id(in_task_id VARCHAR(70))
    RETURNS INT
    LANGUAGE SQL IMMUTABLE
AS $$
SELECT 0;
$$;
CREATE OR REPLACE FUNCTION internal_get_task_status(
    in_task_table_name VARCHAR(63)
)
    RETURNS TABLE(
                     status job_status,
                     percentage_complete DOUBLE PRECISION,
                     failure_details TEXT
                 )
    LANGUAGE plpgsql STABLE
AS $$
BEGIN
    SELECT 'Failed', 0, NULL;
END
$$;
DROP FUNCTION IF EXISTS internal_get_task_table_name(
    in_short_task_id VARCHAR(58)
);
DROP FUNCTION IF EXISTS internal_get_task_table_name(
    in_partition_id VARCHAR(40),
    in_task_id VARCHAR(58)
);
CREATE OR REPLACE FUNCTION internal_get_task_table_name(
    in_partition_id VARCHAR(40),
    in_task_id VARCHAR(70)
)
    RETURNS VARCHAR(63)
    LANGUAGE SQL STABLE
AS $$
SELECT NULL;
$$;
CREATE OR REPLACE FUNCTION internal_has_dependent_jobs(
    in_partition_id VARCHAR(40),
    in_job_id VARCHAR(58)
)
    RETURNS BOOLEAN
    LANGUAGE SQL STABLE
AS $$
SELECT TRUE;
$$;
CREATE OR REPLACE FUNCTION internal_insert_delete_log(
    task_table_name VARCHAR
)
    RETURNS VOID
    LANGUAGE plpgsql VOLATILE
AS $$
BEGIN
END
$$;
CREATE OR REPLACE FUNCTION internal_insert_parent_table_to_delete (
    task_table_name VARCHAR
)
    RETURNS VOID
    LANGUAGE plpgsql VOLATILE
AS $$
BEGIN
END
$$;
CREATE OR REPLACE FUNCTION internal_insert_parent_table_to_delete (
    task_table_name VARCHAR
)
    RETURNS VOID
    LANGUAGE plpgsql VOLATILE
AS $$
BEGIN
END
$$;
CREATE OR REPLACE FUNCTION internal_create_job(
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
AS $$
BEGIN
    SELECT TRUE;
END
$$;
DROP FUNCTION IF EXISTS internal_is_final_task(in_task_id VARCHAR(58));
CREATE OR REPLACE FUNCTION internal_is_final_task(in_task_id VARCHAR(70))
    RETURNS BOOLEAN
    LANGUAGE SQL IMMUTABLE
AS $$
SELECT TRUE;
$$;
DROP FUNCTION IF EXISTS internal_is_task_completed(
    in_partition_id VARCHAR(40),
    in_task_id VARCHAR(58),
    in_short_task_id VARCHAR(58)
);
DROP FUNCTION IF EXISTS internal_is_task_completed(
    in_partition_id VARCHAR(40),
    in_task_id VARCHAR(58)
);
CREATE OR REPLACE FUNCTION internal_is_task_completed(
    in_partition_id VARCHAR(40),
    in_task_id VARCHAR(70)
)
    RETURNS BOOLEAN
    LANGUAGE plpgsql STABLE
AS $$
BEGIN
    SELECT TRUE;
END
$$;
DROP FUNCTION IF EXISTS pause_job(
    in_partition_id VARCHAR(40),
    in_job_id VARCHAR(48),
    in_short_job_id VARCHAR(48)
);
DROP FUNCTION IF EXISTS internal_process_dependent_jobs(in_job_id VARCHAR(58));
CREATE OR REPLACE FUNCTION internal_process_dependent_jobs(
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
AS $$
BEGIN
    SELECT NULL, NULL, NULL, 0, NULL, NULL, NULL;
END
$$;
DROP FUNCTION IF EXISTS internal_process_failed_dependent_jobs(in_partition_id VARCHAR(40), in_job_id VARCHAR(58),
                                                               in_failure_details TEXT);
CREATE OR REPLACE FUNCTION internal_process_failed_dependent_jobs(
    in_partition_id VARCHAR(40),
    in_job_id VARCHAR(48),
    in_failure_details TEXT
)
    RETURNS VOID
    LANGUAGE plpgsql
AS $$
BEGIN
END;
$$;
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
CREATE OR REPLACE FUNCTION internal_report_task_status(
    in_partition_id VARCHAR(40),
    in_task_id VARCHAR(70),
    in_status job_status,
    in_percentage_complete DOUBLE PRECISION,
    in_failure_details TEXT
)
    RETURNS VOID
    LANGUAGE plpgsql
AS $$
BEGIN
END
$$;
CREATE OR REPLACE FUNCTION internal_resolve_status(
    in_current_job_status job_status,
    in_proposed_job_status job_status
)
    RETURNS job_status
    LANGUAGE SQL IMMUTABLE
AS $$
SELECT 'Failed'::job_status;
$$;
DROP FUNCTION IF EXISTS resume_job(
    in_partition_id VARCHAR(40),
    in_job_id VARCHAR(48),
    in_short_job_id VARCHAR(48)
);
CREATE OR REPLACE FUNCTION internal_update_job_progress(
    in_partition_id VARCHAR(40),
    in_job_id ANYELEMENT
)
    RETURNS TABLE(
                     partition_id VARCHAR(40),
                     job_id VARCHAR(48),
                     status job_status
                 )
    LANGUAGE plpgsql VOLATILE
AS $$
BEGIN
    SELECT NULL, NULL, 'Failed'::job_status;
END
$$;
DROP FUNCTION IF EXISTS internal_upsert_into_task_table(
    in_task_table_name VARCHAR(63),
    in_task_id VARCHAR(58),
    in_status job_status,
    in_percentage_complete DOUBLE PRECISION,
    in_failure_details TEXT
);
CREATE OR REPLACE FUNCTION internal_upsert_into_task_table(
    in_task_table_name VARCHAR(63),
    in_task_id VARCHAR(70),
    in_status job_status,
    in_percentage_complete DOUBLE PRECISION,
    in_failure_details TEXT
)
    RETURNS VOID
    LANGUAGE plpgsql
AS $$
BEGIN
END
$$;
