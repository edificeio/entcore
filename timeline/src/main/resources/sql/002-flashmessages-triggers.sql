CREATE OR REPLACE FUNCTION flashmsg.incrementCounter() RETURNS TRIGGER AS $$
    BEGIN
        UPDATE flashmsg.messages SET "readCount" = "readCount" + 1 WHERE id = NEW.message_id;
        RETURN NULL;
    END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER incrementCounter_trigger
    AFTER INSERT ON flashmsg.messages_read
    FOR EACH ROW EXECUTE PROCEDURE flashmsg.incrementCounter();
