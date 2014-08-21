begin transaction
MATCH (p:Profile {name: 'Teacher'})
SET p.defaultQuota = 10737418240, p.maxQuota = 21474836480;
MATCH (p:Profile {name: 'Personnel'})
SET p.defaultQuota = 10737418240, p.maxQuota = 21474836480;
MATCH (p:Profile {name: 'Student'})
SET p.defaultQuota = 1073741824, p.maxQuota = 2147483648;
MATCH (p:Profile {name: 'Relative'})
SET p.defaultQuota = 1073741824, p.maxQuota = 2147483648;
MATCH (u:UserBook)<-[:USERBOOK]-(:User)-[:IN]->(:ProfileGroup)-[:HAS_PROFILE]->(p:Profile)
SET u.quota = p.defaultQuota, u.storage = 0, u.alertSize = false;
commit

