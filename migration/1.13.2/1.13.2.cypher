begin transaction
MATCH (c:Class)
WITH COLLECT(c.externalId) as cIds
MATCH (s:Structure)<-[:BELONGS]-(c:Class)
WHERE c.externalId <> s.externalId + '$' + c.name
SET c.externalId =
CASE WHEN NOT((s.externalId + '$' + c.name) IN cIds) THEN
s.externalId + '$' + c.name
ELSE
s.externalId + '$' + c.name + '-old'
END;

MATCH (s:Structure)<-[:BELONGS]-(c:Class) WHERE c.externalId = s.externalId + '$' + c.name + '-old'
SET c.name = 'Classe dupliquée ' + c.name;

MATCH (s:Structure)<-[:BELONGS]-(c:Class)
WHERE c.externalId <> s.externalId + '$' + c.name
SET c.externalId = s.externalId + '$' + c.name RETURN c.externalId;

MATCH (u:User)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c:Class)-[:BELONGS]->(s:Structure)
WHERE NOT(HAS(u.structures)) AND HEAD(u.profiles) IN ['Teacher', 'Personnel', 'Student']
SET u.structures = coalesce(u.structures, []) + s.externalId, u.classes = coalesce(u.classes, []) + c.externalId;

MATCH (u:User)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(s:Structure)
WHERE NOT(HAS(u.structures)) AND HEAD(u.profiles) IN ['Teacher', 'Personnel', 'Student']
SET u.structures = coalesce(u.structures, []) + s.externalId;

MATCH (u:User {source:'CSV'})-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c:Class)
REMOVE u.classes
SET u.classes = coalesce(u.classes, []) + c.externalId;

MATCH (u:User {profiles:['Student']})-[:RELATED]->(p:User)
WHERE NOT(HAS(u.relative))
SET u.relative = coalesce(u.relative, []) + (p.externalId + '$1$1$1$1$0');
commit
