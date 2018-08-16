--
-- Copyright 2015-2018 Micro Focus or one of its affiliates.
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
 *  Name: internal_resolve_status
 *
 *  Description:
 *  Used when the job table status is being updated to ensure that it doesn't fall back to an earlier status.
 */
CREATE OR REPLACE FUNCTION internal_resolve_status(
    in_current_job_status job_status,
    in_proposed_job_status job_status
)
RETURNS job_status
LANGUAGE SQL
AS $$
    WITH priority_tbl(priority, status) AS
    (
        SELECT 1, CAST('Waiting' AS job_status) UNION ALL
        SELECT 2, 'Active'    UNION ALL
        SELECT 3, 'Paused'    UNION ALL
        SELECT 4, 'Failed'    UNION ALL
        SELECT 5, 'Cancelled' UNION ALL
        SELECT 6, 'Completed'
    )
    SELECT CASE WHEN p1.priority > p2.priority THEN p1.status ELSE p2.status END
    FROM priority_tbl p1
    INNER JOIN priority_tbl p2 ON p1.status = in_current_job_status AND p2.status = in_proposed_job_status;
$$;
