/*
 * Name: delete_jobs
 *
 * Description:
 * Deletes the specified jobs
 */
 CREATE OR REPLACE FUNCTION delete_jobs(
     in_partition_id VARCHAR(40),
     in_job_id_starts_with VARCHAR(48),
     in_status_type VARCHAR (20),
     in_limit INT,
     -- Hard coding offset to 0 --
     -- Hard coding sort-field to 'job_id' --
     -- Hard coding sort_label to null --
     -- Hard coding sort_ascending to false --
     in_labels VARCHAR(255)[],
     in_filter VARCHAR(255)
 )
 RETURNS INTEGER
 LANGUAGE plpgsql
 AS $function$
 DECLARE
     jobIds varchar[];
     jobId varchar;
 BEGIN
    jobIds := ARRAY(SELECT DISTINCT job_id FROM public.get_jobs(in_partition_id, in_job_id_starts_with, in_status_type,
        in_limit, 0, 'job_id', null, false, in_labels, in_filter) ORDER BY job_id);

    FOREACH jobId IN ARRAY jobIds
    LOOP
        PERFORM delete_job(in_partition_id, jobId);
    end loop;

    return ARRAY_LENGTH(jobIds, 1);
END
$function$
;
