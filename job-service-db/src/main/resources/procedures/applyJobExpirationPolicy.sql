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
 *  Name: apply_job_expiration_policy
 *
 *  Description:
 *  Expires or deletes a job based on its expiration policy.
 */
CREATE OR REPLACE FUNCTION apply_job_expiration_policy(
)
RETURNS VOID
LANGUAGE plpgsql
VOLATILE
AS
$$
DECLARE
    v_rec            RECORD;
    v_dateToTest     VARCHAR;
    v_duration       INTERVAL;
    v_expiration     date;
    v_partition_id   VARCHAR;
BEGIN

    CREATE temp table temp_table
        ON COMMIT DROP AS
    SELECT j.partition_id,
           j.job_id,
           j.create_date,
           j.last_update_date,
           jep.operation,
           jep.expiration_time
    FROM job j
             JOIN job_expiration_policy jep
                  ON j.partition_id = jep.partition_id
                         AND j.job_id = jep.job_id
                         AND j.status = jep.job_status
    WHERE expiration_time is not NULL
      AND expiration_time != 'none'
    ORDER BY j.partition_id, j.job_id;

    FOR v_rec IN SELECT * FROM temp_table
        LOOP
            IF v_partition_id != v_rec.partition_id THEN
                v_partition_id = v_rec.partition_id;
            END IF;
            v_dateToTest = v_rec.expiration_time;

            BEGIN
                    -- if related to lastUpdate
                IF LEFT(v_dateToTest, 4) = 'last' THEN
                    v_duration = split_part(v_dateToTest, '+', 2);
                    v_expiration = v_rec.last_update_date + v_duration;

                    -- if related to createDate
                ELSIF LEFT(v_dateToTest, 6) = 'create' THEN
                    v_duration = split_part(v_dateToTest, '+', 2);
                    v_expiration = v_rec.create_date + v_duration;
                ELSE
                    -- pass the date as is
                    --PERFORM v_dateToTest::DATE;
                    v_expiration = v_dateToTest;
                END IF;
                -- if any exception, raise
            EXCEPTION
                WHEN OTHERS THEN
                    RAISE EXCEPTION 'Invalid date %', v_dateToTest;
            END;

            -- now that we checked the date validity, we check if there's any expiration
            IF v_expiration <= now() AT TIME ZONE 'UTC' THEN
                -- if action = 'expire', add to expire array
                BEGIN
                    IF v_rec.operation = 'Delete' THEN
                        PERFORM delete_job(v_rec.partition_id, v_rec.job_id);
                    ELSE
                        UPDATE job j
                        SET status ='Expired'
                        WHERE j.partition_id = v_rec.partition_id
                        AND j.job_id = v_rec.job_id;
                    END IF;
                    -- if any exception, raise
                EXCEPTION
                    WHEN OTHERS THEN
                        RAISE EXCEPTION 'Issue while expiring policies for %/%', v_rec.partition_id, v_rec.job_id;
                END;
            END IF;

        END LOOP;

END;
$$;
