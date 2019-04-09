MATCH (u:User) WHERE HAS(u.email) SET u.emailSearchField=LOWER(u.email);
CREATE INDEX ON :User(emailSearchField);