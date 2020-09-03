--
-- Copyright 2016-2020 Micro Focus or one of its affiliates.
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
CREATE OR REPLACE FUNCTION get_jobs(
    in_partition_id VARCHAR(40),
    in_job_id_starts_with VARCHAR(48),
    in_status_type VARCHAR(20),
    in_limit INT,
    in_offset INT,
    in_sort_field VARCHAR(20),
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
    label_value VARCHAR(255)
)
LANGUAGE plpgsql STABLE
AS $$
DECLARE
    sql VARCHAR;
    escapedJobIdStartsWith VARCHAR;
    whereOrAnd VARCHAR(7) = ' WHERE ';
    andConst CONSTANT VARCHAR(5) = ' AND ';

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
        SELECT job_lbl.job_id,
               job_lbl.name,
               job_lbl.description,
               job_lbl.data,
               job_lbl.create_date,
               job_lbl.last_update_date,
               job_lbl.status,
               job_lbl.percentage_complete,
               job_lbl.failure_details,
               job_lbl.actionType,
               job_lbl.label,
               job_lbl.value
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
               CAST('WORKER' AS CHAR(6)) AS actionType,
               lbl.label,
               lbl.value
        FROM public.job as job LEFT JOIN public.label lbl ON lbl.partition_id = job.partition_id
        AND lbl.job_id = job.job_id

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

    sql := sql || ' ORDER BY ' || quote_ident(in_sort_field) ||
        ' ' || CASE WHEN in_sort_ascending THEN 'ASC' ELSE 'DESC' END;

    IF in_limit > 0 THEN
        sql := sql || ' LIMIT ' || in_limit;
    ELSE
        sql := sql || ' LIMIT 25';
    END IF;

    IF in_offset > 0 THEN
        sql := sql || ' OFFSET ' || in_offset;
    END IF;
    sql := sql || ' ) as job_lbl';
    RETURN QUERY EXECUTE sql;
END
$$;
