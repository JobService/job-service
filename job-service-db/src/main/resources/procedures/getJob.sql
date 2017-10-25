--
-- Copyright 2015-2017 Hewlett Packard Enterprise Development LP.
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
 *  Name: get_job
 *
 *  Description:  Returns the job definition for the specified job.
 */
CREATE OR REPLACE FUNCTION get_job(in_job_id VARCHAR(48))
  RETURNS TABLE ( job_id VARCHAR(48), name VARCHAR(255), description TEXT, data TEXT, create_date TEXT, status job_status, percentage_complete double precision, failure_details TEXT, actionType CHAR(6)) AS $$
BEGIN
  --  Raise exception if the job identifier has not been specified.
  IF in_job_id IS NULL OR in_job_id = '' THEN
    RAISE EXCEPTION 'Job identifier has not been specified' USING ERRCODE = '02000'; -- sqlstate no data;
  END IF;

  --  Return job metadata belonging to the specified job_id.
  --  'WORKER' is the only supported action type for now.
  RETURN QUERY
  SELECT job.job_id, job.name, job.description, job.data, to_char(job.create_date, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'), job.status, job.percentage_complete, job.failure_details, CAST('WORKER' AS CHAR(6)) AS actionType
  FROM job WHERE job.job_id = in_job_id;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'job_id {%} not found', in_job_id USING ERRCODE = 'P0002'; -- sqlstate no_data_found
  END IF;
END
$$ LANGUAGE plpgsql;