begin transaction
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
MATCH (ub:UserBook) WHERE ub.picture='no-avatar.jpg' SET ub.picture='no-avatar.svg';
commit

