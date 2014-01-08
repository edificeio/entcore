begin transaction
CREATE CONSTRAINT ON (conversation:Conversation) ASSERT conversation.userId IS UNIQUE;
CREATE CONSTRAINT ON (visible:Visible) ASSERT visible.id IS UNIQUE;
CREATE CONSTRAINT ON (conversationMessage:ConversationMessage) ASSERT conversationMessage.id IS UNIQUE;
commit

