MATCH (u:User)
WHERE EXISTS(u.removedFromStructures) AND HEAD(u.profiles) = "Teacher"
OPTIONAL MATCH (u)-[r:IN]->(:ProfileGroup)
WITH u, count(r) AS nbPgs
MATCH (:Profile {externalId:"PROFILE_TEACHER"})--(dpg:DefaultProfileGroup)
WHERE nbPgs = 0
MERGE (u)-[:IN]->(dpg);

MATCH (u:User)
WHERE EXISTS(u.removedFromStructures) AND HEAD(u.profiles) = "Personnel"
OPTIONAL MATCH (u)-[r:IN]->(:ProfileGroup)
WITH u, count(r) AS nbPgs
MATCH (:Profile {externalId:"PROFILE_PERSONNEL"})--(dpg:DefaultProfileGroup)
WHERE nbPgs = 0
MERGE (u)-[:IN]->(dpg);

MATCH (u:User)
WHERE EXISTS(u.removedFromStructures) AND HEAD(u.profiles) = "Student"
OPTIONAL MATCH (u)-[r:IN]->(:ProfileGroup)
WITH u, count(r) AS nbPgs
MATCH (:Profile {externalId:"PROFILE_STUDENT"})--(dpg:DefaultProfileGroup)
WHERE nbPgs = 0
MERGE (u)-[:IN]->(dpg);

MATCH (u:User)
WHERE EXISTS(u.removedFromStructures) AND HEAD(u.profiles) = "Relative"
OPTIONAL MATCH (u)-[r:IN]->(:ProfileGroup)
WITH u, count(r) AS nbPgs
MATCH (:Profile {externalId:"PROFILE_RELATIVE"})--(dpg:DefaultProfileGroup)
WHERE nbPgs = 0
MERGE (u)-[:IN]->(dpg);