CREATE OR REPLACE FUNCTION conversation.flagOrphanMessage() RETURNS TRIGGER AS $$
BEGIN
        IF NOT EXISTS (SELECT 1 FROM conversation.usermessages_v2 WHERE message_id = OLD.message_id) THEN
UPDATE conversation.messages SET orphan = TRUE WHERE id = OLD.message_id::TEXT;
END IF;
RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS flagOrphanMessage_trigger ON conversation.usermessages;
DROP TRIGGER IF EXISTS flagOrphanMessage_trigger ON conversation.usermessages_v2;
CREATE TRIGGER flagOrphanMessage_trigger
    AFTER DELETE ON conversation.usermessages_v2
    FOR EACH ROW EXECUTE PROCEDURE conversation.flagOrphanMessage();

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

UPDATE conversation.usermessages_v2
    SET msg_state = p_new_state
    WHERE message_id = p_id::uuid;

END;
$$ LANGUAGE plpgsql;
