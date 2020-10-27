CREATE OR REPLACE FUNCTION task_collapse(tasks VARCHAR[])
    RETURNS VARCHAR[]
    LANGUAGE plpgsql
AS $$
DECLARE
    final_array VARCHAR[] :='{}';
    task VARCHAR;
    last_nb INTEGER :=0;
    body VARCHAR :='';
    counter INTEGER :=2;
    expected VARCHAR;
    collapsing BOOLEAN := false;
    currentT VARCHAR;
    starting INTEGER:=1;
    collapsed VARCHAR:='';
    backup VARCHAR[]:='{}';
    modified BOOLEAN := 't';
    maxi INTEGER:=1000;
    regex_body VARCHAR :='.*(?=\.)\.'; -- takes everything up to the last dot
    regex_last_nb VARCHAR :='([\d]+)\*?$';-- takes everything after the last dot, excluding the *

BEGIN
    -- tasks := array_agg(x ORDER BY x DESC) FROM unnest(tasks) x ;
    SELECT array(SELECT id FROM unnest( tasks ) AS id ORDER BY substring(id,regex_body), substring(id, regex_last_nb)::INTEGER DESC ) INTO tasks;

    -- we loop until the array stop being modified
    WHILE (modified='t') LOOP
            -- we create / update a backup to have something to compare against
            -- as we will modify the initial tasks array
            backup:=tasks;


            --we loop until the tasks array is empty
            WHILE (cardinality(tasks)!=0) LOOP
                    -- we store the first element of the array
                    task = tasks[1];
                    IF task IS NULL THEN RETURN final_array; END IF;

                    -- if the task ends with a *
                    IF (right(task, 1)='*') AND (right(tasks[2],1)!='*' )THEN
                        -- nb of related subtasks expected
                        last_nb := SUBSTRING(substr(task, 1, length(task) - 1), regex_last_nb)::INTEGER;

                        -- store the rest of the body
                        body:= SUBSTRING(task, regex_body);
                        collapsed:= substr(body, 1, length(body) - 1);

                        WHILE(counter<=last_nb) loop
                                expected:= concat(body,(last_nb+1 -counter));
                                currentT:=tasks[counter];

                                IF (currentT!=expected  OR currentT IS NULL) THEN
                                    tasks = array_remove(tasks, task);
                                    final_array := array_append(final_array, task);
                                    collapsing:='f';
                                    EXIT;
                                ELSE
                                    collapsing:='t';
                                END IF;

                                counter:=counter+1;

                            END LOOP;

                        IF(collapsing='t') THEN
                            WHILE(starting<=last_nb)LOOP

                                    -- remove items from tasks
                                    tasks:=array_remove(tasks, tasks[1]);

                                    starting:=starting+1;
                                END LOOP;
                            final_array := array_append(final_array, collapsed);
                        END IF;
                        counter:=2;
                        last_nb:=0;
                        starting:=1;

                    ELSE
                        tasks = array_remove(tasks, task);
                        final_array := array_append(final_array, task);
                    END IF;
                    -- safety to avoid infinite loop
                    if maxi =0 then exit;maxi:=maxi-1;end if;
                END LOOP;

            -- if no modification done, then we break the loop
            IF(final_array=backup) THEN
                modified='f';

            ELSE
                tasks:=array_agg(x ORDER BY x DESC) FROM unnest(final_array) x;

                final_array='{}';

            END IF;

        END LOOP;

    RETURN final_array;

END
$$;