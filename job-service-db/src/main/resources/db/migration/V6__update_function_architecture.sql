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
 **************************************************************
 ************   UPDATES FUNCTIONS ARCHITECTURE    **************
 **************************************************************
 */

DO
$$
    BEGIN

        IF EXISTS (
            -- Checks if the old architecture is used
                SELECT NULL
                FROM flyway_schema_history
                WHERE script LIKE 'procedures/R__%'
            )
        THEN
            DO
            $$
                DECLARE
                    rec   record;
                    rec_string varchar;
                    function_drop varchar;

                BEGIN
                    FOR rec IN
                        SELECT 'DROP FUNCTION ' || ns.nspname || '.' || proname
                                   || '(' || oidvectortypes(proargtypes) || ');'
                        FROM pg_proc INNER JOIN pg_namespace ns ON (pg_proc.pronamespace = ns.oid)
                        WHERE ns.nspname = 'public'  order by proname
                        LOOP

                            rec_string = rec;
                            select substring(rec_string, 3, length(rec_string) - 4) INTO function_drop;
                            EXECUTE  function_drop;
                        END LOOP;
                END
            $$;

        END IF;

    END
$$;
