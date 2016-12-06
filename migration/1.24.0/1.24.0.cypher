begin transaction

//////////////////// initialisation for PROFILEGORUP /////////////////////////
// Teacher
MATCH  (p:Profile {name: 'Teacher'})<-[HAS_PROFILE]-(pf:ProfileGroup)
WHERE not has(pf.maxquota) or pf.maxquota = 0
SET pf.maxquota = 10737418240 return p;

MATCH  (p:Profile {name: 'Teacher'})<-[HAS_PROFILE]-(pf:ProfileGroup)
WHERE not has(pf.quota) or pf.quota = 0
SET pf.quota = 1073741824 return p;

// Personnel
MATCH  (p:Profile {name: 'Personnel'})<-[HAS_PROFILE]-(pf:ProfileGroup)
WHERE not has(pf.maxquota) or pf.maxquota = 0
SET pf.maxquota = 10737418240 return p;

MATCH  (p:Profile {name: 'Personnel'})<-[HAS_PROFILE]-(pf:ProfileGroup)
WHERE not has(pf.quota) or pf.quota = 0
SET pf.quota = 1073741824 return p;

// Student
MATCH  (p:Profile {name: 'Student'})<-[HAS_PROFILE]-(pf:ProfileGroup)
WHERE not has(pf.maxquota) or pf.maxquota = 0
SET pf.maxquota = 10737418240 return p;

MATCH  (p:Profile {name: 'Student'})<-[HAS_PROFILE]-(pf:ProfileGroup)
WHERE not has(pf.quota) or pf.quota = 0
SET pf.quota = 209715200 return p;

// Relative
MATCH  (p:Profile {name: 'Relative'})<-[HAS_PROFILE]-(pf:ProfileGroup)
WHERE not has(pf.maxquota) or pf.maxquota = 0
SET pf.maxquota = 10737418240 return p;

MATCH  (p:Profile {name: 'Relative'})<-[HAS_PROFILE]-(pf:ProfileGroup)
WHERE not has(pf.quota) or pf.quota = 0
SET pf.quota = 104857600 return p;

// Guest
MATCH  (p:Profile {name: 'Guest'})<-[HAS_PROFILE]-(pf:ProfileGroup)
WHERE not has(pf.maxquota) or pf.maxquota = 0
SET pf.maxquota = 10737418240 return p;

MATCH  (p:Profile {name: 'Guest'})<-[HAS_PROFILE]-(pf:ProfileGroup)
WHERE not has(pf.quota) or pf.quota = 0
SET pf.quota = 104857600 return p;

//////////////////// initialisation for USERBOOK /////////////////////////
MATCH (ub:UserBook)<-[USERBOOK]-(u:User)-[IN]->(pf:ProfileGroup)-[HAS_PROFILE]->(p:Profile)
WHERE (not has(ub.quota) or ub.quota = 0) and (p.name = 'Teacher') SET p.quota = 1073741824 return p;

MATCH (ub:UserBook)<-[USERBOOK]-(u:User)-[IN]->(pf:ProfileGroup)-[HAS_PROFILE]->(p:Profile)
WHERE (not has(ub.quota) or ub.quota = 0) and (p.name = 'Personnel') SET p.quota = 10737418240 return p;

MATCH (ub:UserBook)<-[USERBOOK]-(u:User)-[IN]->(pf:ProfileGroup)-[HAS_PROFILE]->(p:Profile)
WHERE (not has(ub.quota) or ub.quota = 0) and (p.name = 'Student') SET p.quota = 209715200 return p;

MATCH (ub:UserBook)<-[USERBOOK]-(u:User)-[IN]->(pf:ProfileGroup)-[HAS_PROFILE]->(p:Profile)
WHERE (not has(ub.quota) or ub.quota = 0) and (p.name = 'Relative') SET p.quota = 104857600 return p;

MATCH (ub:UserBook)<-[USERBOOK]-(u:User)-[IN]->(pf:ProfileGroup)-[HAS_PROFILE]->(p:Profile)
WHERE (not has(ub.quota) or ub.quota = 0) and (p.name = 'Guest') SET p.quota = 104857600 return p;

//////////////////// initialisation for STRUCTURE /////////////////////////
MATCH (s:Structure) where s.quota < 536870912000 set s.quota = 536870912000 return s;

commit
