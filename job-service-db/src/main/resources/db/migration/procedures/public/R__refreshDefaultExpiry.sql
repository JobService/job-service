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
 *  This function is to be updated manually
 *  There must be a value in create_date_offset "or" last_modified_offset
 *  based on 'expiration_time' value
 *  If any value in last_modified_offset, then create_date_offset must be null
 *  If any value in create_date_offset, then last_modified_offset must be null
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
        expiration_time,
        -- create_date_offset can contain 'infinity or an interval'
        -- this is based upon the value from expiration_time.
        -- if expiration_time is related to created_time, then the offset is to be inserted.
        -- if expiration_time equals 'none', then 'infinity' is to be inserted
        create_date_offset,
        -- last_modified_offset can contain a value only if create_date_offset is null
        last_modified_offset
    )
    VALUES ('Active', 'Expire', 'none', 'infinity', NULL),
           ('Cancelled', 'Delete', 'none', 'infinity', NULL),
           ('Completed', 'Delete', 'none', 'infinity', NULL),
           ('Failed', 'Delete', 'none', 'infinity', NULL),
           ('Paused', 'Expire', 'none', 'infinity', NULL),
           ('Waiting', 'Expire', 'none', 'infinity', NULL),
           ('Expired', 'Delete', 'none', 'infinity', NULL);
END;

$$;
