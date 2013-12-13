begin transaction
CREATE CONSTRAINT ON (conversation:Conversation) ASSERT conversation.userId IS UNIQUE;
commit

