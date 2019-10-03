ALTER TABLE conversation.messages DROP CONSTRAINT messages_parent_id_fkey;
ALTER TABLE conversation.messages ADD CONSTRAINT messages_parent_id_fkey FOREIGN KEY (parent_id) REFERENCES conversation.messages(id) ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE conversation.folders DROP CONSTRAINT folders_parent_id_fkey;
ALTER TABLE conversation.folders ADD CONSTRAINT folders_parent_id_fkey FOREIGN KEY (parent_id) REFERENCES conversation.folders(id) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE conversation.usermessages DROP CONSTRAINT usermessages_folder_id_fkey;
ALTER TABLE conversation.usermessages ADD CONSTRAINT usermessages_folder_id_fkey FOREIGN KEY(folder_id) REFERENCES conversation.folders ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE conversation.usermessages DROP CONSTRAINT usermessages_message_id_fkey;
ALTER TABLE conversation.usermessages ADD CONSTRAINT usermessages_message_id_fkey FOREIGN KEY(message_id) REFERENCES conversation.messages ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE conversation.usermessagesattachments DROP CONSTRAINT usermessagesattachments_attachment_id_fkey;
ALTER TABLE conversation.usermessagesattachments ADD CONSTRAINT usermessagesattachments_attachment_id_fkey FOREIGN KEY (attachment_id) REFERENCES conversation.attachments(id) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE conversation.usermessagesattachments DROP CONSTRAINT usermessagesattachments_user_id_fkey;
ALTER TABLE conversation.usermessagesattachments ADD CONSTRAINT usermessagesattachments_user_id_fkey FOREIGN KEY (user_id, message_id) REFERENCES conversation.usermessages(user_id, message_id) ON DELETE CASCADE ON UPDATE CASCADE;