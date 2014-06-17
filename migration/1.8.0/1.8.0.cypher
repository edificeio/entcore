begin transaction
CREATE INDEX ON :ConversationMessage(from);
CREATE INDEX ON :ConversationMessage(to);
CREATE INDEX ON :ConversationMessage(cc);
commit
begin transaction
CREATE (:DeleteGroup:ProfileGroup:Group)
commit
