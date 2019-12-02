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
CREATE OR REPLACE FUNCTION get_jobs(
    in_partition_id VARCHAR(40),
    in_job_id_starts_with VARCHAR(48),
    in_status_type VARCHAR(20),
    in_limit INT,
    in_offset INT,
    in_sort_field VARCHAR(20),
    in_sort_ascending BOOLEAN
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
    actionType CHAR(6)
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
        SELECT job.job_id,
               job.name,
               job.description,
               job.data,
               to_char(job.create_date, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'),
               to_char(job.last_update_date, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'),
               job.status,
               job.percentage_complete,
               job.failure_details,
               CAST('WORKER' AS CHAR(6)) AS actionType
        FROM job$q$;

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

    RETURN QUERY EXECUTE sql;
END
$$;