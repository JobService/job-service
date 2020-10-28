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
 *  Name: internal_task_collapse
 *
 *  Description:
 *  Collapses tasks whenever possible
 */
CREATE OR REPLACE FUNCTION task_collapse(tasks VARCHAR[])
    RETURNS VARCHAR[]
    LANGUAGE plpgsql
AS
$$
DECLARE
    final_array   VARCHAR[] := '{}';
    task          VARCHAR;
    last_nb       INTEGER   := 0;
    body          VARCHAR   := '';
    counter       INTEGER   := 2;
    expected      VARCHAR;
    collapsing    BOOLEAN   := false;
    currentT      VARCHAR;
    starting      INTEGER   := 1;
    collapsed     VARCHAR   := '';
    backup        VARCHAR[] := '{}';
    modified      BOOLEAN   := true;
    regex_body    VARCHAR   := '.*(?=\.)\.'; -- takes everything up to the last dot
    regex_last_nb VARCHAR   := '([\d]+)\*?$';-- takes everything after the last dot, excluding the *

BEGIN

    SELECT array(
                   SELECT id
                   FROM unnest(tasks) AS id
                   ORDER BY substring(id, regex_body),
                            substring(id, regex_last_nb)::NUMERIC DESC
               )
    INTO tasks;


    -- we loop until the array stop being modified
    WHILE (modified)

        LOOP
        -- we create / update a backup to have something to compare against
        -- as we will modify the initial tasks array
            backup := tasks;


            --we loop until the tasks array is empty
            WHILE (cardinality(tasks) > 0)

                LOOP

                    -- we store the first element of the array
                    task = tasks[1];

                    -- if the task ends with a *
                    IF right(task, 1) = '*'
                        -- and the next one too
                        AND right(tasks[2], 1) != '*'
                    THEN

                        -- nb of related subtasks expected
                        last_nb := SUBSTRING(substr(task, 1, length(task) - 1), regex_last_nb)::INTEGER;

                        -- store the rest of the body
                        body := SUBSTRING(task, regex_body);

                        -- create the collapsing result (body - last '.')
                        collapsed := left(body, -1);

                        WHILE (counter <= last_nb)

                            LOOP
                                expected := concat(body, (last_nb + 1 - counter));
                                currentT := tasks[counter];
                                IF (currentT != expected OR currentT IS NULL) THEN

                                    tasks = array_remove(tasks, task);

                                    IF final_array[array_upper(final_array, 1)] IS NULL OR
                                       final_array[array_upper(final_array, 1)] != currentT THEN
                                        final_array := array_append(final_array, task);
                                    END IF;
                                    collapsing := false;
                                    EXIT;

                                ELSE
                                    collapsing := true;
                                END IF;

                                counter := counter + 1;

                            END LOOP;

                        IF (collapsing) THEN
                            WHILE (starting <= last_nb)
                                LOOP

                                    -- remove items from tasks
                                    tasks := array_remove(tasks, tasks[1]);

                                    starting := starting + 1;

                                END LOOP;

                            final_array := array_append(final_array, collapsed);

                        END IF;
                        counter := 2;
                        last_nb := 0;
                        starting := 1;

                    ELSE

                        tasks = array_remove(tasks, task);
                        final_array := array_append(final_array, task);

                    END IF;


                END LOOP;

            -- if no modification done, then we break the loop
            IF (final_array = backup) THEN

                modified = false;

            ELSE
                -- sorting tasks
                SELECT array(
                               SELECT id
                               FROM unnest(final_array)
                                        AS id
                               ORDER BY substring(id, regex_body),
                                        substring(id, regex_last_nb)::INTEGER DESC)
                INTO tasks;

                -- resetting final_array
                final_array = '{}';

            END IF;

        END LOOP;

    RETURN final_array;

END
$$;
