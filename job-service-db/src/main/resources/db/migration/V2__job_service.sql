DO
$$
    BEGIN

        IF EXISTS
            (
            -- Checks if liquibase table exists
                SELECT table_name
                FROM information_schema.tables
                WHERE table_name = 'databasechangelog'
            )
        THEN
            IF NOT EXISTS(
                -- Checks that the db checksum is valid
                    SELECT *
                    FROM databasechangelog
                    WHERE md5sum = '7:6127825a6ab9fb3ed7f2c25e2354f2ce'
                )
            THEN
                RAISE EXCEPTION 'The databasechangelog table is present but does not contain the expected md5sum. Cannot perform migration from Liquibase to Flyway.';
            END IF;
        END IF;

        -- Drop liquibase tables
        DROP TABLE IF EXISTS databasechangelog, databasechangeloglock;

    END
$$;
