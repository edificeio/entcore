DO $mig$
BEGIN
    IF to_regclass('conversation.usermessages_v2') IS NOT NULL THEN
      RAISE NOTICE 'usermessages_v2 deja presente : migration ignoree';
      RETURN;
    END IF;

    PERFORM set_config('work_mem', '256MB', true);

    CREATE TABLE conversation.usermessages_v2 (  user_id UUID not null,
                                             message_id UUID not null,
                                             folder_id VARCHAR(36),
                                             trashed BOOLEAN NOT NULL DEFAULT FALSE,
                                             unread BOOLEAN NOT NULL DEFAULT TRUE,
                                             total_quota BIGINT NOT NULL DEFAULT 0,
                                             is_recipient BOOLEAN,
                                             is_sender    BOOLEAN,
                                             msg_state    VARCHAR(10),
                                             msg_date     BIGINT );

    INSERT INTO conversation.usermessages_v2  (user_id, message_id, folder_id, trashed, unread, total_quota, is_recipient, is_sender, msg_state, msg_date)
    SELECT um.user_id::uuid, m.id::uuid, um.folder_id, um.trashed, um.unread, um.total_quota,
           COALESCE(m.from <> um.user_id
                        OR m.to @> jsonb_build_array(um.user_id)
                        OR m.cc @> jsonb_build_array(um.user_id), false) AS is_recipient,
           (m.from = um.user_id) AS is_sender,
           m.state, m.date
    FROM conversation.messages m
         JOIN conversation.usermessages um ON um.message_id = m.id
    WHERE um.user_id    ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
      AND um.message_id ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$';

    ALTER TABLE conversation.usermessages_v2 ADD CONSTRAINT usermessages_v2_pkey PRIMARY KEY (user_id, message_id);

    ALTER TABLE conversation.usermessages_v2 ADD CONSTRAINT usermessages_v2_folder_id_fkey
        FOREIGN KEY (folder_id) REFERENCES conversation.folders ON DELETE CASCADE;

    ALTER TABLE conversation.usermessagesattachments DROP CONSTRAINT IF EXISTS usermessagesattachments_user_id_fkey;
    ALTER TABLE conversation.usermessages DROP CONSTRAINT IF EXISTS usermessages_folder_id_fkey;
    ALTER TABLE conversation.usermessages DROP CONSTRAINT IF EXISTS usermessages_message_id_fkey;


    CREATE INDEX idx_um_v2_user_folders ON conversation.usermessages_v2 (user_id, folder_id, msg_date DESC)
        INCLUDE (message_id, msg_state, unread, trashed)
        WHERE folder_id IS NOT NULL;

    CREATE INDEX idx_um_v2_message_id ON conversation.usermessages_v2 (message_id);

    -- Boîte de réception
    CREATE INDEX idx_usermes_inbox ON conversation.usermessages_v2 (user_id, msg_date DESC)
        INCLUDE (message_id, msg_state, unread)
        WHERE is_recipient AND NOT trashed AND folder_id IS NULL;

    -- Messages envoyés et brouillons (distingués par msg_state)
    CREATE INDEX idx_um_v2_sender ON conversation.usermessages_v2 (user_id, msg_date DESC)
        INCLUDE (message_id, msg_state, unread)
        WHERE is_sender AND NOT trashed AND folder_id IS NULL;

    -- Corbeille : trashed = true, quel que soit le dossier
    CREATE INDEX idx_um_v2_trash ON conversation.usermessages_v2 (user_id, msg_date DESC)
        INCLUDE (message_id, msg_state, unread)
        WHERE trashed;

    ANALYZE conversation.usermessages_v2;
    END
$mig$;

CREATE OR REPLACE FUNCTION conversation.deleteUserMessages() RETURNS TRIGGER AS $f1$
BEGIN
    DELETE FROM conversation.usermessages_v2 WHERE message_id = OLD.id::uuid;
    RETURN NULL;
END
$f1$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS deleteUserMessage_trigger ON conversation.messages;
CREATE TRIGGER deleteUserMessage_trigger AFTER DELETE ON conversation.messages
    FOR EACH ROW EXECUTE PROCEDURE conversation.deleteUserMessages();

CREATE OR REPLACE FUNCTION conversation.deleteUserAttachments() RETURNS TRIGGER AS $f2$
BEGIN
    DELETE FROM conversation.usermessagesattachments
    WHERE message_id = OLD.message_id::text AND user_id = OLD.user_id::text;
    RETURN NULL;
END
$f2$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS deleteUserAttachements_trigger ON conversation.usermessages;
DROP TRIGGER IF EXISTS deleteUserAttachments_trigger ON conversation.usermessages_v2;
CREATE TRIGGER deleteUserAttachments_trigger AFTER DELETE ON conversation.usermessages_v2
    FOR EACH ROW EXECUTE PROCEDURE conversation.deleteUserAttachments();
