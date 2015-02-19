begin transaction

match (u:User)-[:IN]->(:ProfileGroup)-[:HAS_PROFILE]->(p:Profile) SET u.profiles = [] + p.name;
match (g:ManualGroup) SET g.groupDisplayName = g.name;

commit
