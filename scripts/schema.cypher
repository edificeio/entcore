begin transaction
CREATE INDEX ON :Action(type);
CREATE CONSTRAINT ON (action:Action) ASSERT action.name IS UNIQUE;
CREATE CONSTRAINT ON (role:Role) ASSERT role.id IS UNIQUE;
CREATE CONSTRAINT ON (role:Role) ASSERT role.name IS UNIQUE;
CREATE CONSTRAINT ON (application:Application) ASSERT application.id IS UNIQUE;
CREATE CONSTRAINT ON (application:Application) ASSERT application.name IS UNIQUE;
CREATE CONSTRAINT ON (conversation:Conversation) ASSERT conversation.userId IS UNIQUE;
CREATE CONSTRAINT ON (visible:Visible) ASSERT visible.id IS UNIQUE;
CREATE CONSTRAINT ON (conversationMessage:ConversationMessage) ASSERT conversationMessage.id IS UNIQUE;
CREATE CONSTRAINT ON (u:UserBook) ASSERT u.userid IS UNIQUE;
CREATE CONSTRAINT ON (ma:MessageAttachment) ASSERT ma.id IS UNIQUE;
CREATE INDEX ON :ConversationMessage(from);
CREATE INDEX ON :ConversationMessage(to);
CREATE INDEX ON :ConversationMessage(cc);
CREATE CONSTRAINT ON (sub:Subject) ASSERT sub.id IS UNIQUE;
CREATE CONSTRAINT ON (sub:Subject) ASSERT sub.externalId IS UNIQUE;
CREATE CONSTRAINT ON (fg:FunctionalGroup) ASSERT fg.externalId IS UNIQUE;
CREATE CONSTRAINT ON (fg:FunctionalGroup) ASSERT fg.id IS UNIQUE;
CREATE INDEX ON :User(firstName);
CREATE INDEX ON :User(lastName);
CREATE INDEX ON :User(birthDate);
CREATE INDEX ON :User(source);
CREATE CONSTRAINT ON (u:User) ASSERT u.IDPN IS UNIQUE;
CREATE INDEX ON :User(attachmentId);
commit
begin transaction
CREATE (:DeleteGroup:ProfileGroup:Group);
commit
