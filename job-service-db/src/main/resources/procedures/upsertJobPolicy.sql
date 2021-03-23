CREATE OR REPLACE FUNCTION internal_upsert_job_policy(
    in_partition_id VARCHAR(40),
    in_job_id VARCHAR(58),
    in_policies JOB_POLICY[]
)
    RETURNS VOID
    LANGUAGE plpgsql
AS
$$
DECLARE
    dateToTest     VARCHAR;
    policy         JOB_POLICY;
    policies       JOB_POLICY[];
    reference_date VARCHAR(58);
    duration       INTERVAL;
    create_date    DATE;
BEGIN

    IF in_policies IS NULL OR CARDINALITY(in_policies) = 0 THEN
        RETURN;
    END IF;
    -- Get create_date and store it
    SELECT j.create_date INTO create_date FROM job j WHERE j.partition_id = in_partition_id AND j.job_id = in_job_id;
    FOREACH policy IN ARRAY in_policies
        LOOP
            dateToTest = policy.expiration_time;
            IF dateToTest = 'none' THEN
                policy.expiration_date = NULL;
                POLICY.expiration_after_last_update = 0;
            ELSE
                BEGIN
                    PERFORM dateToTest::DATE;
                    policy.expiration_date = dateToTest;
                    policy.expiration_time = '';
                EXCEPTION
                    WHEN OTHERS THEN
                        reference_date = split_part(dateToTest, '+', 1);
                        duration = split_part(dateToTest, '+', 2);
                        IF LEFT(reference_date, 1) = 'l' THEN
                            policy.expiration_after_last_update = EXTRACT(epoch FROM duration) / 3600;
                            policy.expiration_time = '';
                        ELSE
                            policy.expiration_date = create_date + duration;
                            policy.expiration_time = '';
                        END IF;
                END;
            END IF;
            SELECT ARRAY_APPEND(policies, policy) INTO policies;
        END LOOP;

    INSERT INTO job_expiration_policy (partition_id, job_id, job_status, operation, expiration_after_last_update,
                                       expiration_date)
    SELECT in_partition_id,
           in_job_id,
           p.status,
           p.operation,
           p.expiration_after_last_update,
           p.expiration_date
    FROM UNNEST(policies) AS p;
    RETURN;
END
$$;
