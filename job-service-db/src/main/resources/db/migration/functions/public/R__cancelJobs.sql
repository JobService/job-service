--
-- Copyright 2016-2022 Micro Focus or one of its affiliates.
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--      http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

/*
 *  Name: cancel_jobs
 *
 *  Description:
 *  Cancels the specified jobs.
 */
CREATE OR REPLACE FUNCTION cancel_jobs(
    in_partition_id VARCHAR(40),
    in_job_id_starts_with VARCHAR(48),
    in_status_type VARCHAR(20),
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
    counter integer;
BEGIN
    jobIds := ARRAY(SELECT DISTINCT job_id FROM public.get_jobs(in_partition_id, in_job_id_starts_with, in_status_type,
        in_limit, in_offset, in_sort_field, in_sort_label, in_sort_ascending, in_labels, in_filter));

    counter := 0;

    FOREACH jobId IN ARRAY jobIds
    LOOP
        if cancel_job(in_partition_id, jobId) then
            counter := counter + 1;
        end if;
    end loop;

    return counter;
END
$function$
;
