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
     in_offset INT,
     in_sort_field VARCHAR(20),
     in_sort_label VARCHAR(255),
     in_sort_ascending BOOLEAN,
     in_labels VARCHAR(255)[],
     in_filter VARCHAR(255)
 )
 RETURNS INTEGER
 LANGUAGE plpgsql
 AS $function$
 DECLARE
     jobIds varchar[];
     jobId varchar;
     counter INT;
 BEGIN
    jobIds := ARRAY(SELECT DISTINCT job_id FROM public.get_jobs(in_partition_id, in_job_id_starts_with, in_status_type,
        in_limit, in_offset, in_sort_field, in_sort_label, in_sort_ascending, in_labels, in_filter));

    counter := 0;

    FOREACH jobId IN ARRAY jobIds
    LOOP
        if delete_job(in_partition_id, jobId) then
            counter := counter + 1;
        end if;
    end loop;

    return counter;
END
$function$
;
