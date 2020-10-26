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
    maxi integer:=15;

BEGIN
    tasks := array_agg(x ORDER BY x DESC) FROM unnest(tasks) x ;

    raise notice '%', tasks;

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
                        raise notice 'task %', task;
                        -- nb of related subtasks expected
                        --last_nb := left(right(task,2),1);
                        last_nb := SUBSTRING(substr(task, 1, length(task) - 1), '.([0-9]+)$')::INTEGER;
                        raise notice 'last_nb %', last_nb;

                        -- store the rest of the body
                        body:= concat(SUBSTRING(task, '.*(?=\.)'),'.');
                        collapsed:= substr(body, 1, length(body) - 1);

                        WHILE(counter<=last_nb) loop
                                raise notice E'body %\n', body;
                                expected:= concat(body,(last_nb+1 -counter));
                                currentT:=tasks[counter];
                                raise notice E'expected % current %', expected, currentT;

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
    raise notice '%', final_array;

    RETURN final_array;

END
$$;

drop function test_collapse();
create or replace function test_collapse()
    RETURNS BOOLEAN

    language plpgsql
as $$
declare
begin

    if task_collapse('{ job.7*, job.4.5, job.1.3*, job.1.2, job.1.1}') !=
       '{job.7*,job.4.5, job.1}' then return 'false';

    elsif task_collapse('{job.7*, job.4.5*, job.1.9*, job.1.8, job.1.7, job.1.6, job.1.5, job.1.4, job.1.3, job.1.2,  job.1.1, job.2.3*, job.2.2.2*, job.2.2.1, job.2.1}') !=
          '{job.7*,job.4.5*, job.2, job.1}' then return 'false';

    elsif task_collapse('{ job.2.2.2*, job.2.2.1}') !=
          '{job.2.2}' then return 'false';

    elsif task_collapse('{ job.8.1, job.8.3*, job.8.2}') !=
          '{job.8}' then return 'false';

    elsif task_collapse('{ job.8.1, job.8.3, job.8.2}') !=
          '{job.8.3, job.8.2, job.8.1}' then return 'false';

        /*elsif task_collapse('{ job.88.10*, job.88.9, job.88.8, job.88.7, job.88.6, job.88.5, job.88.4, job.88.3, job.88.2, job.88.1}') !=
        '{job.88}' then return 'false';

        */





    end if;


    return 'true';



end
$$;


select * from test_collapse();




