begin transaction

match (u:User)-[:IN]->(:ProfileGroup)-[:HAS_PROFILE]->(p:Profile) SET u.profiles = [] + p.name;

commit
