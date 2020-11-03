INSERT INTO job (partition_id, job_id, create_date) VALUES ( 'tenant-x', 'job', current_timestamp );
INSERT INTO job (partition_id, job_id, create_date) VALUES ( 'tenant-x', 'jab', current_timestamp );
INSERT INTO job (partition_id, job_id, create_date) VALUES ( 'tenant-x', 'jib', current_timestamp );
INSERT INTO job (partition_id, job_id, create_date) VALUES ( 'tenant-x', 'jub', current_timestamp );

INSERT INTO completed_subtask_report ( partition_id, job_id, task_id )	VALUES ( 'tenant-x', 'job', 'job.3*' );
INSERT INTO completed_subtask_report ( partition_id, job_id, task_id )	VALUES ( 'tenant-x', 'job', 'job.1' );

INSERT INTO completed_subtask_report ( partition_id, job_id, task_id )	VALUES ( 'tenant-x', 'jab', 'job.3*' );
INSERT INTO completed_subtask_report ( partition_id, job_id, task_id )	VALUES ( 'tenant-x', 'jab', 'job.1' );

INSERT INTO completed_subtask_report ( partition_id, job_id, task_id )	VALUES ( 'tenant-x', 'jib', 'job.3*' );
INSERT INTO completed_subtask_report ( partition_id, job_id, task_id )	VALUES ( 'tenant-x', 'jib', 'job.1' );

INSERT INTO completed_subtask_report ( partition_id, job_id, task_id )	VALUES ( 'tenant-x', 'jub', 'job.3*' );
INSERT INTO completed_subtask_report ( partition_id, job_id, task_id )	VALUES ( 'tenant-x', 'jub', 'job.1' );
