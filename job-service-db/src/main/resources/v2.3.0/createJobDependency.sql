--
-- Copyright 2015-2017 EntIT Software LLC, a Micro Focus company.
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
 *  Name: create_job_dependency
 *
 *  Description:  Create a new row in the job_dependency table.
 */
CREATE OR REPLACE FUNCTION create_job_dependency(in_job_id VARCHAR(48), in_dependent_job_id VARCHAR(48))
  RETURNS VOID AS $$
BEGIN

  --  Raise exception if job identifier has not been specified.
  IF in_job_id IS NULL OR in_job_id = '' THEN
    RAISE EXCEPTION 'Job identifier has not been specified' USING ERRCODE = '02000'; -- sqlstate no data;
  END IF;

  --  Raise exception if dependent job identifier has not been specified.
  IF in_dependent_job_id IS NULL OR in_dependent_job_id = '' THEN
    RAISE EXCEPTION 'Dependent job identifier has not been specified' USING ERRCODE = '02000'; -- sqlstate no data;
  END IF;

  -- Create new row in job_dependency.
  insert into public.job_dependency (job_id, dependent_job_id)
  values (in_job_id, in_dependent_job_id);

END
$$ LANGUAGE plpgsql;