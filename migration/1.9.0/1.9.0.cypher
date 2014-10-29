begin transaction
MATCH (g:ProfileGroup) where NOT(g:Group) SET g:Group;
MATCH (s:Structure)<-[:DEPENDS]-(g:ProfileGroup)-[:HAS_PROFILE]->(p:Profile {name:'Personnel'}),
s<-[:BELONGS]-(c:Class)<-[:DEPENDS]-(cpg:ProfileGroup)
WITH g, c, count(distinct cpg) as n
WHERE n < 4
CREATE c<-[:DEPENDS]-(pg:Group:ProfileGroup {name : c.name+'-Personnel'})-[:DEPENDS]->g
SET pg.id = id(pg)+'-'+timestamp();
MATCH (s:Structure)<-[:DEPENDS]-(g:ProfileGroup)-[:HAS_PROFILE]->(p:Profile {name:'Personnel'})
WITH split(g.name, '_') as type, g
WHERE HEAD(TAIL(type)) = 'DIRECTEUR'
SET g.name = HEAD(type) + '-Personnel';
commit
