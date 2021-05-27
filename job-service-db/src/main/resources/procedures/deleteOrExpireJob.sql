--
-- Copyright 2016-2021 Micro Focus or one of its affiliates.
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
 *  Name: delete_or_expire_job
 *
 *  Description:
 *  Expires the specified job.
 */
CREATE OR REPLACE FUNCTION delete_or_expire_job(
    in_partition_id VARCHAR,
    in_job_id VARCHAR,
    in_operation EXPIRATION_OPERATION)
RETURNS VOID
LANGUAGE plpgsql
VOLATILE
AS
$$
BEGIN
    IF in_operation = 'Expire' THEN
        CALL expire_job(in_partition_id, in_job_id);
    ELSE
        PERFORM delete_job(in_partition_id, in_job_id);
    END IF;
END;
$$
