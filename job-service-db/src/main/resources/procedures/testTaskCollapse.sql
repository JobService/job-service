--
-- Copyright 2016-2020 Micro Focus or one of its affiliates.
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
 *  Name: internal_test_task_collapse
 *
 *  Description:
 *  Unit tests for test_task_collapse()
 */
CREATE OR REPLACE FUNCTION internal_test_task_collapse(tasks VARCHAR[], processed_tasks VARCHAR[])
    RETURNS BOOLEAN

    LANGUAGE plpgsql
AS
$$
DECLARE
BEGIN

    IF task_collapse(tasks) !=
       processed_tasks THEN RAISE NOTICE 'failure';
        RETURN 'false';

  /*  ELSIF task_collapse(
                  '{job.7*, job.1.5, job.4.5*, job.2.3*,  job.1.9, job.1.8, job.1.7, job.1.6,  job.1.4, job.1.3, job.1.10*,job.1.2,  job.1.1, job.2.2.2*, job.2.2.1, job.2.1}') !=
          '{job.7*, job.2, job.1,job.4.5*}' THEN
        RETURN 'false';

    ELSIF task_collapse('{ job.2.2.2*, job.2.2.1}') !=
          '{job.2.2}' THEN
        RETURN 'false';

    ELSIF task_collapse('{  job.8.3*, job.8.2, job.8.1}') !=
          '{job.8}' THEN
        RETURN 'false';

        -- dealing with big numbers
    ELSIF task_collapse('{ job.8.1603891656565* }') !=
          '{job.8.1603891656565*}' THEN
        RETURN 'false';
    ELSIF task_collapse('{ job.8.3, job.8.2, job.8.1  }') !=
          '{job.8.3, job.8.2, job.8.1}' THEN
        RETURN 'false';

    ELSIF task_collapse('{ job.3*, job.2, job.1  }') !=
          '{job}' THEN
        RETURN 'false';

    ELSIF task_collapse(
                  '{ job.88.8 ,job.88.10*, job.88.9,  job.89.8, job.88.7, job.88.6, job.88.5, job.88.4, job.88.3, ' ||
                  'job.88.2, job.88.1}') !=
          '{job.88, job.89.8}' THEN
        RETURN 'false';

        --checking that we avoid duplicates
    ELSIF task_collapse('{ job.88.8 ,job.88.10*, job.88.9,  job.89.8,job.88.10*, job.88.7, job.88.6,' ||
                        'job.88.8 ,job.88.10*, job.88.9,  job.89.8,job.88.10*, job.88.7, job.88.6,
	job.88.5, job.88.4, job.88.3, job.88.2, job.88.1, job.89.8,job.88.10*, job.88.7, job.88.6}') !=
          '{job.88, job.89.8}' THEN
        RETURN 'false';
*/

    END IF;

    RETURN 'true';


END
$$;
