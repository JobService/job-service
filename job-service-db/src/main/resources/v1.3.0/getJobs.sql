/*
 *  Name: get_jobs
 *
 *  Description:  Returns the list of job definitions in the system.
 */
CREATE OR REPLACE FUNCTION get_jobs(in_job_id_starts_with VARCHAR(48), in_status_type VARCHAR(20), in_limit INT, in_offset INT)
  RETURNS TABLE ( job_id VARCHAR(48), name VARCHAR(255), description TEXT, data TEXT, create_date TEXT, status job_status, percentage_complete double precision, failure_details TEXT, actionType CHAR(6)) AS $$
DECLARE
  sql VARCHAR;
  escapedJobIdStartsWith VARCHAR;
  whereOrAnd VARCHAR(7) = ' WHERE ';
  andConst CONSTANT VARCHAR(5) = ' AND ';
BEGIN

  -- Return all rows from the job table:
  --   If the in_job_id param is specified, only those rows starting with that param will be returned.
  --   If the in_status_type param is NotCompleted - only those results with statuses other than Completed will be returned; Completed - only those results with Completed status will be returned; Inactive - only those results with inactive statuses (i.e. Completed, Failed, Cancelled) will be returned; Anything else returns all statuses.
  -- Also accepts in_limit and in_offset params to support paging and limiting the number of rows returned.
  -- 'WORKER' is the only supported action type for now and this is returned.
  sql := $q$SELECT job.job_id, job.name, job.description, job.data, to_char(job.create_date, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'), job.status, job.percentage_complete, job.failure_details, CAST('WORKER' AS CHAR(6)) AS actionType FROM job$q$;

  IF in_job_id_starts_with IS NOT NULL AND in_job_id_starts_with != '' THEN
    escapedJobIdStartsWith = replace(replace(quote_literal(in_job_id_starts_with), '_', '\_'), '%', '\%');
    escapedJobIdStartsWith = left(escapedJobIdStartsWith, char_length(escapedJobIdStartsWith) - 1) || $q$%'$q$;
    sql := sql || whereOrAnd || ' job_id LIKE ' || escapedJobIdStartsWith;
    whereOrAnd := andConst;
  END IF;

  IF in_status_type IS NOT NULL THEN
    IF in_status_type = 'NotCompleted' THEN
      sql := sql || whereOrAnd || $q$ status IN ('Active','Paused','Waiting','Cancelled','Failed')$q$;
      whereOrAnd := andConst;
    ELSIF in_status_type = 'Completed' THEN
      sql := sql || whereOrAnd || $q$ status IN ('Completed')$q$;
      whereOrAnd := andConst;
    ELSIF in_status_type = 'Inactive' THEN
      sql := sql || whereOrAnd || $q$ status IN ('Completed','Cancelled','Failed')$q$;
      whereOrAnd := andConst;
    END IF;
  END IF;

  sql := sql || ' ORDER BY create_date DESC';

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
$$ LANGUAGE plpgsql;
