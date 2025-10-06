CREATE INDEX IF NOT EXISTS idx_usermessagesattachments_attachment_id ON conversation.usermessagesattachments(attachment_id);
CREATE INDEX IF NOT EXISTS idx_usermessages_message_id ON conversation.usermessages(message_id);
CREATE INDEX IF NOT EXISTS idx_userthreads_thread_id ON conversation.userthreads(thread_id);
