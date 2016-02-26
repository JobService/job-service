/*
 *  Name: internal_delete_task_table
 *
 *  Description:  Deletes the task tables if all sub tasks reported for the job have been completed.
 *                Internal - used in report_progress() and internal_report_task_completion().
 */
CREATE OR REPLACE FUNCTION internal_delete_task_table(in_job_id VARCHAR(48))
RETURNS VOID AS $$
DECLARE
  v_tables_to_delete text[];
  v_table_name text;
  v_uncompleted_rows_found smallint;
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
    --  Make sure all rows are complete in the table, otherwise raise an exception.
    EXECUTE format('SELECT 1 FROM %I WHERE status <> ''Completed''', v_table_name) INTO v_uncompleted_rows_found;

    IF v_uncompleted_rows_found IS NOT NULL THEN
      RAISE EXCEPTION 'Task table deletion abandoned due to uncompleted task rows in table {%}', v_table_name;
      EXIT;
    END IF;

    --  Drop the table.
    EXECUTE format('DROP TABLE %I', v_table_name);
  END LOOP;

END
$$ LANGUAGE plpgsql;