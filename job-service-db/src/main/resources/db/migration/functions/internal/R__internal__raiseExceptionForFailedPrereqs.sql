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
 *  Name: internal_raise_exception_for_failed_prereqs
 *
 *  Description:
 *  Accepts an array of failed prerequisite job ids and raises an exception if the array is not empty.
 */
CREATE OR REPLACE FUNCTION internal_raise_exception_for_failed_prereqs(in_failed_prereq_jobs_ids VARCHAR(128)[]) RETURNS BOOLEAN
    LANGUAGE plpgsql IMMUTABLE AS
$$BEGIN
    IF array_length(in_failed_prereq_jobs_ids, 1) > 0 THEN
        RAISE EXCEPTION 'One or more prerequisite jobs have failed. Failed Job IDs: %', ARRAY_TO_STRING(in_failed_prereq_jobs_ids, ', ') USING ERRCODE = '02000'; -- sqlstate no data
    END IF;

    RETURN TRUE;
END;
$$;
