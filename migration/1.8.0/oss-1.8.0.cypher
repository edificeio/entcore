begin transaction
CREATE CONSTRAINT ON (u:UserBook) ASSERT u.userid IS UNIQUE;
CREATE INDEX ON :ConversationMessage(from);
CREATE INDEX ON :ConversationMessage(to);
CREATE INDEX ON :ConversationMessage(cc);
commit
begin transaction
CREATE (:DeleteGroup:ProfileGroup:Group);
MATCH (u:User) SET u.manual = true;

MATCH (n:Role)
WHERE n.name = 'archive-all-default'
WITH count(*) AS exists
WHERE exists=0
CREATE (m:Role {id:'98cce3be-ec78-4222-b3c4-68a73266c4a1', name:'archive-all-default'})
WITH m
MATCH (n:Action)
WHERE n.name IN [
'org.entcore.archive.controllers.ArchiveController|view',
'org.entcore.archive.controllers.ArchiveController|export'
]
CREATE UNIQUE m-[:AUTHORIZE]->n;
match (u:Role {name :'archive-all-default'}), (g:DeleteGroup) create g-[:AUTHORIZED]->u;
commit
