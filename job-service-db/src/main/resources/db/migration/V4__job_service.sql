CREATE OR REPLACE FUNCTION public.cancel_job(in_partition_id character varying, in_job_id character varying)
    RETURNS void
    LANGUAGE plpgsql
AS $function$
DECLARE
    v_is_finished BOOLEAN;

BEGIN
    -- Raise exception if job identifier has not been specified
    IF in_job_id IS NULL OR in_job_id = '' THEN
        RAISE EXCEPTION 'The job identifier has not been specified' USING ERRCODE = '02000'; -- sqlstate no data;
    END IF;

    -- Only support Cancel operation on jobs with current status 'Waiting', 'Active' or 'Paused'
    -- And take out an exclusive update lock on the job row
    SELECT status IN ('Completed', 'Failed') INTO v_is_finished
    FROM job
    WHERE partition_id = in_partition_id
      AND job_id = in_job_id
        FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'job_id {%} not found', in_job_id USING ERRCODE = 'P0002'; -- sqlstate no_data_found
    END IF;

    IF v_is_finished THEN
        RAISE EXCEPTION 'job_id {%} cannot be cancelled', in_job_id USING ERRCODE = '02000';
    END IF;

    -- Mark the job cancelled in the job table
    UPDATE job
    SET status = 'Cancelled', last_update_date = now() AT TIME ZONE 'UTC'
    WHERE partition_id = in_partition_id
      AND job_id = in_job_id
      AND status != 'Cancelled';

    -- Drop any task tables relating to the job
    PERFORM internal_drop_task_tables(in_partition_id, in_job_id);

    -- Removes all related subtasks from completed_subtask_report table
    PERFORM internal_cleanup_completed_subtask_report(in_partition_id, in_job_id);
END
$function$
;

CREATE OR REPLACE FUNCTION public.create_job(in_partition_id character varying, in_job_id character varying, in_name character varying, in_description text, in_data text, in_job_hash integer, in_task_classifier character varying, in_task_api_version integer, in_task_data bytea, in_task_pipe character varying, in_target_pipe character varying, in_prerequisite_job_ids character varying[], in_delay integer, in_labels character varying[] DEFAULT NULL::character varying[], in_suspended_partition boolean DEFAULT false)
    RETURNS TABLE(job_created boolean)
    LANGUAGE plpgsql
AS $function$
BEGIN
    -- Raise exception if job identifier has not been specified
    IF in_job_id IS NULL OR in_job_id = '' THEN
        RAISE EXCEPTION 'Job identifier has not been specified' USING ERRCODE = '02000'; -- sqlstate no data
    END IF;

    -- Raise exception if job taskClassifier has not been specified
    IF in_task_classifier IS NULL OR in_task_classifier = '' THEN
        RAISE EXCEPTION 'Job taskClassifier has not been specified' USING ERRCODE = '02000'; -- sqlstate no data
    END IF;

    -- Raise exception if job taskApiVersion has not been specified
    IF in_task_api_version IS NULL THEN
        RAISE EXCEPTION 'Job taskApiVersion has not been specified' USING ERRCODE = '02000'; -- sqlstate no data
    END IF;

    -- Raise exception if job taskData has not been specified
    IF in_task_data IS NULL THEN
        RAISE EXCEPTION 'Job taskData has not been specified' USING ERRCODE = '02000'; -- sqlstate no data
    END IF;

    -- Raise exception if job taskPipe has not been specified
    IF in_task_pipe IS NULL OR in_task_pipe = '' THEN
        RAISE EXCEPTION 'Job taskPipe has not been specified' USING ERRCODE = '02000'; -- sqlstate no data
    END IF;

    -- Raise exception if job targetPipe is empty. Null targetPipe is valid
    IF in_target_pipe = '' THEN
        RAISE EXCEPTION 'Job targetPipe has not been specified' USING ERRCODE = '02000'; -- sqlstate no data
    END IF;

    -- Set default value for delay if no value provided
    IF in_delay IS NULL THEN
        in_delay = 0;
    END IF;

    IF NOT internal_create_job(in_partition_id, in_job_id, in_name, in_description, in_data, in_delay, in_job_hash, in_labels) THEN
        RETURN QUERY SELECT FALSE;
        RETURN;
    END IF;

    -- Store task data and job dependency rows if any of the prerequisite job identifiers are not yet complete

    -- Store dependency rows for those prerequisite job identifiers not yet complete
    -- Include prerequisite job identifiers not yet in the system
    WITH prereqs_with_opts(job_id_with_opts) AS
             (
                 SELECT unnest(in_prerequisite_job_ids)::VARCHAR(128)
             ),
         prereqs AS
             (
                 -- Remove any duplicate pre-requisites, and if a pre-req is mentioned multiple times then merge the options
                 SELECT job_id, precreated FROM
                     (
                         SELECT ROW_NUMBER() OVER (PARTITION BY job_id ORDER BY precreated DESC), job_id, precreated
                         FROM prereqs_with_opts
                                  CROSS JOIN internal_get_prereq_job_id_options(job_id_with_opts)
                     ) tbl
                 WHERE row_number = 1
             ),
         locked_jobs AS
             (
                 -- Lock table job for update
                 SELECT * FROM job
                 WHERE partition_id = in_partition_id
                   AND job_id IN (SELECT job_id FROM prereqs)
                 ORDER BY partition_id, job_id
                     FOR UPDATE
             ),
         updated_jobs AS
             (
                 -- Process outstanding job updates
                 SELECT * FROM internal_update_job_progress(in_partition_id, (SELECT ARRAY(SELECT job_id FROM locked_jobs)))
             ),
         prereqs_created_but_not_complete AS
             (
                 SELECT * FROM updated_jobs uj
                 WHERE uj.partition_id = in_partition_id
                   AND uj.job_id IN (SELECT job_id FROM prereqs)
                   AND uj.status <> 'Completed'
             ),
         prereqs_not_created_yet AS
             (
                 SELECT * FROM prereqs
                 WHERE NOT precreated AND job_id NOT IN (
                     SELECT job_id FROM job WHERE partition_id = in_partition_id
                 )
             ),
         all_incomplete_prereqs(prerequisite_job_id) AS
             (
                 SELECT job_id FROM prereqs_created_but_not_complete
                 UNION
                 SELECT job_id FROM prereqs_not_created_yet
             )

    INSERT INTO public.job_dependency(partition_id, job_id, dependent_job_id)
    SELECT in_partition_id, in_job_id, prerequisite_job_id
    FROM all_incomplete_prereqs;

    IF FOUND OR in_delay > 0 OR in_suspended_partition THEN
        INSERT INTO public.job_task_data(
            partition_id,
            job_id,
            task_classifier,
            task_api_version,
            task_data,
            task_pipe,
            target_pipe,
            eligible_to_run_date,
            suspended
        ) VALUES (
                     in_partition_id,
                     in_job_id,
                     in_task_classifier,
                     in_task_api_version,
                     in_task_data,
                     in_task_pipe,
                     in_target_pipe,
                     CASE WHEN NOT FOUND THEN now() AT TIME ZONE 'UTC' + (in_delay * interval '1 second') END,
                     in_suspended_partition
                 );
    END IF;

    RETURN QUERY SELECT TRUE;
END
$function$
;

CREATE OR REPLACE FUNCTION public.create_job(in_partition_id character varying, in_job_id character varying, in_name character varying, in_description text, in_data text, in_job_hash integer, in_labels character varying[] DEFAULT NULL::character varying[])
    RETURNS TABLE(job_created boolean)
    LANGUAGE plpgsql
