/*
 *  Name: get_status
 *
 *  Description:  Returns the status and percentage complete values for the specified job.
 */
CREATE OR REPLACE FUNCTION get_status(in_job_id VARCHAR(48))
RETURNS TABLE (
  job_status  job_status,
  percentage_complete double precision) AS $$
BEGIN
  --  Raise exception if job identifier has not been specified.
  IF in_job_id IS NULL OR in_job_id = '' THEN
    RAISE EXCEPTION 'Job identifier has not been specified';
  END IF;

  -- Return status and percentage completed for the specified job_id.
  RETURN QUERY
  SELECT job.status, job.percentage_complete FROM job WHERE job.job_id = in_job_id;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'job_id {%} not found', in_job_id;
  END IF;
END
$$ LANGUAGE plpgsql;
