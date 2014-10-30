begin transaction
MATCH (n:User)-[r1:IN]->(fg:FunctionGroup)-[r:HAS_FUNCTION]->(f:Function { externalId : 'SUPER_ADMIN'})
CREATE UNIQUE n-[:HAS_FUNCTION]->f
DELETE r1, r, fg;
commit
