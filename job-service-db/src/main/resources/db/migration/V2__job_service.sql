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
 *
 *  This script allows the migration from a Liquibase configuration onto Flyway
 *
 *  It will check if any databasechangelog (Liquibase specific table) exists.
 *  If so, it will check if the latest md5sum is present. In case it isn't, the migration aborts.
 *  Otherwise, the Liquibase specific tables get dropped (databasechangelog, databasechangeloglock)
 *
 */
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
                    -- The checksum value is from Liquibase V3.0
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
