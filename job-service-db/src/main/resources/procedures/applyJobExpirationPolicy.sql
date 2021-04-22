DROP FUNCTION IF EXISTS apply_job_expiration_policy(
);
CREATE OR REPLACE PROCEDURE apply_job_expiration_policy()
LANGUAGE plpgsql
AS
$$
BEGIN

    PERFORM NULL
    FROM (
             SELECT p_id,
                    j_id,
                    jep.operation AS j_operation,
                    jep.expiration_time,
                    -- Whenever there is a "related date" provided (to create_date or last_update_date)
                    -- we calculate the expiration_date and check if it's expired, returning a boolean
                    CASE
                        WHEN jep.expiration_time IS NOT NULL
                            AND LEFT(jep.expiration_time, 1) = 'l' THEN (
                                (
                                    j_last_update_date + split_part(jep.expiration_time, '+', 2)::INTERVAL
                                )::timestamp
                            ) <= now() AT TIME ZONE 'UTC'
                        WHEN jep.expiration_time IS NOT NULL
                            AND LEFT(jep.expiration_time, 1) = 'c' THEN (
                                (
                                    j_create_date + split_part(jep.expiration_time, '+', 2)::INTERVAL
                                )::timestamp
                            ) <= now() AT TIME ZONE 'UTC'
                        ELSE (
                               jep.expiration_time::timestamp
                             ) <= now() AT TIME ZONE 'UTC'
                        END  expired
             FROM (
                      SELECT partition_id     AS p_id,
                             job_id           AS j_id,
                             create_date      AS j_create_date,
                             last_update_date AS j_last_update_date
                      FROM job j
                  ) t1
                      -- We make sure that we get the latest status for the job
                  CROSS JOIN LATERAL (
                     SELECT status AS current_status
                     FROM get_job( p_id, j_id )
                     LIMIT 1
                 ) t2
                     INNER JOIN job_expiration_policy
                         AS jep
                         ON current_status = jep.job_status
             WHERE LEFT(jep.expiration_time, 4) != 'none'
               AND jep.expiration_time IS NOT NULL
         ) t3

        CROSS JOIN LATERAL (
            SELECT NULL
            FROM delete_or_expire_job(p_id, j_id, j_operation)
        ) t4

    WHERE expired IS TRUE;
END;
$$;
