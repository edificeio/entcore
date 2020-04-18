CREATE OR REPLACE FUNCTION conversation.last_agg ( anyelement, anyelement )
RETURNS anyelement LANGUAGE SQL IMMUTABLE STRICT AS $$
        SELECT $2;
$$;

CREATE AGGREGATE conversation.LAST (
        sfunc    = conversation.last_agg,
        basetype = anyelement,
        stype    = anyelement
);
