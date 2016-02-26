/*
 *  Name: delete_job
 *
 *  Description:  Deletes the job row and corresponding task tables.
 */
CREATE OR REPLACE FUNCTION delete_job(in_job_id VARCHAR(48))
RETURNS VOID AS $$
DECLARE
  v_tables_to_delete text[];
  v_table_name text;
BEGIN
  --  Raise exception if job identifier has not been specified.
  IF in_job_id IS NULL OR in_job_id = '' THEN
    RAISE EXCEPTION 'The job identifier has not been specified';
  END IF;

  --  Identify task tables associated with the specified job.
  EXECUTE 'SELECT ARRAY(SELECT relname FROM pg_class WHERE relname LIKE $1)' INTO v_tables_to_delete  USING 'task_' || in_job_id || '%';

  --  Loop through each task table.
  FOREACH v_table_name IN ARRAY v_tables_to_delete
  LOOP
    --  Drop the table.
    EXECUTE format('DROP TABLE %I', v_table_name);
  END LOOP;

  --  Remove row from the job table.
  DELETE FROM job WHERE job_id =  in_job_id;
END
$$ LANGUAGE plpgsql;