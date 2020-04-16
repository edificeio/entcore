CREATE OR REPLACE FUNCTION conversation.last_agg ( anyelement, anyelement )
RETURNS anyelement LANGUAGE SQL IMMUTABLE STRICT AS $$
        SELECT $2;
$$;

CREATE AGGREGATE conversation.LAST (
        sfunc    = conversation.last_agg,
        basetype = anyelement,
        stype    = anyelement
);

CREATE INDEX messages_id_brin_idx ON conversation.messages USING BRIN (id);
CREATE INDEX usermessages_user_id_brin_idx ON conversation.usermessages USING BRIN (user_id);
