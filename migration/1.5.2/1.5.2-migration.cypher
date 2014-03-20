begin transaction
MATCH (m:ConversationMessage), (c:Conversation)-[:HAS_CONVERSATION_FOLDER]->(f:ConversationFolder), c-[:HAS_CONVERSATION_FOLDER]->(t:ConversationFolder {name: 'TRASH'})
WHERE (c.userId = m.from OR c.userId IN m.to) AND NOT(m<-[:HAS_CONVERSATION_MESSAGE]-f)
CREATE UNIQUE t-[:HAD_CONVERSATION_MESSAGE]->m;
commit

