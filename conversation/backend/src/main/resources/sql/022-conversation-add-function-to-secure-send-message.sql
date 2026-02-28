CREATE OR REPLACE FUNCTION update_message_with_state_transition(
    p_id text,
    p_new_state text
) RETURNS void AS $$
BEGIN
UPDATE conversation.messages
SET state = p_new_state
WHERE id = p_id
  AND state <> p_new_state;

IF NOT FOUND THEN
        RAISE EXCEPTION 'Concurrency error: state mismatch for message %', p_id;
END IF;
END;
$$ LANGUAGE plpgsql;