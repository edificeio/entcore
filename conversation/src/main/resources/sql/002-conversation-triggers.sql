CREATE OR REPLACE FUNCTION conversation.deleteOrphanMessage() RETURNS TRIGGER AS $$
    DECLARE
        messageCount INTEGER;
    BEGIN
        SELECT count(*) INTO messageCount FROM conversation.usermessages WHERE message_id = OLD.message_id;
        IF messageCount = 0 THEN
            DELETE FROM conversation.messages WHERE id = OLD.message_id;
        END IF;
        RETURN NULL;
    END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION conversation.deleteOrphanAttachment() RETURNS TRIGGER AS $$
    DECLARE
        attachmentCount INTEGER;
    BEGIN
        SELECT count(*) INTO attachmentCount FROM conversation.usermessagesattachments WHERE attachment_id = OLD.attachment_id;
        IF attachmentCount = 0 THEN
            DELETE FROM conversation.attachments WHERE id = OLD.attachment_id;
        END IF;
        RETURN NULL;
    END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER deleteMessage_trigger
AFTER DELETE ON conversation.usermessages
    FOR EACH ROW EXECUTE PROCEDURE conversation.deleteOrphanMessage();

CREATE TRIGGER deleteAttachment_trigger
AFTER DELETE ON conversation.usermessagesattachments
    FOR EACH ROW EXECUTE PROCEDURE conversation.deleteOrphanAttachment();
