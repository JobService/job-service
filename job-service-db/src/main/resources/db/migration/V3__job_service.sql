-- DROP TYPE job_status;


DO $$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'job_status') THEN
            CREATE TYPE job_status AS ENUM (
                'Active',
                'Cancelled',
                'Completed',
                'Failed',
                'Paused',
                'Waiting');
        END IF;
    END$$;

-- public.completed_subtask_report definition

-- Drop table

-- DROP TABLE public.completed_subtask_report;

CREATE TABLE IF NOT EXISTS public.completed_subtask_report (
                                                               partition_id varchar(40) NOT NULL,
                                                               job_id varchar(48) NOT NULL,
                                                               task_id varchar(70) NOT NULL,
                                                               report_date timestamp NOT NULL
);

-- public.delete_log definition

-- Drop table

-- DROP TABLE public.delete_log;

CREATE TABLE IF NOT EXISTS public.delete_log (
    table_name varchar(63) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_delete_log_table_name ON public.delete_log USING btree (table_name);


-- public.job definition

-- Drop table

-- DROP TABLE public.job;

CREATE TABLE IF NOT EXISTS public.job (
                                          job_id varchar(48) NOT NULL,
                                          "name" varchar(255) NULL,
                                          description text NULL,
                                          "data" text NULL,
                                          create_date timestamp NOT NULL,
                                          status job_status NOT NULL DEFAULT 'Waiting'::job_status,
                                          percentage_complete float8 NOT NULL DEFAULT 0.00,
                                          failure_details text NULL,
                                          job_hash int4 NULL,
                                          delay int4 NULL DEFAULT 0,
                                          last_update_date timestamp NOT NULL DEFAULT now(),
                                          partition_id varchar(40) NOT NULL DEFAULT 'default'::character varying,
                                          "identity" serial4 NOT NULL,
                                          CONSTRAINT pk_job PRIMARY KEY (partition_id, job_id)
);
CREATE INDEX IF NOT EXISTS idx_job_create_date ON public.job USING btree (create_date);


-- public.job_dependency definition

-- Drop table

-- DROP TABLE public.job_dependency;

CREATE TABLE IF NOT EXISTS public.job_dependency (
                                                     job_id varchar(48) NOT NULL,
                                                     dependent_job_id varchar(48) NOT NULL,
                                                     partition_id varchar(40) NOT NULL DEFAULT 'default'::character varying,
                                                     CONSTRAINT pk_job_dependency PRIMARY KEY (partition_id, job_id, dependent_job_id),
                                                     CONSTRAINT fk_job_dependency FOREIGN KEY (partition_id,job_id) REFERENCES public.job(partition_id,job_id)
);
CREATE INDEX IF NOT EXISTS idx_job_partition_id_and_dependent_job_id ON public.job_dependency USING btree (partition_id,
                                                                                                           dependent_job_id);


-- public.job_task_data definition

-- Drop table

-- DROP TABLE public.job_task_data;

CREATE TABLE IF NOT EXISTS public.job_task_data (
                                                    job_id varchar(48) NOT NULL,
                                                    task_classifier varchar(255) NOT NULL,
                                                    task_api_version int4 NOT NULL,
                                                    task_data bytea NOT NULL,
                                                    task_pipe varchar(255) NOT NULL,
                                                    target_pipe varchar(255) NULL,
                                                    eligible_to_run_date timestamp NULL,
                                                    partition_id varchar(40) NOT NULL DEFAULT 'default'::character varying,
                                                    suspended bool NOT NULL DEFAULT false,
                                                    CONSTRAINT pk_job_task_data PRIMARY KEY (partition_id, job_id),
                                                    CONSTRAINT fk_job_task_data FOREIGN KEY (partition_id,job_id) REFERENCES public.job(partition_id,job_id)
);


-- public."label" definition

-- Drop table

-- DROP TABLE public."label";

CREATE TABLE IF NOT EXISTS public."label" (
                                              partition_id varchar(40) NOT NULL DEFAULT 'default'::character varying,
                                              job_id varchar(48) NOT NULL,
                                              "label" varchar(255) NOT NULL,
                                              value varchar(255) NULL,
                                              CONSTRAINT label_pkey PRIMARY KEY (partition_id, job_id, label),
                                              CONSTRAINT fk_label_job FOREIGN KEY (partition_id,job_id) REFERENCES public.job(partition_id,job_id)
);


-- public.stowed_task definition

-- Drop table

-- DROP TABLE public.stowed_task;

CREATE TABLE IF NOT EXISTS public.stowed_task (
                                                  partition_id varchar(40) NOT NULL,
                                                  job_id varchar(48) NOT NULL,
                                                  task_classifier varchar(255) NOT NULL,
                                                  task_api_version int4 NOT NULL,
                                                  task_data bytea NOT NULL,
                                                  task_status varchar(255) NOT NULL,
                                                  context bytea NOT NULL,
                                                  "to" varchar(255) NOT NULL,
                                                  tracking_info_job_task_id varchar(255) NOT NULL,
                                                  tracking_info_last_status_check_time int8 NULL,
                                                  tracking_info_status_check_interval_millis int8 NULL,
                                                  tracking_info_status_check_url text NULL,
                                                  tracking_info_tracking_pipe varchar(255) NULL,
                                                  tracking_info_track_to varchar(255) NULL,
                                                  source_info bytea NULL,
                                                  correlation_id varchar(255) NULL,
                                                  CONSTRAINT fk_stowed_task FOREIGN KEY (partition_id,job_id) REFERENCES public.job(partition_id,job_id)
);
CREATE INDEX IF NOT EXISTS idx_partition_id_and_job_id ON public.stowed_task USING btree (partition_id, job_id);
