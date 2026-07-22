-- Marquage des entités orphelines via trigger, au lieu de les détecter par anti-jointure.
-- Le trigger marque (léger, couvre tous les points de suppression) ; le cron DeleteOrphan
-- balaye les lignes flaggées et gère la suppression des fichiers dans le storage.

-- 1. Colonne orphan sur les 3 tables "contenu"
ALTER TABLE conversation.messages ADD COLUMN IF NOT EXISTS orphan BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE conversation.attachments ADD COLUMN IF NOT EXISTS orphan BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE conversation.threads ADD COLUMN IF NOT EXISTS orphan BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. Index partiels : le cron ne scanne que les orphelins
CREATE INDEX IF NOT EXISTS idx_messages_orphan ON conversation.messages(id) WHERE orphan;
CREATE INDEX IF NOT EXISTS idx_attachments_orphan ON conversation.attachments(id) WHERE orphan;
CREATE INDEX IF NOT EXISTS idx_threads_orphan ON conversation.threads(id) WHERE orphan;

-- 3. Fonctions trigger : marquage léger (UPDATE bool)
CREATE OR REPLACE FUNCTION conversation.flagOrphanMessage() RETURNS TRIGGER AS $$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM conversation.usermessages WHERE message_id = OLD.message_id) THEN
            UPDATE conversation.messages SET orphan = TRUE WHERE id = OLD.message_id;
        END IF;
        RETURN NULL;
    END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION conversation.flagOrphanAttachment() RETURNS TRIGGER AS $$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM conversation.usermessagesattachments WHERE attachment_id = OLD.attachment_id) THEN
            UPDATE conversation.attachments SET orphan = TRUE WHERE id = OLD.attachment_id;
        END IF;
        RETURN NULL;
    END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION conversation.flagOrphanThread() RETURNS TRIGGER AS $$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM conversation.userthreads WHERE thread_id = OLD.thread_id) THEN
            UPDATE conversation.threads SET orphan = TRUE WHERE id = OLD.thread_id;
        END IF;
        RETURN NULL;
    END;
$$ LANGUAGE plpgsql;

-- 4. Triggers AFTER DELETE sur les tables de liaison.
--    usermessagesattachments se déclenche aussi via le CASCADE depuis usermessages.
--    PostgreSQL n'a pas de CREATE TRIGGER IF NOT EXISTS : le DROP IF EXISTS préalable
--    est la forme idempotente portable (CREATE OR REPLACE TRIGGER exigerait PG >= 14).
DROP TRIGGER IF EXISTS flagOrphanMessage_trigger ON conversation.usermessages;
CREATE TRIGGER flagOrphanMessage_trigger
AFTER DELETE ON conversation.usermessages
    FOR EACH ROW EXECUTE PROCEDURE conversation.flagOrphanMessage();

DROP TRIGGER IF EXISTS flagOrphanAttachment_trigger ON conversation.usermessagesattachments;
CREATE TRIGGER flagOrphanAttachment_trigger
AFTER DELETE ON conversation.usermessagesattachments
    FOR EACH ROW EXECUTE PROCEDURE conversation.flagOrphanAttachment();

DROP TRIGGER IF EXISTS flagOrphanThread_trigger ON conversation.userthreads;
CREATE TRIGGER flagOrphanThread_trigger
AFTER DELETE ON conversation.userthreads
    FOR EACH ROW EXECUTE PROCEDURE conversation.flagOrphanThread();