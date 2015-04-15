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
commit
