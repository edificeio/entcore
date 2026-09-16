-- duplicate index
DROP INDEX IF EXISTS conversation.idx_message_id;

-- Better coverage of various case on folder for super user
CREATE INDEX IF NOT EXISTS idx_um_user_folder_cover
    ON conversation.usermessages (user_id, folder_id)
    INCLUDE (message_id, unread, trashed);

CREATE INDEX IF NOT EXISTS idx_messages_id_state
    ON conversation.messages (id, state);