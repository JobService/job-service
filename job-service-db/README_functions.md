# Types of functions

There are 2 types of functions that we currently use: internal and external. We use _function_ as a general term for both function and 
procedures here.

The internal function's name should start with 'internal_'. Ex: `internal_has_dependent_jobs()` (note. the name of the .sql file might 
differ)

## Adding a function

### External function:
- Create a new file with the function's name preceded with `R__` within the `procedures` folder  
### Internal function:  
- Add `internal__` as prefix for the name of the function 
- Add `R__internal__` as prefix for the function's file.
- Forward declare the function in the migration script. 
- Ensure that we have `/* Forward Declaration */` in the body of the forward declared function.

If working on a `plpgsql` function, declare the body as:  
`AS 'BEGIN /* Forward Declaration */ END';`  

Ex:  

    DO $$
    BEGIN
        CREATE FUNCTION internal_get_task_status(
            in_task_table_name VARCHAR(63)
        )
        RETURNS TABLE(
            status job_status,
            percentage_complete DOUBLE PRECISION,
            failure_details TEXT
        )
        LANGUAGE plpgsql STABLE
        AS 'BEGIN /* Forward Declaration */ END';
    EXCEPTION WHEN duplicate_function THEN
    END $$;  

If working on a `SQL` function **returning a value**, we need to return a dummy value such as: _(Note that we don't pass `BEGIN` `END`)_  
`AS '/* Forward Declaration */SELECT NULL::job_status';`  

Ex:  

    DO $$
    BEGIN
        CREATE FUNCTION internal_get_prereq_job_id_options(
            job_id_with_opts VARCHAR(128)
        )
        RETURNS TABLE(
            job_id VARCHAR(48),
            options_string VARCHAR(128),
            precreated BOOLEAN,
            unknown_options BOOLEAN
        )
        LANGUAGE SQL IMMUTABLE
        AS '/* Forward Declaration */ SELECT NULL, NULL, NULL::BOOLEAN, NULL::BOOLEAN;';
    EXCEPTION WHEN duplicate_function THEN
    END $$;

The function would be empty and return a dummy value when required. The real implementation would occur in the script corresponding to 
the function itself.

## Dropping a function
When dropping a function, we update the main migration script with the `DROP` function and remove the _function.sql_ file.  
Ex: `DROP FUNCTION IF EXISTS internal_report_task_completion(in_task_table_name VARCHAR(63));`

## Updating a function requiring a Change of signature

If we need to change the function signature, then we need to:
- Drop the old function signature. To proceed, we add the drop function element into the main migration script.  
- Add the new signature as a _forward declared_ function (**_only for internal functions_**)
- Update the function in the corresponding script. (Ex: R__clean_job.sql)
