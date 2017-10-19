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
 *  Name: create_job_task_data
 *
 *  Description:  Create a new row in the job_task_data table.
 */
CREATE OR REPLACE FUNCTION create_job_task_data(in_job_id VARCHAR(48), in_task_classifier VARCHAR(255),
in_task_api_version INT, in_task_data TEXT, in_task_data_encoding VARCHAR(32), in_task_pipe VARCHAR(255),
in_target_pipe VARCHAR(255))
  RETURNS VOID AS $$
BEGIN

  --  Raise exception if job identifier has not been specified.
  IF in_job_id IS NULL OR in_job_id = '' THEN
    RAISE EXCEPTION 'Job identifier has not been specified' USING ERRCODE = '02000'; -- sqlstate no data;
  END IF;

  --  Raise exception if job taskClassifier has not been specified.
  IF in_task_classifier IS NULL OR in_task_classifier = '' THEN
    RAISE EXCEPTION 'Job taskClassifier has not been specified' USING ERRCODE = '02000'; -- sqlstate no data;
  END IF;

  --  Raise exception if job taskApiVersion has not been specified.
  IF in_task_api_version IS NULL THEN
    RAISE EXCEPTION 'Job taskApiVersion has not been specified' USING ERRCODE = '02000'; -- sqlstate no data;
  END IF;

  --  Raise exception if job taskData has not been specified.
  IF in_task_data IS NULL OR in_task_data = '' THEN
    RAISE EXCEPTION 'Job taskData has not been specified' USING ERRCODE = '02000'; -- sqlstate no data;
  END IF;

  --  Raise exception if job taskPipe has not been specified.
  IF in_task_pipe IS NULL OR in_task_pipe = '' THEN
    RAISE EXCEPTION 'Job taskPipe has not been specified' USING ERRCODE = '02000'; -- sqlstate no data;
  END IF;

  --  Raise exception if job targetPipe has not been specified.
  IF in_target_pipe IS NULL OR in_target_pipe = '' THEN
    RAISE EXCEPTION 'Job targetPipe has not been specified' USING ERRCODE = '02000'; -- sqlstate no data;
  END IF;
  
  -- Create new row in job_task_table.
  insert into public.job_task_table (job_id, task_classifier, task_api_version, task_data, task_data_encoding, task_pipe, target_pipe)
  values (in_job_id, in_task_classifier, in_task_api_version, in_task_data, in_task_data_encoding, in_task_pipe, in_target_pipe);

END
$$ LANGUAGE plpgsql;