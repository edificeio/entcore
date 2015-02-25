begin transaction
CREATE (p:Profile {id: 'd2bb569f-bb7f-413f-a2e0-2ce5e6058251', externalId: 'PROFILE_GUEST', name: 'Guest'})<-[:HAS_PROFILE]-(g:Group:ProfileGroup:DefaultProfileGroup);
MATCH (p:Profile {name : 'Guest'}), (s:Structure)
CREATE p<-[:HAS_PROFILE]-(g:Group:ProfileGroup {name : s.name+'-'+p.name})-[:DEPENDS]->s
SET g.id = id(g)+'-'+timestamp();
MATCH (s:Structure)<-[:BELONGS]-(c:Class), s<-[:DEPENDS]-(g:ProfileGroup)-[:HAS_PROFILE]->(p:Profile {name : 'Guest'})
CREATE c<-[:DEPENDS]-(pg:Group:ProfileGroup {name : c.name+'-'+p.name})-[:DEPENDS]->g
SET pg.id = id(pg)+'-'+timestamp();
MATCH (u:User {manual:true}) SET u.source = 'MANUAL', u.manual = null;
MATCH (u:User) WHERE NOT(HAS(u.source)) AND HAS(u.joinKey) SET u.source = 'AAF';
MATCH (u:User) WHERE NOT(HAS(u.source)) SET u.source = 'BE1D';
commit
