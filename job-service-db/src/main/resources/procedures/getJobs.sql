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
 *  Name: get_jobs
 *
 *  Description:
 *  Returns the list of job definitions in the system.
 *
 * in_sort_field: name of the column to sort by
 * in_sort_ascending: true to sort ascending, false to sort descending
 */
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
CREATE OR REPLACE FUNCTION get_jobs(
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
)
RETURNS TABLE(
    job_id VARCHAR(48),
    name VARCHAR(255),
    description TEXT,
    data TEXT,
    create_date TEXT,
    last_update_date TEXT,
    status job_status,
    percentage_complete DOUBLE PRECISION,
    failure_details TEXT,
    actionType CHAR(6),
    label VARCHAR(255),
    label_value VARCHAR(255),
    expiration_status job_status,
    operation expiration_operation,
    expiration_time VARCHAR(58)
)
LANGUAGE plpgsql VOLATILE
AS $$
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
               lbl.value,
               jep.job_status,
               jep.operation,
               jep.expiration_time
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
            sql := sql || whereOrAnd || $q$ status IN ('Active', 'Paused', 'Waiting', 'Cancelled', 'Failed', 'Expired')$q$;
            whereOrAnd := andConst;
        ELSIF in_status_type = 'Completed' THEN
            sql := sql || whereOrAnd || $q$ status IN ('Completed')$q$;
            whereOrAnd := andConst;
        ELSIF in_status_type = 'Inactive' THEN
            sql := sql || whereOrAnd || $q$ status IN ('Completed', 'Cancelled', 'Failed', 'Expired')$q$;
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
    -- Join onto the job_expiration_policy table
    sql := sql || ' LEFT JOIN public.job_expiration_policy jep ON jep.partition_id = job.partition_id AND jep.job_id = job.job_id';
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
               at.value,
               at.job_status,
               at.operation,
               CASE
                   WHEN RIGHT(at.expiration_time, 7) = '+SYSTEM' THEN
                         LEFT(at.expiration_time, length(at.expiration_time)-7)
                   ELSE at.expiration_time
                   END
                   expiry
        FROM get_job_temp at
        ORDER BY at.id;
END
$$;
