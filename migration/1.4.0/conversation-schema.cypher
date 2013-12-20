begin transaction
CREATE CONSTRAINT ON (conversation:Conversation) ASSERT conversation.userId IS UNIQUE;
CREATE CONSTRAINT ON (visible:Visible) ASSERT visible.id IS UNIQUE;
MATCH v WHERE (v:User OR v:ProfileGroup) SET v:Visible;
commit

