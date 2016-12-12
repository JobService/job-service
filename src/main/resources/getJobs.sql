/*
 *  Name: get_jobs
 *
 *  Description:  Returns the list of job definitions in the system.
 */
CREATE OR REPLACE FUNCTION get_jobs()
  RETURNS TABLE ( job_id VARCHAR(48), name VARCHAR(255), description TEXT, data TEXT, create_date TEXT, status job_status, percentage_complete double precision, failure_details TEXT, actionType CHAR(6)) AS $$
BEGIN

  -- Return all rows from the job table.
  -- 'WORKER' is the only supported action type for now and this is returned.
  RETURN QUERY
  SELECT job.job_id, job.name, job.description, job.data, to_char(job.create_date, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'), job.status, job.percentage_complete, job.failure_details, CAST('WORKER' AS CHAR(6)) AS actionType
  FROM job;

END
$$ LANGUAGE plpgsql;
