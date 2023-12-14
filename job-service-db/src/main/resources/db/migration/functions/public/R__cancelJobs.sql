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
    -- Hard coding status_type to 'NotFinished' (Active, Paused, Waiting)
    in_limit INT,
    -- Hard coding offset to 0
    -- Hard coding sort_field to 'create_date' --
    -- Hard coding sort_label to null --
    -- Hard coding sort_ascending to false --
    in_labels VARCHAR(255)[],
    in_filter VARCHAR(255)
)
RETURNS INTEGER
LANGUAGE plpgsql
AS $function$
DECLARE

BEGIN
    RETURN (
        SELECT COUNT(cancel_job(in_partition_id, job_id))
        FROM public.get_jobs(in_partition_id, in_job_id_starts_with, 'NotFinished', in_limit, 0,
            'create_date', null, false, in_labels, in_filter)
    );
END
$function$
;