AS $function$
BEGIN
    -- Raise exception if job identifier has not been specified
    IF in_job_id IS NULL OR in_job_id = '' THEN
        RAISE EXCEPTION 'Job identifier has not been specified' USING ERRCODE = '02000'; -- sqlstate no data
    END IF;

    RETURN QUERY
        SELECT internal_create_job(in_partition_id, in_job_id, in_name, in_description, in_data, 0, in_job_hash, in_labels);

END
$function$
;

CREATE OR REPLACE FUNCTION public.delete_dependent_job(in_partition_id character varying, in_job_id character varying)
    RETURNS void
    LANGUAGE plpgsql
AS $function$

BEGIN
    DELETE FROM job_task_data
    WHERE in_partition_id = partition_id
      AND in_job_id = job_id;
END
$function$
;

CREATE OR REPLACE FUNCTION public.delete_job(in_partition_id character varying, in_job_id character varying)
    RETURNS void
    LANGUAGE plpgsql
AS $function$
DECLARE
    v_tables_to_delete TEXT[];
    v_table_name TEXT;

BEGIN
    -- Raise exception if job identifier has not been specified
    IF in_job_id IS NULL OR in_job_id = '' THEN
        RAISE EXCEPTION 'The job identifier has not been specified' USING ERRCODE = '02000'; -- sqlstate no data
    END IF;

    -- Take out an exclusive update lock on the job row
    PERFORM NULL
    FROM job
    WHERE partition_id = in_partition_id
      AND job_id = in_job_id
        FOR UPDATE;

    -- Raise exception if no matching job identifier has been found
    IF NOT FOUND THEN
        RAISE EXCEPTION 'job_id {%} not found', in_job_id USING ERRCODE = 'P0002'; -- sqlstate no_data_found
    END IF;

    -- Drop the task tables associated with the specified job
    PERFORM internal_drop_task_tables(in_partition_id, in_job_id);

    -- Remove job dependency and job task data rows
    DELETE FROM job_dependency jd WHERE jd.partition_id = in_partition_id AND jd.job_id = in_job_id;
    DELETE FROM job_task_data jtd WHERE jtd.partition_id = in_partition_id AND jtd.job_id = in_job_id;

    -- Remove any associated labels
    DELETE FROM label lbl WHERE lbl.partition_id = in_partition_id AND lbl.job_id = in_job_id;

    -- Remove row from the job table
    DELETE FROM job
    WHERE partition_id = in_partition_id
      AND job_id = in_job_id;

    -- Removes all related subtasks from completed_subtask_report table
    PERFORM internal_cleanup_completed_subtask_report(in_partition_id, in_job_id);
END
$function$
;

CREATE OR REPLACE PROCEDURE public.drop_deleted_task_tables()
    LANGUAGE plpgsql
AS $procedure$
DECLARE
    selected_table_names VARCHAR;
    commit_limit integer:=10;
    rec record;

BEGIN
    selected_table_names := $q$SELECT table_name FROM delete_log LIMIT $q$ || commit_limit || $q$ FOR UPDATE SKIP LOCKED$q$;

    WHILE EXISTS (SELECT 1 FROM delete_log)
        LOOP
            FOR rec IN EXECUTE selected_table_names
                LOOP
                    EXECUTE 'DROP TABLE IF EXISTS ' || quote_ident(rec.table_name);
                    DELETE FROM delete_log WHERE table_name = rec.table_name;
                END LOOP;
            COMMIT;
        END LOOP;
END
$procedure$
;

CREATE OR REPLACE FUNCTION public.get_dependent_jobs()
    RETURNS TABLE(partition_id character varying, job_id character varying, task_classifier character varying, task_api_version integer, task_data bytea, task_pipe character varying, target_pipe character varying)
    LANGUAGE plpgsql
    STABLE
AS $function$
BEGIN
    RETURN QUERY
        SELECT
            jtd.partition_id,
            jtd.job_id,
            jtd.task_classifier,
            jtd.task_api_version,
            jtd.task_data,
            jtd.task_pipe,
            jtd.target_pipe
        FROM job_task_data jtd
                 LEFT JOIN job_dependency jd
                           ON jd.partition_id = jtd.partition_id AND jd.job_id = jtd.job_id
        WHERE NOT jtd.suspended
          AND jtd.eligible_to_run_date IS NOT NULL
          AND jtd.eligible_to_run_date <= now() AT TIME ZONE 'UTC'  -- now eligible for running
          AND jd.job_id IS NULL;  -- no other dependencies to wait on

END
$function$
;

CREATE OR REPLACE FUNCTION public.get_job(in_partition_id character varying, in_job_id character varying)
    RETURNS TABLE(job_id character varying, name character varying, description text, data text, create_date text, last_update_date text, status job_status, percentage_complete double precision, failure_details text, actiontype character, label character varying, label_value character varying)
    LANGUAGE plpgsql
