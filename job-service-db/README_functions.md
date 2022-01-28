# Types of functions

There are 2 types of functions that we currently use: internal and external. We use _function_ as a general term for both function and 
procedures here.

The internal function's name should start with 'internal_'. Ex: `internal_has_dependent_jobs()`

## Adding a function

When adding an **internal function**, we would need to forward declare it in the migration script. Ex:  

    CREATE OR REPLACE FUNCTION internal_cleanup_completed_subtask_report(
      in_partition_id VARCHAR(40),
      in_job_id VARCHAR(48)
    )
    RETURNS VOID
    LANGUAGE plpgsql VOLATILE
    AS $$
    BEGIN
    END
    $$;

The function would be empty and return a dummy value when required. The real implementation would occur in the script corresponding to 
the function itself.

## Updating a function requiring a Change of signature

If we need to change the function signature, then we need to drop the old one. To proceed, we add the drop function element into the 
main migration script, then update the function in the corresponding script.
