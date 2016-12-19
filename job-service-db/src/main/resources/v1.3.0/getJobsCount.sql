/*
 *  Name: get_jobs_count
 *
 *  Description:  Returns the number of job definitions in the system matching whatever criteria is specified.
 */
CREATE OR REPLACE FUNCTION get_jobs_count(in_job_id_starts_with VARCHAR(48), in_status_type VARCHAR(20))
  RETURNS TABLE ( row_count BIGINT ) AS $$
DECLARE
  sql VARCHAR;
  escapedJobIdStartsWith VARCHAR;
  whereOrAnd VARCHAR(7) = ' WHERE ';
  andConst CONSTANT VARCHAR(5) = ' AND ';
BEGIN

  -- Returns the number of job definitions in the system matching whatever criteria is specified:
  --   If the in_job_id param is specified, only those rows starting with that param will be returned.
  --   If the in_status_type param is NotCompleted - only those results with statuses other than Completed will be returned; Completed - only those results with Completed status will be returned; Inactive - only those results with inactive statuses (i.e. Completed, Failed, Cancelled) will be returned; Anything else returns all statuses.
  sql := $q$SELECT COUNT(job.job_id) FROM job$q$;

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

  RETURN QUERY EXECUTE sql;

END
$$ LANGUAGE plpgsql;