AS $function$
BEGIN
    -- Raise exception if the job identifier has not been specified
    IF in_job_id IS NULL OR in_job_id = '' THEN
        RAISE EXCEPTION 'Job identifier has not been specified' USING ERRCODE = '02000'; -- sqlstate no data
    END IF;

    -- Take out an exclusive update lock on the job row
    PERFORM NULL
    FROM job j
    WHERE j.partition_id = in_partition_id
      AND j.job_id = in_job_id
        FOR UPDATE;

    -- Process outstanding job updates
    PERFORM internal_update_job_progress(in_partition_id, in_job_id);

    -- Return job metadata belonging to the specified job_id
    -- 'WORKER' is the only supported action type for now
    RETURN QUERY
        SELECT job.job_id,
               job.name,
               job.description,
               job.data,
               to_char(job.create_date, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'),
               to_char(job.last_update_date, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'),
               job.status,
               job.percentage_complete,
               job.failure_details,
               CAST('WORKER' AS CHAR(6)) AS actionType,
               lbl.label,
               lbl.value
        FROM job
                 LEFT JOIN public.label lbl ON lbl.partition_id = job.partition_id AND lbl.job_id = job.job_id
        WHERE job.partition_id = in_partition_id
          AND job.job_id = in_job_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'job_id {%} not found', in_job_id USING ERRCODE = 'P0002'; -- sqlstate no_data_found
    END IF;
END
$function$
;

CREATE OR REPLACE FUNCTION public.get_job_can_be_progressed(in_partition_id character varying, in_job_id character varying)
    RETURNS TABLE(can_be_progressed boolean)
    LANGUAGE plpgsql
    STABLE
AS $function$
BEGIN
    RETURN QUERY
        SELECT NOT EXISTS(
                SELECT 1 FROM job_task_data as jtd
                WHERE jtd.partition_id = in_partition_id
                  AND jtd.job_id = in_job_id
            );
END
$function$
;

CREATE OR REPLACE FUNCTION public.get_jobs(in_partition_id character varying, in_job_id_starts_with character varying, in_status_type character varying, in_limit integer, in_offset integer, in_sort_field character varying, in_sort_label character varying, in_sort_ascending boolean, in_labels character varying[], in_filter character varying)
    RETURNS TABLE(job_id character varying, name character varying, description text, data text, create_date text, last_update_date text, status job_status, percentage_complete double precision, failure_details text, actiontype character, label character varying, label_value character varying)
    LANGUAGE plpgsql
AS $function$
DECLARE
    sql VARCHAR;
    escapedJobIdStartsWith VARCHAR;
    whereOrAnd VARCHAR(7) = ' WHERE ';
    andConst CONSTANT VARCHAR(5) = ' AND ';
    jobId VARCHAR(48);
    jobIdArray VARCHAR(48)[];

BEGIN
    -- Return all rows from the job table:
    --   If the in_job_id param is specified, only those rows starting with that param will be returned.
    --   If the in_status_type param is
    --      NotCompleted - only those results with statuses other than Completed will be returned;
    --      Completed - only those results with Completed status will be returned;
    --      Inactive - only those results with inactive statuses (i.e. Completed, Failed, Cancelled) will be returned;
    --      NotFinished - only those results with unfinished statuses (ie. Active, Paused, Waiting) will be returned;
    --      Anything else returns all statuses.
    -- Also accepts in_limit and in_offset params to support paging and limiting the number of rows returned.
    -- 'WORKER' is the only supported action type for now and this is returned.
    sql := $q$
        SELECT job.job_id,
               job.name,
               job.description,
               job.data,
               job.create_date,
               job.last_update_date,
               job.status,
               job.percentage_complete,
               job.failure_details,
               CAST('WORKER' AS CHAR(6)) AS actionType,
               lbl.label,
               lbl.value
        FROM
        (SELECT
               job.partition_id,
               job.job_id,
               job.name,
               job.description,
               job.data,
               to_char(job.create_date, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as create_date,
               to_char(job.last_update_date, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as last_update_date,
               job.status,
               job.percentage_complete,
               job.failure_details,
               CAST('WORKER' AS CHAR(6)) AS actionType
        FROM job

        $q$;

    IF in_labels IS NOT NULL AND ARRAY_LENGTH(in_labels, 1) > 0 THEN
        sql := sql || whereOrAnd || ' EXISTS ( SELECT 1 FROM public.label lbl WHERE lbl.partition_id = job.partition_id'
                   || ' AND lbl.job_id = job.job_id AND lbl.label = ANY(' || quote_literal(in_labels) || ')) ';
        whereOrAnd := andConst;
    END IF;

    sql := sql || whereOrAnd || ' job.partition_id = ' || quote_literal(in_partition_id);
    whereOrAnd := andConst;

    IF in_job_id_starts_with IS NOT NULL AND in_job_id_starts_with != '' THEN
        escapedJobIdStartsWith = replace(replace(quote_literal(in_job_id_starts_with), '_', '\_'), '%', '\%');
        escapedJobIdStartsWith = left(escapedJobIdStartsWith, char_length(escapedJobIdStartsWith) - 1) || '%''';
        sql := sql || whereOrAnd || ' job.job_id LIKE ' || escapedJobIdStartsWith;
        whereOrAnd := andConst;
    END IF;


    IF in_status_type IS NOT NULL THEN
        IF in_status_type = 'NotCompleted' THEN
            sql := sql || whereOrAnd || $q$ status IN ('Active', 'Paused', 'Waiting', 'Cancelled', 'Failed')$q$;
            whereOrAnd := andConst;
        ELSIF in_status_type = 'Completed' THEN
            sql := sql || whereOrAnd || $q$ status IN ('Completed')$q$;
            whereOrAnd := andConst;
        ELSIF in_status_type = 'Inactive' THEN
            sql := sql || whereOrAnd || $q$ status IN ('Completed', 'Cancelled', 'Failed')$q$;
            whereOrAnd := andConst;
        ELSIF in_status_type = 'NotFinished' THEN
            sql := sql || whereOrAnd || $q$ status IN ('Active', 'Paused', 'Waiting')$q$;
            whereOrAnd := andConst;
        END IF;
    END IF;

    IF in_filter IS NOT NULL THEN
        sql := sql || whereOrAnd || in_filter;
    END IF;

    sql := sql || ' ORDER BY ' ||
           CASE WHEN in_sort_label IS NOT NULL AND in_sort_label != ''
                    THEN '(SELECT value FROM label l WHERE job.partition_id = l.partition_id AND job.job_id = l.job_id AND l.label = ' ||
                         quote_literal(in_sort_label) || ')'
                ELSE quote_ident(in_sort_field)
               END ||
           ' ' || CASE WHEN in_sort_ascending THEN 'ASC' ELSE 'DESC' END;

    IF in_limit > 0 THEN
        sql := sql || ' LIMIT ' || in_limit;
    ELSE
        sql := sql || ' LIMIT 25';
    END IF;

    IF in_offset > 0 THEN
        sql := sql || ' OFFSET ' || in_offset;
    END IF;
    -- Join onto the labels after paging to avoid them bloating the row count
    sql := sql || ' ) as job LEFT JOIN public.label lbl ON lbl.partition_id = job.partition_id '
        || 'AND lbl.job_id = job.job_id';
    sql := sql || ' ORDER BY ' ||
           CASE WHEN in_sort_label IS NOT NULL AND in_sort_label != ''
                    THEN '(SELECT value FROM label l WHERE job.partition_id = l.partition_id AND job.job_id = l.job_id AND l.label = ' ||
                         quote_literal(in_sort_label) || ')'
                ELSE quote_ident(in_sort_field)
               END ||
           ' ' || CASE WHEN in_sort_ascending THEN 'ASC' ELSE 'DESC' END;

    -- Create temporary table as a base to update the job progress
    EXECUTE 'CREATE TEMPORARY TABLE get_job_temp ON COMMIT DROP AS ' || sql;

    ALTER TABLE get_job_temp ADD COLUMN id SERIAL PRIMARY KEY;

    -- Create an array of job_id(s) based on the get_job_temp table
    jobIdArray := ARRAY(SELECT DISTINCT (jt.job_id) FROM get_job_temp jt ORDER BY jt.job_id);

    -- Check that the array is not empty
    IF array_length(jobIdArray, 1) > 0 THEN

        FOREACH jobId IN ARRAY jobIdArray LOOP
                -- Take out an exclusive update lock on the job row
                PERFORM NULL FROM job j
                WHERE j.partition_id = in_partition_id
                  AND j.job_id = jobId
                    FOR UPDATE;

                -- Process outstanding job updates
                PERFORM internal_update_job_progress(in_partition_id, jobId);
                UPDATE get_job_temp nt SET
                                           status = j.status,
                                           percentage_complete = j.percentage_complete
                FROM job j
                WHERE nt.job_id = j.job_id;
            END LOOP;

    END IF;

    -- Return the new table created
    RETURN QUERY
        SELECT at.job_id,
               at.name,
               at.description,
               at.data,
               at.create_date,
               at.last_update_date,
               at.status,
               at.percentage_complete,
               at.failure_details,
               CAST('WORKER' AS CHAR(6)) AS actionType,
               at.label,
               at.value
        FROM get_job_temp at
        ORDER BY at.id;
END
$function$
;

CREATE OR REPLACE FUNCTION public.get_jobs_count(in_partition_id character varying, in_job_id_starts_with character varying, in_status_type character varying, in_filter character varying)
    RETURNS TABLE(row_count bigint)
    LANGUAGE plpgsql
    STABLE
AS $function$
DECLARE
    sql VARCHAR;
    escapedJobIdStartsWith VARCHAR;
    whereOrAnd VARCHAR(7) = ' WHERE ';
    andConst CONSTANT VARCHAR(5) = ' AND ';

BEGIN
    -- Returns the number of job definitions in the system matching whatever criteria is specified:
    --   If the in_job_id param is specified, only those rows starting with that param will be returned.
    --   If the in_status_type param is
    --      NotCompleted - only those results with statuses other than Completed will be returned;
    --      Completed - only those results with Completed status will be returned;
    --      Inactive - only those results with inactive statuses (i.e. Completed, Failed, Cancelled) will be returned;
    --      NotFinished - only those results with unfinished statuses (ie. Active, Paused, Waiting) will be returned;
    --      Anything else returns all statuses.
    sql := $q$SELECT COUNT(job.job_id) FROM job$q$;

    sql := sql || whereOrAnd || ' partition_id = ' || quote_literal(in_partition_id);
    whereOrAnd := andConst;

    IF in_job_id_starts_with IS NOT NULL AND in_job_id_starts_with != '' THEN
        escapedJobIdStartsWith = replace(replace(quote_literal(in_job_id_starts_with), '_', '\_'), '%', '\%');
        escapedJobIdStartsWith = left(escapedJobIdStartsWith, char_length(escapedJobIdStartsWith) - 1) || '%''';
        sql := sql || whereOrAnd || ' job_id LIKE ' || escapedJobIdStartsWith;
        whereOrAnd := andConst;
    END IF;

    IF in_status_type IS NOT NULL THEN
        IF in_status_type = 'NotCompleted' THEN
            sql := sql || whereOrAnd || $q$ status IN ('Active', 'Paused', 'Waiting', 'Cancelled', 'Failed')$q$;
            whereOrAnd := andConst;
        ELSIF in_status_type = 'Completed' THEN
            sql := sql || whereOrAnd || $q$ status IN ('Completed')$q$;
            whereOrAnd := andConst;
        ELSIF in_status_type = 'Inactive' THEN
            sql := sql || whereOrAnd || $q$ status IN ('Completed', 'Cancelled', 'Failed')$q$;
            whereOrAnd := andConst;
        ELSIF in_status_type = 'NotFinished' THEN
            sql := sql || whereOrAnd || $q$ status IN ('Active', 'Paused', 'Waiting')$q$;
            whereOrAnd := andConst;
        END IF;
    END IF;

    IF in_filter IS NOT NULL THEN
        sql := sql || whereOrAnd || in_filter;
        whereOrAnd := andConst;
    END IF;

    RETURN QUERY EXECUTE sql;
END
$function$
;

CREATE OR REPLACE FUNCTION public.internal_cleanup_completed_subtask_report(in_partition_id character varying, in_job_id character varying)
    RETURNS void
    LANGUAGE plpgsql
AS $function$
BEGIN
    -- Delete all rows from completed_subtask_report table matching the job and partition provided
    DELETE FROM completed_subtask_report
    WHERE partition_id = in_partition_id
      AND job_id = in_job_id;
END
$function$
;

CREATE OR REPLACE FUNCTION public.internal_create_job(in_partition_id character varying, in_job_id character varying, in_name character varying, in_description text, in_data text, in_delay integer, in_job_hash integer, in_labels character varying[] DEFAULT NULL::character varying[])
    RETURNS boolean
    LANGUAGE plpgsql
AS $function$
DECLARE
    t VARCHAR(255)[];
BEGIN

    INSERT INTO public.job(
        partition_id,
        job_id,
        name,
        description,
        data,
        create_date,
        last_update_date,
        status,
        percentage_complete,
        failure_details,
        delay,
        job_hash
    ) VALUES (
                 in_partition_id,
                 in_job_id,
                 in_name,
                 in_description,
                 in_data,
                 now() AT TIME ZONE 'UTC',
                 now() AT TIME ZONE 'UTC',
                 'Waiting',
                 0.00,
                 NULL,
                 in_delay,
                 in_job_hash
             );

    IF in_labels IS NOT NULL AND in_labels != '{}' THEN
        FOREACH t SLICE 1 IN ARRAY in_labels LOOP
                INSERT INTO public.label(
                    partition_id,
                    job_id,
                    label,
                    value
                )
                SELECT in_partition_id, in_job_id, t[1], t[2];
            END LOOP;
    END IF;

    RETURN TRUE;

EXCEPTION WHEN unique_violation THEN

    -- updating the job is disallowed, so on conflict we can only succeed if the hash indicates the
    -- provided job is exactly the same as the existing job
    IF EXISTS(
            SELECT 1 FROM job
            WHERE job.partition_id = in_partition_id
              AND job.job_id = in_job_id
              AND job.job_hash = in_job_hash
        ) THEN
        RETURN FALSE;
    ELSE
        RAISE;
    END IF;

END
$function$
;

CREATE OR REPLACE FUNCTION public.internal_create_task_table(in_table_name character varying)
    RETURNS void
    LANGUAGE plpgsql
AS $function$
BEGIN
    -- Create a new task table
    EXECUTE format($FORMAT_STR$
        CREATE TABLE IF NOT EXISTS %I
        (
            subtask_id INT NOT NULL,
            create_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
            status job_status NOT NULL DEFAULT 'Waiting'::job_status,
            percentage_complete DOUBLE PRECISION NOT NULL DEFAULT 0.00,
            failure_details TEXT,
            is_final BOOLEAN NOT NULL DEFAULT FALSE,
            CONSTRAINT %I PRIMARY KEY (subtask_id)
        )
    $FORMAT_STR$, in_table_name, 'pk_' || in_table_name);
END
$function$
;

CREATE OR REPLACE FUNCTION public.internal_does_table_exist(in_table_name character varying)
    RETURNS boolean
    LANGUAGE sql
    STABLE
AS $function$
SELECT internal_to_regclass(quote_ident(in_table_name)) IS NOT NULL;
$function$
;

CREATE OR REPLACE FUNCTION public.internal_drop_task_tables(in_partition_id character varying, in_task_id character varying)
    RETURNS void
    LANGUAGE plpgsql
AS $function$
DECLARE
    task_table_ident TEXT;
    subtask_suffix TEXT;
    task_table_name VARCHAR;

BEGIN
    -- Put together the task table identifier
    task_table_name := internal_get_task_table_name(in_partition_id, in_task_id);
    task_table_ident = quote_ident(task_table_name);

    -- Check if the table exists
    IF internal_to_regclass(task_table_ident) IS NOT NULL THEN
        -- Drop the referenced subtask tables
        FOR subtask_suffix IN
            EXECUTE $ESC$SELECT '.' || subtask_id || CASE WHEN is_final THEN '*' ELSE '' END AS subtask_suffix FROM $ESC$ || task_table_ident
            LOOP
                PERFORM internal_drop_task_tables(in_partition_id, in_task_id || subtask_suffix);
            END LOOP;

        -- Insert table name to be dropped later
        PERFORM internal_insert_delete_log(task_table_name);
    END IF;
END
$function$
;

CREATE OR REPLACE FUNCTION public.internal_get_job_id(in_task_id character varying)
    RETURNS character varying
    LANGUAGE sql
    IMMUTABLE
AS $function$
SELECT SUBSTRING(in_task_id FROM '^[^\.]*');
$function$
;

CREATE OR REPLACE FUNCTION public.internal_get_options(in_options character varying)
    RETURNS TABLE(option character varying)
    LANGUAGE sql
    IMMUTABLE
AS $function$
SELECT DISTINCT * FROM regexp_split_to_table(in_options, ',') option
WHERE option <> '';
$function$
;

CREATE OR REPLACE FUNCTION public.internal_get_parent_task_id(in_task_id character varying)
    RETURNS character varying
    LANGUAGE sql
    IMMUTABLE
AS $function$
SELECT SUBSTRING(in_task_id FROM '^(.*)\.');
$function$
;

CREATE OR REPLACE FUNCTION public.internal_get_prereq_job_id_options(job_id_with_opts character varying)
    RETURNS TABLE(job_id character varying, options_string character varying, precreated boolean, unknown_options boolean)
    LANGUAGE sql
    IMMUTABLE
AS $function$
WITH job_id_with_opts_tbl AS
         (
             SELECT SUBSTRING(job_id_with_opts FROM '[^,]*') AS job_id,
                    SUBSTRING(job_id_with_opts FROM ',(.*)') AS options_string
         )
SELECT job_id,
       options_string,
       EXISTS(SELECT NULL FROM internal_get_options(options_string) WHERE option = 'pc'),
       EXISTS(SELECT NULL FROM internal_get_options(options_string) WHERE option NOT IN ('pc'))
FROM job_id_with_opts_tbl;
$function$
;

CREATE OR REPLACE FUNCTION public.internal_get_subtask_count(in_task_table_name character varying)
    RETURNS integer
    LANGUAGE plpgsql
    STABLE
AS $function$
DECLARE
    subtask_count INT;

BEGIN
    EXECUTE format($FORMAT_STR$
        SELECT(
            SELECT subtask_id
            FROM %1$I
            WHERE is_final
        )
    $FORMAT_STR$, in_task_table_name)
        INTO STRICT subtask_count;

    RETURN subtask_count;
END
$function$
;

CREATE OR REPLACE FUNCTION public.internal_get_subtask_id(in_task_id character varying)
    RETURNS integer
    LANGUAGE sql
    IMMUTABLE
AS $function$
SELECT CAST(SUBSTRING(in_task_id FROM '\.(\d*)\*?$') AS INT);
$function$
;

CREATE OR REPLACE FUNCTION public.internal_get_task_status(in_task_table_name character varying)
    RETURNS TABLE(status job_status, percentage_complete double precision, failure_details text)
    LANGUAGE plpgsql
    STABLE
AS $function$
DECLARE
    v_subtask_count INT;
    v_completed_task_count INT;

BEGIN
    -- Get the total number of subtasks
    v_subtask_count = internal_get_subtask_count(in_task_table_name);

    -- Get the number of completed subtasks
    EXECUTE format($FORMAT_STR$
        SELECT COUNT(*) FROM %1$I
        WHERE status = 'Completed';
    $FORMAT_STR$, in_task_table_name)
        INTO STRICT v_completed_task_count;

    -- Return the overall status
    IF v_subtask_count = v_completed_task_count THEN
        -- All the subtasks are completed so just return this
        RETURN QUERY SELECT 'Completed'::job_status, CAST(100.00 AS DOUBLE PRECISION), CAST(NULL AS TEXT);
    ELSE
        -- Add an extra 'Active' row in to represent missing rows
        -- (arguably we should only do this if some rows are already complete)
        RETURN QUERY EXECUTE format($FORMAT_STR$
            SELECT status, percentage_complete, failure_details FROM
            (
                SELECT status,
                CASE status
                    WHEN 'Failed' THEN 1
                    WHEN 'Cancelled' THEN 2
                    WHEN 'Paused' THEN 3
                    WHEN 'Active' THEN 4
                    WHEN 'Waiting' THEN 5
                    WHEN 'Completed' THEN 6
                END AS importance
                FROM
                (
                    SELECT status
                    FROM %1$I
                    UNION ALL
                    SELECT 'Active'
                ) tbl
                ORDER BY importance
                LIMIT 1
            ) tbl1
            CROSS JOIN
            (
                SELECT LEAST(COALESCE(SUM(percentage_complete) / COALESCE($1, MAX(subtask_id) + 1), 0.00), 99.7) AS percentage_complete
                FROM %1$I
            ) tbl2
            CROSS JOIN
            (
                SELECT string_agg(failure_details, E'\n') AS failure_details
                FROM %1$I
                WHERE status = 'Failed'
            ) tbl3
        $FORMAT_STR$, in_task_table_name)
            USING v_subtask_count;
    END IF;
END
$function$
;

CREATE OR REPLACE FUNCTION public.internal_get_task_table_name(in_partition_id character varying, in_task_id character varying)
    RETURNS character varying
    LANGUAGE sql
    STABLE
AS $function$
SELECT 'task_' || regexp_replace(
        in_task_id,
        '^[^\.]*',
        (SELECT identity::text FROM job WHERE partition_id = in_partition_id AND job_id = internal_get_job_id(in_task_id)));
$function$
;

CREATE OR REPLACE FUNCTION public.internal_has_dependent_jobs(in_partition_id character varying, in_job_id character varying)
    RETURNS boolean
    LANGUAGE sql
    STABLE
AS $function$
    -- Checks if job has any dependency
SELECT EXISTS(
               SELECT NULL FROM job_dependency
               WHERE partition_id = in_partition_id
                 AND dependent_job_id = in_job_id
           );
$function$
;

CREATE OR REPLACE FUNCTION public.internal_insert_delete_log(task_table_name character varying)
    RETURNS void
    LANGUAGE plpgsql
AS $function$
BEGIN
    INSERT INTO public.delete_log VALUES (task_table_name);
END
$function$
;

CREATE OR REPLACE FUNCTION public.internal_is_final_task(in_task_id character varying)
    RETURNS boolean
    LANGUAGE sql
    IMMUTABLE
AS $function$
SELECT SUBSTRING(in_task_id FROM '\..*\*$') IS NOT NULL;
$function$
;

CREATE OR REPLACE FUNCTION public.internal_is_task_completed(in_partition_id character varying, in_task_id character varying)
    RETURNS boolean
    LANGUAGE plpgsql
    STABLE
AS $function$
DECLARE
    v_parent_task_id VARCHAR(58);
    v_parent_task_table VARCHAR(63);
    v_is_task_completed BOOLEAN;

BEGIN
    -- Get the parent task id
    v_parent_task_id = internal_get_parent_task_id(in_task_id);

    -- Check if we are dealing with the top level job or a subtask
    IF v_parent_task_id IS NULL THEN

        -- Check the status in the job table
        -- If the job isn't present then throw an error
        SELECT status = 'Completed'
        INTO STRICT v_is_task_completed
        FROM job
        WHERE partition_id = in_partition_id
          AND job_id = in_task_id;

        -- Check if the parent task has completed
    ELSIF internal_is_task_completed(in_partition_id, v_parent_task_id) THEN

        -- Since the parent task has completed then we can say that this task has
        v_is_task_completed = TRUE;

    ELSE

        -- Put together the parent task table name
        v_parent_task_table = internal_get_task_table_name(in_partition_id, v_parent_task_id);

        -- Check if the parent task table exists
        IF internal_does_table_exist(v_parent_task_table) THEN

            -- Lookup the status in the parent task table
            EXECUTE format($FORMAT_STR$
                SELECT COALESCE((
                    SELECT status = 'Completed'
                    FROM %1$I
                    WHERE subtask_id = $1
                ), FALSE)
            $FORMAT_STR$, v_parent_task_table)
                USING internal_get_subtask_id(in_task_id)
                INTO STRICT v_is_task_completed;

        ELSE

            -- Since the parent task table doesn't exist, and we established earlier that it has not completed,
            -- we can say that this task has not completed.
            v_is_task_completed = FALSE;

        END IF;

    END IF;

    -- Return whether the task has been completed
    RETURN v_is_task_completed;

END
$function$
;

CREATE OR REPLACE FUNCTION public.internal_process_dependent_jobs(in_partition_id character varying, in_job_id character varying)
    RETURNS TABLE(partition_id character varying, job_id character varying, task_classifier character varying, task_api_version integer, task_data bytea, task_pipe character varying, target_pipe character varying)
    LANGUAGE plpgsql
AS $function$
BEGIN
    -- Get a list of jobs that depend on in_job_id
    CREATE TEMPORARY TABLE tmp_dependent_jobs
        ON COMMIT DROP
    AS
    SELECT j.job_id, j.delay
    FROM job_dependency jd
             INNER JOIN job j ON j.partition_id = jd.partition_id AND j.job_id = jd.job_id
    WHERE jd.partition_id = in_partition_id
      AND jd.dependent_job_id = in_job_id;

    -- Ensure that no other `job_dependency` deletion can run until we've committed, so we don't
    -- miss the deletion of the last dependency.  Lock ALL possibly conflicting rows up-front to
    -- avoid deadlocks.
    PERFORM NULL FROM job_dependency AS jd
    WHERE jd.partition_id = in_partition_id
      AND jd.job_id IN (SELECT tmp_dependent_jobs.job_id FROM tmp_dependent_jobs)
    ORDER BY jd.partition_id, jd.job_id, jd.dependent_job_id
        FOR UPDATE;

    -- Remove corresponding dependency related rows for jobs that can be processed immediately
    DELETE
    FROM job_dependency AS jd
    WHERE jd.partition_id = in_partition_id
      AND jd.dependent_job_id = in_job_id;

    --Set the eligible_to_run_date for jobs with a delay, these will be picked up by scheduled executor
    UPDATE job_task_data
    SET eligible_to_run_date = now() AT TIME ZONE 'UTC' + (tmp_dependent_jobs.delay * interval '1 second')
    FROM tmp_dependent_jobs
    WHERE
        job_task_data.eligible_to_run_date IS NULL
      AND job_task_data.partition_id = in_partition_id
      AND tmp_dependent_jobs.job_id = job_task_data.job_id
      AND tmp_dependent_jobs.delay <> 0
      AND NOT EXISTS (
            SELECT job_dependency.job_id FROM job_dependency
            WHERE job_dependency.partition_id = job_task_data.partition_id
              AND job_dependency.job_id = job_task_data.job_id
        );

    -- Return jobs with no delay that we can now run and delete the tasks
    RETURN QUERY
        WITH del_result AS (DELETE
            FROM job_task_data jtd
                WHERE jtd.partition_id = in_partition_id AND jtd.job_id IN (
                    SELECT jtd.job_id
                    FROM job_task_data jtd
                             INNER JOIN tmp_dependent_jobs dp ON dp.job_id = jtd.job_id
                    WHERE dp.delay = 0 AND NOT EXISTS (
                            SELECT job_dependency.job_id
                            FROM job_dependency
                            WHERE job_dependency.partition_id = in_partition_id
                              AND job_dependency.job_id = dp.job_id
                        )
                )
                RETURNING
                    jtd.partition_id,
                    jtd.job_id,
                    jtd.task_classifier,
                    jtd.task_api_version,
                    jtd.task_data,
                    jtd.task_pipe,
                    jtd.target_pipe
        )
        SELECT
            del_result.partition_id,
            del_result.job_id,
            del_result.task_classifier,
            del_result.task_api_version,
            del_result.task_data,
            del_result.task_pipe,
            del_result.target_pipe
        FROM del_result;

END
$function$
;

CREATE OR REPLACE FUNCTION public.internal_process_failed_dependent_jobs(in_partition_id character varying, in_job_id character varying, in_failure_details text)
    RETURNS void
    LANGUAGE plpgsql
AS $function$
DECLARE
    failure_details_var TEXT;
BEGIN
    CREATE TEMPORARY TABLE tmp_dependent_jobs
        ON COMMIT DROP
    AS
    WITH RECURSIVE all_job_dependencies AS (
        SELECT partition_id, dependent_job_id, job_id
        FROM job_dependency
        WHERE partition_id = in_partition_id AND dependent_job_id = in_job_id
        UNION
        SELECT adj.partition_id, adj.dependent_job_id, jd.job_id
        FROM all_job_dependencies adj
                 INNER JOIN job_dependency jd ON adj.partition_id = jd.partition_id AND adj.job_id = jd.dependent_job_id
        WHERE adj.partition_id = in_partition_id
    )
    SELECT DISTINCT job_id FROM all_job_dependencies;

    -- Ensure that no other `job_dependency` deletion can run until we've committed.
    -- Lock ALL possibly conflicting rows up-front to avoid deadlocks.
    PERFORM NULL FROM job_dependency AS jd
    WHERE jd.partition_id = in_partition_id
      AND jd.job_id IN (SELECT tmp_dependent_jobs.job_id FROM tmp_dependent_jobs)
    ORDER BY jd.partition_id, jd.job_id, jd.dependent_job_id
        FOR UPDATE;

    -- Ensure that no other `job` updates can run on these jobs until we've committed
    -- Lock ALL possibly conflicting rows up-front to avoid deadlocks.
    PERFORM NULL FROM job AS j
    WHERE j.partition_id = in_partition_id
      AND j.job_id IN (SELECT tmp_dependent_jobs.job_id FROM tmp_dependent_jobs)
    ORDER BY j.partition_id, j.job_id
        FOR UPDATE;

    failure_details_var = '{"root_failure": "' || in_partition_id || ':' || in_job_id || '", "failure_details": ' || in_failure_details || '}';

    UPDATE job AS j
    SET status = 'Failed', percentage_complete = 0.00, failure_details = failure_details_var, last_update_date = now() AT TIME ZONE 'UTC'
    WHERE j.job_id IN (SELECT tmp_dependent_jobs.job_id FROM tmp_dependent_jobs) AND partition_id = in_partition_id;

    DELETE
    FROM job_dependency AS jd
    WHERE jd.partition_id = in_partition_id
      AND jd.job_id IN (SELECT tmp_dependent_jobs.job_id FROM tmp_dependent_jobs);
END;
$function$
;

CREATE OR REPLACE FUNCTION public.internal_report_task_status(in_partition_id character varying, in_task_id character varying, in_status job_status, in_percentage_complete double precision, in_failure_details text)
    RETURNS void
    LANGUAGE plpgsql
AS $function$
DECLARE
    v_parent_task_id VARCHAR(70);
    v_parent_task_table VARCHAR(63);

BEGIN
    -- Ignore the status report if the task has already been completed
    IF internal_is_task_completed(in_partition_id, in_task_id) THEN
        RETURN;
    END IF;

    -- If the task is being marked completed, then drop any subtask tables
    IF in_status = 'Completed' THEN
        PERFORM internal_drop_task_tables(in_partition_id, in_task_id);
    END IF;

    -- Get the parent task id
    v_parent_task_id = internal_get_parent_task_id(in_task_id);

    -- Check if we are dealing with the top level job or a subtask
    IF v_parent_task_id IS NULL THEN
        -- Mark up the job status in the job table
        UPDATE job
        SET status = internal_resolve_status(status, in_status),
            percentage_complete = round(in_percentage_complete::numeric, 2),
            failure_details = in_failure_details,
            last_update_date = now() AT TIME ZONE 'UTC'
        WHERE partition_id = in_partition_id
          AND job_id = in_task_id;
    ELSE
        -- Put together the parent task table name
        v_parent_task_table = internal_get_task_table_name(in_partition_id, v_parent_task_id);

        -- Create the parent task table if necessary
        PERFORM internal_create_task_table(v_parent_task_table);

        -- Mark up the task status in the parent task table
        PERFORM internal_upsert_into_task_table(
                v_parent_task_table,
                in_task_id,
                in_status,
                in_percentage_complete,
                in_failure_details);

        -- Get the overall status of the parent task and recursively call into this function to update the parent tasks
        PERFORM internal_report_task_status(
                in_partition_id,
                v_parent_task_id,
                status,
                percentage_complete,
                failure_details)
        FROM internal_get_task_status(v_parent_task_table);
    END IF;
END
$function$
;

CREATE OR REPLACE FUNCTION public.internal_resolve_status(in_current_job_status job_status, in_proposed_job_status job_status)
    RETURNS job_status
    LANGUAGE sql
    IMMUTABLE
AS $function$
WITH priority_tbl(priority, status) AS
         (
             SELECT 1, CAST('Waiting' AS job_status) UNION ALL
             SELECT 2, 'Active'    UNION ALL
             SELECT 3, 'Paused'    UNION ALL
             SELECT 4, 'Failed'    UNION ALL
             SELECT 5, 'Cancelled' UNION ALL
             SELECT 6, 'Completed'
         )
SELECT CASE WHEN p1.priority > p2.priority THEN p1.status ELSE p2.status END
FROM priority_tbl p1
         INNER JOIN priority_tbl p2 ON p1.status = in_current_job_status AND p2.status = in_proposed_job_status;
$function$
;

CREATE OR REPLACE FUNCTION public.internal_to_regclass(rel_name character varying)
    RETURNS regclass
    LANGUAGE plpgsql
    STABLE
AS $function$
BEGIN
    -- Add backwards compatibility support for to_regclass argument type change introduced in Postgres 9.6.
    IF current_setting('server_version_num')::INT < 90600 THEN
        RETURN to_regclass(rel_name::cstring);
    ELSE
        RETURN to_regclass(rel_name::text);
    END IF;
END
$function$
;

CREATE OR REPLACE FUNCTION public.internal_update_job_progress(in_partition_id character varying, in_job_id anyelement)
    RETURNS TABLE(partition_id character varying, job_id character varying, status job_status)
    LANGUAGE plpgsql
AS $function$
DECLARE
    taskId VARCHAR(70);
    subtask_array VARCHAR[];
    type_param regtype = pg_typeof(in_job_id);
    job_id_array VARCHAR[];

BEGIN
    -- Check in_job_id type and return an exception if invalid
    -- If in_job_id is an array, its value is passed onto job_id_array
    -- If in_job_id is a varchar, it increments job_id_array
    IF type_param = 'character varying[]'::regtype THEN
        job_id_array = in_job_id;
    ELSEIF type_param ='character varying'::regtype THEN
        job_id_array = array_agg(in_job_id);
    ELSE
        RAISE EXCEPTION 'Invalid type for in_job_id: %', type_param;
    END IF;

    -- Deleting the completed subtasks for that job from completed_subtask_report table
    -- Add the results to subtask_array
    WITH completed_subtask AS (
        DELETE FROM completed_subtask_report csr
            WHERE csr.partition_id = in_partition_id
                AND csr.job_id = ANY(job_id_array)
            RETURNING csr.task_id
    )

    SELECT array_agg(task_id)
    FROM completed_subtask
    INTO subtask_array;

    -- Loop through subtask_array and update the job percentage_complete
    IF subtask_array IS NOT NULL THEN
        FOREACH taskId IN ARRAY subtask_array
            LOOP
                PERFORM internal_report_task_status(in_partition_id, taskId , 'Completed', 100.00, NULL);
            END LOOP;
    END IF;

    RETURN QUERY
        SELECT j.partition_id, j.job_id, j.status
        FROM job j;
END
$function$
;

CREATE OR REPLACE FUNCTION public.internal_upsert_into_task_table(in_task_table_name character varying, in_task_id character varying, in_status job_status, in_percentage_complete double precision, in_failure_details text)
    RETURNS void
    LANGUAGE plpgsql
AS $function$
BEGIN
    -- ASSERT support was only introduced in PostgresSQL 9.5
    --ASSERT in_task_table_name = 'task_' || internal_get_parent_task_id(in_task_id),
    --       'Invalid arguments passed to internal_upsert_into_task_table';

    EXECUTE format($FORMAT_STR$
        WITH upsert AS
        (
            UPDATE %1$I
            SET status = $3,
                percentage_complete = $4,
                failure_details = $5
            WHERE subtask_id = $2
            RETURNING NULL
        )
        INSERT INTO %1$I(subtask_id, create_date, status, percentage_complete, failure_details, is_final)
        SELECT $2, now() AT TIME ZONE 'UTC', $3, $4, $5, internal_is_final_task($1)
        WHERE NOT EXISTS(SELECT * FROM upsert);
    $FORMAT_STR$, in_task_table_name)
        USING in_task_id, internal_get_subtask_id(in_task_id), in_status, in_percentage_complete, in_failure_details;
END
$function$
;

CREATE OR REPLACE FUNCTION public.pause_job(in_partition_id character varying, in_job_id character varying)
    RETURNS void
    LANGUAGE plpgsql
AS $function$
DECLARE
    v_job_status job_status;

BEGIN
    -- Raise exception if job identifier has not been specified
    IF in_job_id IS NULL OR in_job_id = '' THEN
        RAISE EXCEPTION 'The job identifier has not been specified' USING ERRCODE = '02000'; -- sqlstate no data;
    END IF;

    -- Only support Pause operation on jobs with current status 'Active', 'Paused' or 'Waiting'
    SELECT status INTO v_job_status
    FROM job
    WHERE partition_id = in_partition_id
      AND job_id = in_job_id
        FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'job_id {%} not found', in_job_id USING ERRCODE = 'P0002'; -- sqlstate no_data_found
    END IF;

    IF v_job_status NOT IN ('Active', 'Paused', 'Waiting') THEN
        RAISE EXCEPTION 'job_id {%} cannot be paused as it has a status of {%}. '
            'Only jobs with a status of Active or Waiting can be paused.',
            in_job_id, v_job_status USING ERRCODE = '02000';
    END IF;

    -- Mark the job paused in the job table
    UPDATE job
    SET status = 'Paused', last_update_date = now() AT TIME ZONE 'UTC'
    WHERE partition_id = in_partition_id
      AND job_id = in_job_id
      AND status != 'Paused';
END
$function$
;

CREATE OR REPLACE FUNCTION public.report_complete(in_partition_id character varying, in_task_id character varying)
    RETURNS TABLE(partition_id character varying, job_id character varying, task_classifier character varying, task_api_version integer, task_data bytea, task_pipe character varying, target_pipe character varying)
    LANGUAGE plpgsql
AS $function$
DECLARE
    v_job_id VARCHAR(48);
    v_job_status job_status;

BEGIN
    -- Raise exception if task identifier has not been specified
    IF in_task_id IS NULL OR in_task_id = '' THEN
        RAISE EXCEPTION 'Task identifier has not been specified';
    END IF;

    -- Get the job id
    v_job_id = internal_get_job_id(in_task_id);

    -- Get the job status
    -- And take out an exclusive update lock on the job row
    SELECT status INTO v_job_status
    FROM job j
    WHERE j.partition_id = in_partition_id
      AND j.job_id = v_job_id
        FOR UPDATE;

    -- Check that the job hasn't been deleted, cancelled or completed
    IF NOT FOUND OR v_job_status IN ('Cancelled', 'Completed') THEN
        RETURN;
    END IF;

    -- Check if the job has dependencies
    IF internal_has_dependent_jobs(in_partition_id, v_job_id) THEN

        -- Update the task statuses in the tables
        PERFORM internal_report_task_status(in_partition_id, in_task_id, 'Completed', 100.00, NULL);

        -- If job has just completed, then return any jobs that can now be run
        IF internal_is_task_completed(in_partition_id, v_job_id) THEN
            -- Get a list of jobs that can run immediately and update the eligibility run date for others
            RETURN QUERY
                SELECT * FROM internal_process_dependent_jobs(in_partition_id, v_job_id);
        END IF;

    ELSE

        -- Insert values into completed_subtask_report table
        INSERT INTO completed_subtask_report (partition_id, job_id, task_id, report_date)
        VALUES (in_partition_id, v_job_id, in_task_id, now() AT TIME ZONE 'UTC');

    END IF;
END
$function$
;

CREATE OR REPLACE FUNCTION public.report_complete_bulk(in_partition_id character varying, in_job_id character varying, in_task_ids character varying[])
    RETURNS TABLE(partition_id character varying, job_id character varying, task_classifier character varying, task_api_version integer, task_data bytea, task_pipe character varying, target_pipe character varying)
    LANGUAGE plpgsql
AS $function$
DECLARE
    v_job_status job_status;
    v_task_id VARCHAR(70);

BEGIN
    -- Raise exception if task identifier has not been specified
    IF array_length(in_task_ids, 1) = 0 THEN
        RAISE EXCEPTION 'Task identifier has not been specified';
    END IF;

    -- Get the job status
    -- And take out an exclusive update lock on the job row
    SELECT status INTO v_job_status
    FROM job j
    WHERE j.partition_id = in_partition_id
      AND j.job_id = in_job_id
        FOR UPDATE;

    -- Check that the job hasn't been deleted, cancelled or completed
    IF NOT FOUND OR v_job_status IN ('Cancelled', 'Completed') THEN
        RETURN;
    END IF;

    -- Check if the job has dependencies
    IF internal_has_dependent_jobs(in_partition_id, in_job_id) THEN

        -- Update the task statuses in the tables
        FOREACH v_task_id IN ARRAY in_task_ids
            LOOP
                PERFORM internal_report_task_status(in_partition_id, v_task_id, 'Completed', 100.00, NULL);
            END LOOP;

        -- If job has just completed, then return any jobs that can now be run
        IF internal_is_task_completed(in_partition_id, in_job_id) THEN
            -- Get a list of jobs that can run immediately and update the eligibility run date for others
            RETURN QUERY
                SELECT * FROM internal_process_dependent_jobs(in_partition_id, in_job_id);
        END IF;

    ELSE

        -- Insert all the incoming tasks into the completed_subtask_report table
        INSERT INTO completed_subtask_report (partition_id, job_id, task_id, report_date)
        SELECT in_partition_id, in_job_id, x.task_id, now() AT TIME ZONE 'UTC'
        FROM unnest(in_task_ids) AS x(task_id);

    END IF;
END
$function$
;

CREATE OR REPLACE FUNCTION public.report_failure(in_partition_id character varying, in_task_id character varying, in_failure_details text, in_propagate_failures boolean)
    RETURNS void
    LANGUAGE plpgsql
AS $function$
DECLARE
    v_job_id VARCHAR(48);
    v_job_status job_status;

BEGIN
    -- Raise exception if task identifier has not been specified
    IF in_task_id IS NULL OR in_task_id = '' THEN
        RAISE EXCEPTION 'Task identifier has not been specified';
    END IF;

    -- Raise exception if failure details have not been specified
    IF in_failure_details IS NULL OR in_failure_details = '' THEN
        RAISE EXCEPTION 'Failure details have not been specified';
    END IF;

    -- Get the job id
    v_job_id = internal_get_job_id(in_task_id);

    -- Get the job status
    -- And take out an exclusive update lock on the job row
    SELECT status INTO v_job_status
    FROM job AS j
    WHERE j.partition_id = in_partition_id
      AND j.job_id = v_job_id
        FOR UPDATE;

    -- Check that the job hasn't been deleted, cancelled or completed
    IF NOT FOUND OR v_job_status IN ('Cancelled', 'Completed') THEN
        RETURN;
    END IF;

    -- Update the task statuses in the tables
    PERFORM internal_report_task_status(in_partition_id, in_task_id, 'Failed', 0.00, in_failure_details);

    IF in_propagate_failures THEN
        PERFORM internal_process_failed_dependent_jobs(in_partition_id, v_job_id, in_failure_details);
    END IF;
END
$function$
;

CREATE OR REPLACE FUNCTION public.report_progress(in_partition_id character varying, in_task_id character varying, in_percentage_complete double precision)
    RETURNS void
    LANGUAGE plpgsql
AS $function$
DECLARE
    v_job_id VARCHAR(48);
    v_job_status job_status;

BEGIN
    -- Raise exception if task identifier has not been specified
    IF in_task_id IS NULL OR in_task_id = '' THEN
        RAISE EXCEPTION 'Task identifier has not been specified';
    END IF;

    -- Raise exception if task progress is < 0 or > 100
    IF in_percentage_complete < 0 OR in_percentage_complete > 100 THEN
        RAISE EXCEPTION 'Invalid in_percentage_complete %', in_percentage_complete USING ERRCODE = '22023'; -- invalid_parameter_value
    END IF;

    -- Get the job id
    v_job_id = internal_get_job_id(in_task_id);

    -- Get the job status
    -- And take out an exclusive update lock on the job row
    SELECT status INTO v_job_status
    FROM job
    WHERE partition_id = in_partition_id
      AND job_id = v_job_id
        FOR UPDATE;

    -- Check that the job hasn't been deleted, cancelled or completed
    IF NOT FOUND OR v_job_status IN ('Cancelled', 'Completed') THEN
        RETURN;
    END IF;

    -- Update the task tables
    PERFORM internal_report_task_status(in_partition_id, in_task_id, 'Active', LEAST(in_percentage_complete, 99.9), NULL);
END
$function$
;

CREATE OR REPLACE FUNCTION public.resume_job(in_partition_id character varying, in_job_id character varying)
    RETURNS void
    LANGUAGE plpgsql
AS $function$
DECLARE
    v_job_status job_status;

BEGIN
    -- Raise exception if job identifier has not been specified
    IF in_job_id IS NULL OR in_job_id = '' THEN
        RAISE EXCEPTION 'The job identifier has not been specified' USING ERRCODE = '02000'; -- sqlstate no data;
    END IF;

    -- Only support Resume operation on jobs with current status 'Paused'
    SELECT status INTO v_job_status
    FROM job
    WHERE partition_id = in_partition_id
      AND job_id = in_job_id
        FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'job_id {%} not found', in_job_id USING ERRCODE = 'P0002'; -- sqlstate no_data_found
    END IF;

    IF v_job_status NOT IN ('Active', 'Paused') THEN
        RAISE EXCEPTION 'job_id {%} cannot be resumed as it has a status of {%}. Only jobs with a status of Paused can be resumed.',
            in_job_id, v_job_status USING ERRCODE = '02000';
    END IF;

    -- Mark the job Active in the job table
    UPDATE job
    SET status = 'Active', last_update_date = now() AT TIME ZONE 'UTC'
    WHERE partition_id = in_partition_id
      AND job_id = in_job_id
      AND status != 'Active';
END
$function$
;
