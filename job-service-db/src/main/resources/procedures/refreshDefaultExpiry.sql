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
 *  Name: refresh_default_expiry
 *
 *  Description:
 *  Refreshes the default expiration policy
 */
CREATE OR REPLACE PROCEDURE refresh_default_expiry(
)
    LANGUAGE plpgsql AS
$$
BEGIN
    -- Removes all existing policy from the table
    DELETE
    FROM default_job_expiration_policy
    WHERE job_status IS NOT NULL;

    -- Insert the default expiration policy
    INSERT INTO default_job_expiration_policy (
            job_status,
            operation,
            expiration_time
            )
    VALUES ('Active', 'Expire', 'none'),
           ('Cancelled', 'Expire', 'none'),
           ('Completed', 'Expire', 'none'),
           ('Failed', 'Expire', 'none'),
           ('Paused', 'Expire', 'none'),
           ('Waiting', 'Expire', 'none'),
           ('Expired', 'Expire', 'none');
END;

$$;
