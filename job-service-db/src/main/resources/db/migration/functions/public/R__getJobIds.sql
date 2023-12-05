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
 *  Name: get_jobs
 *
 *  Description:
 *  Returns the list of job definitions in the system.
 *
 * in_sort_field: name of the column to sort by
 * in_sort_ascending: true to sort ascending, false to sort descending
 */
CREATE OR REPLACE FUNCTION get_job_ids(
    in_partition_id VARCHAR(40),
    in_filter VARCHAR(255)
)
RETURNS TABLE(
    job_id VARCHAR(48)
)
LANGUAGE plpgsql VOLATILE
AS $$
DECLARE
    sql VARCHAR;
    whereOrAnd VARCHAR(7) = ' WHERE ';
    andConst CONSTANT VARCHAR(5) = ' AND ';

BEGIN
    -- Returns Job ID from the job table
    sql := $q$
        SELECT job.job_id
        FROM job

        $q$;

    sql := sql || whereOrAnd || ' job.partition_id = ' || quote_literal(in_partition_id);
    whereOrAnd := andConst;

    IF in_filter IS NOT NULL THEN
        sql := sql || whereOrAnd || in_filter;
    END IF;

    return sql;
END
$$;
