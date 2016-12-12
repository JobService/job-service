/*
 *  Name: internal_create_task_table
 *
 *  Description:  Create a new task table when the first sub-task is reported for a job.
 *                Internal - used in report_progress().
 */
CREATE OR REPLACE FUNCTION internal_create_task_table(in_table_name varchar(63))
RETURNS VOID AS $$
DECLARE
  v_index_name VARCHAR(63);
BEGIN

  --  Raise exception if task table name has not been specified.
  IF in_table_name IS NULL OR in_table_name = '' THEN
    RAISE EXCEPTION 'Task table name has not been specified';
  END IF;

  --  Create a new task table.
  EXECUTE format('
  CREATE TABLE IF NOT EXISTS %I
  (
    task_id varchar(58) NOT NULL,
    create_date timestamp without time zone NOT NULL,
    status job_status NOT NULL DEFAULT ''Waiting''::job_status,
    percentage_complete double precision NOT NULL DEFAULT 0.00,
    failure_details text,
    is_final boolean NOT NULL DEFAULT false,
    CONSTRAINT %I PRIMARY KEY (task_id)
  )', in_table_name, 'pk_' || in_table_name);

  -- Create indexes.
  v_index_name = 'idx_' || in_table_name || '_s';
  IF (SELECT to_regclass(v_index_name::cstring)) IS NULL THEN
    EXECUTE format('CREATE INDEX %1$I ON %2$I (%3$I)',v_index_name, in_table_name, 'status');
  END IF;

  v_index_name = 'idx_' || in_table_name || '_if';
  IF (SELECT to_regclass(v_index_name::cstring)) IS NULL THEN
    EXECUTE format('CREATE INDEX %1$I ON %2$I (%3$I)',v_index_name, in_table_name, 'is_final');
  END IF;

END
$$ LANGUAGE plpgsql;