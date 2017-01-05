begin transaction
CREATE CONSTRAINT ON (sub:Subject) ASSERT sub.id IS UNIQUE;
CREATE CONSTRAINT ON (sub:Subject) ASSERT sub.externalId IS UNIQUE;
CREATE CONSTRAINT ON (fg:FunctionalGroup) ASSERT fg.externalId IS UNIQUE;
CREATE CONSTRAINT ON (fg:FunctionalGroup) ASSERT fg.id IS UNIQUE;
commit

