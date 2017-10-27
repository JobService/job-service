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
 *  Name: get_status
 *
 *  Description:  Returns the status and percentage complete values for the specified job.
 */
CREATE OR REPLACE FUNCTION get_status(in_job_id VARCHAR(48))
RETURNS TABLE (
  job_status  job_status,
  percentage_complete double precision) AS $$
BEGIN
  --  Raise exception if job identifier has not been specified.
  IF in_job_id IS NULL OR in_job_id = '' THEN
    RAISE EXCEPTION 'Job identifier has not been specified';
  END IF;

  -- Return status and percentage completed for the specified job_id.
  RETURN QUERY
  SELECT job.status, job.percentage_complete FROM job WHERE job.job_id = in_job_id;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'job_id {%} not found', in_job_id;
  END IF;
END
$$ LANGUAGE plpgsql;
