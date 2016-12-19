/*
 *  Name: internal_get_last_position
 *
 *  Description:  Returns the location of the last char in the specified text.
 */
CREATE OR REPLACE FUNCTION internal_get_last_position(text, char)
RETURNS INTEGER
LANGUAGE SQL AS $$
SELECT LENGTH($1) - POSITION($2 IN REVERSE($1)) + 1;
$$;