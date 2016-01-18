CREATE INDEX ON :User(displayNameSearchField);
CREATE CONSTRAINT ON (ma:MessageAttachment) ASSERT ma.id IS UNIQUE;
