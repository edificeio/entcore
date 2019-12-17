MATCH (user:User)-[:USERBOOK]->(ub) 
WHERE user.activationCode IS NULL AND ub.visibleInfos IS NULL
WITH user, ub
OPTIONAL MATCH ub-[r0:SHOW_EMAIL]->em 
OPTIONAL MATCH ub-[r1:SHOW_MAIL]->ma 
OPTIONAL MATCH ub-[r2:SHOW_PHONE]->ph 
OPTIONAL MATCH ub-[r3:SHOW_MOBILE]->mo 
OPTIONAL MATCH ub-[r4:SHOW_BIRTHDATE]->bir 
OPTIONAL MATCH ub-[r5:SHOW_HEALTH]->hea 
WITH user, ub, 
FILTER(x IN [type(r0),type(r1),type(r2),type(r3),type(r4),type(r5)] WHERE x IS NOT NULL) as visibleInfos 
SET ub.visibleInfos = visibleInfos
RETURN COUNT(ub);