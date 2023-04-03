--
-- Copyright 2016-2022 Micro Focus or one of its affiliates.
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
 *  Creating database schema
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
