/*
 *  Name: cancel_job
 *
 *  Description:  Cancels the specified job.
 */
CREATE OR REPLACE FUNCTION cancel_job(in_job_id VARCHAR(48))
RETURNS VOID AS $$
DECLARE
  v_cancel_supported int;
BEGIN
  --  Raise exception if job identifier has not been specified.
  IF in_job_id IS NULL OR in_job_id = '' THEN
    RAISE EXCEPTION 'The job identifier has not been specified' USING ERRCODE = '02000'; -- sqlstate no data;
  END IF;

  --  Only support Cancel operation on jobs with current status 'Waiting', 'Active' or 'Paused'.
  SELECT CASE
          WHEN status = 'Waiting' THEN 1
          WHEN status = 'Active' THEN 1
          WHEN status = 'Paused' THEN 1
          ELSE 0
         END
  INTO v_cancel_supported
  FROM job
  WHERE job_id = in_job_id FOR UPDATE;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'job_id {%} not found', in_job_id USING ERRCODE = 'P0002'; -- sqlstate no_data_found
  END IF;

  IF v_cancel_supported = 0 THEN
    RAISE EXCEPTION 'job_id {%} cannot be cancelled', in_job_id USING ERRCODE = '02000';
  END IF;

  --  Delete task tables belonging to the specified job.
  PERFORM internal_delete_task_table(in_job_id, true);

  --  Update the job row to 'Cancelled'
  UPDATE job SET status = 'Cancelled' WHERE job_id = in_job_id;

END
$$ LANGUAGE plpgsql;