/*
 *  Name: create_job
 *
 *  Description:  Create a new row in the job table.
 */
CREATE OR REPLACE FUNCTION create_job(in_job_id VARCHAR(48), in_name VARCHAR(255), in_description TEXT, in_data TEXT, in_job_hash INT)
  RETURNS VOID AS $$
BEGIN

  --  Raise exception if job identifier has not been specified.
  IF in_job_id IS NULL OR in_job_id = '' THEN
    RAISE EXCEPTION 'Job identifier has not been specified' USING ERRCODE = '02000'; -- sqlstate no data;
  END IF;

  -- Create new row in job and return the job_id.
  insert into public.job (job_id, name, description, data, create_date, status, percentage_complete, failure_details, job_hash)
  values (in_job_id, in_name, in_description, in_data, now(), 'Waiting', 0.00, null, in_job_hash);

END
$$ LANGUAGE plpgsql;