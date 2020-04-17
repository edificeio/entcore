ALTER TABLE conversation.usermessages ADD COLUMN is_sender BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE conversation.usermessages ADD COLUMN is_receiver BOOLEAN NOT NULL DEFAULT FALSE;