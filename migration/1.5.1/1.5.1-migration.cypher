begin transaction
match (a:Application) where a.scope = '' set a.scope = [''];
MATCH n WHERE n:User OR n:School OR n:Class OR n:ProfileGroup REMOVE n.health, n.mood, n.picture, n.motto;
MATCH (u:User) WHERE HAS(u.schoolLevel) AND (NOT(HAS(u.level)) OR u.level = '') SET u.level = u.schoolLevel, u.schoolLevel = null;
MATCH (u:User) WHERE HAS(u.schoolSector) AND (NOT(HAS(u.sector)) OR u.sector = '') SET u.sector = u.schoolSector, u.schoolSector = null;
commit

